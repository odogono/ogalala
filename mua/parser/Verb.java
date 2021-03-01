// $Id: Verb.java,v 1.13 1999/04/29 10:01:36 jim Exp $
// Verb class
// Alexander Veenendaal, 18 September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

/** 
* Represents a verb
*
* This version of Verb stores its templates slightly differently. While
* it still contains arg types, the number has been increased to three.
* Prepositions are now included as part of the template, and it is the
* prepositions which identify the verb template.
* For example, the verb 'put' is defined, and it has two template forms.
* The first is 'put current under thing', the second 'put thing on current'.
* When the time comes to identify the verb form, either 'under' or 'on' 
* may be passed to the verb which returns the arg types if true
*/

import java.util.Vector;

import com.ogalala.util.Privilege;
import com.ogalala.util.Privileged;

class Verb implements Privileged, java.io.Serializable
{

	/** Holds the privilege level for this Verb
	*/
	private int privilege = Privilege.DEFAULT;
	
    /** A Vector of templates
    */
    private Vector templates = new Vector();
    
    /** Is this a Communicative Verb ? Such verbs do not require Templates
    */
    private boolean isCommunicative = false;
    
    //----------------------------- Constructors ---------------------------------//
    public Verb()
    {
    }
    
    //----------------------------- Public Methods --------------------------------//
    
    /**
    * Adds a template to the store, while following certain conditions:
    *  - there cannot be more than one single argument template
    */
    public void addTemplate(VerbTemplate template)
    {
    	VerbTemplate vtemp = matchTemplate();
    	
    	//### Suspended for the time being, as there needs to be more than one single argument
    	// template attached to a verb.
    	/*if(	template.getPreposition(0).equals("") && vtemp != null &&
    		vtemp.getArgType(0) != VerbTemplate.EMPTY ) 
    	{
    		throw new ParserException("Only one single argument template allowed: " + template.getPropertyID());
        }//*/
        templates.addElement(template);
    }
    
    /**
    * As it says. Rather drastic, but hey.
    */
    public void removeAllTemplates()
    {
        templates.removeAllElements();
    }
    
    /**
    * Removes all templates with the supplied propertyID
    *
    * @param propertyID			all templates with this ID will be removed
    */
    public void removeTemplate(String propertyID)
    {
    	for(int i=0;i<templates.size();i++)
    		if( ((VerbTemplate)templates.elementAt(i)).getPropertyID().equals(propertyID) )
    			templates.removeElementAt(i);
    	
    }
    /**
    * Attempt to find a template that matches.
    *  templates are found by comparing the position of the prepositions
    *  this could be:
    *       two prepositions in a template
    *       a single preposition within a template
    *       no prepositions within a template
    * The matching will always go for a exact match;
    *  if an attempt is made to match a template by passing one prep, then
    *   only templates with a single preposition will be looked at.
    *   The same with other match types.
    * @returns the propertyID of the template if successful, null if not.
    */
    public VerbTemplate matchTemplate(String prep1, String prep2)
    {
    	//Communicative Verbs do not require Templates
    	if(!isCommunicative)
    	{
	        if(templates.size()==0) 
	            return null;
	        VerbTemplate temp;    
	        //loop through all the templates
	        for(int i=0;i<templates.size();i++)
	        {
	            temp = (VerbTemplate)templates.elementAt(i);
	            if(temp.getPreposition(0).equals(prep1) && temp.getPreposition(1).equals(prep2))
	                //return temp.getPropertyID();
	                return temp;
	        }
	    }
        return null;
    }
    
    
    /**
    * Try to match a template on a single Preposition
    */
    public VerbTemplate matchTemplate(String prep)
    {
    	if(!isCommunicative)
    	{
	        if(templates.size()==0)
	            return null;
	        VerbTemplate temp;    
	        //loop through all the templates
	        for(int i=0;i<templates.size();i++)
	        {
	            temp = (VerbTemplate)templates.elementAt(i);
	            if(temp.getPreposition(0).equals(prep) && temp.getPreposition(1).equals(""))
	                //return temp.getPropertyID();
	                return temp;
	        }
	    }
        return null;
    }
    
    /**
    * Try to match a template with no prepositions on its only
    *  argument type
    */
    public VerbTemplate matchTemplate(int argType)
    {
    	if(templates.size() == 0)
    		return null;
    	VerbTemplate temp;
    	//loop through all of the templates
    	for(int i=0;i<templates.size();i++)
    	{
    		temp = (VerbTemplate)templates.elementAt(i);
    		if( temp.getPreposition(0).equals("") && temp.getPreposition(1).equals(""))
    		{
    			if( temp.checkForArgType(argType,0) )
    				return temp;
    		}
    	}
    	return null;
    }
    
    /**
    * Find a template which has no prepositions in it at all.
    */
    public VerbTemplate matchTemplate()
    {
        if(templates.size() == 0)
            return null;
        VerbTemplate temp;    
        //loop through all the templates
        for(int i=0;i<templates.size();i++)
        {
            temp = (VerbTemplate)templates.elementAt(i);
            if(temp.getPreposition(0).equals("") && temp.getPreposition(1).equals(""))
                //return temp.getPropertyID();
                return temp;
        }
        return null;
    }
    
    /**
    * Uses a different method of searching for the correct template.
    * The returning string array contains:
    *		at [0] - the verb property ID
    *		all further indexs contain the found prepositions
    */
	public String[] matchTemplate(String preps[])
    {
    	//Communicative Verbs do not require Templates
    	if(!isCommunicative)
    	{
	    	String result[]; //what comes back!
	    	if(templates.size()==0)
	            return null;
	        VerbTemplate temp;    
	        //loop through all the templates
	        for(int i=0;i<templates.size();i++)
	        {
	        	temp = (VerbTemplate)templates.elementAt(i);
	        	//is the first prep in this template within the preps array
	        	for(int j=0;j<preps.length;j++)
	        		if( temp.getPreposition(0).equals(preps[j]) )
	        		{
	        			//look for the 2nd prep somwhere after the first.
	        			for(int k=j;k<preps.length;k++)
	        				if( temp.getPreposition(1).equals(preps[k]) )
	        				{
	        					//we have found all we need, so return everything in the array
	        					result = new String[3];
	        					result[0] = temp.getPropertyID();
	        					result[1] = temp.getPreposition(0);
	        					result[2] = temp.getPreposition(1);
	        					return result;
	        				}
	        			//no 2nd prep, so bundle what we have left back
	        			result = new String[2];
	        			result[0] = temp.getPropertyID();
	        			result[1] = temp.getPreposition(0);
	        			return result;
	        		}
	        }
        }
        return null;
    }
    
    //-------------------- Accessor/Mutator Privilege Methods --------------------//
    
    public void setCommunicative(boolean isCommunicative)
    {
    	this.isCommunicative = isCommunicative;
    }
    public boolean isCommunicative()
    {
    	return isCommunicative;
    }
    
    public void setPrivilege(int privilege)
    {
        this.privilege = privilege;
    }
    public int getPrivilege()
    {
        return privilege;
    }
}
