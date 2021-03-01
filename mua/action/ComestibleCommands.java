// $Id: ComestibleCommands.java,v 1.14 1999/03/31 15:54:48 alex Exp $
// Commands which involve the Actor consuming something
// Alexander Veenendaal, 27 November 1998
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;


public class Eat extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
    	//display the eating message
    	actor.output( current.getString("eat_msg") );
    	
    	//display the eating message to everyone else
    	container.output( current.getString("eat_omsg") , actor );
    	
    	//delete this eaten object from the world
    	world.deleteAtom( current );
		
		return true;
	}
}

/**
* Pour moves object from a container to another container en masse !!
*/
public class Pour extends MoveTo
{
	private static final long serialVersionUID = 1;
	
	LiquidUtil liquidUtil = new LiquidUtil();	

	/**	container the contents of the thing to pour
	*/
	Vector contents;
    
    
    public void init()
    {
    	// Get the container we are pouring.
    	source = (Container)current;
    	
    	// Get the contents of the container that will be poured
    	contents = (Vector)AtomData.enumToVector(current.getContents());
    	
    	// Get the destination we are pouring into
    	// note that if none is specified, we are pouring onto the 'floor' (the room)
    	if( event.getArg() == null )
    		destination = (Container)actor.getContainer();
    	else
    		destination = (Container)event.getArg();
    }
    
    public boolean execute()
    {
    	// do initialisation functions
    	init();
    	
    	// Is the container closed (if its not a room)?
    	if( !(actor.getContainer() == destination) && isClosed() )
    	{
    		destinationClosed();
    		return true;
    	}
    	
    	// Is there anything to actually pour ?
    	if( contents.size() == 0 )
    	{
    		sourceEmpty();
    		return true;
    	}
    	
    	//loop through the enumeration of objects in the contents
    	for( int i=0; i<contents.size(); i++ )
    	{
    		object = (Atom)contents.elementAt(i);
    		
    		//will the object fit in the container
    		if( !fits() )
    		{
    			doesNotFit();
    			return true;
    		}
    		
    		//Is the container full?
    		else if( !isRoomFor() )
    		{
    			destinationFull();
    			return true;
    		}
    		
    		//Move the atom ...
    		else
    			doMove();
    	}
    	//...and notify users
    	broadcastMove();
    	return true;
    }
    
    /**
    *	Reports to the user that the item does not fit
    */
    public void doesNotFit()
    {
        actor.output( container.getString( "too_big_msg" ) );
    }
	
	/**
	*	Reports to the user and the room that the container has been 'poured'
	*/
	public void broadcastMove()
    {
    	String ident = "into";
    	if( destination == actor.getContainer() )
    		ident = "onto";
    	
    	container.output( container.getString("pour_omsg"), new String[] { ident, destination.getString("name") }, actor );
    	actor.output( container.getString("pour_msg"), new String[] { ident, destination.getString("name") } );
    }
    
    /**
    *	Reports to the user that the proposed destination is closed
    */
	public void destinationClosed()
    {
        actor.output( destination.getString( "is_closed_msg" ) );
    }
    
    public void sourceEmpty()
    {
    	actor.output( source.getString("is_empty_msg") );
    }
    
    public void destinationFull()
    {
        actor.output( destination.getString( "is_full_msg" ) );
    }

    public boolean isRoomFor()
    {
        return true;
    }
}

class LiquidUtil implements Serializable
{
	protected static final String LIQUID_VOLUME = "liquid_volume";
    protected static final String LIQUID_CAPACITY = "liquid_capacity";
    
    protected static final String LIQUID_TYPE = "liquid_type";
    protected static final String IS_FULL_MSG = "is_full_msg";
	protected static final String IS_EMPTY_MSG = "is_empty_msg";
	
	
	/*
	* Checks for the prescence of a liquid inside a container
	*/
	static boolean checkForLiquid( Atom actor, Atom container )
	{
		//check that there is liquid inside the container to move
		if( container.getInt(LIQUID_VOLUME) <= 0  || container.getAtom(LIQUID_TYPE) == null )
		{
			//actor.output( liquid.getString(IS_EMPTY_MSG) );
			return false;
		}
		return true;
	}
	
	static boolean checkForLiquidCapactity( Atom actor, Atom container )
	{
		//check whether the destination liquid container has the capacity to hold this liquid
    	if( container.getInt(LIQUID_VOLUME) >= container.getInt(LIQUID_CAPACITY) )
    	{
    		//actor.output( liquid.getString(IS_FULL_MSG) );
    		return false;
    	}
    	
    	return true;
    }
}



/**
* Does one of three things:
*
*	1. Pours from a liquid container(current) onto the floor
*	2. Pours from a liquid container(current) onto some sort of object
*	3. Pours from a liquid container(current) into another container
*/
/*public class Pour extends LiquidMove
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        liquidContainer = current;
		
		//the default destination for the liquid is the floor of the room
		liquidDestination = container;
		
		//check that there is a liquid inside the container
		if( !checkForLiquid(liquidContainer) )
			return true;
		
		//now check whether we have specified a destination for our pourings
		// if we have, make that the destination
		if( event.getArg(0) != null )
			liquidDestination = (Atom)event.getArg(0);
    	
    	//get the liquid inside the source
    	liquid = liquidContainer.getAtom(LIQUID_TYPE);
    		
    	//if the destination is another container, handle that...
    	if( liquidDestination.isDescendantOf( world.getAtom("container") ) )
    		pourInto();
    	
    	//otherwise, we are pouring on to something...
    	else
    		pourOnto();
    	
    	return true;
    }
    
    protected void pourOnto()
    {
    	//decrement the sources liquid count
    	liquidContainer.setInt(LIQUID_VOLUME, current.getInt(LIQUID_VOLUME)-1 );
	    		
    	//if there is nothing left in the source container, remove its liquid reference
    	if( liquidContainer.getInt(LIQUID_VOLUME) <= 0 )
    		liquidContainer.setProperty( LIQUID_TYPE, null);
	    		
    	//report the pouring
    	actor.output( liquidContainer.getString("pour_on_msg") );
	    		
    	//tell everyone else 
        container.output( liquidContainer.getString("pour_on_omsg") , actor );
    }
    
    protected void pourInto()
    {
    	if( !checkForLiquidCapactity(liquidContainer) )
    		return;
    		
    	//transfer the liquid into the container
    	//addLiquidToContainer(current, destination);
    	liquidDestination.setAtom( "liquid_type", liquid );
    		
    		
    	//decrement the sources liquid count
    	liquidContainer.setInt( LIQUID_VOLUME, current.getInt(LIQUID_VOLUME)-1 );
    		
    	//if there is nothing left in the source container, remove its liquid reference
    	if( liquidContainer.getInt(LIQUID_VOLUME) <= 0 )
    		liquidContainer.setProperty( "liquid_type", null);
    		
    	//increase the destinations liquid volume
    	liquidDestination.setInt(LIQUID_VOLUME, (liquidDestination.getInt(LIQUID_VOLUME)+1) );
    		
    	//report the pouring
    	actor.output( liquidContainer.getString("pour_in_msg") );
    		
    	//tell everyone else 
        container.output( current.getString("pour_in_omsg") , actor );
        	
    	return;
    }

}//*/

/**
* fills an object with a liquid_container
*/
/*public class Fill extends Pour
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		
		return true;
	}
}//*/

/**
*
*/
/*public class Drink extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
    	
    	//check that there is something inside the container to pour
		if( current.getInt("liquid_volume") <= 0 || current.getAtom("liquid_type") == null )
		{
			actor.output( current.getString("drink_empty_msg") );
			return true;
		}
		
    	//decrement the sources liquid count
    	current.setInt("liquid_volume", current.getInt("liquid_volume")-1 );
    	
    	//if there is nothing left in the source container, remove its liquid reference
    	if( current.getInt("liquid_volume") <= 0 )
    		current.setProperty( "liquid_type", null);
    	
    	//tell the actor they have drunked
        actor.output( current.getString("drink_msg") );
        
        //tell everyone else the actor has drunked
        container.output( current.getString("drink_omsg") , actor );
        
        return true;
    }
}//*/



/**
*
*/
public class Wear extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		actor.output("Wear is not yet implemented.");
		return true;
	}
}


