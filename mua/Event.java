// $Id: Event.java,v 1.33 1999/04/29 13:49:32 jim Exp $
// Context for action 'execute' functions
// James Fryer, 30 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;

/** The Event is passed to 'Action.execute'. It contains information
    about the context of the command being executed. 
*/
public class Event
    implements java.io.Serializable
    {
    public static final int serialVersionUID = 1;
    
    /** The game world
    */
    protected World world;
    
    /** The action ID
    */
    protected String id;
    
    /** The Mobile which caused this event to take place
    */
    protected Atom actor;
    
    /** The current atom is the one which called the action (cf 'this', 'self' etc.)
    */
    protected Atom current;
    
    /** The target atom is the atom that is searched to find the event 
        property when the event is executed.
        <p>
        Some discussion of the relationship between current and target is
        required. Normally these will be the same except in two situations:
        <ul>
        <li> When the event is passed to a parent atom. In this case 'current'
            will be unchanged, but 'target' will be the parent atom.
        <li> When the event is offered to other objects such as the room and 
            the event argument. In this case 'target' will be the room (or the 
            argument) while 'current' will be unchanged.
        </ul>
    */
    protected Atom target;
    
    /** The arguments to this event
    */
    protected Object args[];
    
    /** The result of the action
    */
    protected Object eventResult = null;
    
    /** The game time (in seconds) when this event is to be processed. 
        Zero means right away.
    */
    protected long time = 0L;
    
    /** The system time when the event was put on the event queue, used for performance logging
    */
    protected transient long timeQueued = 0L;
    
    /** Do we want output resulting from this event to be expanded?
    */
    private boolean expandFormats = true;
    
    /** Create an Event 
    */
    protected Event(World world, Atom actor, String id, Atom current, Object args[])
        {
        if (id == null)
            throw new NullPointerException("Null event ID");
        if (actor == null)
            throw new NullPointerException("Null event actor");
            
        this.world = world;
        this.id = id;
        this.actor = actor;
        this.current = this.target = current;
        this.args = args;
        }
        
    /** Get the World
    */
    public World getWorld()
        {
        return world;
        }

    /** Get the event ID
    */
    public String getID()
        {
        return id;
        }

    /** Get the Mobile which initiated the event.
    */
    public Atom getActor()
        {
        return actor;
        }

    /** Get the container.
        <p>
        This is calculated each time so that events occur in the correct place, e.g.
        if the actor moves around with an active timer.
    */
    public Atom getContainer()
        {
        // If there is no current object or it is held by the actor, return the actor's container
        if (current == null || current.isRoot() || actor.containsDeep(current))
            return actor.getContainer();
            
        // Else, if current is a Thing then return its container
        else if (current instanceof Thing)
            return current.getContainer();
        
        // Else return limbo as a default
        else        
            return world.getLimbo();
       }

    /** Get the current atom
        <p>
        The current atom is to an action as 'this' is to a Java function
    */
    public Atom getCurrent()
        {
        return current;
        }

    /** Get the target atom.
        <p>
        The target atom is searched for the event property when the event
        is executed.
    */
    public Atom getTarget()
        {
        return target;
        }

    /** Set the target atom.
    */
    public void setTarget(Atom newTarget)
        {
        target = newTarget;
        }

    /** Get all arguments
    */
    public final Object[] getArgs()
        {
        return args;
        }
        
    /** Get the number of arguments
    */
    public int getArgCount()
        {
        if (args != null)
            return args.length;
        else
            return 0;
        }

    /** Get the first argument
    */
    public final Object getArg()
        {
        return getArg(0);
        }
        
    /** Get an argument
    */
    public Object getArg(int n)
        {
        if (args != null && n >= 0 && n < args.length)
            return args[n];
        else
            return null;
        }
        
    /** Set the result object.
    */
    public void setResult(Object newResult)
        {
        eventResult = newResult;
        }
        
    /** Clear the result object.
    */
    public void clearResult()
        {
        eventResult = null;
        }
        
    /** Get the result object
    */
    public Object getResult()
        {
        return eventResult;
        }

    /** Set the output format mode
        <p>
        This is useful when you want to output strings without expanding 
        their formats -- see the !EXAM command for an example of this.
    */
    public void setExpandFormats(boolean flag)
        {
        expandFormats = flag;
        }
        
    /** Format output in the context of this event
    
        @see com.ogalala.mua.AtomUtil.formatOutput
    */
    public String formatOutput(String s)
        {
        if (expandFormats)
            return AtomUtil.formatOutput(this, s);
        else
            return s;
        }

	/** 
	*	Format output in the context of this event
    *
    *	The array of strings is intended to be substituted into 
    *	the message string
    *   @see com.ogalala.mua.AtomUtil.formatOutput
    */
	public String formatOutput(String s, String value[])
		{
		if (expandFormats)
			return AtomUtil.formatOutput(this, s, value);
		else
			return s;
		}
		
    /** Set the time when this event is to be processed
    */
    public void setTime(long newTime)
        {
        time = newTime;
        }

    /** Get the time when this event is to be processed
    */
    public long getTime()
        {
        return time;
        }
        
    /** If this function returns true, the event is 'bound' and can be executed
        immediately. If this is false, the 'getBindings' function must be called.
    */
    public boolean isBound()
        {
        return true;
        }

    /** If the event needs to be bound to game objects, this function will 
        return an enumeration of the bound events. An unbound event cannot 
        be executed directly; instead the events returned by this function 
        should be executed.
    */
    public Enumeration getBindings()
        {
        return null;
        }

    public String toString()
        {
        StringBuffer result = new StringBuffer();
        result.append(actor.getID());
        result.append(":");
        result.append(current == null ? "null" : current.getID());
        result.append(".");
        result.append(id);
        if (getArgCount() > 0)
            {
            result.append("(");
            for (int i = 0; i < getArgCount(); i++)
                {
                result.append(AtomData.toString(getArg(i)));
                if (i < (getArgCount() - 1))
                    result.append(",");
                }
            result.append(")");
            }
        if (eventResult != null)
            {
            result.append("=");
            result.append(AtomData.toString(eventResult));
            }
        return result.toString();
        }
        
    /**
    * Returns true if the event has sent out an error message to 
    * the calling actor. As this is a ### Alex function the comment ends in mid-sentence?
    */
    public boolean hasHandledError()
        {
    	return false;
        }
    
    /** Convert the event to the export format:
        <ul>
        <li>Id
        <li>Actor
        <li>Current
        <li>Args
        <li>Time
        </ul>
    */
    protected String toExportFormat()
        {
        // ID, actor, current
        StringBuffer result = new StringBuffer(id);
        result.append(AtomDatabase.EXPORT_DELIMITER);
        result.append(actor.getID());
        result.append(AtomDatabase.EXPORT_DELIMITER);
        if (current == null)
            result.append("null");
        else
            result.append(current.getID());
        result.append(AtomDatabase.EXPORT_DELIMITER);
        
        // Args
        result.append("[ ");
        if (args != null)
            {
            for (int i = 0; i < args.length; i++)
                {
                result.append(AtomData.toString(args[i]));
                result.append(" ");
                }
            }
        result.append("]");
        result.append(AtomDatabase.EXPORT_DELIMITER);
        
        // Time
        result.append(Long.toString(time));
        
        return result.toString();
        }
        
    }
