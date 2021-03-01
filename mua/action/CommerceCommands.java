// $Id: CommerceCommands.java,v 1.13 1999/04/09 10:08:08 alex Exp $
// Game Logic Commerce Commands
// Alexander Veenendaal, 30 November 1998
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;

/**
* Address marks something to be posted to someone else
*/
public class Address extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        actor.output("Address is not yet implemented.");
        return true;
    }
}

/**
* Balance reports the money owned by the actor
*/
public class Balance extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        actor.output("Balance is not yet implemented.");
        return true;
    }
}



/**
* Plays a game of Scissors/Paper/Stone with another mobile
*/
public class ScissorsPaperStone extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	protected static final int SCISSORS = 1;
	protected static final int PAPER	= 2;
	protected static final int STONE	= 3;
		
	public boolean execute()
	{
		//The opponent
		Mobile otherPlayer = (Mobile)current;
		
		String playerName = actor.getString("name");
		String otherPlayerName = otherPlayer.getString("name");
		
		//roll for the initiator
		int playerCall = Dice.roll(3);
		
		//roll for the other Player
		int otherPlayerCall = Dice.roll(3);
		
		//declare the game
		actor.output("You challenge " + otherPlayerName + " to a game of Scissors Paper Stone !");
		otherPlayer.output( playerName + " challenges you to a game of Scissors/Paper/Stone. You feel compelled to play !");
		container.output( playerName + " challenges " + otherPlayerName + " to a game of Scissors/Paper/Stone !", actor, otherPlayer);
		
		
		switch(playerCall)
		{
			case SCISSORS: 	actor.output("You draw Scissors !"); 
							otherPlayer.output( playerName + " draws Scissors !");
							break;
			case PAPER: 	actor.output("You draw Paper !"); 
							otherPlayer.output( playerName + " draws Paper !");
							break;
			case STONE: 	actor.output("You draw Stone !"); 
							otherPlayer.output( playerName + " draws Stone !");
							break;
			default: break;
		}
		
		switch(otherPlayerCall)
		{
			case SCISSORS: 	actor.output( otherPlayerName + " draws Scissors !");
							otherPlayer.output("You draw Scissors !"); 
							break;
			case PAPER: 	actor.output( otherPlayerName + " draws Paper !");
							otherPlayer.output("You draw Paper !"); 
							break;
			case STONE: 	actor.output( otherPlayerName + " draw Stone !");
							otherPlayer.output("You draw Stone !"); 
							break;
			default: break;
		}
		
		if( playerCall == SCISSORS  )
		{
			if( otherPlayerCall == SCISSORS )
			{
				actor.output("A draw!");
				otherPlayer.output("A draw!");
				container.output("Its a draw between " + playerName + " and " + otherPlayerName + ".", actor, otherPlayer);
			}
			else if( otherPlayerCall == PAPER )
			{
				actor.output("You cut " + otherPlayerName + " Paper with your Scissors !");
				otherPlayer.output( playerName + " cuts your Paper with Scissors !");
				container.output(playerName + " cuts " + otherPlayerName + " Paper with Scissors !", actor, otherPlayer);
			}
			else if( otherPlayerCall == STONE )
			{
				actor.output( otherPlayerName + " blunts your Scissors with a Stone !");
				otherPlayer.output( "You blunt " + playerName + " Scissors with your Stone !");
				container.output(playerName + " Scissors get blunted by " + otherPlayerName + " Stone !", actor, otherPlayer);
			}
		}
		else if( playerCall == PAPER )
		{
			if( otherPlayerCall == SCISSORS )
			{
				actor.output("Your Paper gets cut by " + otherPlayerName + " Stone !");
				otherPlayer.output("You cut " + playerName + " Paper !");
				container.output(playerName + " Paper gets cut by " + otherPlayerName + " Scissors !", actor, otherPlayer);
			}
			else if( otherPlayerCall == PAPER )
			{
				actor.output("A draw!");
				otherPlayer.output("A draw!");
				container.output("Its a draw between " + playerName + " and " + otherPlayerName + ".", actor, otherPlayer);
			}
			else if( otherPlayerCall == STONE )
			{
				actor.output("You wrap your paper around " + otherPlayerName + " Stone !");
				otherPlayer.output("Your Stone gets wrapped by " + playerName + " Paper !");
				container.output(playerName + " Paper wraps around " + otherPlayerName + " Stone !", actor, otherPlayer);
			}
		}
		else if( playerCall == STONE )
		{
			if( otherPlayerCall == SCISSORS )
			{
				actor.output("Your Stone blunts " + otherPlayerName + " Scissors !");
				otherPlayer.output("Your Scissors are blunted by " + playerName + " Stone !");
				container.output(playerName + " Stone blunts " + otherPlayerName + " Scissors !", actor, otherPlayer);
			}
			else if( otherPlayerCall == PAPER )
			{
				actor.output("Your Stone gets wrapped by " + otherPlayerName + " Paper !");
				otherPlayer.output("You wrap your Paper around " + playerName + " Stone !");
				container.output(playerName + " Stone get wrapped by " + otherPlayerName + " Paper !", actor, otherPlayer);
			}
			else if( otherPlayerCall == STONE )
			{
				actor.output("A draw!");
				otherPlayer.output("A draw!");
				container.output("Its a draw between " + playerName + " and " + otherPlayerName + ".", actor, otherPlayer);
			}
		}
		return true;
	}
}

/**
* Registers a valuable item with the game
*/
public class Register extends JavaAction
{
	private static final long serialVersionUID = 1;
	
	//contains information and strings for this command
	Atom registered;
	Atom registrar;
	
	public boolean execute()
	{
		init();
		
		//check that the item is not already registered.
		if( !checkForPriorRegistration() )
			return true;
		
		//check that the item is valuable enough to be registered
		if( !checkItemValue() )
			return true;
		
		//register the object
		registerItem();
		
		//confirm the actions
		actor.output( registered.getString("item_registered_msg") );
		
		return true;
	}
	
	
	/**
	* Initialise variables
	*/
	private final void init()
	{
		registered = world.getAtom("registered");
	
		registrar = world.getAtom("item_registrar");
	}
	
	
	/**
	* Checks whether the item is already registered or no.
	*/
	private final boolean checkForPriorRegistration()
	{
		if( current.isDescendantOf( registered ) )
		{
			actor.output( registered.getString("item_already_reg_msg") );
			return false;
		}	
		return true;
	}
	
	
	/**
	* Checks whether the item is valuable enough to be registered.
	*/
	private final boolean checkItemValue()
	{
		if( current.getInt("value") < registrar.getInt("minimum_value") )
		{
			actor.output( registered.getString("item_not_valuable_msg") );
			return false;
		}
		return true;
	}
	
	/**
	* Registers the item
	*/
	private final void registerItem()
	{
		//make the item inherit from 'registered'
		current.inherit( registered );
		
		//set the owner field on the atom to be the actor
		current.setString( "owner", actor.getID() );
		
		//add information to the registrar table
		Dictionary registrants = registrar.getTable("registered_items");
		
		//it may not have been initialised yet
		if( registrants == null )
			registrants = new Hashtable();
			
		//add the item and the actor to the table (only their ID strings though)
		registrants.put( current.getID(), actor.getID() );
		
		//set the new table onto the registrar
		registrar.setTable( "registered_items", registrants );
	}
	
	
}


public class UnRegister extends JavaAction
{
	
	private static final long serialVersionUID = 1;
	
	//contains information and strings for this command
	Atom registered;
	Atom registrar;
	
	public boolean execute()
	{
		init();
		
		//check whether the item is actually registered
		if( ! checkItemRegistration() )
			return true;
		
		if( !unregisterItem() )
			return true;
		
		//report the deed and finish
		actor.output( registered.getString("item_unregistered_msg") );
		return true;
		
	}
	
	private final void init()
	{
		//assign the registered item
		registered = world.getAtom("registered");
		
		//get the item_registrar
		registrar = world.getAtom("item_registrar");
	}
	
	private final boolean checkItemRegistration()
	{
		if( ! registered.isDescendantOf( world.getAtom("registered") ) )
		{
			actor.output( registered.getString("item_not_registered_msg") );
			return false;
		}
		return true;
	}
	
	private final boolean unregisterItem()
	{
		//remove information from the registrar table
		Dictionary registrants = registrar.getTable("registered_items");
		
		//it may be that the registrants table has not been set
		//if( registrants == null )
		//	return false;
		
		//check that the item is registered to the actor trying to unregister
		if( ! ((String)registrants.get(current.getID())).equals(actor.getID()) || 
			! current.getString("owner").equals(actor.getID()) )
		{
			actor.output( registered.getString("actor_not_registrant_msg") );
			return false;
		}
		
		//remove the item and its actor reference from the table
		registrants.remove( current.getID() );
		
		//set the table back to the item
		registrar.setTable( "registered_items", registrants );
		
		//uninherit the item from registered
		current.uninherit( registered );
		
		//set the owner field
		current.setString( "owner", null );
		
		return true;
	}
		
}

/**
* The actor may sell an item
*/
public class Sell extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        actor.output("Sell is not yet implemented.");
        return true;
    }
}

/**
* Funds may be withdrawn from the actors account
*/
public class WithDraw extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        actor.output("Withdraw is not yet implemented.");
        return true;
    }
}


/**
*
*/
public class Read extends JavaAction
{
	private static final long serialVersionUID = 1;

	public boolean execute()
	{
		String text = current.getString("text");
		
		//if the text has no length, then there is nothing to read
		if( text.trim().length() <= 0 )
			actor.output( current.getString("read_fail_msg") );
			
		//otherwise, tell us what it says !
		else
		{
			//tell the actor about it
			actor.output( current.getString("read_msg") );
			
			//tell everyone in the room about it
			container.output( current.getString("read_omsg") , actor );
		}
		
		return true;
	}
}
/**
*	Allows one to write on a readable object
*/
public class Write extends JavaAction
{
    private static final long serialVersionUID = 1;
	
	public boolean execute()
	{
		//sort out the message that needs to be written
		if( !assignMessage() )
			return true;
			
		//find a writing implement to use to write with
		if( !validateWritingImplement() )
			return true;
		
		//confirm the action to the actor and room
		broadCastMessages( current.getString("write_msg"), current.getString("write_omsg") );
		
		return true;
	}

	/**
	* Sends a message 
	*/
	protected void broadCastMessages(String actorMessage, String roomMessage)
	{
		actor.output( actorMessage );
		container.output( roomMessage, actor );
	}
	
	/**
	* Retrieves the message (if any) to be written from the event and
	* sets it on the target.
	*/
	protected boolean assignMessage()
	{
		// get the message to write
    	if( event.getArg(0) == null )
    	{
    		actor.output( current.getString("message_missing_msg") );
    		return false;
    	}
    	
    	// get the message to write on the thing
        String message = event.getArg(0).toString();
		
		// set the things text to be the message
        current.setString("text", message);
        
		return true;
		
	}
	/**
	* Searches the inventory for a atom the inherits from another atom
	*/
	protected boolean validateWritingImplement()
	{
		Atom writingImplement;
		if( event.getArg(1) == null  )
		{
			writingImplement =  AtomUtil.findDescendant( actor, current.getAtom("implement") );
			if( writingImplement == null )
				actor.output( current.getString("implement_missing_msg") );
		}
		else
		{
			writingImplement = (Atom)event.getArg(1);
		}
		
		if( writingImplement == null )
			return false;
		else
			return true;
	}
}

/**
* Allows the removal of a message on a readable object
*/
public class Erase extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		//find a writing implement to use to write with
		if( !validateErasingImplement() )
			return true;
		
		if( !eraseMessage() )
			return true;

		//confirm the action to the actor
		actor.output( current.getString("erase_msg") );
		
		//confirm the action to the rest of the room
		container.output( current.getString("erase_omsg"), actor );
		
		return true;
	}
	
	
	/**
	* Tests whether there is a message on the thing-to-be-erased.
	* If there is - erase it.
	*/
	protected boolean eraseMessage()
	{
		//check whether there is something to erase on the object
		String message = current.getString("text");
		
		if(message == null || message.length() < 1)
		{
			actor.output( current.getString("erase_no_msg") );
			return false;
		}
		//otherwise, lets erase this message (set it to nothing);
		current.setString("text", null);
		
		return true;
	}
	
	/**
	* Searches the inventory for a atom the inherits from another atom
	*/
	protected boolean validateErasingImplement()
	{
		Atom erasingImplement;
		if( event.getArg(1) == null  )
		{
			erasingImplement =  AtomUtil.findDescendant( actor, current.getAtom("eraser") );
			if( erasingImplement == null )
				actor.output( current.getString("eraser_missing_msg") );
		}
		else
		{
			erasingImplement = (Atom)event.getArg(1);
		}
		
		if( erasingImplement == null )
			return false;
		else
			return true;
	}
	
}