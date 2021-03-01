// $Id: VerbTemplate.java,v 1.11 1999/04/29 10:01:36 jim Exp $
// Verb Templates belong to Verbs
// Alexander Veenendaal, 17 September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.util.StringTokenizer;
import java.util.Hashtable;

/**
* 
*
*/

public class VerbTemplate implements java.io.Serializable
{
	
    /** Verb argument types
    */
    public static final int UNSET 				= 0;
    public static final int EMPTY 				= 1;
    public static final int STRING 				= 4;
    public static final int THING 				= 8;
    public static final int CURRENT 			= 16;
    
    public static final int NUMERIC 			= 32;
    public static final int DATE 				= 64;
    public static final int DIRECTION 			= 128;
    
    public static final int SEARCH_CONTAINER 	= 256;
    public static final int SEARCH_INVENTORY 	= 512;
    public static final int SEARCH_WORLD 		= 1024;
    
    public static final int AQUIRE_SEARCH		= 2048;
    public static final int LOOK_SEARCH			= 4096;
    
    public static final int COMMUNICATIVE 		= 16384;
	
	/** contains up to three argument types
	*/
    private int argType[];
    
    /** contains up to two prepositions
    */
    private String preposition[];
    
    /**
    */
    private String propertyID;
    
    /** 
    * Each argument may contain a special kind of modifier
    * which demands that a property must exist and be either
    * true or false.
    */
    private Hashtable modifierProperties[] = new Hashtable[3];
    
    private Vocabulary vocabulary;
    //----------------------------- Constructors ---------------------------------//

    public VerbTemplate(Vocabulary vocabulary, String rawtemplate,String propertyID)
    {
        this(vocabulary, propertyID);
        decomposeRawTemplate(rawtemplate);
    }
        
    public VerbTemplate(Vocabulary vocabulary, String propertyID)
    {
    	this.vocabulary = vocabulary;
        this.propertyID = propertyID;
    }

    //----------------------------- Public Methods --------------------------------//
    
    /**
    * Returns true if an argument at 'index' contains 'argtype'
    * @param argtype				the argtype to check for
    * @param index					the argument index to check
    * @returns						true if argument(index) contains argtype
    */
    public boolean checkForArgType(int argtype, int index)
    {
    	if(index<argType.length)
	    	if( (argType[index] & argtype) != 0 ) 
	            return true;
	    return false;
    }
    
    /**
    * Returns the argument specified at 'index'
    * @param index					the argument to return
    * @returns 						the argument at index
    */
    public int getArgType(int index)
    {
        if(index<argType.length)
            return argType[index];
        return UNSET;
    }
    
    /**
    * Returns the string preposition at 'index'
    * @param index					the prepostion to return
    * @returns						the preposition at 'index'
    */
    public String getPreposition(int index)
    {
        if(index<argType.length)
            return preposition[index];
        return "";
    }
    /**
    * Returns the verb propertyID of this template
    * @returns						the verb propertyID of this template
    */
    public String getPropertyID()
    {
        return propertyID;
    }
    
    /**
    * 
    */
    public Hashtable getModProperties(int index)
    {
    	return modifierProperties[index];
    }
    
    /**
    * Returns a string representation of this VerbTemplate
    * @returns						a string representing this VerbTemplate
    */
    public String toString()
    {
    	return getArgType(0) + getPreposition(0) + getArgType(1) + getPreposition(1) + getArgType(2);
    }
    
    
    //----------------------------- Private Methods -------------------------------//
    
    /**
    * Attempts to decompose the raw template into integer arguments
    *  and string prepositions.
    */
    private void decomposeRawTemplate(String raw)
    {
        StringTokenizer st = new StringTokenizer(raw.toLowerCase(), " ");
        
        //initialise everything so that we are ready for the process
        int argcount=0; int prepcount=0;
        int argtemp[] = new int[3];
        String preptemp[] = new String[2];
        for(int i=0;i<argtemp.length;i++)
            argtemp[i] = UNSET;
        for(int i=0;i<preptemp.length;i++)
            preptemp[i] = "";
        
        //must make sure we keep to our arg and prep limit
        while(st.hasMoreTokens() && argcount<3 && prepcount<3)
        {
            String s = st.nextToken();
            
            //try for an argtype
            argtemp[argcount] = parseRawArgType(s);
            
            //extract any property Modifiers
            modifierProperties[argcount] = parseModifierProperties(s);
            
            //now see if we got a valid arg
            if(argtemp[argcount]==UNSET)
            {
                //make it a prep, if we have enough room
                if(prepcount<2)
                {
                	//add the prepostition to the vocab
                	vocabulary.addWordToDictionary(s, Word.WT_PREPOSITION);
                    preptemp[prepcount] = s;
                    prepcount++;
                }
            }
            else if(argcount<3)
                argcount++;
        }
        //If the verb template is communicative, then we don't care about any of the following stuff
        if( !(( argtemp[0] & COMMUNICATIVE ) != 0) )
        {
	        //now check for the correctness of the template      
	        // Can't have three CURRENTs (this isn't a bun you know)
	        if ( ((argtemp[0] & CURRENT) != 0 && (argtemp[1] & CURRENT) != 0) ||
	            ( (argtemp[0] & CURRENT) != 0 && (argtemp[2] & CURRENT) != 0 ) ||
	            ( (argtemp[1] & CURRENT) != 0 && (argtemp[2] & CURRENT) != 0 ) )
	                throw new ParserException("Current specified more than once in command template: " + raw);
	        
	        // Can't have THING and CURRENT in same arg
	        byte illegal = THING | CURRENT;
	        if( (argtemp[0] & illegal) == illegal)
	            throw new ParserException("Current and Thing cannot both be the first argument");
	        if( (argtemp[1] & illegal) == illegal)
	            throw new ParserException("Current and Thing cannot both be the second argument");
	        if( (argtemp[2] & illegal) == illegal)
	            throw new ParserException("Current and Thing cannot both be the third argument");          
	        
	        // Can't have EMPTY as first arg unless it is second and third as well
	        if( argcount==2 && (argtemp[0] & EMPTY) != 0  && (argtemp[1] & EMPTY) == 0 && (argtemp[2] & EMPTY) == 0)
	            throw new ParserException("First argument cannot be empty unless second and third are as well: " + raw);
        }
        
        // There must be one preposition for every argument after the first
        // ### not very good implementation of this
        if( argcount>1 && prepcount==0 )
            throw new ParserException("Must specify prepositions within template. Verb: " + propertyID + " args: "+ raw);
        if( argcount==0 && prepcount>0 )
            throw new ParserException("Must specify arguments within template. Verb: " + propertyID + " args: "+ raw);
            
        //ok, set the verbtemplates arguments and prepositions
        argType = argtemp;
        preposition = preptemp;
    }
    
    
    /**
    * Takes a raw String and parses it into a meaningful internal
    *  representation.
    */
    private int parseRawArgType(String raw)
    {
        int result = UNSET;
        if (raw.startsWith("string"))
            result = STRING;
        else if (raw.startsWith("thing"))
            result = THING;
        else if (raw.startsWith("current"))
            result = CURRENT;
        else if (raw.startsWith("communicative"))
            result = COMMUNICATIVE;
        else if (raw.startsWith("none"))
            result = EMPTY;
        else if (raw.startsWith("numeric"))
            result = NUMERIC;
        else if (raw.startsWith("date"))
            result = DATE;
        else if (raw.startsWith("direction"))
            result = DIRECTION;
        
        //if we have a Communicative Template, then we know where
        // we will be searching for.
        if( result == COMMUNICATIVE )
        {
        	result |= SEARCH_CONTAINER;
        	result &= ~SEARCH_INVENTORY;
        	result &= ~SEARCH_WORLD;
        }
        // If the result is an atom type, look for a search modifier
        else if (result == CURRENT || result == THING)
        {
			// The Default is to search the inventory and room
			// for things that can only be gotten.
			result |= AQUIRE_SEARCH;
			result &= ~LOOK_SEARCH;
			
			result |= SEARCH_INVENTORY;
			result |= SEARCH_CONTAINER;
			result &= ~SEARCH_WORLD;

			// Search for things that can be seen
			if( raw.indexOf("(see)") >= 0 )
			{
				result &= ~AQUIRE_SEARCH;
				result |= LOOK_SEARCH;
			}
			
			// Search for things that can be taken
			if( raw.indexOf("(touch)") >= 0 )
			{
				result |= AQUIRE_SEARCH;
				result &= ~LOOK_SEARCH;
			}
			
			// Search the Inventory only
			if( raw.indexOf("(inv)") >= 0 )
			{   
                result |= SEARCH_INVENTORY;
                result &= ~SEARCH_CONTAINER;
                result &= ~SEARCH_WORLD;
			}
			
			// Search the Room only
			if( raw.indexOf("(room)") >= 0 )
			{
				result |= SEARCH_CONTAINER;
                result &= ~SEARCH_INVENTORY;
                result &= ~SEARCH_WORLD;
			}
			
			// Search the World only
			if( raw.indexOf("(world)") >= 0 )
			{
				result |= SEARCH_WORLD;
				result &= ~SEARCH_CONTAINER;
				result &= ~SEARCH_INVENTORY;
			}
        }

        return result;    
    }
        
    /**
    * As each argument may contain several argument modifier properties,
    * each one is inserted into a hashtable
    *
    *
    */
    protected Hashtable parseModifierProperties(String modifiers)
    {
    	//if there are no '{'s, then there can be nothing of interest
    	// to us here
    	if( modifiers.indexOf('{') == -1)
    		return null;
    		
    	Hashtable result = new Hashtable();
    	
    	StringTokenizer st = propertyModifierTokenizer(
    		modifiers.substring( 
    			modifiers.indexOf('{'),
    			modifiers.lastIndexOf('}')
    		)
    	);
    	
    	String mod;
    	
    	while(st.hasMoreTokens())
    	{
    		mod = st.nextToken();
    		//check for a false property
    		if(mod.startsWith("!"))
    			//its a property with a false value
    			result.put(mod.substring(1,mod.length()), new Boolean(false) );
    		else
    			//its a property with a true value
    			result.put(mod, new Boolean(true) );
    	}
    	return result;
    }
    
    
    
    /**
    * Create a string tokenizer for argument modifiers
    *
    * @param modifers			a string of modifierness
    * @returns					a StringTokenizer ready to go
    */
    protected static StringTokenizer propertyModifierTokenizer(String modifers)
    {
    	return new StringTokenizer(modifers, " {}");
    }
    
}
