// $Id: Vocabulary.java,v 1.35 1999/04/29 10:01:36 jim Exp $
// Vocabulary class for multi-user games (semi-temporary)
// James Fryer, 16 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.*;

import com.ogalala.util.Debug;
import com.ogalala.util.Privilege;

/**
* The Vocabulary is used by several classes, most notably the Parser.
*
* 	Generic Words 			-
*  								Add Generic Word syntax:
*
*									!Word words wordtype
*
*  								Where words is a delimited list of strings
*  								Where wordtype is a single string matching to one of the known 
*								types.
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
*	Adding Adverbs			-	Adverbs are recognised by the parser as words which will translate the given
*								verb into a new form. For example, the presence of the adverb 'quickly' and the
*								verb 'walk' will result in the verb being translated into 'run'.
*								The syntax is as follows
*		
*									!Adverb "adverb + verb > translated-verb"
*
*								Where adverb,verb and translated-verb are all single strings.
*
*	Adding Nouns			-
*
*	Removing Nouns			-
*


*/
public class Vocabulary implements Serializable
{
    public static final int serialVersionUID = 1;
    
	//-------------------------- Properties -----------------------------//
	
    /**
    * Master list of all words, along with an integer bitmap
    * holding all possible word types (Noun,verb etc).
    */
    protected Dictionary words = new Hashtable();

    /** List of Verbs
    */
    protected Dictionary verbs = new Hashtable();

	/** List of Adverbs
	*/
	protected Dictionary adverbs = new Hashtable();
	
    /** List of Nouns
    */
    protected Dictionary nouns = new Hashtable();
    
    /** Reverse lookup table for nouns from atoms.
    */
    protected Dictionary reverseNouns = new Hashtable();

    /** List of Adjectives
    */
    protected Dictionary adjectives = new Hashtable();

    /** List of Raw verbs
    */
    protected Dictionary rawVerbs = new Hashtable();
	
	/** 
	* Words such as 'all' and 'every' when used as numerics get replaced
	*  with this lovely number.
	*/
	public static final int INFINITY = 666;
	
	/** Suffix for the vocab file
	*/
	private static final String SUFFIX = ".vocabulary";

	
	
    //------------------------------- Constructor ---------------------------------//
    public Vocabulary()
    {
    }

    //--------------------------- WordType Methods --------------------------------//
    /**
    * Returns a list of possible Word Types (Verb,adjective,Noun) in the
    * form of a bitmap, from a given word.
    * @param 						input the word we wish to know the class of
    * @returns 						the integer bitmap containing all possible word types
    */
    public int getWordTypes(String word)
    {
        int result = 0;
        Integer wordValue = (Integer)words.get(word.toLowerCase());
        if (wordValue != null)
            result = wordValue.intValue();
        return result;
    }
    
    /**
    * Gets a particular word, and returns its list of possible word types OR'd
    *  with a supplyed list of possible words.
    * Used almost purely for plural words.
    * @param word					the word to look up
    * @param possiblewordlist		the list gets combined with the looked up list
    * @returns						a new list of possible words
    */
    public int composeWordTypes(String word, int possiblewordlist)
    {
        word = word.toLowerCase();
    	
    	if( ((Hashtable)words).containsKey(word))
    		return possiblewordlist |= ((Integer)words.get(word)).intValue();
    		
    	return possiblewordlist;
    }


    /**
    * Adds a word into the words dictionary. This dictionary is used by
    *  the parser to find out the type of word.
    *
    * @param word 					the string value of the word
    * @param type 					the word type (verb,noun) expressed as a Constant (see Word.java)
    */
    public void addWordToDictionary(String word,int type)
    {
        word = word.toLowerCase();

        //do we already have this word in our dictionary ?
        if(((Hashtable)words).containsKey(word))
        {
            //we do, so...
            //get the bitmap value from the dictionary
            int value = ((Integer)words.get(word)).intValue();
            //add the new type to the integer
            value |= type;
            //place the word, and its new value back into the dictionary.
            words.put(word,new Integer(value));
        }
        //otherwise, this word has not yet been put into the vocab, so we must
        //add it along with its word type
        else
            words.put(word,new Integer(type));
    
    }

    /**
    * Removes a given word from the master words dictionary
    *
    * @param word 					the word to remove
    */
    public void removeWordFromDictionary(String word)
    {
        word = word.toLowerCase();
        words.remove(word);
    }
    
    
    //------------------------- Generic Word Methods -------------------------------//
    
    /**
    *
    */
    public Adverb getAdverb(String adverbName)
    {
    	return (Adverb)adverbs.get(adverbName);
    }
    
    /**
    *
    */
    public void addAdverb(String adverbName, Adverb adverb)
    {
    	adverbs.put(adverbName, adverb);
    }

    
    //--------------------------- Adjective Methods --------------------------------//
    
    /**
    *
    */
    public void addAdjective(String adjective, String propertyID)
    {
    	adjectives.put(adjective, propertyID);
    }
    
    /**
    * Get the property name corresponding to an adjective. Null if not found.
    * Example:<br>
    *    getAdjective("crimson");        // Returns 'is_red'
    */
    public String getAdjective(String adjective)
    {
        return (String)adjectives.get(adjective);
    }
    
    /**
    * Remove an adjective from the vocabulary.
    *
    * @param adjective				the adjective to remove
    */
    public void removeAdjective(String adjective)
    {
    	adjectives.remove(adjective);
        removeWordFromDictionary(adjective);
    }
    
    //------------------------------------------------------------------------------//


    //--------------------------- Adverb Methods --------------------------------//
   
    
    /**
    * Removes adverbs from the vocabulary.
    * @param actor				the actor who commissioned this operation
    * @param synonyms			a delimited list of adverbs to remove.
    */
    public void removeAdverb(Atom actor, String synonyms)
    {
    	//seperate out the delimted list of strings
    	StringTokenizer st = newSynonymTokenizer(synonyms);
    	
    	//remove each of the adverbs
    	while(st.hasMoreTokens())
    	{
    		String adverbName = st.nextToken().toLowerCase();
    		adverbs.remove(adverbName);
    		removeWordFromDictionary(adverbName);
    		actor.output("Adverb '" + adverbName + "' removed.");
    	}
    }
    	
    /**
    * Fetches an adverb (if it exists), and attempts to return a
    * successful adverb/verb mapping to it.
    * @param adverb the adverb to search for
    * @param verb the verb to transform
    * @returns a new transformed verb
    */
    public String applyAdverbMapping(String adverb, String verb)
    {
    	Adverb target = (Adverb)adverbs.get(adverb.toLowerCase());
    	if(target == null)
    		return "";
    	else
    		return target.getVerbName(verb);
    }
    
    //------------------------------------------------------------------------------//



    //-------------------------------- Verb Methods -------------------------------//

    
    /**
    * Removes a verb from the Hashtable, and also removes
    * it as a word
    *
    * @param verbName			the verb to remove
    */
    public void removeVerb(String verbName)
    {
    	verbs.remove(verbName);
    	removeWordFromDictionary(verbName);
    }

    /**
    * Removes all Templates from the Verb which have a given propertyID
    *
    * @param actor				the actor who commissioned this command
    * @param verbSynonym		identifies the Verb to remove the templates from
    * @param verbPropertyID		Templates with this propertyID will be removed.
    */
    public void removeTemplate(Atom actor, String verbSynonym, String verbPropertyID)
    {
    	Verb target = (Verb)verbs.get(verbSynonym);
    	
    	//we tell the Verb to remove all templates that have this propertyID
    	target.removeTemplate(verbPropertyID);
    }
    
    
	/**
	* Returns a verb Object from a string
	* 
	* @param verbName			the string referring to a Verb object
	* @returns					the Verb object corresponding to the given string
	*/
    public Verb getVerb(String verbName)
    {
        // Ensure case-insensitivity
        verbName = verbName.toLowerCase();
        return (Verb)verbs.get(verbName);
    }
	
	/**
	* A direct accessor to the Verbs Hashtable
	*/
	public void setVerb(String verbName, Verb verb)
	{
		verbs.put(verbName, verb);
	}
	/** 
	* Returns a verb as long as the supplied privilege is greater
	* or equal to the verbs privilege setting.
	*
	* @param verbName			the string referring to a Verb object
	* @param privilege			a minimum privilege level the verb has to be
	* @returns					the Verb object corresponding to the given string
	*/
	public Verb getVerb(String verbName, int privilege)
	{
		Verb result = (Verb)verbs.get(verbName.toLowerCase());
		if( result.getPrivilege() <= privilege )
			return result;
		else return null;
	}
	
    /**
    * Adds a rawverb to the vocabulary
    *
    * @param rawVerbName		the raw verb identifier
    * @param rawVerb			the raw verb Object
    */
    public void addRawVerb(String rawVerbName, RawVerb rawVerb)
    {
    	rawVerbs.put(rawVerbName, rawVerb);
    	addWordToDictionary(rawVerbName, Word.WT_RAWVERB);
    }
    
    /**
    * Get the property ID of a 'raw' verb.
    *
    * @param rawVerbName		A string indicating the required RawVerb Object
    * @param privilege			A minimum privilege level the verb has to be
    * @returns 					A property ID, or null if the raw verb doesn't exist.
    */
    public RawVerb getRawVerb(String rawVerbName)
    {
    	return (RawVerb)rawVerbs.get( rawVerbName.toLowerCase() );
    }
    
    
    /** 
	* Returns a rawverb as long as the supplied privilege is greater
	* or equal to the rawverbs privilege setting.
	*
	* @param rawVerbName		A string indicating the required RawVerb Object
	* @param privilege			A minimum privilege level the verb has to be
    * @returns 					A property ID, or null if the raw verb doesn't exist.	
	*/
	public String getRawVerb(String rawVerbName, int privilege)
	{
		RawVerb result = (RawVerb)rawVerbs.get(rawVerbName.toLowerCase());
		if( result.getPrivilege() <= privilege )
			return result.getPropertyID();
		else return null;
	}
	
    //-------------------------------- Noun Methods -------------------------------//
    
    /** 
    * Adds a noun
    *
    * @param actor				the actor who initiated the addNoun command
    * @param atom				the atom to which the Nouns will attach
    * @param synonyms			a delimited list of noun strings
    */
    public boolean addNoun(Atom actor, Atom atom, String synonyms)
    {
        // Nouns are delimited
        StringTokenizer st = newSynonymTokenizer(synonyms.toLowerCase());
        while (st.hasMoreTokens())
        {
            // Get the next noun
            String noun = st.nextToken().intern();
            Atom existingAtom = (Atom)nouns.get(noun);
			
            // If a noun of this name is already defined
            if (existingAtom != null)
            {
                // If the existing noun points to an ancestor of the new definition, we can ignore this
                if (atom.isDescendantOf(existingAtom))
                    return false;

                // Else it's an attempt at noun redefinition.
                else
                {
                	if ( actor != null )
                		actor.output("Noun \"" + noun + "\" is already defined");
                    return false;
                    //throw new ParserException("Noun \"" + noun + "\" is already defined");
                }
            }
            // Add the noun to the dictionary
            nouns.put(noun, atom);
            //add the noun to the master words dictionary
            addWordToDictionary(noun,Word.WT_NOUN);
            
            // add the noun to the reverse lookup table
            // this is a one-to-many mapping, so the nouns
            // are kept in a Vector
            if ( reverseNouns.get(atom) == null )
            {
            	Vector nounList = new Vector();
            	nounList.addElement( noun );
            	reverseNouns.put ( atom, nounList );
            }
            else
            	((Vector) reverseNouns.get(atom)).addElement( noun );
        }  
        return true;
    }

    /** 
    * Remove a noun
    *
    * @param actor				the actor who initiated the command
    * @param synonyms			a delimited list of string nouns to remove
    */
    public void removeNoun(Atom actor, String synonyms)
    {
        // Nouns are delimited with semicolon
        StringTokenizer st = newSynonymTokenizer(synonyms.toLowerCase());
        while (st.hasMoreTokens())
        {
            String noun = st.nextToken().intern();
            Atom nounAtom = (Atom) nouns.remove(noun);
            
            // remove the noun from the atom's entry in the reverse lookup table
            if ( nounAtom != null )
            {
            	Vector nounList = (Vector) reverseNouns.get ( nounAtom );
            	if ( nounList != null )
            	{
	            	nounList.removeElement( noun );
	            	if ( nounList.size() == 0 )
	            		reverseNouns.remove ( nounAtom );
	            }
            }
            
            //remove the noun from the master words dictionary
            removeWordFromDictionary(noun);
            
            if ( actor != null )
            	actor.output("Noun '" + noun + "' removed.");
        }
    }

    /** 
    * Get a noun
    *
    * @param nounName			the string name of the noun to return
    * @returns					the atom to which the given noun refers
    */
    public Atom getNoun(String nounName)
    {
        return (Atom)nouns.get(nounName.toLowerCase());
    }
    
    /**
    * Enumerate all defined nouns.
    */
   	public Enumeration getNouns ()
   	{
   		return nouns.keys();
   	}
    
    /**
    * Get a list of the nouns defined by a particular atom.
    *
    * @param atom       The atom for which all locally-defined nouns are sought.
    * @returns          A list of all the nouns defined by that atom, or <tt>null</tt>
    *                   if the atom defines no nouns.
    */
    public Vector getAtomNouns(Atom atom)
    {
    	return (Vector) reverseNouns.get(atom);
    }

	//----------------------------- Utility Methods ----------------------------------//
	
	/**
	* A not terribly sophisticated way of turning words into
	*  numbers.
	* @param word the word to convert
	* @returns the int value of the word.
	*/
	public static int getNumberFromWord(String word)
	{
        word = word.toLowerCase();
		if( "zero".equals(word) )			return 0;
		else if( "one".equals(word) )		return 1;
		else if( "once".equals(word) )		return 1;
		else if( "two".equals(word) )		return 2;
		else if( "twice".equals(word) )		return 2;
		else if( "three".equals(word) )		return 3;
		else if( "thrice".equals(word) )	return 3;
		else if( "four".equals(word) )		return 4;
		else if( "some".equals(word) )		return 4;
		else if( "five".equals(word) )		return 5;
		else if( "six".equals(word) )		return 6;
		else if( "seven".equals(word) )		return 7;
		else if( "eight".equals(word) )		return 8;
		else if( "nine".equals(word) )		return 9;
		else if( "ten".equals (word) )		return 10;
		else if( "eleven".equals(word) )	return 11;
		else if( "twelve".equals(word) )	return 12;
		else if( "thirteen".equals(word) )	return 13;
		else if( "fourteen".equals(word) )	return 14;
		else if( "fifteen".equals(word) )	return 15;
		else if( "sixteen".equals(word) )	return 16;
		else if( "seventeen".equals(word) )	return 17;
		else if( "eighteen".equals(word) )	return 18;
		else if( "nineteen".equals(word) )	return 19;
		else if( "twenty".equals(word) )	return 20;
		else if( "thirty".equals(word) )	return 30;
		else if( "forty".equals(word) )		return 40;
		
		else if( "all".equals(word) )		return INFINITY;
		else if( "every".equals(word) )		return INFINITY;
		else if( "everything".equals(word) )return INFINITY;
		
		return 0;
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
    * Convert a list of words into a synonym string.
    *
    * @param wordList           A list of words to be turned into a synonym string.
    */
    public static String toSynonym ( Vector wordList )
    {
    	if ( wordList == null || wordList.size() == 0 )
    		return "";
    	
    	String result = "";
    	Enumeration wordEnum = wordList.elements();
    	while ( wordEnum.hasMoreElements() )
    	{
    		result += (String) wordEnum.nextElement();
    		if ( wordEnum.hasMoreElements() )
    			result += ",";
    	}
    	
    	return result;
    }
    
    /**
	* Returns the size of the words Hashtable
	*
	* @returns				the size of the words Hashtable
	*/
	public int getWordsSize()
	{
		return words.size();
	}
	
	/**
	* Returns the size of the verbs Hashtable
	*
	* @returns				the size of the verbs Hashtable
	*/
	public int getVerbsSize()
	{
		return verbs.size();
	}
	
	/**
	* Returns the size of the adverbs Hashtable
	*
	* @returns 				the size of the adverbs Hashtable
	*/
	public int getAdverbsSize()
	{
		return adverbs.size();
	}
	
	/**
	* Returns the size of the nouns Hashtable
	*
	* @returns 				the size of the nouns Hashtable
	*/
	public int getNounsSize()
	{
		return nouns.size();
	}
	
	/**
	* Returns the size of the adjectives Hashtable
	*
	* @returns 				the size of the adjectives Hashtable
	*/
	public int getAdjectivesSize()
	{
		return adjectives.size();
	}
	
	/**
	* Returns the size of the Rawverbs Hashtable
	*
	* @returns				the size of the Rawverbs Hashtable
	*/
	public int getRawVerbsSize()
	{
		return rawVerbs.size();
	}
   
}
