// $Id: Adverb.java,v 1.7 1999/04/29 10:01:35 jim Exp $
// Adverb Wrapper class
// Alexander Veenendaal, 21	September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.util.Vector;
import java.io.Serializable;

/**
* Adverbs are treated by the Parser as a device for tramsforming the
* given verb into a new form.
*
* For example:
*
*		'quickly walk into the cupboard'
*
* The verb here is obviously 'walk', and the adverb is 'quickly'. The
* parser notes the adverb, and looks up its mapping in the vocabulary
* which returns with the new verb 'run'.
*
* Adverbs may have many transforms associated with them. For example:
*
*		'loudly' transforms 'talk' into 'shout'
*		'loudly' also transforms 'walk' into 'stomp'
*
* Adverbs are added into the vocabulary using the following syntax:
*		
*		!Adverb "adverb + verb > translated-verb"
*
* Where adverb,verb and translated-verb are all single strings.
*
*/
public class Adverb 
    implements Serializable
{
    public static final int serialVersionUID = 1;
    
	//-------------------------- Properties -----------------------------//
	
	/** A list of verbs that are transformed by this adverb
	*/
    private Vector applicatorVerbs;
    
    /** A list of the resultant property IDs
    */
    private Vector verbPropertyIDs;
    
    

    //-------------------------- Constructor -----------------------------//
    public Adverb()
    {
        applicatorVerbs = new Vector();
        verbPropertyIDs = new Vector();
    }



    //------------------------- Public Methods ----------------------------//
    
    /**
    * Adds an applicator verb to this Adverbs internal list.
    *
    * @param verb 			the verb that the adverb transforms
    * @param verbPropertyID		the new verb propertyID , post adverb
    */
    public void addNewMapping(String verb, String verbPropertyID)
    {
        applicatorVerbs.addElement(verb);
        verbPropertyIDs.addElement(verbPropertyID);
    }
    
    /**
    * Sets up the adverbs mapping
    *
    * @param verb			the verb that the adverb transforms
    * @param verbPropertyID		the new verb propertyID , post adverb
    */
    public void setMapping(String verb, String verbPropertyID)
    {
    	for(int i=0;i<applicatorVerbs.size();i++)
    		if( applicatorVerbs.elementAt(i).equals(verb) )
    			verbPropertyIDs.setElementAt(verbPropertyID, i);
    }
    
    /**
    * Returns the verb propertyID that this adverb transforms
    *
    * @param verb			the verb that the adverb transforms
    * @returns				the resultant verb property ID
    */
    public String getVerbName(String verb)
    {
        for(int i=0;i<applicatorVerbs.size();i++)
    		if( applicatorVerbs.elementAt(i).equals(verb) )
    			return (String)verbPropertyIDs.elementAt(i);
    	return null;		
    }
}
