// $Id: Word.java,v 1.14 1999/04/29 10:01:36 jim Exp $
// Encapsulates a Single word in a Natural Language Sentence
// Alexander Veenendaal, 10 September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.util.Vector;

/**
* Word encapsulates a natural language word, mostly for the use of Sentence,
* which is in turn for the use of the Parser.
* Along with the storage of the words string value, its word type or class is
* stored as well. ie. Verb or Adjective.
* As well as the storage of the type of word, there is accomodation of a number
* of possible Word types, which is useful when the word is still ambiguous.
* The final major attribute caters for the concept of Words signifiying numbers
* , so a numerical field value is available.
*/
public class Word
    implements java.io.Serializable
{
    public static final int serialVersionUID = 1;
    
	/**
	* A full list of all the word types a word can be.
	* Heavily used by other parts of this package.
	*/
    public static final int WT_UNKNOWN          = 0;    
    public static final int WT_VERB             = 1;  
    public static final int WT_ADVERB           = 2;  
    public static final int WT_ADJECTIVE        = 4;  
    public static final int WT_NOUN             = 8;  
    public static final int WT_PRONOUN          = 16; 
    public static final int WT_PREPOSITION      = 32; 
    public static final int WT_CONNECTOR        = 64;
    public static final int WT_NEGATION         = 128;
    public static final int WT_STRING           = 256; 
    public static final int WT_NUMERIC          = 512;
    public static final int WT_ARTICLE          = 1024;
    public static final int WT_SUPERLATIVE      = 2048; 
    public static final int WT_STOPWORD         = 4096;
    public static final int WT_TERMINATOR       = 8192;
    public static final int WT_RAWVERB          = 16384;
    public static final int WT_DIRECTION		= 32768;
    
    public static final String S_UNKNOWN          	= "unknown";    
    public static final String S_VERB             	= "verb";  
    public static final String S_ADVERB           	= "adverb";  
    public static final String S_ADJECTIVE        	= "adjective";  
    public static final String S_NOUN             	= "noun";  
    public static final String S_PRONOUN          	= "pronoun"; 
    public static final String S_PREPOSITION      	= "preposition"; 
    public static final String S_CONNECTOR        	= "connector";
    public static final String S_NEGATION         	= "negation";
    public static final String S_STRING           	= "string"; 
    public static final String S_NUMERIC          	= "numeric";
    public static final String S_ARTICLE          	= "article";
    public static final String S_SUPERLATIVE      	= "superlative"; 
    public static final String S_STOPWORD         	= "stopword";
    public static final String S_TERMINATOR       	= "terminator";
    public static final String S_RAWVERB          	= "rawverb";
    public static final String S_DIRECTION			= "direction";
    
	/** The word may well have a numeric value as well eg. three = 3
	*/
	private int numvalue;
	
	/** The String value of this word
	*/
    private String value;
    
    /** Signifies the type of the word - see the list of constants
    */
    private int wordtype = 0;
    
    /** A bitfield of multiple types of word. 
    */
    private int possiblewordtypes = 0;
    
    
    //--------------------- Constructors --------------------------//
    public Word()
    {
    }
    
    /**
    * Constructs a new word with an assigned string value
    * @param value			the string value of this word
    */
    public Word(String value)
    {
        this(value,0);
    }
    
    /**
    * Constructs a new word with an assigned string value
    * and a word type.
    * @param value			the string value of this word
    * @param wordtype		the type of this word
    */
    public Word(String value,int wordtype)
    {
        this.value = value; this.wordtype = wordtype;
    }
    //-------------------- Public Methods --------------------------//

    
    //-------------------- Accessor/Mutator ------------------------//
    
    /**
    * Gets the String value of this word
    * @returns				The string value of this word
    */
    public String getValue()
    { 
    	return value; 
    }
    
    /**
    * Gets the int value of this word.
    * This is basically an overloaded function, but the extra
    * parameter was added because you can't overload return types.
    * @param num			If true, return the numeric value
    * @returns				the numeric value of this word or 0 if this word is not numeric.
    */
    public int getNumericValue()
    {
    	return numvalue;
    }
    
    /**
    * Sets the String value of this word
    * @param value			the new string value of this word
    */
	public void setValue(String value)
	{ 
		this.value = value; 
	}
	
	/**
	* Sets the numerical value of this word
	* @param numvalue		the new numeric value of this word
	*/
	public void setValue(int numvalue)
	{ 
		this.numvalue = numvalue;
		//sets the value of this Word as a string version of
		// numvalue
		value = new Integer(numvalue).toString();
	}
	
	/**
	* Returns the word type of this word
	* @returns					the word type
	*/
	public int getType()
	{ 
		return wordtype; 
	}
	
	/**
	* Sets the word type of this word
	* @param newvalue			the new type of this word
	*/
	public void setType(int newvalue)
	{ 
		wordtype = newvalue; 
	}
	
	/**
	* The possible word types integer holds a bitmask of all possible
	*  types this word could be.
	*/
    public int getPossibleWordTypes()
    { 
    	return possiblewordtypes; 
    }
    
    /**
    * Sets the possible word types for this word.
    * Note that this is different from just setting *a*
    * wordtype.
    * @param newvalue			the new set of possible word types
    */
    public void setPossibleWordTypes(int newvalue)
    { 
    	possiblewordtypes = newvalue; 
    }
    
    /**
    * Adds a possible word type to the already existing
    * possible word types list
    * @param type				a new type to add to the possibles
    */
    public void addPossibleWordType(int type)
    {
        possiblewordtypes |= type;
    }
    
    /**
    * Removes a possible word type from the already existing
    * possible word types list
    * @param type				the type to remove from the possibles
    */
    public void removePossibleWordType(int type)
    {
        possiblewordtypes &= ~type;
    }
    
    /**
    * Checks for the existence of a type within the possible
    * word types list.
    * @param type				the type to check for amongst the possibles
    * @returns					true if the type was found, false otherwise
    */
    public boolean checkForPossibleWordType(int type)
    {
        if( (possiblewordtypes & type) != 0 ) 
            return true;
        else 
            return false;
    }
    /**
    * Checks for the existence of a type within the candidates
    * @param candidates			an integer bitarray of types.
    * @param type				the type to look for
    * @returns					true if the type was found, false otherwise
    */
    public static boolean checkForPossibleWordType(int candidates, int type)
    {
    	if( (candidates & type) != 0 )
    		return true;
    	else
    		return false;
    }
    
    /**
    *
    */
    public String toString()
    {
    	return value;
    } 
    
    public static String[] lookUpWordValues(int wordtype)
    {
    	Vector result = new Vector();
    	
    	if( checkForPossibleWordType( wordtype, WT_VERB ) )
    		result.addElement( wordTypeToString(WT_VERB) );
    		
    	if( checkForPossibleWordType( wordtype, WT_NOUN ) )
    		result.addElement( wordTypeToString(WT_NOUN) );
    		
    	if( checkForPossibleWordType( wordtype, WT_ADJECTIVE ) )
    		result.addElement( wordTypeToString(WT_ADJECTIVE) );
    		
    	if( checkForPossibleWordType( wordtype, WT_RAWVERB ) )
    		result.addElement( wordTypeToString(WT_RAWVERB) );
    		
    	if( checkForPossibleWordType( wordtype, WT_ADVERB ) )
    		result.addElement( wordTypeToString(WT_ADVERB) );
    		
    	if( checkForPossibleWordType( wordtype, WT_PREPOSITION ) )
    		result.addElement( wordTypeToString(WT_PREPOSITION) );
    	
    	if( checkForPossibleWordType( wordtype, WT_NEGATION ) )
    		result.addElement( wordTypeToString(WT_NEGATION) );
    		
    	if( checkForPossibleWordType( wordtype, WT_CONNECTOR ) )
    		result.addElement( wordTypeToString(WT_CONNECTOR) );
    		
    	if( checkForPossibleWordType( wordtype, WT_STOPWORD ) )
    		result.addElement( wordTypeToString(WT_STOPWORD) );
    	
    	if( checkForPossibleWordType( wordtype, WT_ARTICLE ) )
    		result.addElement( wordTypeToString(WT_ARTICLE) );
    		
    	if( checkForPossibleWordType( wordtype, WT_NUMERIC ) )
    		result.addElement( wordTypeToString(WT_NUMERIC) );
    		
    	if( checkForPossibleWordType( wordtype, WT_PRONOUN ) )
    		result.addElement( wordTypeToString(WT_PRONOUN) );
    		
    	if( checkForPossibleWordType( wordtype, WT_TERMINATOR ) )
    		result.addElement( wordTypeToString(WT_TERMINATOR) );
    	
    	if( checkForPossibleWordType( wordtype, WT_DIRECTION ) )
    		result.addElement( wordTypeToString(WT_DIRECTION) );
    		
    	if( checkForPossibleWordType( wordtype, WT_SUPERLATIVE ) )
    		result.addElement( wordTypeToString(WT_SUPERLATIVE) );
    		
    	if( checkForPossibleWordType( wordtype, WT_STRING ) )
    		result.addElement( wordTypeToString(WT_STRING) );
		
		String types[] = new String[result.size()];
		result.copyInto(types);
		return types;
    }
    
    public static int stringToWordType(String wordtype)
    {
    	if(S_VERB.equals(wordtype))
    		return WT_VERB;
    	else if(S_NOUN.equals(wordtype))
    		return WT_NOUN;
    	else if(S_ADJECTIVE.equals(wordtype))
    		return WT_ADJECTIVE;
    	else if(S_RAWVERB.equals(wordtype))
    		return WT_RAWVERB;
    	else if(S_ADVERB.equals(wordtype))
    		return WT_ADVERB;
    	else if(S_PREPOSITION.equals(wordtype))
    		return WT_PREPOSITION;
    	else if(S_NEGATION.equals(wordtype))
    		return WT_NEGATION;
    	else if(S_CONNECTOR.equals(wordtype))
    		return WT_CONNECTOR;
    	else if(S_ARTICLE.equals(wordtype))
    		return WT_ARTICLE;
    	else if(S_NUMERIC.equals(wordtype))
    		return WT_NUMERIC;
    	else if(S_PRONOUN.equals(wordtype))
    		return WT_PRONOUN;
    	else if(S_TERMINATOR.equals(wordtype))
    		return WT_TERMINATOR;
    	else if(S_STOPWORD.equals(wordtype))
    		return WT_STOPWORD;
    	else if(S_SUPERLATIVE.equals(wordtype))
    		return WT_SUPERLATIVE;
    	else if(S_STRING.equals(wordtype))
    		return WT_STRING;
    	else if(S_DIRECTION.equals(wordtype))
    		return WT_DIRECTION;
    	
    	return WT_UNKNOWN;
    }
    /**
    *
    */
    public static String wordTypeToString(int wordtype)
    {
    	switch (wordtype)
    	{
    		case WT_VERB:
    			return S_VERB;
    		case WT_NOUN:
    			return S_NOUN;
    		case WT_ADJECTIVE:
    			return S_ADJECTIVE;
    		case WT_RAWVERB:
    			return S_RAWVERB;
    		case WT_ADVERB:
    			return S_ADVERB;
    		case WT_PREPOSITION:
    			return S_PREPOSITION;
    		case WT_NEGATION:
    			return S_NEGATION;
    		case WT_CONNECTOR:
    			return S_CONNECTOR;
    		case WT_ARTICLE:
    			return S_ARTICLE;
    		case WT_NUMERIC:
    			return S_NUMERIC;
    		case WT_PRONOUN:
    			return S_PRONOUN;
    		case WT_TERMINATOR:
    			return S_TERMINATOR;
    		case WT_STOPWORD:
    			return S_STOPWORD;
    		case WT_SUPERLATIVE:
    			return S_SUPERLATIVE;
    		case WT_STRING:
    			return S_STRING;
    		case WT_DIRECTION:
    			return S_DIRECTION;
    		default:
    			return "Unknown";
    	}
    }
    	
}
