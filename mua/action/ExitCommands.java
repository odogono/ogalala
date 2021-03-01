// $Id: ExitCommands.java,v 1.33 1999/04/29 10:01:35 jim Exp $
// Exit commands
// James Fryer, 16 Oct 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.util.*;
import com.ogalala.mua.*;

/** Dig an exit
*/
public class ModDig
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    /** The direction of the exit in the 'from' container
    */
    protected int fromDirection;

    /** Are we creating a one-way exit?
    */
    protected boolean isOneWay = false;

    /** The container we are starting from
    */
    protected Atom fromContainer;

    /** The atom to instantiate for the 'from' exit
    */
    protected Atom fromExit;

    /** The direction of the exit in the 'to' container
    */
    protected int toDirection;

    /** The container we are going to
    */
    protected Atom toContainer;

    /** The atom to instantiate for the 'to' exit
    */
    protected Atom toExit;
    
    public boolean execute()
        {
        if (event.getArgCount() < 3)
            throw new ParserException("!DIG direction from to [ from-exit-atom [ to-exit-atom ] ]");
        parseArgs();
        checkExits();
        createExits();
        linkExits();
        actor.output((isOneWay ? "One way exit" : "Exit") + " added between " + AtomData.toString(fromContainer) + " and " + AtomData.toString(toContainer));
        return true;
        }
        
    protected void parseArgs()
        {
        // Get the direction flags
        parseDirectionSpec(event.getArg(0).toString());
        
        // Get the 'from' container
        String fromName = event.getArg(1).toString();
        fromContainer = world.getAtom(fromName);
        if (fromContainer == null)
            throw new AtomException("Atom not found: " + fromName);
            
        // Get the 'to' container
        String toName = event.getArg(2).toString();
        toContainer = world.getAtom(toName);
        if (toContainer == null)
            throw new AtomException("Atom not found: " + toName);
            
        // Get the 'from' exit atom
        if (event.getArgCount() > 3)
            {
            String toExitName = event.getArg(3).toString();
            fromExit = world.getAtom(toExitName);
            if (fromExit == null)
                throw new AtomException("Atom not found: " + toExitName);
            }
        else {
            fromExit = world.getAtom("exit");
            com.ogalala.util.Debug.assert(fromExit != null, "(ExitCommands.java/87)");
            }
                
        // Get the 'to' exit atom
        if (event.getArgCount() > 4)
            {
            if (isOneWay)
                throw new ParserException("No return exit required in one-way exit.");
            else {
                String toExitName = event.getArg(4).toString();
                toExit = world.getAtom(toExitName);
                if (toExit == null)
                    throw new AtomException("Atom not found: " + toExitName);
                }
            }
        else
            toExit = fromExit;
        }
    
    /** Parse a two-way direction specifier in the format:
        <pre>
        DIRECTION_LIST [ "-" DIRECTION_LIST ]
        
        DIRECTION_LIST ::= DIRECTION [, DIRECTION]...
        
        DIRECTION ::= "NORTH", "SOUTH", etc.
        </pre>
    */
    protected void parseDirectionSpec(String directionSpec)
        {
        // Break the string into the "from" and "to" sides
        StringTokenizer st = new StringTokenizer(directionSpec, "-");
        
        // Parse the "from" side
        String s = st.nextToken();
        fromDirection = parseDirection(s);
        
        // Parse the "to" side if any. If this is "NONE" then we are creating
        //  a one-way exit.
        if (st.hasMoreTokens())
            {
            s = st.nextToken();
            if ("none".equalsIgnoreCase(s))
                isOneWay = true;
            else 
                toDirection = parseDirection(s);
            }
        }
    
    /** Parse a one-way direction spec, e.g. "NORTH,OUT"
    */
    private int parseDirection(String directionLabels)
        {
        int result = 0;
        StringTokenizer st = new StringTokenizer(directionLabels, ";,");
        while (st.hasMoreTokens())
            {
            String s = st.nextToken();
            int direction = ExitTable.toDirection(s);
            if (direction == 0)
                throw new ParserException("Bad direction: " + s);
            result |= direction;
            }
        return result;
        }

    /** Create the exit Things
    */
    protected void createExits()
        {
        fromExit = world.newThing(null, fromExit);
        if (!isOneWay)
            toExit = world.newThing(null, toExit);
        }

    /** Check if there are exits already defined in any of the directions
    */
    protected void checkExits()
        {
        if (fromContainer.getExit(fromDirection) != null)
            throw new AtomException("Container " + fromContainer.getID() + " already has exit leading " + ExitTable.toString(fromDirection));
        if (!isOneWay)
            {
            if (toContainer.getExit(toDirection) != null)
                throw new AtomException("Container " + toContainer.getID() + " already has exit leading " + ExitTable.toString(toDirection));
            }
        }
        
    /** Link the exits into the world
    */
    protected void linkExits()
        {
        if (isOneWay)
            world.addExit(fromDirection, fromContainer, fromExit, toContainer);
        else {
            if (toDirection == 0)
                toDirection = ExitTable.getOppositeDirection(fromDirection);
            world.addExit(fromDirection, fromContainer, fromExit, toDirection, toContainer, toExit);
            }
        }
    }


/** Remove an exit
*/
public class ModUnDig
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    /** The direction of the exit we are removing
    */
    protected int direction;

    /** The container the exit starts from
    */
    protected Atom fromContainer;

    /** The container the exit goes to
    */
    protected Atom toContainer;

    public boolean execute()
        {
        if (event.getArgCount() != 2)
            throw new ParserException("!UNDIG direction container");
        parseArgs();
        world.removeExit(direction, container);
        if (toContainer != null)
            actor.output("Exit between " + AtomData.toString(fromContainer) + " and " + AtomData.toString(toContainer) + " removed.");
        else
            actor.output("Exit from " + AtomData.toString(fromContainer) + " removed.");
        return true;
        }
    
    /** Get the direction and containers
    */
    private void parseArgs()
        {
        // Direction
        String directionLabel = event.getArg(0).toString();
        direction = ExitTable.toDirection(directionLabel);
        if (direction == 0)
            throw new ParserException("Bad direction: " + directionLabel);
            
        // "From" container
        String containerID = event.getArg(1).toString();
        fromContainer = world.getAtom(containerID);
        if (fromContainer == null)
            throw new AtomException("Atom not found: " + containerID);

        // "To" container
        Object exit = fromContainer.getExit(direction);
        if (exit == null)
            throw new AtomException("No exit " + ExitTable.toString(direction) + " from container " + fromContainer.getID());
        else if (exit instanceof Atom)
            {
            Atom atom = (Atom)exit;
            toContainer = atom.getAtom("destination");
            }
        }
    }

    
/** Go in a direction
*/
public class Go
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public static final String ON_ENTER = "on_enter";
    public static final String ON_EXIT = "on_exit";
    public static final String DESTINATION = "destination";
    public static final String BLOCK = "block";
    public static final String LOOK = "look";
    public static final String OTHER_SIDE = "other_side";
    public static final String GO_MSG = "go_msg";
    public static final String ENTER_OMSG = "enter_omsg";
    public static final String EXIT_OMSG = "exit_omsg";
    public static final String IS_CLOSED = "is_closed";
    public static final String IS_CLOSED_MSG = "is_closed_msg";
    
    /** The container we are leaving
    */
    protected Atom fromContainer;
    
    /** The container we are going to
    */
    protected Atom toContainer;
    
    /** An atom blocking the door
    */
    protected Atom block;
    
    public boolean execute()
        {
        // Get the properties
        fromContainer = current.getContainer();
        toContainer = current.getAtom(DESTINATION);
        block = current.getAtom(BLOCK);
        
        // Call the pre-processing hook
        if( !beforeGo() )
        	return true;

        // Ask the 'from' container if we can leave it
        if (!world.callEvent(actor, ON_EXIT, fromContainer))
            return true;
        
        // Check for blocks
        //###
        
        // Check if the exit is open
        if (current.getBool(IS_CLOSED))
            {
            actor.output(current.getString(IS_CLOSED_MSG));
            return true;
            }
        
        // Check the hook function
        if (!onGo())
            return true;
            
        // See if we can enter the 'to' container
        if (!world.callEvent(actor, ON_ENTER, toContainer))
            return true;
            
        // Move to the new container and notify actor
        world.moveAtom(actor, toContainer);
        
        //broadcast going and leaving messages to all involved
        broadCastMessages();
        
        // Call the post-processing hook
        afterGo();

        // Do a LOOK command
        return world.callEvent(actor, LOOK, world.getRoot());
        }
    
    /** Hook function, called before any processing takes place
    */
    protected boolean beforeGo()
        {
        return true;
        }

    /** Hook function, called during processing. If it returns false,
        the operation will not succeed.
    */
    protected boolean onGo()
        {
        return true;
        }
    
    /**
    *	Sends out messages to the actor, and all who witnessed
    */
    protected void broadCastMessages()
    	{
    	actor.output(current.getString(GO_MSG));
        
        // Notify actor/users. This is mildly complex:
        //  - If the exit has an exit message display it
        //  - Else display the "from" container's exit message
        //  - If the "other side" has an enter message display it
        //  - Else display the "to" container's enter message
        String s = current.getString(EXIT_OMSG);
        if (s == null)
            s = fromContainer.getString(EXIT_OMSG);
        fromContainer.output(s, actor);

        Atom other_side = current.getAtom(OTHER_SIDE);
        if (other_side != null)
            s = other_side.getString(ENTER_OMSG);
        else
            s = null;
        if (s == null)
            s = toContainer.getString(ENTER_OMSG);
        toContainer.output(s, actor);
    	}


    /** Hook function, called after processing takes place
    */
    protected void afterGo()
        {
        }
    }
    
    
/**
*	A restricted exit needs some sort of pass to allow traversal
*/
public class RequiredItemGo 
    extends Go
    {
    private static final long serialVersionUID = 1;
    
	protected boolean onGo()
	    {	
		//are there any items which match the criteria ?
		//if( actorContents != null && actorContents.hasMoreElements() )
		if( AtomUtil.containsDescendant( actor, current.getAtom("required_item") ) )
		    {
			//display the success message
			actor.output(current.getString("success_msg"));
			
			//hand back to the superclass to complete the movement transaction
			return true;
    		}
		
		actor.output(current.getString("failure_msg"));
		return false;
	    }
	
    }

/**
*	Effectively the opposite of the RequiredItemGo, in that if the actor is carrying a specified
*	item, they will not be allowed to pass.
*/
public class DenyItemGo 
    extends Go
    {
    private static final long serialVersionUID = 1;
    
	protected boolean onGo()
	    {
		//does the actor have this item in their possession ?
		if( !AtomUtil.containsDescendant(actor, current.getAtom("denyed_item")) )
		{
			//they do, so let them pass
			actor.output(current.getString("success_msg"));
			return true;
		}
		
		//they have this item, so deny them access and print a message saying so
		actor.output(current.getString("failure_msg"));
		return false;
	    }	    
    }

/**
*   Picks out an exit in the current room, and tries to send the actor 
*   through it
*   This action acts as an intermediary between the actors command and
*   the go action.
*/
public class GoRandom extends JavaAction
{
    /**
    *   The main execute function
    */
    public boolean execute()
    {
        //get a random object
        Atom exit = chooseRandomExit();
        
        //if there are no exits, we cannot go anywhere !
        if( current == null )
            return true;
        
        //now we need to send off the a new event, containing the specified
        // exit
        Event event = world.newEvent(actor, "go", exit );
		world.postEvent(event);
		
        return true;
    }
    
    /**
    *   Selects an exit randomly from the room the actor is inhabiting.
    *   If there are no exits, the method will return null.
    */
    private final Atom chooseRandomExit()
    {
        //get a list of all the exits in the room
        Vector exits = (Vector)AtomData.enumToVector( newExitEnumeration() );
        
        if( exits.size() == 0 )
            return null;
        
        if( exits.size() == 1 )
        	return (Atom)exits.elementAt(0);
        	
        int random = com.ogalala.util.Dice.roll( exits.size() ) -1;
        
        return (Atom)exits.elementAt( random );
    }
    
    /**
    *   Returns an enumeration of all the exits in the actors container
    */
    private Enumeration newExitEnumeration()
    {
        return new ListenEnumeration( actor.getContainer(), world.getAtom("exit") );
    }
}


/** Go into a container
*/
public class GoIn
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public static final String IS_CLOSED = "is_closed";
    public static final String IS_CLOSED_MSG = "is_closed_msg";
    public static final String GO_IN_MSG = "go_in_msg";
    public static final String GO_IN_OMSG = "go_in_omsg";
    public static final String LOOK = "look";
    public static final String SIZE = "size";
    public static final String MOBILE_TOO_BIG_MSG = "mobile_too_big_msg";

    public boolean execute()
        {
        // Get the properties
        Atom fromContainer = actor.getContainer();
        Atom toContainer = current;

        // Check if the container is open
        if (toContainer.getBool(IS_CLOSED))
            {
            actor.output(toContainer.getString(IS_CLOSED_MSG));
            return true;
            }
        
        // Check if the actor can fit
        if (actor.getInt(SIZE) > toContainer.getInt(SIZE))
            {
            actor.output(toContainer.getString(MOBILE_TOO_BIG_MSG));
            return true;
            }
        
        // Move to the new container, notify actor and room
        world.moveAtom(actor, toContainer);
        actor.output(toContainer.getString(GO_IN_MSG));
        fromContainer.output(toContainer.getString(GO_IN_OMSG), actor);

        // Do a LOOK command
        return world.callEvent(actor, LOOK, world.getRoot());
        }
    }

/** Go out of a container into its supercontainer
*/
public class GoOut
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public static final String IS_CLOSED = "is_closed";
    public static final String GO_OUT_MSG = "go_out_msg";
    public static final String GO_OUT_OMSG = "go_out_omsg";
    public static final String LOOK = "look";
    public static final String GO = "go";

    public boolean execute()
        {
        // Get the properties
        Atom fromContainer = actor.getContainer();
        Atom toContainer = fromContainer.getContainer();

        // If the container has an OUT exit, pass the event on to the normal GO handler.
        Atom outExit = (Atom)fromContainer.getExit(ExitTable.DIR_OUT);
        if (outExit != null)
            return world.callEvent(actor, GO, outExit);
            
        // Check if the container is open
        if (fromContainer.getBool(IS_CLOSED))
            {
            actor.output(fromContainer.getString("is_closed_msg"));
            return true;
            }
        
        // Move to the new container, notify actor and room
        world.moveAtom(actor, toContainer);
        actor.output(fromContainer.getString(GO_OUT_MSG));
        toContainer.output(fromContainer.getString(GO_OUT_OMSG), actor);

        // Do a LOOK command
        return world.callEvent(actor, LOOK, world.getRoot());
        }
    }

/** Block an exit
*/
public class BlockExit
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        actor.output("Not implemented");
        return true;
        }
    }

/** This class handles the functionality of the generic exit
*/
public class Exit 
    implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;
    
    public static String OTHER_SIDE = "other_side";
    public static String DESTINATION = "destination";
    
    /** Each exit has two sides, this function returns the twin
        exit in the 'other room'
    */
    public Thing getOtherExit( Atom atom )
        {
        return ( Thing ) atom.getAtom( OTHER_SIDE );
        }
    
    /** An exit leads to another room / container. This function
        returns that other room / container.
    */
    public Container getOtherRoom( Atom atom )
        {
        return ( Container ) atom.getAtom( DESTINATION );
        }
    }

/** This class extends the functionality of an exit by providing 
    open / close.
*/
class Door implements 
    java.io.Serializable
    {
    private static final long serialVersionUID = 1;
    
    Exit exit = new Exit();
    Openable openable = new Openable();

    Atom otherDoor;
    
    /** given a door type exit, this function will do the action
        of opening both sides
    */
    public boolean open( Atom current )
        {
        openable.open( current );
        
        otherDoor = exit.getOtherExit( current );
        openable.open( otherDoor );
        return true;
        }
        
    /** given a door type exit, this function will do the action
        of opening both sides
    */
    public boolean close( Atom current )
        {
        openable.close( current );

        otherDoor = exit.getOtherExit( current );
        openable.close( otherDoor );
        return true;
        }
    }

/** This class represents the extention of a door to a lockable door.
*/
class LockableDoor 
    implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;
    
    Lockable lockable = new Lockable();
    
    Atom otherDoor;
    Exit exit = new Exit();
    
    /** Given an lockable exit this function will lock
        both sides of the door
    */
    public boolean lock( Atom current )
        {
        lockable.lock( current );    

        otherDoor = exit.getOtherExit( current );
        lockable.lock( otherDoor );
        return true;
        }

    /** Given an lockable exit this function will unlock
        both sides of the door
    */
    public boolean unlock( Atom current )
        {
        lockable.unlock( current );    

        otherDoor = exit.getOtherExit( current );
        lockable.unlock( otherDoor );
        return true;
        }
    }


/** Open a door
*/
public class OpenDoor
    extends Open
    {
    private static final long serialVersionUID = 1;
    
    Container otherRoom;

    Exit exit = new Exit();
    Door door = new Door();

    /** setup locals
    */
    public void init()
        {
        super.init();
        otherRoom = exit.getOtherRoom( current );
        }
        
    /** open both exits
    */
    public boolean tryAction()
        {
        door.open( current );
        return true;
        }
    
    /** inform the parties
    */
    public void successAction()
        {
        // ### better message handling req.
        otherRoom.output( "the door opens ..." );
        super.successAction();
        }
    }

/** Close a door
*/
public class CloseDoor
    extends Close
    {
    private static final long serialVersionUID = 1;
    
    Container otherRoom;
    
    Exit exit = new Exit();
    Door door = new Door();
    
    /** setup locals
    */
    public void init()
        {
        super.init();
        otherRoom = exit.getOtherRoom( current );
        }

    /** open both exits
    */
    public boolean tryAction()
        {
        door.close( current );
        return true;
        }
    
    /** inform the parties
    */
    public void successAction()
        {
        // ### better message handling req.
        otherRoom.output( "the door closes ..." );
        super.successAction();
        }
    }

/** Lock a door
*/
public class LockDoor
    extends Lock
    {
    private static final long serialVersionUID = 1;
    
    Container otherRoom;
    Thing otherDoor;
  
    Exit exit = new Exit();
    LockableDoor door = new LockableDoor();
    
    /** setup locals
    */
    public void init()
        {
        super.init();
        otherRoom = exit.getOtherRoom( current );
        }

	/** do the unlock action
	*/
	protected boolean tryAction()
	    {
        door.lock( current );
        return true;
	    }

  	/** the doAction is succesfull
	*/
	protected void successAction()
		{
        super.successAction();
        otherRoom.output( "The door clicks ..." );
		}
    }

/** Unlock a door
*/
public class UnlockDoor
    extends Unlock
    {
    private static final long serialVersionUID = 1;
    
    Container otherRoom;
    Exit exit = new Exit();

    LockableDoor door = new LockableDoor();
    
    /** setup locals
    */
    public void init()
        {
        super.init();
        otherRoom = exit.getOtherRoom( current );
        }

	/** do the unlock action
	*/
	protected boolean tryAction()
	    {
        door.unlock( current );
        return true;
	    }

  	/** the doAction is succesfull
	*/
	protected void successAction()
		{
        super.successAction();
		otherRoom.output( "The door clicks ..." );
		}
    }

public class OpenLockableDoor
    extends OpenLock
    {
    private static final long serialVersionUID = 1;
    
    Container otherRoom;
    Thing otherDoor;

    Exit exit = new Exit();
    Door door = new Door();
    
    /** setup locals
    */
    public void init()
        {
        super.init();
        otherRoom = exit.getOtherRoom( current );
        }
        
    /** open both exits
    */
    public boolean tryAction()
        {
        door.open( current );
        return true;
        }
    
    /** inform the parties
    */
    public void successAction()
        {
        // ### better message handling req.
        otherRoom.output( "the door opens ..." );
        super.successAction();
        }
    }

/** Attack a door
*/
public class AttackDoor
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        actor.output("Take that you god damned door !slap!, and that !kick! ... Actually this is not really implemented but continue if you wish.");
        return true;
        }
    }
    
