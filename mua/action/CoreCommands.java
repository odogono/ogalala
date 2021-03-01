// $Id: CoreCommands.java,v 1.67 1999/03/24 13:27:16 alex Exp $
// Misc commands 
// James Fryer, 18 Sept 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;

/** GET
*/
public class Get extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public static final String ON_ADD = "on_add";
	public static final String ON_REMOVE = "on_remove";
	
    /** The location of the thing to get
    */
    protected Atom location;
    
    public boolean execute()
    {
        location = container;
        doGet();
        return true;
    }
        
    protected void doGet()
    {
        // See if the actor is strong enough
        int weight = current.getInt("weight");
        int strength = actor.getInt("strength") * 1000;
        if ( weight >= strength )
            {
    		actor.output(current.getString("too_heavy_msg"));
    		return;
            }
		
		Object args[] = {current};
		
		// See if we can add the current to the actor
		if( !world.callEvent(actor, ON_ADD, actor, args) )
			return;
			
        // Move the object to the actor's bag and notify users
        world.moveAtom(current, actor);
        container.output(current.getString("get_omsg"), actor);
        actor.output(current.getString("get_msg"));
    
        return;
    }
}

/** DROP
*/
public class Drop extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public static final String ON_ADD = "on_add";
	public static final String ON_REMOVE = "on_remove";
	
    public boolean execute()
    {
        //wrap the argument (what we are dropping) into an array
        Object args[] = {current};
		
		// See if we can remove the object from the actor
		// We are REMOVING the current from the actor
		if( !world.callEvent(actor, ON_REMOVE, actor, args) )
			return true;
		
		// See if we can move the current into the container
		// We are ADDing the current into the container
		if( !world.callEvent(actor, ON_ADD, container, args) )
			return true;
		
        // move the atom from the actor to the current room and notify users
        world.moveAtom(current, container);
        container.output(current.getString("drop_omsg"), actor);
		actor.output(current.getString("drop_msg"));
        return true;
    }
}

/**
* Give transmits an object in the possession of the actor (enforced by the parser),
* to another mobiles contents. Simple as that really.
*/
public class Give extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		//check that the proposed destination is really a mobile
		if( ! (event.getArg(0) instanceof Mobile) )
		{
			actor.output( "You can't give something to that." );
			return true;
		}
		
		Mobile destination = (Mobile)event.getArg(0);
		
		//check that the receiving mobile can receive this item
		//### insert checking code here
		
		//give the atom to the Mobile destination
		world.moveAtom(current, destination);
		
		//Report the transaction to the actor
		actor.output( current.getString("give_msg") );
		
		//Report the transaction to the receiving actor
		destination.output( current.getString("receive_msg") );
		
		//Report the transaction to everyone else
		container.output( current.getString("give_omsg"), actor, destination );
		
		return true;
	}
}


/** 
*/
public abstract class MoveTo extends JavaAction
{
    Atom object;
    Container destination;
    Container source;    

    Openable openable = new Openable();
    ContainerUtil containerUtil = new ContainerUtil();
    
    public boolean execute()
    {
        // do initalization functions
        init();
        
        // Is the container closed??
        if ( isClosed() )
        {
            destinationClosed();
            return true;
        }
            
        // Will the object fit in the container?
        if ( !fits() )
        {
            doesNotFit();
            return true;
        }
            
        // Is the container full?
        else if ( isRoomFor() )
        {
            destinationFull();
            return true;
        }
            
        // Move the atom and notify users
        else 
        {
            doMove();
            broadcastMove();
        }
        
        return true;
    }

    /** setup the member varibles for this class
    */
    public abstract void init();
    
    /** it the destination object closed???
    */
    public boolean isClosed()
    {
        return !openable.isOpen( destination );
    }

    /** the destination object is closed do the messages
    */
    public abstract void destinationClosed();

    /** does the object fit inside the container??
    */
    public boolean fits()
    {
        return containerUtil.fits( destination, object );
    }

    /** the object does not fit inside the container
    */
    public abstract void doesNotFit();

    /** is the container too full to accept this object??
    */
    public boolean isRoomFor()
    {
        return containerUtil.isRoomFor( destination, object );
    }

    /** the destination is full
    */
    public abstract void destinationFull();

    /** move the atom from one container to another
    */
    public void doMove()
    {
        world.moveAtom( object , destination );
    }

    /** the move has happen notifiy the observers the actors
    */
    public abstract void broadcastMove();
}

/** Container utility functions ...
*/
class ContainerUtil implements Serializable
{
	private static final long serialVersionUID = 1;

    protected final static String CAPACITY = "capacity";
    protected final static String SIZE = "size";

    /** Will the object fit in the container?
    */
    boolean fits( Container container, Atom object )
    {
        int size = object.getInt( SIZE );
        int capacity = container.getInt( SIZE );
        return  size <= capacity;
    }
    
    /** is there room in the container for the object??
    */
    boolean isRoomFor( Container container, Atom object )
    {
        return object.getCount() >= container.getInt( CAPACITY );
    }
}

/** PUT ... IN ...
*/
public class PutIn extends MoveTo
{

	private static final long serialVersionUID = 1;
	
    public void init()
    {
        // Get the object we are moving
        object = ( Atom ) current;
        
        // Get the container we will move the object into
        destination = ( Container ) event.getArg(0);
    }

    public void destinationClosed()
        {
        actor.output( destination.getString( "dest_closed_msg") );
        // ##previously, this would display the current as being closed instead of the
        // destination object. So a new message has been added, which kind of unneatens things a bit. :(
        //actor.output( destination.getString( "is_closed_msg" ) );
        }

    public void destinationFull()
        {
        actor.output( destination.getString( "is_full_msg" ) );
        }

    public void broadcastMove()
        {
        container.output( destination.getString( "put_in_omsg" ), actor );
        actor.output( destination.getString( "put_in_msg" ) );
        }

    public void doesNotFit()
        {
        actor.output( container.getString( "too_big_msg" ) );
        }
}

    
/** this action 'sits' an actor in a seat (container). This 
    uses SeatUtil to obtain the messages associated with the process
*/
public class SitOn 
    extends PutIn
    {
    private static final long serialVersionUID = 1;
    
    static final SeatUtil seat = new SeatUtil();    
        
    /** current is the seat we are going to sit on
    */
    public void init()    
        {
        // Get the objects we are moving
        object = actor;
        destination = ( Container ) current;
        }

    /** people always can fit in a chair
    */
    public boolean fits()
        {
        return true;
        }

    public void destinationFull()
        {
        actor.output( destination.getString( seat.SIT_FULL_MSG ) );
        }

    public void broadcastMove()
        {
        container.output( destination.getString( seat.SIT_OMSG ), actor );
        actor.output( destination.getString( seat.SIT_MSG ) );
        }

    public void doesNotFit()
        {
//        actor.output( current.getString( SIT_FULL_MSG ) );
        }
    }


/** Utility funcitions and key strings for the seat atom
*/
class SeatUtil
    implements Serializable
    {
    private static final long serialVersionUID = 1;
    
    // perminate ref to the seat Atom
    static Atom seatAtom;

	final static String SIT_MSG = "seat_sit_msg"; // "You sit on {-t name}."
	final static String SIT_OMSG = "seat_sit_omsg"; // "{-u actor.name} sits on {-t name}."
	final static String SIT_FULL_MSG = "seat_full_msg"; // "There is not enough room to sit on {-t name}."

    final static String STAND_MSG = "stand_msg"; //"You get up from {-t Container.name}."
    final static String STAND_OMSG = "stand_omsg"; //"{-u actor.name} stands up from {-t container.name}."
    final static String STAND_FAIL_MSG = "stand_fail_msg"; //"You are not sitting on anything."

    // name of the seat atom
    final static String SEAT ="seat";

    /** This function checks to see if the pasted in atom 
        in a descendant of the seat atom.
    */
    boolean isASeat( Atom seat )
        {
        // if seat is equal to null then we KNOW it is not a seat
        if ( seat == null )
            return false;
        
        // do we have a ref to the seat atom yet?
        if ( seatAtom == null )
            {
            // get atom from DB
            seatAtom = seat.getWorld().getAtom( SEAT );
            }
 
        return seat.isDescendantOf( seatAtom );
        }
    }

/** Standup - this action is tided to the root atom as it has no 'current'.
    Hence this action needs to deal with two separate situtations, when you are
    standing up from a seat and when you are standing from sitting on the ground.
    At the moment there is no implementation for sitting down with out a seat so 
    this functionality is missing.
*/
public class Stand
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    Atom seat;
    Atom destination;

    SeatUtil seatUtil = new SeatUtil();
    
    /** output pkt for actor
    */
    OutPkt outActor;

    /** output pkt for observers
    */
    OutPkt outOther;
    
    /** This init function does two things. Firstly it gains a refrence 
        to the container the once the actor is currently in and assigns 
        this to the member variable seat. Also it prepares the output pkts 
        for a successfull stand. This is because once the actor has been moved 
        we lose the context information which includes the seat that they left.
    */
    void init()
        {
        // the actors current container would be the seat ...
        seat = container;
        
        // the actors containers container is the 'room' they will stand up in
        destination = container.getContainer();
        }

    /** once the actor has been moved we lose the context information which 
        includes the chair. However we must be sure we are sitting before we
        can prepare these messages
    */         
    void prepareMessages()
        {
      	// construct the message for others
        String omsg = seat.getString( seatUtil.STAND_OMSG );
		outOther = new OutPkt( "misc", "msg", event.formatOutput( omsg ) );

		// construct the message for actor
        String msg = seat.getString( seatUtil.STAND_MSG );
		outActor = new OutPkt( "misc", "msg", event.formatOutput( msg ) );
        }

    boolean isSittingInAContainer()
        {
        return seatUtil.isASeat( seat );
        }
    
    boolean tryStandUpFromContainer()
        {
        world.moveAtom( actor, destination );
        return true;
        }

    void successfulStandUpFromContainer()
        {
        actor.output( outActor );
        destination.output( outOther );
        }
    
    void failedStandUpFromContainer()
        {
        // currently this is imposible ...    
        }

    void notSittingOnASeatMsg()
        {
        actor.output( "you are not sitting ..." );
        }
    
    public boolean execute()
        {
        // set up the member varibles
        init();
        
        // is the actor is sitting in a container?
        if ( isSittingInAContainer() )
            {
            prepareMessages();    
                
            // try and stand
            if ( tryStandUpFromContainer() )
                {
                // stand msgs
                successfulStandUpFromContainer();
                }            
            else
                {
                // cannot stand messages
                failedStandUpFromContainer();
                }
            }
        else 
            {
            // we should check now to see if they are sitting on the ground ...
            
            // fail, they are not sitting in a container
            notSittingOnASeatMsg();
            }
        
        // return handled
        return true;
        }
    }

/** HELP
	<p>
	The default help file is "default.help". If an argument is passed to the 
	HELP command then it will have ".help" appended to it and a file of this 
	name will be used. The file has newlines removed and is sent to the user
	as the MSG part of a HELP packet.	
*/
public class Help
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        String helpContext = null;
        if (event.getArgCount() > 0)
            helpContext = event.getArg(0).toString();
        String helpMsg = readHelpFile(world, helpContext);
		actor.output(new OutPkt("help", "msg", helpMsg));
        return true;
        }

    /** Read a help file and format it into a help packet.
    */
    private String readHelpFile(World world, String helpContext)
        {
        // Build the file name
        String fileName = "default";
        if (helpContext != null && helpContext.length() > 0)
            fileName = helpContext.trim().toLowerCase();
        fileName = fileName + ".help";
        
        // Look for the file
        File file = world.findFile(fileName);
        if (!file.exists())
            return "No help available for " + helpContext;
            
        // Convert the file into a string with newlines removed
        try {
            StringBuffer result = new StringBuffer();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String s;
            while ((s = in.readLine()) != null)
                result.append(s);
            in.close();
             
            return result.toString();
            }
            
         //### Inconsistency here as this is not an error packet... but this
         //###  seems more helpful somehow.
         catch (IOException e)
            {
            return "Can't read help file: " + fileName;
            }
        }
    }   

/** Base class for factory actions
*/
abstract public class FactoryAction
    extends JavaAction
    {
    static final String FACTORY_TEMPLATE = "factory_template";
    static final String FACTORY_COUNT = "factory_count";
    static final String FACTORY_MSG = "factory_msg";
    static final String FACTORY_OMSG = "factory_omsg";
    static final String FACTORY_END_MSG = "factory_end_msg";
    
    /** The type of atom this factory will create
    */
    protected Atom template;
    
    /** The number of atoms we can create
    */
    protected int count;
    
    /** Message to display to actor when factory operates 
    */
    protected String factory_msg;

    /** Message to display to room when factory operates 
    */
    protected String factory_omsg;
    
    /** Message to display to all when count reaches 0
    */
    protected String factory_end_msg;
    
    /** User-supplied function. Return true if the event is handled.
    */
    protected abstract boolean factoryExecute();
    
    public boolean execute()
        {
        // Initialise member variables
        init();
        
        // Call the user's function
        boolean result = factoryExecute();
        
        // If the factory has only one item left, replace it with the template object
        if (count <= 1)
            {
            // Create one object in the container
            make(container);
            
            // Remove the exhausted factory from the world.
            deleteFactory();
            }
        
        return result;
        }
        
    /** Create a new instance of the template in 'where'
    */
    protected void make(Atom where)
        {
        // If the count is 0 or less, do nothing. This should not happen unless the 
        //  factory is improperly set up.
        if (count <= 0)
            return;
        
        // Create the new Thing
        Atom atom = world.newThing(null, template);
        
        // Move it to its new location
        world.moveAtom(atom, where);
        
        // Send the user messages
        if (factory_msg != null)
            actor.output(factory_msg);
        if (factory_omsg != null)
            container.output(factory_omsg, actor);
                
        // Update the count and write it back
        --count;
        current.setInt(FACTORY_COUNT, count);
        }
    
    /** Set up the instance variables
    */
    private void init()
        {
        // Get the template atom
        template = current.getAtom(FACTORY_TEMPLATE);
        if (template == null)
            throw new AtomException("Template atom not defined.");
        
        // Get the number of objects we are allowed to create
        count = current.getInt(FACTORY_COUNT);
        
        // If the factory count is 0 or less, there is no point in continuing as 
        //  we will never use the messages anyway. This should not happen in practice.
        if (count <= 0)
            return;
        
        // Get the messages
        factory_msg = current.getString(FACTORY_MSG);
        factory_omsg = current.getString(FACTORY_OMSG);
        factory_end_msg = current.getString(FACTORY_END_MSG);
        }
    
    /** Remove the factory from the world
    */
    private void deleteFactory()
        {
        // Send the message
        if (factory_end_msg != null)
            container.output(factory_end_msg);
            
        // Delete the factory
        world.deleteAtom(current);
        }
    }

/** GET something from a factory
*/
public class FactoryGet
    extends FactoryAction
    {
    private static final long serialVersionUID = 1;
    
    protected boolean factoryExecute()
        {
        //### No checks for weight etc. Thses belong in a central rules class
        //###   so we can access them from the various places that might implement 
        //###   GET...
        
        // Create the new atom in the actor's bag
        make(actor);
        container.output(template.getString("get_omsg"), actor);
        actor.output(template.getString("get_msg"));
        
        return true;
        }
    }
    
/** LIST_DESC field for Things.
*/
public class DescThing
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    static final String IS_UNIQUE = "is_unique";
    static final String OWNER = "owner";

    public boolean execute()
        {
        Atom owner = null;
        
        // Get the name of the current atom
        String eventResult = current.getName();
        
        // Is this a unique object? If so, prefix with "the"
        if (current.getBool(IS_UNIQUE))
            eventResult = StringUtil.addDefinite(eventResult);
        
        // Does it have an owner? If so, prefix with owner's name
        else if ((owner = current.getAtom(OWNER)) != null)
            {
            String ownerName = owner.getName();
            ownerName = StringUtil.addPossessive(ownerName);
            eventResult = ownerName + " " + eventResult;
            }
        
        // Otherwise, prefix with "a" or "an"
        else
            eventResult = StringUtil.addIndefinite(eventResult);
        
        // Write it back to the event
        event.setResult(eventResult);
        
        return true;
        }
    }

/**
* Sets the value of the visibility property to be the value of size
*/
public class SetVisibility extends JavaAction
{
	private static final long serialVersionUID = 1;
	
	public boolean execute()
	{
		event.setResult( new Integer(current.getInt("size")) );
		return true;
	}
}

/** 
*	Create a Transformer atom
*/
public class CreateTransformer 
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
	public static final String TRANSFORM = "on_transform";
	
	public boolean execute()
	    {
		int transform_time = current.getInt("transform_time");
		
		// If no transform time is given, this implies that the transformation is
		//  achieved manually
		if( transform_time > 0 )
		    {
			//schedule the transforming event for whenever.
	        world.timerEvent( actor, TRANSFORM, current, transform_time );
    	    }
		return pass();
	    }
    }

/** Perform the transformation operation
*/
public class Transform 
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
	public boolean execute()
    	{
		//get the target atom. it may be null.
		Atom targetParent = (Atom)current.getAtom("transform_template");
		
		// Create an instance of the target atom if it isn't null
		if( targetParent != null )
	    	{
	        // create a new atom
	        Atom target = world.newThing(null, targetParent);
        	
        	//replace the existing atom with this new one
			world.replaceAtom(current, target);
            }
        
        // Send transform messages to the actor and the room
   		String msg = current.getString("transform_msg");
		if (msg != null)
			actor.output( msg );
		String omsg = current.getString("transform_omsg");
  		if (omsg != null)
  		    {
  		    // If the actor's message was null, send the omsg to 
  		    //  everyone in the room. Otherwise avoid sending the omsg
  		    //  to the actor.
  		    if (msg == null)
  		        container.output( omsg, actor );
  		    else
  		        container.output( omsg );
			}
			
		//destroy the old atom - note that if the new atom has not been created
		// we are left with nothing.
		world.deleteAtom(current);
		
		return true;	
	}
}

/** Create a Ticker atom
*/
public class CreateTicker 
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
  	public static final String ON_TICK = "on_tick";
    
    public boolean execute()
        {
    	int tick_time = current.getInt("tick_time");
    	if (tick_time != 0)
    	    {
	        //schedule the next tick to occur in 'tick_time' from now
	        world.timerEvent( actor, CreateTicker.ON_TICK, current, tick_time );
            }
        
        // Pass the create event to the parent atom
        return pass();
        }
    }

/**
* The default OnTick class. Please feel free to embrace and extend to
* achieve your desired functionality.
*/
public class OnTick 
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
    	//broadcast all relevent messages.
        broadcastMessages();
        
        //get the tick_time (when the next tick occurs)
        int tick_time = current.getInt("tick_time");
        if(tick_time != 0)
            {
	        //schedule the next tick to occur in 'tick_time' from now
	        world.timerEvent( actor, CreateTicker.ON_TICK, current, tick_time );
            }
        
        return true;
        }
    
    /** Override to change message types
    */
    protected void broadcastMessages()
        {
        // Send transform messages to the actor and the room
   		String msg = current.getString("tick_msg");
		if (msg != null)
			actor.output( msg );

		String omsg = current.getString("tick_omsg");
  		if (omsg != null)
  		    {
  		    // If the actor's message was null, send the omsg to 
  		    //  everyone in the room. Otherwise avoid sending the omsg
  		    //  to the actor.
  		    if (msg == null)
  		        container.output( omsg );
  		    else
  		        container.output( omsg, actor );
  		    }
        }
    }

/** Create a Container. The contents of the container are initialised from 
    the property 'create_contents'.
*/
public class CreateContainer
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
  	public static final String CREATE_CONTENTS = "create_contents";
    
    public boolean execute()
        {
        Enumeration contents = current.getEnum(CREATE_CONTENTS);
        while (contents.hasMoreElements())
            {
            // Create a new atom from the template and move it to the container
            Atom template = (Atom)contents.nextElement();
            Atom atom = world.newThing(null, template);
            world.moveAtom(atom, current);
            }
        
        // Pass the create event to the parent atom
        return pass();
        }
    }
