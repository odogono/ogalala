// $Id: Action.java,v 1.24 1999/04/29 10:01:34 jim Exp $
// Executable code that can be attached to an Atom field
// James Fryer, 26 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;

/** An Action is a class containing a single function that can
    be stored in an Atom field.
    <p>
    To call the Action, retrieve it, create an event object for it,
    then call 'execute'.
*/
public abstract class Action
    implements java.io.Serializable, Cloneable
    {
    /** All actions must have this variable defined, because they may be
        serialised. The convention is to set it to 1 in all actions.
    */
    private static final long serialVersionUID = 1;
    
    /** The action context variables
    */
    protected Event event;      // The event that is being handled
    protected World world;      // Back-pointer to world
    protected Atom actor;       // The atom which caused the event
    protected Atom current;     // The atom on which the event takes place (cf 'this' in Java)
    protected Atom container;   // The location where the event happens

    /** Perform the action.
        @return true if the action has been handled
    */
    public boolean execute(Event event)
        {
        // Don't allow unbound events to be executed
        if (!event.isBound())
            throw new ParserException("Cannot execute unbound event.");
            
        // We don't execute this action, we clone it and execute the duplicate.
        Action action = makeClone();
            
        // "Crack" the event open to make writing handlers easier
        action.event = event;
        action.world = event.getWorld();
        action.actor = event.getActor();
        action.current = event.getCurrent();
        action.container = event.getContainer();
    	
        // Execute the action
        return action.execute();
        }
        
    /** Pass the action on to a superclass
        @return true if the action has been handled
    */
    protected boolean pass()
        {
        // Get the precursor atom -- the ancestor which defines the precursor to the current 
        //  action property
        Atom precursor = event.getTarget().getPrecursorAtom(event.getID());
        
        // If the precursor is null, we return true: a little strange perhaps, but 
        //  logical in the context of the way that 'pass' is used, usually as the 
        //  return value to an action.
        if (precursor == null)
            return true;
            
        // Set the target and call the event
        event.setTarget(precursor);
        return world.callEvent(event);
        }
        
    /** Clone the action. This has two effects: it allows actions to be run
        concurrently, and it means serialising the action won't write whatever
        field values happen to have been set when the action was last executed.
    */
    private Action makeClone()
        {
        Action result = null;
        try {
            result = (Action)this.clone();
            }
        catch (CloneNotSupportedException e)
            {
            // In the words of Bart Simpson: "It'll never happen".
            }
        return result;
        }
        
    /** Perform the action with a cracked event
    */
    public abstract boolean execute();
    
    /** Produce a string representation of the Action, consisting
        of its unqualified class name.
    */
    public String toString ()
        {
        String classname = getClass().getName();
        if ( classname.indexOf('.') != -1 )
        	return classname.substring(classname.lastIndexOf('.') + 1);
        else
        	return classname;
        }
    }
