// $Id: Parser.java,v 1.40 1999/04/29 10:01:36 jim Exp $
// Parser class	all	kinds of input
// Alexander Veenendaal, 19	August 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.File;

import java.util.Date;
import java.util.Vector;
import java.util.StringTokenizer;

import com.ogalala.util.Debug;
import com.ogalala.util.Privilege;
import com.ogalala.util.Privileged;

/**
* The Parser is responsible for converting a natural language (English) sentence 
* into a representation that the system can understand. Along the way, it attempts
* to resolve any of the ambiguities that are common within Natural language.
*
* The sentences themselves generally take the form: 
*
*		Verb NounPhrase
*
* Although they can be simpler or more complex, the parser is tuned to handle this
* particular pattern only; there is currently no support for sentences other than
* imperitive sentences.
* 
* More than one sentence may be entered at once by using fullstops.
* The previous sentence may be repeated by entering 'again'
* 
* Verbs may be declared communicative, in which case they take two forms:
*
*		<verb> string 			-  	anything after the verb is automatically assumed to be
*									a string
*		<verb> to noun string	-	the key words here is the preposition 'to'. Immediately
*									after this
*/

public class Parser
    implements java.io.Serializable
{
    public static final int serialVersionUID = 1;
    
	//--------------------------- Properties ---------------------------//
	
    /** Debug flag
    */
    protected static final boolean DEBUG = false;
    
	/** Back-pointer to world
    */
    private World world;
    
    /** The vocabulary contains all the words which the parser understands
    */
	private static Vocabulary vocabulary;
	
	/** stores sentences, usually heavily decomposed
	*/
	private Vector sentenceStore = new Vector();
	
	/** used for sentences entered over several lines
	*/
	private Sentence multiLineSentence = new Sentence();
	
	/** the very last sentence entered
	*/
	private Sentence cachedSentence;
	
	/** 
	* The pronoun refers to the previously mentioned Atom.
	* Whether that be in the current or preceeding sentence
	* depends on the sentence form.
	*/
	private Atom pronoun = null;
	
	/** The actor to which this parser belongs.
	*/
	private Atom actor;
    
    /** privilege level of the user of this particular parser instance.
    */
    private int privilege;
    
    
    private static final String ALL = "all";
    private static final String GO = "go";
    private static final String THING = "thing";
    private static final String SAY = "say";
    private static final String TO = "to";
    
	//--------------------------- Constructors ---------------------------//
	
	/**
	* Creates a parser. As a privilege level is not given, the default is
	* used. (See Privilege.java)
	*
	* @param world					a reference to the game world from where
	*								commands are passed.
	*/
	public Parser(World world)
	{
	    this(world, Privilege.DEFAULT);
	}
	
	/**
	* Creates a parser, with a defined privilege level 
	*
	* @param world					a reference to the game world
	*								commands are passed.
	* @param privilege				the privilege level for this parser.
	*/
	public Parser(World world, int privilege)
	{
		this.world = world;
	    this.vocabulary = world.getVocabulary();
		this.privilege = privilege;
	}
	
	
	
	//--------------------------- Public Methods ---------------------------//
	
	/**
	* Overridden method. For full details see other parseSentence
	* @param sentence				a string of words to parse
	* @param actor					the actor from which this sentence originated.	
	*/
	public void parseSentence(String sentence, Atom actor)
	{
	    this.actor = actor;
	    parseSentence(sentence);
	}
	
	/**
	* The parseSentence method could validly be considered the heart of the
	* parser.
	*
	* A Sentence (a string of words) passes through the 'parser pipeline' a set
	* of calls to various other methods, to eventually become a valid Event if
	* successful, or an error message to the player if not.
	*
	* The sentence itself, goes through encapsulation into Sentence and Word objects
	* early on in the pipeline, which allows the following methods to manipulate the
	* sentence correctly.
	
	* In certain parts of the pipeline, a method may cause the pipeline to be 
	* aborted for one or another reason. In the case of the method lexicallyAnalyse(),
	* it will exit the pipeline if the sentence is ungrammatical. It does this by
	* throwing a ParserPipelineException, which is then caught at the end of the 
	* pipeline. ( This is a fairly unorthodox way of exiting such a pattern, but it
	* does keep the code relatively clean. )
	* Depending on the severity of the ParserPipelineException, a message may be 
	* printed to the Debug log.
	*
	* Finally, if the Parsers debug flag is turned on, the method will display a 
	* summary of the sentences internal state.
	*
	* @param sentence				a string of words to parse
	*/
	public void parseSentence(String sentence)
	{
	    //a check for nothingness
	    if( (sentence == null)||(sentence.trim().length() == 0) ) 
	    	return;
		try
		{		
			//------------ Begin Parser	Pipeline ----------------//
		    try
		    {
		    	//-----//
	    		//--1--// We begin with only a single empty sentence.
	    		//-----//
	    		Sentence first = new Sentence();
	    		
	    		//-----//
	    		//--2--// load a tokenized sentence into a sentence
	    		//-----//
	    		loadStringIntoSentence(first,tokenizeSentence(sentence));
				
	    		// If the input is a comment, we may have an empty sentence, which should be ignored.
	            if( first.size() <= 0 )
	                return;
	    		
	    		//-----//
	    		//--3--// Handle special sentences.
	    		//-----//
	    		first = handleLowLevelSentences(first);
	    		
	    		//-----//
	    		//--4--// Sentence cached
	    		//-----//
	    		cachedSentence = first;
	    		
	    		//print out the compounded sentence
	    		if(DEBUG)
	    			Debug.println(" [parser] " +  first );
	    		
	    		//-----//
	    		//--5--// Look up all possible word	classes	for	this sentence
	    		//-----//
	    		lookUpWords(first);
	    		
	    		//-----//
	    		//--6--// split the sentence up along the fullstops
	    		//-----// note, we now deal	with the vector of sentences known as the sentenceStore,
	    			   // not the single sentence anymore.
	    		splitSentenceAlongTerminators(first,sentenceStore);
	    		
	    		//-----//
	    		//--7--// Lexically analyse each sentence in the sentence storage
	    		//-----//
	    		lexicallyAnalyse(actor, sentenceStore, false);
	    		
	    		//-----//
	    		//--8--// Split the sentences we have so far along their noun/conjunction borders.
	    		//-----//
	    		splitSentencesAlongConjunctions( sentenceStore );
	    		
	    		
	    		// apply any alterations
	    		correctSentences( sentenceStore );
	    		
	    		
	    		//-----//
	    		//--9--// Fold adverbs in to the main verb, and isolate the verb and adverb parts
	    		//-----//
	    		assignVerbsAndAdverbs(sentenceStore, vocabulary, privilege);
	    		
	    		//------//
	    	    //--10--// Verb Template Search - Verbs get looked up here
	    	    //------//  
	    	    generateParserEvents(this, sentenceStore);
	    		
	    		//------//
	    	    //--11--// Events get sent to the event queue
	    	    //------//
	    	    postParserEvents(this, sentenceStore);
	    	
	    	}
	    	
	    	catch(ParserPipelineException p)
	    	{ 
	    		//only report the exception, if its REALLY bad.
	    		if(p.getState() == ParserPipelineException.FATAL)
	    			Debug.println(" [parser] (" + actor.getName() + ") " +p);//*/ 
	    	}
			//---------------- End Parser Pipeline -------------------//
			
			if (DEBUG)
			{
				StringBuffer s = new StringBuffer();
				Sentence tempSentence;
	    		//print	our	results	to the output (temporary).
	    		for(int	j=0;j<sentenceStore.size();j++)
	    		{
	    			tempSentence = (Sentence)sentenceStore.elementAt(j);
	    			for(int i=0;i<tempSentence.size();i++)
	    				s.append(  tempSentence.getWordValue(i) + "\t" );
	    			    
	    			Debug.println(" [parser] " + s.toString() );
	    			
	    			Debug.println( s.toString() );
	    			s = new StringBuffer("\tVerb PropertyID:\t");
	    			
	    			if(tempSentence.getVerbPropertyID() != null)
	    				s.append( tempSentence.getVerbPropertyID() );
	    			
	    			Debug.println(" [parser] " + s.toString() );
	    			
	    			if(tempSentence.firstNounPhrase.size()!=0)
	    			{
	    				s = new StringBuffer( "\tCurrent NP:\t" );
	    				for(int i=0;i<tempSentence.firstNounPhrase.size();i++)
	    					s.append( ((Word)tempSentence.firstNounPhrase.elementAt(i)).getValue() + "\t" );
	    				Debug.println(" [parser] " +  s.toString() );
	    		    }
	    		    
	    		    if(tempSentence.secondNounPhrase.size()!=0)
	    			{
	    				s = new StringBuffer( "\tArg1 NP:\t" );
	    				for(int i=0;i<tempSentence.secondNounPhrase.size();i++)
	    					s.append( ((Word)tempSentence.secondNounPhrase.elementAt(i)).getValue() + "\t" );
	    		    	Debug.println(" [parser] " +  s.toString() );
	    		    }
	    		    if(tempSentence.thirdNounPhrase.size()!=0)
	    			{
	    				s = new StringBuffer("\tArg2 NP:\t");
	    				for(int i=0;i<tempSentence.thirdNounPhrase.size();i++)
	    					s.append( ((Word)tempSentence.thirdNounPhrase.elementAt(i)).getValue() + "\t" );
	    		    	Debug.println(" [parser] " +  s.toString() );
	    		    }
	    		    
	    		    //die StringBuffer, die
	    			s = null;
	    		}//*/
	        }
        } catch (Exception e)
        {
        	
        	if( DEBUG && sentenceStore.size() > 0 )
        	{
	        	Debug.println(" [parser] " + sentenceStore.size() + " sentences processed");
	        	Debug.println(" [parser] ------------------------------------------------");
	        }
			//clean	up any odds	and	sods - this must occur
			sentenceStore.removeAllElements();
			//throw e;
		}
		if( DEBUG && sentenceStore.size() > 0 )
    	{
        	Debug.println(" [parser] " + sentenceStore.size() + " sentences processed");
        	Debug.println(" [parser] ------------------------------------------------");
        }
		//clean	up any odds	and	sods - this must occur
		sentenceStore.removeAllElements();
	}
	//--------------------------- Private Methods ---------------------------//

	/**
	* Loops through a Vector of Sentences, which we assume have been processed
	* and fires off each sentences event into the World and onwards to the
	* EventQueue.
	*
	* @param parser					an instance of the parser
	* @param store					a vector of Sentence objects
	*/
	private static void postParserEvents(Parser parser, Vector store)
	{
		Sentence sentence;
		for(int i=0;i<store.size();i++)
		{
			sentence = (Sentence)store.elementAt(i);
			if( sentence.getParserEvent() != null )
				parser.getWorld().postEvent( sentence.getParserEvent() );
		}
	}
	/**
	* Posts an event onto the event queue. This is usually a RAWVERB event, as
	*  there is no need to bind the current or the arguments.
	*
	* @param actor					the actor who commissioned this event
	* @param propertyID				the verb property Identifer
	* @param args					the arguments of the rawverb
	*/
	private void postEvent(Atom actor, String propertyID, Object args[])
	{
		Event event = world.newEvent(actor, propertyID, world.getRoot(), args);
		world.postEvent(event);
	}
	
	
	/**
	* The generation of ParserEvents happens mostly within the sentences to which they
	* belong, as a ParserEvent is merely a more condensed and game friendly Sentence.
	*
	* @param parser					an instance of the parser
	* @param store					a storage vector of sentences from which we generate parser events
	*/
	private static void generateParserEvents(Parser parser, Vector store)
	{
		VerbTemplate match;		
		Sentence sentence;
		
		for(int i=0;i<store.size();i++)
		{
			sentence = (Sentence)store.elementAt(i);
			
			//place all the sentences prepositions into a Vector
			sentence.groupPrepositions();

			// make sure the verb exists before we move on.
			if(sentence.getVerb() != null)
			{
				if( sentence.getPrepositionsSize() == 0 )
					sentence.generateSingleArgParserEvent(parser);
				
				else if( sentence.getPrepositionsSize() == 1 )
					sentence.generateDoubleArgParserEvent(parser);
				
				else if( sentence.getPrepositionsSize() == 2 )
					sentence.generateTripleArgParserEvent(parser);
			}
			//No matches have been found with any VerbTemplates, so give up
			if(sentence.getParserEvent() == null) 
			{
				parser.getActor().output( "I do not know how to '" + sentence.getFirstVerb() + "'." );
				throw new ParserPipelineException("[parser:378] I do not know the verb " + sentence.getFirstVerb() );
			}
		}
	}
	
	/**
	* The first verb and adverb strings found in the sentence are pulled out, and an attempt
	* is made to apply to two to form a new verb.
	* The method then attempts to set each of the sentences Verb objects by looking it up 
	* in the vocab.
	* Note that the privilege level (which comes from the parser) may not be sufficient, 
	* in which case a null value will be set.
	*
	* In certain cases, it may be that a sentence does not contain a verb. This happens in 
	* situations such as:
	*
	* 		'get wumpus and n64 and box and chest'
	*
	* where the four sentences will be (after conjunction splitting):
	*
	*		'get wumpus'
	*		'n64'
	*		'box'
	*   	'chest'
	*
	* The first sentence will be executed correctly unaided, but the following sentences will not.
	* What happens is this: a request is made for the sentences verb. If one is not found, the
	* previous sentences verb is requested and patched into the sentence. Following this algorithim,
	* the four sentences will correctly become:
	*
	*		'get wumpus'
	*		'get n64' (get from previous sentence)
	*		'get box' (get from previous sentence (which is also from previous sentence) )
	* 		'get chest' (get from previous sentence (which is from previous sentence (which is...) ) )
	*
	* @param store				the parsers store of sentences
	* @param vocabulary			a reference to the main vocabulary
	* @param privilege			the parsers privilege level.
	*/
	private static void assignVerbsAndAdverbs(Vector store, Vocabulary vocabulary, int privilege)
	{
		Sentence sentence;
		for(int i=0;i<store.size();i++)
		{
			sentence = (Sentence)store.elementAt(i);
			
			//the verb may already have been set, so.....
			if( sentence.getVerb() == null )
			{
				//get the sentences verb
				String verb = sentence.getFirstVerb();
				
				//..and save this form for later.
				sentence.setVerbString(verb);
				
				//get the sentences adverb
				String adverb = sentence.getFirstAdverb();
				
				//if the sentences verb is empty, retrieve the verb from the previous sentence
				if( verb.length() == 0 )
				{
					sentence.setVerb( ((Sentence)store.elementAt(i-1)).getVerb() );
				}
				
				// otherwise, make sure we have a valid verb and adverb
				else
				{
					//if no valid adverb is found, the vocab function will return ""
					String postverb = vocabulary.applyAdverbMapping( adverb, verb );
					
					//if we have a valid new verb, set it.
					if(postverb != null && postverb.length() != 0)
						verb = postverb;
				
					//set the sentences Verb object
					sentence.setVerb( vocabulary.getVerb( verb, privilege ) );
				}
			}
		}
	}
	
	/**
	* This function deals with low level sentences coming into the parser that do not 
	*  warrant the full attention of the parser pipeline.
	*  Feature list:
	*
	*	Previous Sentence 		-	triggered by entering 'again' or 'g' as the first word.
	*	repetition  				Repeats previous sentence.
	*
	*	Sentence Concatination  - 	the ability enter a sentence over two or more lines. 
	*								Such sentences end with a '\'.
	*
	*	Privilege setting & 	-	In the system, both users and verbs have privilege levels.
	*	querying					In order for a user to 'use' a verb, they must have a
	*								privilege which equals or exceeds the verb privilege level.
	*								A set of commands beginning with !Priv allow several
	*								possiblities:
	*
	*									!priv			-	reports the users privilege level
	*									!priv <verb>	-	reports the verbs privilege level
	*									!priv <verb> <priv> - sets the privilege level
	*
	*	Adding Verbs			-	Verbs are the basic unit of action within the game.
	*								Sentences beginning with !Verb are requests which are
	*								passed directly to the Vocabulary, such sentences would
	*								cause fairly major problems if they were allowed to pass
	*								through to the Event Queue.
	*								The syntax of the add verb command is as follows:
	*
	*									!Verb <privilege> <"synonyms"> <"arguments>propertyID">
	*
	*								Where privilege indicates the min. level required by the
	*								actor to use this verb. This parameter is optional. In its
	*								abscence, the Privilege default will be used (see com.ogalala.
	*								util.Privilege).
	*								Where synonyms is a delimited list. 
	*								Where arguments are composed of arg types divided by
	*								prepositions (see VerbTemplate.java), and a single propertyID
	*								associated to it.
	*
	*	Removing Verbs			-	This command removes a verb, or rather its synonym from the
	*								vocabulary. The distinction is notable, because many synonyms
	*								point to one verb object; therefore the verb object remains until
	*								its last synonym is removed.
	*								The syntax of the remove verb command is as follows:
	*								
	*									!RemoveVerb <"synonyms">
	*
	*								Where synonyms is a list of strings.
	*
	*	Adding RawVerbs			-	RawVerbs, like verbs, activate actions. But RawVerbs are
	*								passed straight through to their handler with no Parser
	*								Pipeline processing.
	*								The syntax of the add rawverb command is as follows:
	*
	*									!RawVerb <privilege>  <verb-pattern> <propertyID>
	*					
	*								Where privilege is the min. level required by the actor to use
	*								this rawverb. The privilege parameter is optional. In its abscence
	*								the Privilege default will be used. (see com.ogalala.util.Privilege)
	*								Where verb-pattern is a delimited list of synonyms.
	*								Where propertyID is a single value which maps to the internal name
	*								of this RawVerb.
	*
	*	Running scripts			-	The !Run command tells the system to load the named script file and
	*								execute it. The syntax is as follows:
	*								
	*									!Run <scriptfile>
	*								
	*								Where scriptfile is a file located somewhere in the path of the system.
	*
	*	Adding Generic Words	-	Generic words have usually have no intrinsic value to the system, but are
	*								words entered by the user which are then parsed out of the sentence to leave
	*								the 'meat' ie verbs, nouns.
	*								Note that words may belong to more than Wordtype, it is then up to the parser
	*								to decide the genus of the word.
	*								The syntax is as follows:
	*									
	*									!Word <words> <wordtype>
	*
	*								Where words is a delimted list of strings designated to be the wordtype
	*								Where wordtype is a single string describing the words.
	*
	*	Adding Adverbs			-	Adverbs are recognised by the parser as words which will translate the given
	*								verb into a new form. For example, the presence of the adverb 'quickly' and the
	*								verb 'walk' will result in the verb being translated into 'run'.
	*								The syntax is as follows
	*		
	*									!Adverb "adverb + verb > translated-verb"
	*
	*								Where adverb,verb and translated-verb are all single strings.
	*
	*	Adding Adjectives		-	Adjectives within the system are another term for property. An example would
	*								be the adjective 'red'. Items within the world have 'redness' the property, and
	*								may be addressed by the phrase "red <thing>".
	*								As such, the adjective syntax is fairly straightforward:
	*								
	*									!Adjective <adjective> <propertyID>
	*
	*								Where adjective is a single string.
	*								Where propertyID is a single string.
	*
	*	Executing RawVerbs		-	The first word in the sentence is checked for being a RawVerb by being looked
	*								up in the Vocabulary. If it is a rawverb, it is packaged into an Event, and
	*								sent to the EventQueue.
	*
	* @param sentence				the sentence to process
	*
	* @exception ParserPipeLineException	is thrown when a command has been found and has been
	*										satisfactorily executed.
	*										The exception is caught at the end of the Parser pipeline
	*										and is a way to exit the pipeline
	*/
    private Sentence handleLowLevelSentences(Sentence sentence) throws ParserPipelineException
    {
    	// ---------------------------------------
    	// handle the repeat last sentence command
    	// ---------------------------------------
    	if( ( sentence.getWordValue(0).equals("again") ||
	          sentence.getWordValue(0).equals("g") )  &&
	          cachedSentence != null)
	    {
	        return cachedSentence;
	    } 
	    // --------------------------------------------------------------------
        // if we find a backslash at the end of the sentence, then it is not yet
        // finished, store the sentence for later.
        // --------------------------------------------------------------------
        if( sentence.getWordValue(sentence.size()-1).equals("\\"))
        {
            //remove this backslash
            sentence.removeWord(sentence.size()-1);
            //add the sentence into the special store
            for(int i=0;i<sentence.size();i++)
                multiLineSentence.addWord(sentence.getWord(i));
            //finish with the sentence.
            sentence = null;
            //return a value to halt the parser pipline.
            throw new ParserPipelineException("append sentence found.");
        }
        // --------------------------------
        // A request for the privilege level
        // --------------------------------
        else if( sentence.getWordValue(0).toLowerCase().equals("!priv") )
        {
        	if( sentence.size() > 1 )
            	handlePrivilegeCommand(sentence.getWordValue(1,sentence.size()-1));
            else
            {
        		actor.output("Usage: !Priv - Display Current Privilege level. ");
        		actor.output("!Priv <verb> - Display Verb Privilege level.");
        		actor.output("!Priv <verb> <level> - Set Verb Privilege level.");
        	}
        	throw new ParserPipelineException("privilege information request found.");
        }

        // ------------------------------------
        // Handle sentences over multiple lines
        // ------------------------------------
        else if(multiLineSentence.size()!=0)
        {
            //we have to add the first part of the sentence to this one
            //add the sentence into the special store
            for(int i=0;i<sentence.size();i++)
                multiLineSentence.addWord(sentence.getWord(i));
            //the composite sentence now becomes the current sentence
            sentence.setWords( multiLineSentence.cloneWords() );
            multiLineSentence.flush();
        }
        
        // ---------------------------------------
        // !VERB sentences adds a verb definition
        // ---------------------------------------
        if( sentence.getWordValue(0).toLowerCase().equals("!verb") )
        {
            //add this verb to the vocabulary
            addVerb(sentence.getWordValue(1,sentence.size()-1));
            
            //exit the parser pipeline
            throw new ParserPipelineException("Add verb found");
        }
        
        // -------------------------------------------
        // !REMOVEVERB sentences remove verb synonyms
        // -------------------------------------------
        if( sentence.getWordValue(0).toLowerCase().equals("!removeverb") )
        {
        	if( sentence.size() < 2 )
        	{
        		actor.output("Badly formed RemoveVerb command. Usage: !removeverb <verbs>");
            	throw new ParserPipelineException(" [parser] Badly formed RemoveVerb command");
			}
        	//pass the first word (the list of synonyms to remove) to the vocab
        	removeVerb( sentence.getWordValue(1) );
        	//exit the parser pipeline
        	throw new ParserPipelineException("Remove verb found");
        }
        
        // -------------------------------------------
        // !RAWVERB sentence adds raw verb definition
        // !RAWVERB verb-pattern propertyID value
        // -------------------------------------------
        if( sentence.getWordValue(0).toLowerCase().equals("!rawverb") )
        {
        	if( sentence.size() < 2 )
	    	{
	    		actor.output("Usage: !RAWVERB <privilege> verb-pattern <propertyID> <value> :" + sentence.getWordValue(1) + " "+ sentence.getWordValue(2));
	            throw new ParserException(" [parser] Usage: !RAWVERB verb-pattern propertyID value");
	    	}
			//pass this sentence on the vocabulary
			addRawVerb(sentence.getWordValue(1,sentence.size()-1) );
            
            //exit the parser pipeline
            throw new ParserPipelineException("Add rawverb found");
        }
        
        // -------------------------------------------
        // !RUN sentences execute scripts
        // -------------------------------------------
        else if( sentence.getWordValue(0).toLowerCase().equals("!run") )
        {
            //pass this sentence on to the world, and exit the pipline
            try 
            {
                String fileName = sentence.getWordValue(1);
                world.execScript(fileName, actor);
            }
            catch (Exception e)//(java.io.IOException e)
            {
                throw new ParserException(e.getMessage());
            }
            throw new ParserPipelineException("Run script found");
        }
		// ------------------------------------------
        // Adds Generic Words
        // ------------------------------------------
        else if( sentence.getWordValue(0).toLowerCase().equals("!word") )
        {
            //pass this sentence on to the vocabulary
            //vocabulary.addGenericWord(actor, sentence.getWordValue(1,sentence.size()-1));
            
            if( sentence.size() < 3)
            {
            	actor.output("Incorrect syntax for adding generic words. !word \"word1,word2\" wordtype");
            	throw new ParserException(" [parser] Incorrect syntax for adding generic words. !word \"word1,word2\" wordtype");
            }
            else
            	addGenericWord( sentence.getWordValue(1), sentence.getWordValue(2) );
 
            //exit the parser pipeline
            throw new ParserPipelineException("Add Vocabulary Word found");
        }
        
        // -----------------------------------------
        // removes generic words from the vocabulary
        // -----------------------------------------
        else if( sentence.getWordValue(0).toLowerCase().equals("!removeword") )
        {
        	if( sentence.size() < 2 )
        	{
        		actor.output("Incorrect syntax for removing generic words. !removeword \"word1,word2\".");
            	throw new ParserException(" [parser] Incorrect syntax for removing generic words. !removeword \"word1,word2\".");
            }
            else
            	removeGenericWord(sentence.getWordValue(1));
            	
	        throw new ParserPipelineException("Remove Word found");
        }
        // -------------------
        // addition of adverbs
        // -------------------
        else if( sentence.getWordValue(0).toLowerCase().equals("!adverb") )
        {
        	if(sentence.size()<2)
        	//if(args.length<1)
        	{
        		actor.output("Badly formed Adverb definition. Usage: !adverb verb+adverb>modified_verb.");
            	throw new ParserPipelineException(" [parser] Badly formed Adverb definition : expected argument");
			}
			
			//add the adverb
			addAdverb(sentence.getWordValue(1));
			
        	//exit the parser pipeline
        	throw new ParserPipelineException("Add adverb found");
        }
        // ----------------------
        // support for adjectives
        // ----------------------
        else if( sentence.getWordValue(0).toLowerCase().equals("!adjective") )
        {
        	if( sentence.size() < 3 )
        	{
        		actor.output("Badly formed Adjective definition. Usage: !adjective <adjective> <property ID>.");
        		throw new ParserPipelineException(" [parser] Badly formed Adjective definition submited");
        	}
        	
        	addAdjective( sentence.getWordValue(1), sentence.getWordValue(2) );
        	
        	//exit the parser pipeline
        	throw new ParserPipelineException("Add adjective found");
        }
        // ------------------------
        // check for other raw verbs
        // ------------------------
        else if( (vocabulary.getWordTypes(sentence.getWordValue(0)) & Word.WT_RAWVERB) != 0)
        {
            //pass this to the event wrapping method, and exit the pipeline
            //we check here for a correct privilege level, if it doesn't check out, ignore and pass through
            if( vocabulary.getRawVerb(sentence.getWordValue(0), privilege) != null )
            {
            	postEvent( actor, vocabulary.getRawVerb(sentence.getWordValue(0)).getPropertyID(), sentence.getWordValue(1,sentence.size()-1) );
            	throw new ParserPipelineException("Raw Verb found");
            }
            else 
            	throw new ParserPipelineException("Raw Verb found, but insufficient Privilege to excecute");//*/
        }
        return sentence;
    }

	/**
	* Takes a string and tokenises it into an array of strings
	*
	* @param input				the string to tokenize
	* @returns					an array of post-tokenized strings
	*/
	private static String[] tokenizeSentence(String input)
	{
	    Vector result = new Vector();
	    ParserTokenizer st = new ParserTokenizer(input);
		
		while(st.hasMoreElements())
		{
		    result.addElement(st.nextToken());
		}
		String sresult[] = new String[result.size()];
		result.copyInto(sresult);
	    return sresult;
	}
	
	/**
	* Takes a string array of words and loads them into a sentence whilst assigning a
	* wordtype.
	*
	* This may be less straightforwards than it first appears because there may be
	* the added complication of fullstops or commas somewhere in the strings.
	*
	* It is also here that certain tokens embedded into the strings from the tokenizer
	* are interpreted.
	*
	* The following interpretations occur:
	*
	* 	RawVerbs		-	A check is made for a exclamation mark at the beginning of 
	*						the string. If it is found, then certain other processing is
	*						turned off for this sequence of strings.
	*	
	*	Strings			-	Come out of the tokenizer surrounded with quotes
	*
	*	Numbers			-	Come out of the tokenizer prefixed by a '~'
	*
	*	Commas			-	The string is split about the commas position, creating up
	*						to two new words. the comma itself is added as a CONNECTOR
	*	
	*	Fullstops		-	The string is split about the fullstops position, creating
	*						up to two new words. the fullstop itself is added as a 
	*						TERMINATOR
	*
	* @param sentence					the sentence to load the input string into
	* @param input						a string array of words.
	*/
	private static void loadStringIntoSentence(Sentence sentence, String input[])
	{
		//we add a check for a raw verb, as various conditions within this method
		// may not apply

		boolean raw = false;
		if( input.length > 1 && input[0].charAt(0) == ('!') )
			raw = true;
		
	    for(int i=0;i<input.length;i++)
	    {
	        //Either cope with strings or commas and fullstops.
	        if(input[i].length()==1)
				sentence.addWord(input[i], Word.WT_UNKNOWN );
		    
		    //handle strings. they begin and end with quotes
			else if(input[i].charAt(0) == '"' && input[i].charAt(input[i].length()-1) == '"'){
			    sentence.addWord(input[i].substring(1,input[i].length()-1),Word.WT_STRING);
			}
			
			//handle numbers. they begin and end with ~s
			else if(input[i].charAt(0) == '~' && input[i].charAt(input[i].length()-1) == '~'){
			    sentence.addWord(input[i].substring(1,input[i].length()-1),Word.WT_NUMERIC);   
			}
	        
	        //check	for	comma at the end of	the	token
			else if(input[i].charAt(input[i].length()-1) ==	',') {
				sentence.addWord( input[i].substring(0,input[i].length()-1), Word.WT_UNKNOWN );
				sentence.addWord( "," , Word.WT_CONNECTOR );
			}
	        
	        //check for a comma somewhere in the word
	        else if(input[i].indexOf(",")!=-1)	{
				sentence.addWord( input[i].substring(0,input[i].indexOf(',')), Word.WT_UNKNOWN );
				sentence.addWord( ",", Word.WT_CONNECTOR );
				sentence.addWord( input[i].substring(input[i].indexOf(',')+1, input[i].length()), Word.WT_UNKNOWN );
			}
			
	        //check	for	a full stop	at the end of the token
			else if(input[i].charAt(input[i].length()-1) == '.') {
			   sentence.addWord( input[i].substring(0,input[i].length()-1), Word.WT_UNKNOWN );
			   sentence.addWord( ".", Word.WT_TERMINATOR );
			}
			
			//check for a fullstop somewhere in the word
       		else if( (!raw) && input[i].indexOf(".") !=-1 )
			{
				sentence.addWord( input[i].substring(0,input[i].indexOf(".")), Word.WT_UNKNOWN );
				sentence.addWord( ".", Word.WT_TERMINATOR);
				sentence.addWord( input[i].substring(input[i].indexOf('.')+1, input[i].length()), Word.WT_UNKNOWN );
			}
			//if nothing fun happens within the word, just add it to the sentence.
			else
				sentence.addWord( input[i], Word.WT_UNKNOWN );
	    }
	}
	
	/**
	* The overall purpose is to determine the wordtype(s) of each of the words
	* in the sentence. In most cases, the best the method will do is to compile
	* a list of possible wordtypes; it is then up to the lexical analyser to 
	* determine the final type.
	*
	* Certain words, however, are decided on almost immediatly; either because
	* they are the only type found, or because there position in the sentence
	* forces a conclusion.
	*
	* The types that are decided instantly are:
	*
	*	Sentence starts with String		-	The string wordtype is decided as soon as
	*										it has been tokenized. The verb word "say"
	*										is inserted at the beginning.
	*
	*	Sentence starts with Noun		-	It is assumed that the actor is attempting
	*										to communicate with the noun in question, so
	*										"say to" is added to the beginning of the 
	*										sentence.
	*
	*	Sentence starts with			-	Once the first word has been determined to be
	*	communicative verb					a verb, a check is made to see if it has been
	*										defined as being 'communicative' - that is,
	*										whether this verb will be used to communicate
	*										in some way.
	*										There are two forms of communicative verb: the
	*										first is an undirected form. ie. say "hello".
	*										The second form is directed, and is indicated by
	*										the prescence of the preposition 'to'.
	*
	*	Sentence starts with Direction 	-	It is assumed that the actor wishes to move in
	*										that direction; a 'go' is added to the beginning
	*										of the sentence.
	*
	*	Numeric handling				-	Normal numerics (4,5) will be identified straight
	*										from tokenization, numbers that are represented by
	*										words need to be looked up with the Vocabulary into
	*										normal numerics.
	*
	* @param sentence			the Sentence to lookup
	*/
	private static void lookUpWords(Sentence sentence)
	{
		
		//traps sentences beginning with a string
		//we automatically assume that the sentence was meant to 'say' something.
		if( sentence.getWordType(0) == Word.WT_STRING || sentence.getWord(0).checkForPossibleWordType(Word.WT_STRING) )
		{
		   	sentence.insertWord( SAY, Word.WT_VERB, 0);
		   	return;
		}
		
		//look at the first word, and figure out whether it could be a verb
		// which is the most likely scenario. In that case, cast it instantly
		// to a verb type, and look up the appropriate Verb class.
		//use the vocabulary to set the possible word types for this word.
		sentence.getWord(0).setPossibleWordTypes( vocabulary.getWordTypes(sentence.getWordValue(0)) );
		
		//if the sentence begins with a noun (and it couldn't possibly be a verb), then turn this
		// into an attempt to communicate with that noun
		if( sentence.getWord(0).checkForPossibleWordType(Word.WT_NOUN) && 
			!(sentence.getWord(0).checkForPossibleWordType(Word.WT_VERB)) )
		{
			sentence.insertWord( TO, Word.WT_PREPOSITION, 0);
			sentence.insertWord( SAY, Word.WT_VERB, 0);
		}
		
		//if the sentence begins with a verb...
		if( sentence.getWordType(0) == Word.WT_VERB ||
			sentence.getWord(0).checkForPossibleWordType(Word.WT_VERB) 
		  )
		{
			//get the Verb object from the possible verb string at position 0
			Verb possible = vocabulary.getVerb(sentence.getWordValue(0));
			
			//Is this verb communicative ? If so, the sentence form is predictable....
			if( possible.isCommunicative() )
			{
				//set the sentences Verb Object
				sentence.setVerb(possible);
				
				//set the word type
				sentence.setWordType(Word.WT_VERB,0);
				
				//### OH MY GOD - clunky as hell - redo at some point
				//check for a directed communicative verb form
				if( sentence.size() > 2 )
				{
					//get the possible word types of the next two words, hoping that they will be preposition or noun
					// followed by a noun
					sentence.getWord(1).setPossibleWordTypes( vocabulary.getWordTypes(sentence.getWordValue(1)) );
					sentence.getWord(2).setPossibleWordTypes( vocabulary.getWordTypes(sentence.getWordValue(2)) );
					
					if( sentence.getWord(1).checkForPossibleWordType(Word.WT_PREPOSITION) && 
						sentence.getWord(2).checkForPossibleWordType(Word.WT_NOUN) 
						)
					{
						sentence.setWordType(Word.WT_PREPOSITION,1);
						sentence.setWordType(Word.WT_NOUN,2);
						for( int i=3;i<sentence.size();i++ )
							sentence.setWordType(Word.WT_STRING, i);
						return;
					}
						
				}
				//its a straight communicative verb...
				for( int i=1;i<sentence.size();i++ )
					sentence.setWordType(Word.WT_STRING, i);

				//we have done all we can for the sentence, so leave quietly
				return;	
			}
		}
		
		//loop through each	word in	the	sentence
		for(int	i=0;i<sentence.size();i++)
		{
			//look up each word, as	long as	we don't already know its wordtype
			if(sentence.getWordType(i) == Word.WT_UNKNOWN)
			{
				//use the vocabulary to set the possible word types for this word.
			    sentence.getWord(i).setPossibleWordTypes( vocabulary.getWordTypes(sentence.getWordValue(i)) );
			    
			    //catch numbers early
				if( sentence.getWord(i).checkForPossibleWordType(Word.WT_NUMERIC) )
					sentence.setWordType(Word.WT_NUMERIC,i);
					
				//catch directions early
				else if( sentence.getWord(i).checkForPossibleWordType(Word.WT_DIRECTION) )
				{
					//sentence.setWordType(Word.WT_DIRECTION,i);
					if( i == 0 )
						sentence.insertWord( GO, Word.WT_VERB, 0);
				}
			}
			
		}
		
	}
	
	/**
	* 
	*/
	private final static void correctSentences(Vector store)
	{
		Sentence sentence;
		
		for(int j=0;j<store.size();j++)
		{
			sentence = (Sentence)store.elementAt(j);
			
			//if the sentence ends with "all", then add "thing" to the end of it
			if( sentence.getWordValue( sentence.size()-1 ).toLowerCase().equals(ALL) )
				sentence.addWord( THING, Word.WT_NOUN );
			
			// look for numerics among the sentence
			for(int	i=0;i<sentence.size();i++)
			{
				// If there is already a known numeric, turn into a well known int.
				if(sentence.getWordType(i) == Word.WT_NUMERIC)
				{
					//assume the number is in the form (2,4,5) first
					try {
						sentence.setWordValue( new Integer( sentence.getWordValue(i) ).intValue(), i);
					//if an exception is thrown, this number must be in english form (two,three,four)
					} catch (NumberFormatException e)
					{
						//set the wordvalue to be a true number
						sentence.setWordValue(Vocabulary.getNumberFromWord(sentence.getWordValue(i)), i);
					}
				}
			}
			
		}
	}
	/**
	* Kicks	off	the	recursive Lexical Analysis function	on a given sentence.
	* Stores all results inside	the	sentence itself
	*
	* @param actor				the actor who commissioned the command
	* @param store				a vector of sentences to analyse
	* @param trace				if true, a trace of the analysis is sent to the std out
	*/
	private final static void lexicallyAnalyse(Atom actor, Vector store, boolean trace)
	{
		Sentence sentence;
		
		for(int	i=0;i<store.size();i++)
		{
			sentence = (Sentence)store.elementAt(i);
			
			//conject that this sentence begins with a verb, and if that fails - an adverb
			if(	(conjecture(sentence,Word.WT_VERB,0,trace))	|| (conjecture(sentence,Word.WT_ADVERB,0,trace)) ) 
			{
				if(DEBUG)
					Debug.println(" [parser] Sentence seems sensible!");
			}
			
			else 
			{
				if(DEBUG)
					Debug.println(" [parser] Sentence ungrammatical: '" + sentence + "'");
				String sensible = sentence.getSensibleWords();
				
				//if the sentence ended with a preposition, then surely there was something missing from the end ?
				if( sentence.getWordType( sentence.size()-1 ) == Word.WT_PREPOSITION )
				{
					actor.output("What do you want to " + sentence + "?");
				}
				
				else if( sensible.length() == 0 )
					actor.output("I simply don't understand : '" + sentence.getWordValue(0) + "'");
				
				else
					actor.output("I understand: '" + sentence.getSensibleWords() + "', but what the devil is '" + sentence.getFirstUnknownWord() + "'");
					
				throw new ParserPipelineException("Can't understand sentence: " + sentence,ParserPipelineException.FATAL);
			}
		}
	}
	
	/**
	* Looks for any terminator wordtypes amongst the sentence. If any are found,
	* the sentence is split about the teminator, resulting in two new sentences.
	*
	* @param sentence				the sentence to process
	* @param result					the sentence split along its terminators
	*/
	private final static void splitSentenceAlongTerminators(Sentence sentence,Vector result)
	{
		//check	for	the	prescence of terminators within	the	sentence	sentence
		int	start = 0;
		
		for(int i=0;i<sentence.size();i++)
		{
		    if( (sentence.getWordType(i) == Word.WT_TERMINATOR )|| 
		        sentence.getWord(i).checkForPossibleWordType(Word.WT_TERMINATOR) )
		    {
		        //if we	have found a terminator,cut	a sentence out from	the	last start to this position
		        result.addElement(sentence.subSentence(start,i-1));
		        start = i  +1;
		    }
		}
		if( start <= sentence.size()-1 )
		    result.addElement(sentence.subSentence(start,sentence.size()-1));
	}

	/**
	* Searches through a storage vector	of sentences, looking for a noun/conjunction combination.
	* When it finds	one, it	splits the sentence into two, and stores the resultant vectors
	* back into	the	storage	vector.
	* For example:
	* 	'get the red apple and the green apple'
	* will result in two sentences
	* 	'get the red apple'
	* and
	* 	'the green apple'
	*
	* @param store					vector of Sentences
	*/
	private final static void splitSentencesAlongConjunctions(Vector store)
	{
		Vector temp;
		for(int	j=0;j<store.size();j++)
		{
			temp = Sentence.splitNounPhrases((Sentence)store.elementAt(j));
			if(temp.size()!=0)
			{
				store.removeElementAt(j);
				for(int	k=temp.size()-1;k>-1;k--)
					store.insertElementAt(temp.elementAt(k),j);
			}
		}
	}
	
	
	/**
	* The conjecture method determines the final wordtypes of each of the words in the
	* sentence by the context in which the word occurs.
	*
	* It works like this: the method looks at the words possible types, picks the first one,
	* and then calls itself (recursively) on the next word in the sentence, sending as a 
	* parameter, the wordtype it thinks this next word should be. Complicated, I know.
	*
	* For example, lets take the sentence
	*
	*		'get the pot plant'
	*
	* The calling method (lexicallyAnalyse), begins by conjecturing that the first word is
	* a verb, which is confirmed by the first part of the method.
	*
	* Next, a conjecture is made about the type of the next word 'the', by working through
	* the list of wordtypes that are associated with a Verb. As 'the' is an article, the
	* conjecture succeeds, and continues to recurse.
	*
	* The next word 'pot' has three possible types: 'verb','adjective' and 'noun'. 'Verb' never
	* appears in the articles list, so if this were the only type then the wordtype attached
	* to 'the' would have to be redecided.
	*
	* As it is, 'noun' is the first wordtype that fits the possible types, so the chain continues.
	* The next word 'plant', however, has the possible wordtypes 'verb' 'adjective' and 'noun',
	* none of which are suitable to follow a 'noun'. This level returns false, and 'pot' has to
	* be reevaluated.
	*
	* This time 'pot' is decided to be an adjective (next in the list), which makes 'plant' a
	* 'noun' which follows perfectly. 
	*
	* It is this ability of the analyser to backtrack which makes it such a powerful system which
	* is able to cope with a number of ambiguous situations.
	*
	* @param sentence				the sentence to try and interpret
	* @param wordtype				the supposed word type of this word
	* @param nextword				the index of the nextword in the sentence
	* @param trace					whether to print out debug information
	* @returns						true if the conjecture about this words type was correct, false otherwise
	*/
	private	static boolean conjecture(Sentence sentence, int wordtype, int nextword, boolean trace)
	{
		if(trace)
		{
			for(int i=0;i<nextword;i++) 
				System.out.print("\t");
			System.out.println("conject @ position " + nextword + "!");
		}
		//if we	have reached the end of	the	sentence, skip out.
		if(nextword	>= sentence.size()) 
			return	false;
		
		//If we	find a stopword, we	need to	bypass this	completely.Firstly mark	this word 
		//as a stopword, and then move the counter on the next word	and	return the conjecture 
		//for the next word	in line.
		if (sentence.getWord(nextword).checkForPossibleWordType(Word.WT_STOPWORD) )
		{
		    sentence.setWordType(Word.WT_STOPWORD,nextword);
		    if(trace)
		    	for(int i=0;i<nextword;i++) 
		    		System.out.print("\t");
		    nextword++;
		    return conjecture(sentence,wordtype,nextword,trace);
		}
		
		//is this current conjecture about the wordclass true ?	
		//if it	is,	go on to conject about the next	word
		//notice we	do two checks. The first asks whether the wordClass	is known, and the
		//second asks if it	is possible	that the word is of	a WordClass.
		if( (sentence.getWordType(nextword) == wordtype) ||
		    sentence.getWord(nextword).checkForPossibleWordType(wordtype) )
		{
			//set the final	wordtype for this word
			sentence.setWordType(wordtype,nextword);
			//look to the next word	along
			nextword++;
			
			switch(wordtype)
			{
				case Word.WT_VERB:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a verb");
					}
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )	
						return true;
					
					if (
						(conjecture(sentence,Word.WT_ADVERB,nextword,trace)) ||
						(conjecture(sentence,Word.WT_DIRECTION,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_STRING,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ARTICLE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_PRONOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) ||
						(conjecture(sentence,Word.WT_SUPERLATIVE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	||
						(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace)) ||
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	)
						return true;
					break;
				case Word.WT_ADVERB:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its an adverb");
					}
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )	
						return true;
					
					if(	(conjecture(sentence,Word.WT_ARTICLE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_VERB,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	||
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) ||
						(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADVERB,nextword,trace)) )
						return true;
					break;
				case Word.WT_ADJECTIVE:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its an	adjective");
					}
					
					if(	(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_PRONOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	||
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	)
						return true;
					
					else
						//check	for	a plural noun
						if(sentence.pluralCheckingFlag)
							if(	conjecture(	correctPluralWord(sentence, nextword) , Word.WT_NOUN , nextword , trace ) )
								return true;
					break;
					
				case Word.WT_NOUN:
					if(trace)
					{	
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a noun");
					}
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )	
						return true;
					
					if(	(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ARTICLE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace)) ||
						(conjecture(sentence,Word.WT_STRING,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NEGATION,nextword,trace)) || 
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADVERB,nextword,trace)) ) 
					return true;
					break;
					
				case Word.WT_NEGATION:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a negation");
					}
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )
						return true;
					
					if(	(conjecture(sentence,Word.WT_ARTICLE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace)) || 
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	) 
					return true;
					break;
				
				case Word.WT_PRONOUN:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a pronoun");
					}
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )
						return true;
					
					if(	(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace)) || 
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	) 
					return true;
					break;
					
				case Word.WT_PREPOSITION:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t"); 
						System.out.println("its a preposition");
					}
					if(	(conjecture(sentence,Word.WT_ARTICLE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_PRONOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	)
						return true;
					break;
				
				case Word.WT_STRING:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a string");
					}
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )	
						return true;
					
					if(	(conjecture(sentence,Word.WT_STRING,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace))  )
						return true;
					break;
				case Word.WT_DIRECTION:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a direction");
					}
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )	
						return true;
					
					if( (conjecture(sentence,Word.WT_ADVERB,nextword,trace)) ||
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) )
						return true;
					break;
				case Word.WT_SUPERLATIVE:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a superlative");
					}
					
					if(	(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_SUPERLATIVE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	||
						(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace))  )
						return true;
					break;
				case Word.WT_CONNECTOR:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a connector");
					}
					
					if(	(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_SUPERLATIVE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ARTICLE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) ||
						(conjecture(sentence,Word.WT_VERB,nextword,trace)) || 
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	) 
						return true;
					break;
				case Word.WT_NUMERIC:
					if(trace)
					{	
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its a numeric");
					}
					
					//the following	line basically means that we should	look out for plural	words coming next.
					sentence.pluralCheckingFlag = true;
					
					// check for an end of sentence
					if(	checkForEOL(sentence,nextword) )	
						return true;
						
					if(	(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ARTICLE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	 ||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) ||
						(conjecture(sentence,Word.WT_NEGATION,nextword,trace)) ||
						(conjecture(sentence,Word.WT_CONNECTOR,nextword,trace))	||
						(conjecture(sentence,Word.WT_PREPOSITION,nextword,trace)) )
						return true;
					else
						//no noun found, but what if we	remove the 's' or 'es' from	the	end	?
						if(sentence.pluralCheckingFlag)
							if(	conjecture(	correctPluralWord(sentence, nextword), Word.WT_NOUN, nextword, trace ) )
								return true;
					break;
				
				//###article is	near enough	redundant, remove soon !
				//### ### no, no, no - well maybe
				case Word.WT_ARTICLE:
					if(trace)
					{
						for(int i=0;i<nextword;i++) System.out.print("\t");
						System.out.println("its an article");
					}
					
					if(	(conjecture(sentence,Word.WT_NOUN,nextword,trace)) ||
						(conjecture(sentence,Word.WT_SUPERLATIVE,nextword,trace)) ||
						(conjecture(sentence,Word.WT_ADJECTIVE,nextword,trace))	||
						(conjecture(sentence,Word.WT_NUMERIC,nextword,trace)) )
						return true;//*/
					else
						//no noun found, but what if we	remove the 's' or 'es' from	the	end	?
						if(sentence.pluralCheckingFlag)
							if(	conjecture(	correctPluralWord(sentence, nextword), Word.WT_NOUN, nextword, trace ) )
								return true;
					break;
					
				case Word.WT_STOPWORD:
					//simply pass on to	the	next word, as if this wasn't here !
					nextword++;
					return true;
				default:
					break;
			}
		}
		return false;
	}
	
	/**
	* Returns a	new	updated	word structure,	based on considering the word a
	* plural.
	*
	* @param sentence				the sentence to check for
	* @param position				the position of the word suspected of plurality
	*/
	private static Sentence correctPluralWord(Sentence sentence,int position)
	{
	    String pluralword = sentence.getWordValue(position);
	    
	    //check for 's' plurals
	    if( pluralword.charAt(pluralword.length()-1) ==	's'	)
	    {
	        sentence.getWord(position).setPossibleWordTypes(vocabulary.composeWordTypes( pluralword.substring(0,pluralword.length()-1),sentence.getWord(position).getPossibleWordTypes() ) );
	        sentence.setWordValue( pluralword.substring(0,pluralword.length()-1), position );
	    }
	    
	    //check for 'es' plurals
	    if( (pluralword.charAt(pluralword.length()-2) == 'e') && (pluralword.charAt(pluralword.length()-1) == 's'))
	    {
	        sentence.getWord(position).setPossibleWordTypes( vocabulary.composeWordTypes(pluralword.substring(0,pluralword.length()-2),sentence.getWord(position).getPossibleWordTypes()) );
	        sentence.setWordValue( pluralword.substring(0,pluralword.length()-2), position );
	    }//*/
	    return sentence;
	}
	
	/**
	* Used mostly by the Conjecture method, checkForEOL wraps up a simple condition
	*  which makes for better readability.
	* It returns true if the supplies position integer exceeds the size of the 
	*  supplied sentence (signifiying EOL), and false if the position is still
	*  within the boundries.
	*
	* @param sentence				the sentence to check
	* @param position				check whether this position exceeds the sentences size
	* @returns						true if the position is beyond the sentences size, false otherwise
	*/
	private	static boolean checkForEOL(Sentence sentence,int position)
	{
		if(position	>= sentence.size())
			return true;
		return false;
	}
	
	/**
	* The Parser keeps track of a 'Pronoun', that is, the last thing
	*  that was mentioned in the sentence.
	* Most of the Pronoun handling occurs either when the NounPhrases
	*  are compiled, or in the BinderEnumeration when the NounPhrases
	*  are interpreted into Atoms.
	*
	* @returns						this parsers current Pronoun Atom
	*/
	public Atom getPronoun()
	{
		return pronoun;
	}
	
	/**
	* The Parser keeps track of a 'Pronoun', that is, the last thing
	*  that was mentioned in the sentence.
	* Most of the Pronoun handling occurs either when the NounPhrases
	*  are compiled, or in the BinderEnumeration when the NounPhrases
	*  are interpreted into Atoms.
	*
	* @parama pronoun				sets this parsers pronoun atom
	*/
	public void setPronoun(Atom pronoun)
	{
		this.pronoun = pronoun;
	}
	
	/**
	* Returns this parsers 'actor'. The actor is the player representation
	*  within the world, through which events happen.
	*
	* @returns						this parsers 'actor'
	*/
	public Atom getActor()
	{
		return actor;
	}
	
	/**
	* Sets this parsers 'actor'. The actor is the player representation
	*  within the world, through which events happen.
	* @param actor					sets this parsers actor
	*/
	public void setActor(Atom actor)
	{
		this.actor = actor;
	}
	
	/**
	* Returns this parsers World object.
	*
	* @returns						this parser instances World object.
	*/
	public World getWorld()
	{
		return world;
	}
	
	/**
	* Sets this parsers world.
	*
	* @param world					the world to associate with this parser instance
	*/
	public void setWorld(World world)
	{
		this.world = world;
	}
	
	/** 
    * Create a string tokenizer for synonym patterns.
    *
    * @param pattern			a string to chop up
    * @returns					a StringTokenizer ready to go
    */
    protected StringTokenizer newSynonymTokenizer(String pattern)
    {
        return new StringTokenizer(pattern, " ,;\"");
    }
    
    /**
    * Adds a verb
    * <p>
    * Verbs are defined in the format: <br>
    * actionID "synonym;synonym" "arg">propertyID "arg">propertyID "arg">propertyID
    *
    * The first argument is expected to be the synonyms list, with quotes preferably
    *  the second and after arguments are all stuff
    *
    * @param actor				the actor who commissioned this command
    * @param args				the first argument contains synonyms, the following arguments
    *							contain template definitions
    */
    protected void addVerb(String args[]) 
    {
        if (args.length < 2)
        {
        	actor.output("Badly formed Verb definition: missing template");
            throw new ParserException(" [vocab] Badly formed Verb definition: missing template");
        }
        // Create the verb from the very first argument, which is the actionID
        Verb verb = new Verb();
        
        //the first token will contain the synonyms of the new Verb
        // in the form "syn1,syn2,syn3"
        String synonyms;
        
        // where do the template definitions begin ? depends on prescence of Privilege indicator.
        int templateStart = 1;
        String verbName="";
        
		//if a privilege has been set....
		if( Privilege.contains( interpretPrivilegeProperty(args[0]) ) )
		{
			verb.setPrivilege( Privilege.getValue(interpretPrivilegeProperty(args[0])) );
			synonyms = args[1];
			templateStart = 2;
		}
		//if a privilege has not been set
		else
		{
			synonyms = args[0];
			templateStart = 1;
		}
        	
        StringTokenizer st = newSynonymTokenizer(synonyms);
        
        while(st.hasMoreTokens())
        {
            verbName = st.nextToken().toLowerCase();
            
            //check this verb isn't already in the vocabulary
            Object o = vocabulary.getVerb(verbName);

            // when in fact it should have.
            if( o != null )
            {
            	actor.output("Verb '" + verbName + "' is already defined. (Clue)" + synonyms);
                throw new ParserException(" [vocab] Verb '" + verbName + "' is already defined. (Clue)" + synonyms);
            //if it isn't, add the verb
            }
            else
            {
            	vocabulary.setVerb( verbName, verb );
                vocabulary.addWordToDictionary(verbName,Word.WT_VERB);
                actor.output("Verb '" + verbName + "'(" + Privilege.getDescription(verb.getPrivilege()) + ") added.");
            }
        }
               	
        //begin decomposing the template to action definitions
        // the first part of each arg will be the template. ie "current at thing"
        // the second part will be a mapping to propertyID. ie >look
        for (int i = templateStart; i < args.length; i++)
        {
        	
            // Split the string into template and property ID
            String uncutTemplate = args[i];
            int splitPos = uncutTemplate.indexOf(">");
            if (splitPos < 0)
            {
            	actor.output("Badly formed Verb definition: missing > in template: " + synonyms);
                throw new ParserException("Badly formed Verb definition: missing > in template: " + synonyms);
            }
            String template = uncutTemplate.substring(0, splitPos);
            String propertyID = uncutTemplate.substring(splitPos + 1);
            
            //we add the template, plus the propertyID after it.
            //pass the template string plus the propertyID to the new verbtemplate
            VerbTemplate verbTemplate = new VerbTemplate(vocabulary, template, propertyID);
            
            //If the verbTemplate indicates a Communicative type, set the flag in the verb
            if( verbTemplate.checkForArgType(VerbTemplate.COMMUNICATIVE,0) )
            	verb.setCommunicative(true);
            	
            verb.addTemplate(verbTemplate);
        }
    }
    
    /** 
    * Remove a verb from the Vocabulary
    *
    * @param actor				the actor who commissioned this command
    * @param synonyms			a list of verbs to remove
    */
	protected void removeVerb(String synonyms)
    {
        // Templates are delimited with semicolon
        StringTokenizer st = newSynonymTokenizer(synonyms);

        // Remove the verb
        while (st.hasMoreTokens())
        {
            String verbName = st.nextToken().toLowerCase();
            vocabulary.removeVerb(verbName);
            actor.output("Verb '" + verbName + "' removed.");
        }
    }
    
    /**
    * Returns the privilege string inbetween the quotes
    *
    * @param input				a string hopefully containing a privilege indicator
    */
    protected static String interpretPrivilegeProperty(String input)
    {
    	if( input.startsWith("(") && input.endsWith(")") )
    		return input.substring(1,input.length()-1);
    	return null;
    }
    
    /**
    * Adds an adjective to the list along with the property ID that must be
    * tested to enquire if an atom matches an adjective.
    *
    * Example:
    *       addAdjective("red", "is_red");
    *       addAdjective("crimson", "is_red");
    *
    * @param adjective			 the adjective to add
    * @param propertyID			 the internal system ID of the adjective
    */
    public void addAdjective(String adjective, String propertyID)
    {
  	
        //add this adjective to the word/wordclass lookup table
        vocabulary.addWordToDictionary(adjective, Word.WT_ADJECTIVE);

        //add this adjective to the adjective list as long as it doesn't already exist
        String test = vocabulary.getAdjective(adjective);
        if( test == null )
        {
        	vocabulary.addAdjective(adjective, propertyID);
        	actor.output("Adjective '" + adjective + "' added.");
        }   
    }
    
    /**
    * Adds an adverb defined in a special format to the vocabulary
    * Form:
    * 	verb+adverb>modified_verb
    *
    * @param args - the first argument is the verb it will affect
    *             - the second argument is the adverb itself
    *             - the third argument is the resultant modified verb
    */
    public void addAdverb(String argument)
    {
    	// Tokenize the argument
    	StringTokenizer st = newSynonymTokenizer(argument);
	        
        String adverbName = "", verbName = "", newVerbName = "";
        
        while(st.hasMoreTokens())
        {
            verbName = st.nextToken().toLowerCase();
            
            if( ! st.nextToken().equals("+") )
            	throw new ParserException(" [vocab] Badly formed Adverb definition : expected '+' ");
            
            adverbName = st.nextToken().toLowerCase();
            
            if( ! st.nextToken().equals(">") )
            	throw new ParserException(" [vocab] Badly formed Adverb definition : expected '>' ");
            
            newVerbName = st.nextToken().toLowerCase();
        }
        
     	//try and get the adverb from the Dictionary 
        Adverb existingAdverb = vocabulary.getAdverb(adverbName);
        
        if(existingAdverb == null)
        {
        	//if the adverb does not exist, create a new one and add it to the adverbs
	        Adverb adverb = new Adverb();
	        adverb.addNewMapping( verbName, newVerbName );
	        vocabulary.addAdverb(adverbName, adverb);
	        
	        actor.output("Adverb '" + adverbName + "' added.");
	    }
	    else
	    {
	    	//otherwise, just add a new mapping to the existing adverb
	    	existingAdverb.addNewMapping( verbName, newVerbName );
	    	
	    	//report our dirty deed
	    	actor.output("New adverb (" + adverbName + ") mapping on " + verbName + " added"); 
	    }
        //add this adverb to the word/wordclass lookup table
        vocabulary.addWordToDictionary(adverbName , Word.WT_ADVERB);
	
    }
    
    /**
    * Adds generic type words to the vocabulary
    *
    * @param pattern		a word or list of words to add
    * @param wordtype		the type of the added words
    */
    protected void addGenericWord(String pattern, String wordtype)
    {
    	//tokenize the words and add each word in turn to the master word list
        StringTokenizer st = newSynonymTokenizer(pattern);
        String word = "";
        while(st.hasMoreTokens())
        {
        	word = st.nextToken();
			//add the word to the dictionary
            vocabulary.addWordToDictionary( word, Word.stringToWordType(wordtype) );
            //report our dirty deed to the initiator
        	actor.output("Generic word " + word + " added, of type " + wordtype + ".");
        }
    }
    
    /**
    * Removes Generic type words from the vocabulary
    *
    * @param pattern			a word or list of words to remove
    */
    protected void removeGenericWord(String pattern)
    {
    	//tokenize the given string to a more useful form
        StringTokenizer st = newSynonymTokenizer(pattern);
        String word;
        while(st.hasMoreTokens())
        {
        	word = st.nextToken();
            vocabulary.removeWordFromDictionary(word);
            actor.output(word + " removed.");
        }
    }
    /**
    * Add a 'raw' verb.
    * 
    * Raw verbs are not fully parsed, they are tokenised and sent to the 
    * event handler. They are useful for low-level commands and commands which
    * take arguments that don't fit in to the conventional framework.
    * 
    * This command adds the raw verb (and its synonyms) to the vocabulary and
    * sets a property in the root atom.
    *
	* @param actor				the actor who initiated this command
	* @param args				an array containing the parameters: pattern, propertyID and propertyValue
	*  							see addRawVerb above
	*/
    public void addRawVerb(String args[])
    {
    	/*if (args.length < 1)
    	{
    		actor.output("Usage: !RAWVERB <privilege> verb-pattern <propertyID> <value> :" + args[0] + " "+ args[1]);
            throw new ParserException(" [vocab] Usage: !RAWVERB verb-pattern propertyID value");
    	}//*/
    	
    	//### potential for huge errors here
		String temp = interpretPrivilegeProperty(args[0]);
		int privilege = -1;
		
		//try and interpret the privilege into an integer
		if( temp != null && Privilege.contains(temp) )
    		privilege = Privilege.getValue(temp);
		else
			temp = null;
			
		//if a privilege has been set....
		if( temp != null )
		{
			if( args.length == 4)
        		addRawVerb( actor, privilege, args[1], args[2], AtomData.parse(args[3], actor.getWorld()) );
        	else if( args.length == 2 )
        		addRawVerb( actor, privilege, args[0], null, null );
        }
        //if a privilege has not been set...
        else
        {
        	if( args.length == 3 )
        		addRawVerb( actor, -1, args[0], args[1], AtomData.parse(args[2], actor.getWorld()) );
        	else if( args.length == 1 )
        		addRawVerb( actor, -1, args[0], null, null );
        }
    }
    
    /**
    * Add a 'raw' verb.
    * 
    * Raw verbs are not fully parsed, they are tokenised and sent to the 
    * event handler. They are useful for low-level commands and commands which
    * take arguments that don't fit in to the conventional framework.
    * 
    * This command adds the raw verb (and its synonyms) to the vocabulary and
    * sets a property in the root atom.
    *
    * @param privilege			required level of privilege to use this command
    * @param pattern			roughly equivilent to verb synonyms
    * @param propertyID			the internal property identifier of this rawverb
    * @param propertyValue		a value for the propertyID
    */
    public void addRawVerb(Atom actor, int privilege, String pattern, String propertyID, Object propertyValue)
    {
    	// Ensure case-insensitivity
    	pattern = pattern.toLowerCase();
    	
    	RawVerb rawVerb = new RawVerb();
    	
    	// Scan for recognisable privilege level
    	if( privilege != -1 )
    		rawVerb.setPrivilege( privilege );
    		
    	// Insert recognisable priv. level
    	if( propertyID != null )
    		rawVerb.setPropertyID( propertyID );
    	
    	// Split up the pattern into verb names
    	StringTokenizer st = newSynonymTokenizer(pattern);
    	while(st.hasMoreTokens())
    	{
    		
    		String verbName = st.nextToken().toLowerCase();
    		
    		// check to see whether the rawverb is already defined
    		if(vocabulary.getRawVerb(verbName) != null)
    		{
    			actor.output("RawVerb '" + verbName + "' is already defined");
    			throw new ParserException("RawVerb '" + verbName + "' is already defined");
    		}
    		vocabulary.addRawVerb(verbName, rawVerb);
    		actor.output("RawVerb '" + verbName + "'(" + Privilege.getDescription(rawVerb.getPrivilege()) + ")" + " added.");
    	}
    	//set the rawverb on the root atom
    	if( propertyID != null && propertyValue != null )
	    	//define the property
	    	actor.getWorld().getRoot().setProperty(propertyID, propertyValue);
    }//*/
    
    /**
    * Handles the !Priv command. Normally we would like this to be a JavaAction,
    * but because we cannot pass the parsers privilege property through an event,
    * this is impossible
    */
	protected void handlePrivilegeCommand(String args[])
	{
		//report the users privilege level
    	if( args.length == 0 )
    	{
    		actor.output("Privilege level set at " + Privilege.getDescription(privilege) + ".");
			throw new ParserPipelineException("user privilege report request found");
		}
		Privileged request;
		
		//try for a verb
		request = (Privileged)vocabulary.getVerb( args[0] );

		if( request != null )
    	{
    		//probably trying to change the privilege level
    		if( args.length == 2 )
    		{
    			int newPrivilege = Privilege.getValue(args[1]);
    			actor.output( args[0] + " privilege previously set at " + Privilege.getDescription(request.getPrivilege()) + ".");
    			if( newPrivilege != -1 )
    			{
    				request.setPrivilege(newPrivilege);
    				actor.output( args[0] + " privilege set to " + Privilege.getDescription(request.getPrivilege()) + ".");
    			}
    			throw new ParserPipelineException("verb privilege set request found");
    		}
			//report the verbs privilege level
    	
    		actor.output( args[0] + " privilege set at " + Privilege.getDescription(request.getPrivilege()) + ".");
    		throw new ParserPipelineException("verb privilege report request found");
    	}
    	//try a rawverb.
		else
			request = (Privileged)vocabulary.getRawVerb( args[0] );
	}
}
