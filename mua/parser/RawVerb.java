// $Id: RawVerb.java,v 1.6 1999/04/29 10:01:36 jim Exp $
// RawVerb class
// Alexander Veenendaal, 20 October 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import com.ogalala.util.Privilege;
import com.ogalala.util.Privileged;

/**
* Raw Verbs are quite similar to Verbs; together they comprise a
* front-end of 'doing' things in the engine.
*
* Where Raw Verbs differ, however, is in the way that they are
* handled by the parser. Normal Verb sentences are fully analysed 
* by the Parser pipeline; Raw Verbs are not. They are emmitted as
* events near enough straight away, with the following words as 
* arguments.
*
* Most Raw Verbs will be system level commands used only by admins,
* for example
*
*       !Exam     -   	displays the propertys of a given atom.
*       !Dig      -   	Tunnels a passage from one room to another
*
* Other Raw Verbs are user level:
*
*       Inventory -   	displays the users inventory
*       Time      -		displays the current time
*
* Any arguments passed , if any, are processed within the assigned
* action code. This action is referred to by using Raw Verbs property 
* ID.
*
* Like Verbs, Raw Verbs may have a privilege level set. Meaning that
* only a user of equivilent or higher rating may use the command.
*/
class RawVerb implements Privileged, java.io.Serializable
{
	
	//---------------------------- Properties --------------------------------//
	
    /** Holds the privilege level for this Verb
    */
    private int privilege = Privilege.DEFAULT;

	/** The rawverbs propertyID is its internal representation
	*/
    private String propertyID;

	
    //---------------------------- Constructors ------------------------------//
    
    public RawVerb(String propertyID, int privilege)
    {
        setPropertyID(propertyID);
        setPrivilege(privilege);
    }
    
    public RawVerb(String propertyID)
    {
        setPropertyID(propertyID);
    }
	
	public RawVerb()
	{
	}
    
    //----------------------------- Public Methods ----------------------------//

    /**
    * Sets this rawverbs privilege rating
    *
    * @param privilege  the new privilege rating
    */
    public void setPrivilege(int privilege)
    {
        this.privilege = privilege;
    }
    
    /**
    * Returns this rawverbs privilege rating
    *
    * @returns           the rawverbs privilege rating   
    */
    public int getPrivilege()
    {
        return privilege;
    }

    /**
    * Sets the rawverbs propertyID
    * 
    * @param propertyID     sets the rawverbs property ID
    */
    public void setPropertyID(String propertyID)
    {
        this.propertyID = propertyID;
    }
    
    /**
    * Returns the rawverbs propertyID
    *
    * @returns              the rawverbs property ID
    */
    public String getPropertyID()
    {
        return propertyID;
    }
}
