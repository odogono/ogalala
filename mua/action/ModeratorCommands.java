// $Id: ModeratorCommands.java,v 1.11 1999/07/08 14:42:05 jim Exp $
// Moderator commands
// James Fryer, 18 Sept 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;
import com.ogalala.util.*;

//import com.ogalala.test.mua.*;
/** !GO
*/
public class ModGo
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!GO containerID");

        // Get the container to go to
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);
        if (!(atom instanceof Container))
            throw new AtomException("You can only move to containers.");
        Container toContainer = (Container)atom;

		// See if we can enter the 'to' container
        if (!world.callEvent(actor, "on_enter", toContainer))
            return true;
        
        // Move the actor to the new container
        world.moveAtom(actor, toContainer);
        
		// Notify users
		Atom fromContainer = actor.getContainer();
        String s = fromContainer.getString("exit_omsg");
        fromContainer.output(s, actor);
        s = toContainer.getString("enter_omsg");
        toContainer.output(s, actor);

        // Report back to the user
		actor.output("You teleport to location " + toContainer.getID() + " (" + toContainer.getString("name") + ").");
		
        // Call the LOOK command to display the new location details
        return world.callEvent(actor, "look", current);
        }
    }
    
/** !GET
*/
public class ModGet
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!GET atomID");

        // Get the atom
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);
        if (!(atom instanceof Thing))
            throw new AtomException("Atoms cannot be picked up.");

        // Move the atom to the user's inventory
        //### MAgic moderator's bag?
        world.moveAtom(atom, actor);
        
        // Report back to the user
		actor.output("You get atom " + atom.getID() + " (" + atom.getString("name") + ").");
        
        return true;
        }
    }
        
/** !DROP
*/
public class ModDrop
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!DROP atomID [containerID]");

        // Get the thing
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);
        if (!(atom instanceof Thing))
            throw new AtomException("Atoms cannot be contained.");
            
        // The thing must be in the actor's inventory
        if (atom.getContainer() != actor)
            throw new AtomException("You don't have atom " + atom.getID() + ".");
        
        // Get the destination (default is the actor's container)
        Atom destination = actor.getContainer();
        if (event.getArgCount() > 1)
            {
            atomID = event.getArg(1).toString();
            destination = world.getAtom(atomID);
            if (destination == null)
                throw new AtomException("Atom not found: " + atomID);
            if (!(destination instanceof Container))
                throw new AtomException("Not a container: " + atomID);
            }

        // Move the atom to the new location
        world.moveAtom(atom, destination);
        
        // Report back to the user
        actor.output("You drop atom " + atom.getID() + " (" + atom.getString("name") + ").");

        return true;
        }
    }

/** !ROLL
*/
public class ModRollDice
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Default to 1d100
        String s = "1d100";
        if (event.getArgCount() > 0)
            {
            // Need to join all the args together because they are split after the first number
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < event.getArgCount(); i++)
                buf.append(event.getArg(i).toString());
            s = buf.toString();
            }
            
        // Roll the dice and report to the user
        Dice dice = new Dice(s);
		actor.output("You rolled " + dice.roll());
        
        return true;
        }
    }

//----------------------------------------------------------------------------------

/**
*	The NarrowCast command allows a message(string) to be sent to
*	a defined subset of actors in the world.
*
*	The syntax is:
*
*		!NarrowCast string room property ...
*
*	where:
*
*		string		-	is the quoted message to send to the subset. 
*						string is required.
*
*		place		-	is the starting place to narrowcast from. The message will
*						be picked up by actors in this room, and all rooms contained
*						within. So narrowcasting from Limbo, would result in all
*						actors receiving the message.
*						place is required.
*
*		property	-	A condition that must be satisfied by the actor in order for
*						them to receive the message. 
*						property is optional.
*/
public class ModNarrowCast extends JavaAction
{
	//--------------------------------------------------------------
	//  class variables
	private static final long serialVersionUID = 1;
	
	//--------------------------------------------------------------
	//  instance variables
	
	/** The room in the 
	*/
	protected Atom startRoom;
	
	//--------------------------------------------------------------
	//  main action execution
	public boolean execute()
	{
		init();
		
		return true;
	}
	//--------------------------------------------------------------
	
	
	//--------------------------------------------------------------
	// initialise the action
	protected void init()
	{
		
	}
}