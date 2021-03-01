// $Id: Sentence.java,v 1.35 1999/04/29 10:01:36 jim Exp $
// Encapsulates	a Natural Language Sentence
// Alexander Veenendaal, 25	August 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.Vector;
import com.ogalala.util.Debug;

public class Sentence
    implements java.io.Serializable
{
    public static final int serialVersionUID = 1;
    
	//--------------------------- Properties ---------------------------//

	/** a vector of Word objects
	*/
	private Vector words = new Vector(); 
	
	/** the sentences Verb
	*/
	private Verb verb;
	
	/** the sentences internal verb signifier
	*/
	private String verbPropertyID;
	
	/** the native form of the verb as typed by the user
	*/
	private String verbString;
	
	/** Stores all the prepositions within this sentence
	*/
	private Vector prepositions;
	
	/**
	*/
	private Vector verbPhrase = new Vector();
	
	/**
	*/
	NounPhrase pronounCandidate;
	
	/**
	*/
	Vector firstNounPhrase = new Vector();
	Vector secondNounPhrase = new Vector();
	Vector thirdNounPhrase = new Vector();
	
	/** 
	* hopefully, this is what the Sentence will end up as.
	* note that the event gets initialised from the parser.
	*/
	private ParserEvent parserEvent; 
	
	/**
	* used during the lex analysis stage, if this is true,
	* we check for plural words
	*/
	boolean	pluralCheckingFlag;
	
	
	//--------------------------- Constructor ---------------------------//
	public Sentence()
	{
	}
  
  
  
  
	//----------------------------- Methods -----------------------------//
	
	/**
	* Generates this objects ParserEvent using the single argument
	*
	* @param parser			An instance of the parser. Needed to pass into the parserEvent
	* @param verb			This sentences verb looked up as an object, which contains templates within
	*/
	public boolean generateSingleArgParserEvent(Parser parser)
	{
		VerbTemplate match = null;
		
		//in this case, the nounphrase is everything apart from the VP
		isolateNounPhrases();
		
		//start off assuming that the VerbTemplate is of type empty
		match = verb.matchTemplate(VerbTemplate.EMPTY);
		
		// try for a communicative verb
		//Communicative verbs essentially use the same template, but the structure of the
		// current and arguments is hardwired in.
		if( verb.isCommunicative() )
			match = verb.matchTemplate();
			
		//If the firstNounPhrase has stuff in it ...
		else if( (firstNounPhrase.size() != 0) )
		{
			//...and the first element is a DIRECTION
			if( ((Word)firstNounPhrase.elementAt(0)).getType() == Word.WT_DIRECTION )
				match = verb.matchTemplate(VerbTemplate.DIRECTION);
			
			// ... or a numeric ...
			// there are two cases here. The first is if the numeric is the only word in the
			// NP. The second is if the numeric is used to qualify a word that appears after it.
			// In the first case, we try for a VerbTemplate match, in the second we ignore and go
			// for a current match.
			else if( ((Word)firstNounPhrase.elementAt( firstNounPhrase.size()-1 )).getType() == Word.WT_NUMERIC )
				match = verb.matchTemplate(VerbTemplate.NUMERIC);
				
			//added to allow instance such as "look current" and "look" and "look direction" to coexist
			else
				match = verb.matchTemplate(VerbTemplate.CURRENT);
		}
		
		//find a template which has no prepositions
		if(match!=null)
		{
			verbPropertyID = match.getPropertyID();
			
			// decide whether this single argument is the current or not
			// Is the first argument in the verb template a CURRENT ?
			if( match.checkForArgType(VerbTemplate.CURRENT,0)  )
			{
				//compile the first argument into a nounphrase
				//NounPhrase theCurrent = new NounPhrase( parser.getWorld(), firstNounPhrase, match.getArgType(0) );
				NounPhrase theCurrent = new NounPhrase( parser.getWorld(), firstNounPhrase, match.getArgType(0), match.getModProperties(0) );
													
				parserEvent = newParserEvent( parser, theCurrent, null, null );
				
				if( !theCurrent.getPronounStatus() )
					pronounCandidate = theCurrent;
				
				return true;
			}
			
			// Is the first argument in the verb template a THING ?
			else if ( match.checkForArgType(VerbTemplate.THING,0) )
			{
				NounPhrase theThing = new NounPhrase( parser.getWorld(), firstNounPhrase, match.getArgType(0), match.getModProperties(0));
				
				//the current becomes the words root atom
				parserEvent = newParserEvent( parser, parser.getWorld().getRoot(), theThing, null );
				
				//if the pronoun has not yet been set, set it now
				if( !theThing.getPronounStatus() )
					pronounCandidate = theThing;
				
				return true;
			}
			
			// Is the first argument in the verb template a DIRECTION ?
			else if ( match.checkForArgType(VerbTemplate.DIRECTION,0) )
			{
				parserEvent = newParserEvent( parser, NounPhrase.compileToDirection(firstNounPhrase), null, null );
				return true;
			}
			// Is the first argument in the verb template a STRING ?
			else if ( match.checkForArgType(VerbTemplate.STRING,0) )
			{
				//the current is the root atom, and the first argument is a string
				parserEvent = newParserEvent( parser, parser.getWorld().getRoot(), NounPhrase.compileToString(firstNounPhrase), null );
				return true;
			}
			else if( match.checkForArgType(VerbTemplate.COMMUNICATIVE,0) )
			{
				parserEvent = newParserEvent( parser, parser.getWorld().getRoot(), NounPhrase.compileToString(firstNounPhrase), null );									
				return true;
			}
			else if( match.checkForArgType(VerbTemplate.NUMERIC,0) )
			{
				//### the current in this case is the actors container
				parserEvent = newParserEvent( parser, parser.getActor().getContainer(), NounPhrase.compileToNumeric(firstNounPhrase), null );										
				return true;
			}
			// Otherwise, just make the ParserEvent have no arguments and send the Worlds root as the current. 
			else
			{
				//send it as a zero argument event
				parserEvent = newParserEvent( parser, parser.getWorld().getRoot(), null, null	);							
				return true;
			}
		}
		return false;
	}
	/**
	* Attempts to generate a ParserEvent using a single Preposition and two arguments
	*/
	public boolean generateDoubleArgParserEvent(Parser parser)
	{
		VerbTemplate match = null;
		
		//we will have two nounphrases to isolate around the first preposition in the sentence
		// For example
		// 		'put book in box in cupboard'
		// will become
		//		'book' and 'box in cupboard'
		isolateNounPhrases(getPrepositionValue(0));
		
		//start by looking for a match around the first preposition in the sentence
		match = verb.matchTemplate(getPrepositionValue(0));
		
		//if that didn't work, try for its second 
		if(match == null)
			match = verb.matchTemplate(getPrepositionValue(1));
		
		//Communicative verbs essentially use the same template, but the structure of the
		// current and arguments is hardwired in.
		if( match == null && verb.isCommunicative() )
			match = verb.matchTemplate();
			
		//otherwise, go for the default
		if(match == null)
		{
			//clear out the nounphrases, so that the next attempt isnot polluted.
			firstNounPhrase.removeAllElements();
			secondNounPhrase.removeAllElements();
			thirdNounPhrase.removeAllElements();
			//now go for a single arg parserevent
			return generateSingleArgParserEvent(parser);
		}
		if(match != null)
		{
			verbPropertyID = match.getPropertyID();
			
			//if the match shows communicative, then we know what to do
			if( match.checkForArgType(VerbTemplate.COMMUNICATIVE,0) )
			{
				parserEvent = newParserEvent( parser, new NounPhrase( parser.getWorld(), firstNounPhrase, match.getArgType(0) ),
													NounPhrase.compileToString( secondNounPhrase ),
													null 		);							
				return true;
			}
			
			// Is the first argument a CURRENT ?
			else if( match.checkForArgType(VerbTemplate.CURRENT,0) )
			{
				//compile the current into a nounphrase
				NounPhrase theCurrent = new NounPhrase( parser.getWorld(),firstNounPhrase,match.getArgType(0), match.getModProperties(0));

				//if the pronoun has not yet been set, set it to be the current we just defined
				if( !theCurrent.getPronounStatus() )
						pronounCandidate = theCurrent;
				
				// Is the second argument a THING ?
				if( match.checkForArgType(VerbTemplate.THING,1) ) 
				{
					parserEvent = newParserEvent( parser, theCurrent, new NounPhrase( parser.getWorld(),secondNounPhrase, match.getArgType(1)), null );									
					return true;
				}
				//Otherwise, just send the second argument as it is
				else 
				{
					parserEvent = newParserEvent( parser, theCurrent, secondNounPhrase, null );									
					return true;
				}
			}
			// Is the second argument a CURRENT ?
			else if( match.checkForArgType(VerbTemplate.CURRENT,1) )
			{
				//wrap up the current into a nounphrase
				NounPhrase theCurrent = new NounPhrase( parser.getWorld(), secondNounPhrase, match.getArgType(1), match.getModProperties(1) );
				
				//if the pronoun has not yet been set, do so now
				if( !theCurrent.getPronounStatus() )
						pronounCandidate = theCurrent;
				
				// Is the first argument a THING ?
				if( match.checkForArgType(VerbTemplate.THING,0) )
				{
					parserEvent = newParserEvent( parser, theCurrent, 
														new NounPhrase( parser.getWorld(),firstNounPhrase, match.getArgType(0), match.getModProperties(0) ), 
														null	);
					return true;
				}
				
				// Is the first argument a STRING ?
				else if ( match.checkForArgType(VerbTemplate.STRING,0) )
				{
					parserEvent = newParserEvent( parser, theCurrent, NounPhrase.compileToString(firstNounPhrase), null	);
					return true;
				}									
				
				// Otherwise, just send the argument as it is
				else 
				{	
					parserEvent = newParserEvent( parser, theCurrent, firstNounPhrase, null	);									
					return true;
				}
			}
			
			// Is the second argument a THING ?
			else if( match.checkForArgType(VerbTemplate.THING,1) )
			{
				parserEvent = newParserEvent( parser, parser.getWorld().getRoot(),
											new NounPhrase( parser.getWorld(), secondNounPhrase, match.getArgType(1), match.getModProperties(1) ),
											null	);
				return true;
			}
			
			// tricky handling bit here:
			// in the case of 'look at box', there is no current, and a thing falls as the first argument type
			// Is the first argument a THING ? 
			else if( match.checkForArgType( VerbTemplate.THING,0 ) )
			{
				// as long as the second argument type is not meant to be empty, and there is something in the 2nd NounPhrase...
				if( !match.checkForArgType( VerbTemplate.EMPTY,1 ) || secondNounPhrase.size() != 0  )
				{
					parserEvent = newParserEvent( parser, parser.getWorld().getRoot(),
													new NounPhrase( parser.getWorld(), firstNounPhrase, match.getArgType(0), match.getModProperties(0)),
														secondNounPhrase	);
				}
				else
				{
					parserEvent = newParserEvent( parser, parser.getWorld().getRoot(), 
													new NounPhrase( parser.getWorld(), firstNounPhrase, match.getArgType(0), match.getModProperties(0)), 
													null );
					
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	*
	*/
	protected ParserEvent newParserEvent(Parser parser, Object arg1, Object arg2, Object arg3)
	{
		return new ParserEvent( parser, parser.getWorld(), parser.getActor(), verbPropertyID, verbString, arg1, arg2, arg3 );
	}
	/**
	*
	*
	*/
	public boolean generateTripleArgParserEvent(Parser parser)
	{
		VerbTemplate match = null;
		
		//we will have three nounphrases to isolate
		isolateNounPhrases(getPrepositionValue(0),getPrepositionValue(1));
		
		//start by looking for a match around the first two prepositions in the sentence
		match = verb.matchTemplate(getPrepositionValue(0), getPrepositionValue(1));
		
		//If thats no good, try for a double Argument ParserEvent
		if( match == null )
		{
			//clear out the nounphrases, so the next attempt isn't polluted
			firstNounPhrase.removeAllElements();
			secondNounPhrase.removeAllElements();
			thirdNounPhrase.removeAllElements();
			
			return generateDoubleArgParserEvent(parser);
		}
		if(match!=null)
		{
			verbPropertyID = match.getPropertyID();
			
			// Is the first argument a CURRENT ?
			if( match.checkForArgType(VerbTemplate.CURRENT,0) )
			{
				// Are the second and third arguments are things ?
				if( match.checkForArgType(VerbTemplate.THING,1) && match.checkForArgType(VerbTemplate.THING,2) ) 
				{
					parserEvent = new ParserEvent( parser, parser.getWorld(), parser.getActor(), verbPropertyID, verbString, 
														new NounPhrase( parser.getWorld(),firstNounPhrase,match.getArgType(0), match.getModProperties(0)), 
														new NounPhrase( parser.getWorld(),secondNounPhrase,match.getArgType(1), match.getModProperties(1)), 
														new NounPhrase( parser.getWorld(),thirdNounPhrase,match.getArgType(2), match.getModProperties(2))	);
					return true;
				}
				// Is the second argument a thing ?
				else if( match.checkForArgType(VerbTemplate.THING,1) ) 
				{
					parserEvent = new ParserEvent( parser, parser.getWorld(), parser.getActor(), verbPropertyID, verbString, 
														new NounPhrase( parser.getWorld(),firstNounPhrase,match.getArgType(0), match.getModProperties(0)), 
														new NounPhrase( parser.getWorld(),secondNounPhrase,match.getArgType(1), match.getModProperties(1)),
														thirdNounPhrase );
					return true;
				}
				// Is the third argument a thing
				else if( match.checkForArgType(VerbTemplate.THING,2) ) 
				{
					parserEvent = new ParserEvent( parser, parser.getWorld(), parser.getActor(), verbPropertyID, verbString,
														new NounPhrase( parser.getWorld(),firstNounPhrase,match.getArgType(0), match.getModProperties(0)),
														secondNounPhrase,
														new NounPhrase( parser.getWorld(),thirdNounPhrase,match.getArgType(2), match.getModProperties(2)) );
					return true;
				}
			}
			// Is the second argument a current ?
			else if( match.checkForArgType(VerbTemplate.CURRENT,1) )
			{
				// Are the first and third arguments THINGS ?
				if( match.checkForArgType(VerbTemplate.THING,0) && match.checkForArgType(VerbTemplate.THING,2) ) 
				{
					parserEvent = new ParserEvent( parser, parser.getWorld(), parser.getActor(), verbPropertyID, verbString, 
														new NounPhrase( parser.getWorld(),secondNounPhrase,match.getArgType(1), match.getModProperties(1)), 
														new NounPhrase( parser.getWorld(),firstNounPhrase,match.getArgType(0), match.getModProperties(0)),
														new NounPhrase( parser.getWorld(),thirdNounPhrase,match.getArgType(2), match.getModProperties(2))	);
					return true;
				}
				// Is the first argument a THING ?
				else if( match.checkForArgType(VerbTemplate.THING,0) ) 
				{
					parserEvent = new ParserEvent( parser, parser.getWorld(), parser.getActor(), verbPropertyID, verbString, 
														new NounPhrase( parser.getWorld(),secondNounPhrase,match.getArgType(1), match.getModProperties(1)), 
														new NounPhrase( parser.getWorld(),firstNounPhrase,match.getArgType(0), match.getModProperties(0)),
														thirdNounPhrase	);
					return true;
				}
				// Is the third argument a THING ?
				else if( match.checkForArgType(VerbTemplate.THING,2) )
				{
					parserEvent = new ParserEvent( parser, parser.getWorld(), parser.getActor(), verbPropertyID, verbString, 
														new NounPhrase( parser.getWorld(),secondNounPhrase,match.getArgType(1), match.getModProperties(1)),
														firstNounPhrase, 
														new NounPhrase( parser.getWorld(),thirdNounPhrase,match.getArgType(2), match.getModProperties(2))	);
					return true;
				}
			}
		}
		// nothing has been resolved, everythings failed, hang head in shame...
		return false;
	}
	
	
	/**
	* fills the first noun phrase slot with whatever it can find
	*/
	public void isolateNounPhrases()
	{
		for(int i=0;i<words.size();i++)
		{
			if( getWordType(i) == Word.WT_NOUN || 
				getWordType(i) == Word.WT_ADJECTIVE ||
				getWordType(i) == Word.WT_DIRECTION ||
				getWordType(i) == Word.WT_PRONOUN ||
				getWordType(i) == Word.WT_NUMERIC ||
				getWordType(i) == Word.WT_STRING ||
				getWordType(i) == Word.WT_NUMERIC ||
				getWordType(i) == Word.WT_NEGATION ||
				getWordType(i) == Word.WT_PREPOSITION ||
				getWordType(i) == Word.WT_SUPERLATIVE )
			{
				firstNounPhrase.addElement( words.elementAt(i) );
			}
		}
	}
	
	public void isolateNounPhrases(String prep)
	{
		//initially we add the NP to the first NP
		Vector bucket = firstNounPhrase;
		int wordtype;
		
		for(int i=0;i<words.size();i++)
		{
			wordtype = getWordType(i);
			
			if( wordtype == Word.WT_PREPOSITION && getWordValue(i).equals(prep) )
			{
				//the splitting preposition has been found;
				// we now put the NP into the second NP container.
				//HOWEVER, it may be that the preposition occurs right after
				// the verb, therefore the 1st NP would not have entries - which
				// is undesirable.
				if(firstNounPhrase.size()!=0)
					bucket = secondNounPhrase;
				//else i++;
			}
			else if(
				wordtype == Word.WT_NOUN || 
				wordtype == Word.WT_ADJECTIVE ||
				wordtype == Word.WT_DIRECTION ||
				wordtype == Word.WT_PRONOUN ||
				wordtype == Word.WT_NUMERIC ||
				wordtype == Word.WT_NUMERIC ||
				wordtype == Word.WT_NEGATION ||
				wordtype == Word.WT_PREPOSITION ||
				wordtype == Word.WT_SUPERLATIVE )
				
				bucket.addElement( words.elementAt(i) );
			else if( wordtype == Word.WT_STRING )
			{
				if(firstNounPhrase.size()!=0) 
					bucket = secondNounPhrase;
				//else if(secondNounPhrase.size()!=0)
				bucket.addElement( words.elementAt(i) );
			}
		}
	}
	public void isolateNounPhrases(String prep1, String prep2)
	{
		//initially we add the NP to the first NP
		Vector bucket = firstNounPhrase;
		
		for(int i=0;i<words.size();i++)
		{
			String hayley = getWordValue(i);
			int hayint = getWordType(i);
			
			if( getWordType(i) == Word.WT_PREPOSITION && getWordValue(i).equals(prep1) )
			{
				//the third splitting preposition has been found;
				// we now put the NP into the second NP container.
				if(firstNounPhrase.size()!=0)
					bucket = secondNounPhrase;
				
			}
			else if( getWordType(i) == Word.WT_PREPOSITION && getWordValue(i).equals(prep2) )
			{
				//the second splitting preposition has been found;
				// we now put the NP into the third NP container.
				if(secondNounPhrase.size()!=0)
					bucket = thirdNounPhrase;
			}
			else if( 	getWordType(i) == Word.WT_NOUN || 
						getWordType(i) == Word.WT_ADJECTIVE ||
						getWordType(i) == Word.WT_PRONOUN ||
						getWordType(i) == Word.WT_NUMERIC ||
						getWordType(i) == Word.WT_STRING ||
						getWordType(i) == Word.WT_NUMERIC ||
						getWordType(i) == Word.WT_NEGATION ||
						getWordType(i) == Word.WT_PREPOSITION ||
						getWordType(i) == Word.WT_SUPERLATIVE 
					)
				
				bucket.addElement( words.elementAt(i) );
		}
	}
	/**
	* Adds the Sentences prepositions into a single Vector
	*/
	public void groupPrepositions()
	{
		prepositions = new Vector();
		for(int i=0;i<words.size();i++)
		{
			if( getWordType(i) == Word.WT_PREPOSITION )
				prepositions.addElement(words.elementAt(i));
		}
	}
	
	/**
	* Returns all the words up to the specified index
	*
	* @returns				a string of words up to the index
	*/ 
	public String getWords(int upto)
	{
		if( upto > words.size() )
			upto = words.size();
			
		StringBuffer result = new StringBuffer();
		for(int i=0;i<upto;i++)
		{
			result.append( getWordValue(i) + " " );
		}
		return result.toString().trim();
	}
	
	/**
	* Returns all the words up to, but not including, the
	* very first unknown word.
	*
	* @returns				a string of words preceeding the first unknown word
	*/
	public String getSensibleWords()
	{
		StringBuffer result = new StringBuffer();
		for(int i=0;i<words.size();i++)
		{
			if( getWordType(i) != Word.WT_UNKNOWN )
				result.append( getWordValue(i) + " " );
			else
				return result.toString().trim();
		}
		return result.toString().trim();
	}
	
	/**
	* Returns the first Word within the sentence which is
	* of type UNKNOWN
	*
	* @returns 				the first unknown word in the sentence
	*/
	public String getFirstUnknownWord()
	{
		for(int i=0;i<words.size();i++)
			if( getWordType(i) == Word.WT_UNKNOWN )
				return getWordValue(i);
		return "";
	}
	
	/**
	* Returns the first identifiable verb within the sentence
	* @returns				the first identifiable verbs string value
	*/
	public String getFirstVerb()
	{
		for(int i=0;i<words.size();i++)
		{
			if( getWordType(i) == Word.WT_VERB )
				return getWordValue(i);
		}
		return "";
	}
	
	/**
	* Returns the first identifiable adverb within the sentence
	* @returns				the first identifiable 
	*/ 
	public String getFirstAdverb()
	{
		for(int i=0;i<words.size();i++)
		{
			if( getWordType(i) == Word.WT_ADVERB )
				return getWordValue(i);
		}
		return "";
	}
	
	/**
	*	Loop through a sentence looking for N->Conn combinations.	Cut	these out
	*	and return them as a Vector.
	*	One exception	is when	we come	across a preposition - all following nouns are then 
	*	treated as arguments
	*
	*	@param sentence			the sentence to process
	*/
	public static Vector splitNounPhrases(Sentence sentence)
	{
		Vector result = new Vector(4,4);
		int startmarker = 0;
		boolean preparg = false;
		
		for(int i=0;i<sentence.words.size();i++)
		{
			//if we have come across some sort of preposition (negation is a type of prep), then set a flag.
			if( sentence.getWordType(i) == Word.WT_PREPOSITION || sentence.getWordType(i) == Word.WT_NEGATION )
				preparg = true;
			
			//if the current word is a connector, and the next word is either a verb or adverb,
			// add what we have so far into a vector.
			else if( (sentence.getWordType(i) == Word.WT_CONNECTOR) && 
				( 	sentence.getWordType(i+1) == Word.WT_VERB ||
					sentence.getWordType(i+1) == Word.WT_ADVERB  ) )
			{
					
					result.addElement( sentence.subSentence(startmarker,i-1) );
					startmarker = i+1;
					if( i<sentence.words.size() )
						i++;
					else
						break;
			}
			
			else if( (!preparg) && sentence.getWordType(i) == Word.WT_CONNECTOR &&
				(sentence.getWordType(i-1) == Word.WT_NOUN || sentence.getWordType(i-1) == Word.WT_PRONOUN) )
			{
				result.addElement( sentence.subSentence(startmarker,i-1) );
				startmarker = i+1;
				if( i<sentence.words.size())
					i++;
				else 
					break;
			}
				
		}
		
		if( (startmarker<sentence.words.size()) )
			result.addElement( sentence.subSentence(startmarker, sentence.words.size()-1) );
		
		return result;
	}

	
	/**
	* Splits the sentence along a word, producing two new Sentences.
	* Note:	the	word that is split upon	is lost. So	be careful out there
	* All relevent properties are also split into two.
	* Should really	only be	done before	parsing	proper.
	*/
	public final Sentence[] splitSentence(int index)
	{
		//if the index is not within bounds, then nothing sensible is being demanded
		// so return
		if((index>words.size()-2)||(index<1)) 
			return null;
		
		//intialise	a return object
		Sentence result[] = new Sentence[2];result[0]=new Sentence();result[1]=new Sentence();
		
		for(int i=0;i<index;i++)
		    result[0].words.addElement(this.words.elementAt(i));
		
		for(int i=index+1;i<words.size();i++)
		    result[1].words.addElement(this.words.elementAt(i));
		
		return result;
	}
	
	/**
	* Returns a	selected subsection	of the sentence as a sentence
	* returns null if stupid params	are	based.
	* @param start			the start index, inclusive
	* @param end			the end index, exclusive
	*/
	public final Sentence subSentence(int start,int end)
	{
		//check	for	stupid demands
		if(	(start<0) ||
			(end>=words.size())	||
			(start>end)
			) 
		    return null;
		//init the returning sentence
		Sentence result = new Sentence();
		for(int i=start;i<end+1;i++)
		{
		    result.words.addElement(this.words.elementAt(i));
		}
		return result;
	}
	
	/**
	* Flushes all the words from this sentence.
	*/
	public void flush()
	{
	    words.removeAllElements();
	}
	
	/**
	* Returns a String representation of this sentence
	* @returns				a string representation of this sentence
	*/
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		for(int	i=0;i<words.size();i++)
			buf.append( ((Word)words.elementAt(i)).getValue() +" " );
		return buf.toString().trim();
	}
	
	//--------------- Public Accessor/Mutator Methods -------------------//	  
	
	/**
	* Returns this sentences associated ParserEvent
	* @returns				this sentences associated ParserEvent
	*/
	public ParserEvent getParserEvent()
	{
		return parserEvent;
	}
	
	/**
	* Returns the number of words in this sentence
	* @returns				the number of words in this sentence
	*/
	public int size()
	{
		return words.size();
	}
	
	/**
	* Returns a duplicate of the words within this sentence.
	* @returns				a clone of the words within this sentence
	*/
	public Vector cloneWords()
	{
		return (Vector)words.clone();
	}
		
	/**
	* Adds a Word object to the sentence
	* @param w				the word to add
	*/
	public void addWord( Word word )
	{ 
		words.addElement( word ); 
	}
	
	/**
	* Adds a word string to the sentence
	* @param w 				the word to add
	* @param wt 			the wordtype of the word
	*/
	public void	addWord( String word,int wordtype )
	{ 
		addWord( new Word( word, wordtype ) );    
	}
	
	/**
	* Sets the entire word store with a new set
	* @param words			the replacement set of words
	*/
	public void setWords(Vector words)
	{
		this.words = words;
	}
	/**
	* Sets a word within the sentence to a new Value
	* @param word 			the new word to add
	* @param index 			where to place this new word within the sentence
	*/
	public void setWord( Word word, int index )
	{
		words.setElementAt( word, index );
	}
	
	/**
	* Sets a word within the sentence to a new Value
	* @param w 				the string value of the new word
	* @param wt 			the wordtype of the new word
	* @param index 			where to place this word within the sentence
	*/
	public void	setWord( String word, int wordtype, int index )
	{
		setWord( new Word( word, wordtype ), index );
	}
	
	
	/**
	* Sets the string value of a word within the sentence
	* @param word 			the new string value
	* @param index 			the position within the sentence
	*/
	public void setWordValue( String word, int index )
	{
		//note that setValue is a Word method
	    ((Word)words.elementAt( index )).setValue( word );
	}
	
	/**
	* Sets the numeric value of a word within the sentence.
	* Words have numeric values when they are words
	* @param number 		the new numeric value
	* @param index 			the position within the sentence
	*/
	public void setWordValue(int number, int index)
	{
		((Word)words.elementAt(index)).setValue( number );
	}
	
	/**
	* Removes a word from the sentence
	* @param index 			the position of the word to remove
	*/
	public void removeWord(int index)
	{ 
		words.removeElementAt(index); 
	}
	
	/**
	* Inserts a new word into the sentence
	* @param word 			the string value of the new word
	* @param wordtype 		the WordType of the new word
	* @param index 			where to insert the word within the sentence
	*/
	public void	insertWord(String word, int wordtype, int index)
	{
		insertWord(new Word(word, wordtype), index);
	}
	
	/**
	* Inserts a new word into the sentence
	* @param word 			the new Word to insert
	* @param index 			where to insert the word within the sentence
	*/
	public void insertWord(Word word, int index)
	{
		words.insertElementAt(word, index);
	}
	
	/**
	* Returns a word from the sentence
	* @returns				a specified word from the sentence	
	*/
	public Word getWord(int index)
	{
		if( index < words.size() )
			return (Word)words.elementAt(index);
		return null;
	}
	
		
	/**
	* Returns a words numeric value
	* @param index			the position of the word within the sentence
	*/
    /*public int getWordNumericValue(int index)
    { 
    	if( index < words.size() )
    		return ((Word)words.elementAt(index)).getNumericValue();
    	return 0;
    }//*/
    
    /**
    * Returns a set of word values from the sentence
    *
    * @param start 			the index of the first word
    * @param end			the index of the last word
    */
    public String[] getWordValue(int start,int end)
    {
        if(start<0 || start>end || end>words.size() || end<0) 
            return null;
        String result[] = new String[(end+1) - start];
        for(int i=0;i<result.length;i++)
            result[i] = ((Word)words.elementAt(i+start)).getValue();
        
        return result;
    }

	/**
	* Returns a words string value
	*
	* @param index			the position of the word within the sentence
	*/
	public String getWordValue(int index)
	{ 
		if( index < words.size() )
			return ((Word)words.elementAt(index)).getValue();
		return null;
	}
	
	/**
	* Returns a words type
	* @param index 			the position of the word we are interested in
	*/
	public int getWordType(int index)
	{ 
		return ((Word)words.elementAt(index)).getType(); 
	}
	
	/**
	* Sets a specified words type
	* @param index			the position of the word we are interested in
	*/
	public void setWordType(int newvalue,int index)
	{
	    ((Word)words.elementAt(index)).setType(newvalue);
	}
	
	
	
	/**
	* Returns the number of prepositions held
	* @returns				the number of prepositions held
	*/
	public int getPrepositionsSize()
	{
		return prepositions.size();
	}
	
	/**
	* Gets a prepositions from the special preposition store
	* @returns				a specified preposition
	*/
	public Word getPreposition(int index)
	{
		if( index < prepositions.size() )
			return (Word)prepositions.elementAt(index);
		return null;
	}
	
	/**
	* Gets a prepositions Value (String) from the special preposition store
	* @returns				a specified prepositions string value
	*/
	public String getPrepositionValue(int index)
	{
		if( index < prepositions.size() )
			return ((Word)prepositions.elementAt(index)).getValue();
		return null;
	}
	
	public String getVerbPropertyID()
	{
		return verbPropertyID;
	}
	
	/**
	* Sets this sentences Verb Property ID.
	*/
	public void setVerbPropertyID(String verbPropertyID)
	{
		this.verbPropertyID = verbPropertyID;
	}
	
	
	/**
	* Returns this Sentences designated verb
	*
	* @returns 				the sentences designated verb
	*/
	public Verb getVerb()
	{
		return verb;
	}
	
	
	/**
	* The native verb is the verb in its form as first entered by the user.
	*
	* @param verbString		the verb entered by the user
	*/
	public void setVerbString(String verbString)
	{
		this.verbString = verbString;
	}

	/**
	* Returns the verb string
	*/
	public String getVerbString()
	{
		return verbString;
	}
	
	/**
	* Sets this sentences designated verb
	*
	* @param verb			the sentences designated verb
	*/
	public void setVerb(Verb verb)
	{
		this.verb = verb;
	}
	
	/**
	* Sets this Sentences VerbPhrase. The VerbPhrase is a 
	* Vector of Words.
	* @param verbPhrase		the new VerbPhrase
	*/
	public void setVerbPhrase(Vector verbPhrase)
	{
		this.verbPhrase = verbPhrase;
	}
	
	/**
	* Returns this sentences VerbPhrase. The VerbPhrase is a 
	* Vector of Words.
	* @returns				this sentences VerbPhrase.
	*/
	public Vector getVerbPhrase()
	{
		return verbPhrase;
	}
	
}

