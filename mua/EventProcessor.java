// $Id: EventProcessor.java,v 1.37 1999/04/29 13:49:32 jim Exp $
// Event processor for multi-user adventure games
// James Fryer, 13 July 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;
import com.ogalala.util.*;

/** The event processor is the main thread of execution for the game.
    It retrieves events from the queue, binds them to atom actions,
    and calls the actions. The result of the action is returned to the user.
*/
class EventProcessor
    extends EventQueue
    {
    public static final int serialVersionUID = 1;

    protected World world;

    /** Constructor
    */
    EventProcessor(World world)
        {
        this.world = world;
        }

    /** Dispatch the event
    */
    protected void processEvent(Event event)
        {
        // The packet that will be sent to the user if the event can't be handled
        OutPkt errorPkt = null;
        
        // If the current atom has been deleted then we can ignore the event.
        Atom current = event.getCurrent();

        // ParserEvent.current is always null. Allow ParserEvents to pass through to callEvent
        if (current != null && current.isDeleted())
            return;
        
        // Send the event to the object associated with it
        try {
            boolean eventHandled = callEvent(event);
            if (!eventHandled)
                errorPkt = new OutPkt("misc", "msg", "You can't do that.", "debug", "False event result.");
            }
            
        // No errors can get past this point!
        catch (Throwable e)
            {
            String msg = e.getMessage();
            if (msg == null)
                msg = "Unknown error";
            errorPkt = new OutPkt("error");
            errorPkt.addField("msg", msg);

            // get clean stack trace and add clean crs out
            Debug.printStackTrace(e);
            String stackTrace = StringUtil.crsToHTML( Debug.getStackTrace( e ) );
            errorPkt.addField("debug", stackTrace );
            }
        
        // If there is an error packet, send it to the user
        if (errorPkt != null)
            {
            try {
                event.getActor().output(errorPkt);
                }
            
            // Errors can also occur while sending output -- e.g. if a Watcher throws 
            //  an exception. If this happens it's a bug so we need to tell the console.
            catch (Throwable e)
                {
                System.err.println("EventProcessor.processEvent: Error outputting to actor: " + e);
                Debug.printStackTrace( e );
                }
            }
        }
    
    /** Call the event, binding if necessary. Return true if the event
        is handled.
    */
    private boolean callEvent(Event event)
        {
        boolean result = false;
        
        // If the event is bound, call it
        if (event.isBound())
        {
            result = world.callEvent(event);
            //return result;
        }   
        // Else, get the event bindings and call each one in turn
        else 
        {
            Enumeration bindings = event.getBindings();
            while (bindings.hasMoreElements())
                {
                Event boundEvent = (Event)bindings.nextElement();
                
                // Offer the event to the room, the arg
                if (callContainerEvent(boundEvent))
                    result = true;
                else if (callArgEvent(boundEvent))
                    result = true;                
                else if (world.callEvent(boundEvent))
                    result = true;
                }
        }
        if( event.hasHandledError() )
        	result = true;
        
        return result;
        }
        
    /** Offer a bound event to the container.
        Return true if the event is handled.
    */
    private final boolean callContainerEvent(Event event)
        {
        // Ignore if the container is Limbo
        Atom container = event.getContainer();
        if (container.isLimbo())
            return false;
        return redirectEvent(event, "container_" + event.getID(), container);
        }
        
    /** Offer a bound event to the argument.
        Return true if the event is handled.
    */
    private final boolean callArgEvent(Event event)
        {
        // If no args, or the arg is not an atom, we can't do anything
        if (event.getArgCount() == 0)
            return false;
        Object argObject = event.getArg(0);
        if (!(argObject instanceof Atom))
            return false;
        return redirectEvent(event, "arg_" + event.getID(), (Atom)argObject);
        }
        
    /** Call an event based on the 'event' parameter, but changing the ID to 
        'newEventID' and setting the target to 'target'.
    */
    private final boolean redirectEvent(Event event, String newEventID, Atom target)
        {
        Event newEvent = world.newEvent(event.getActor(), newEventID, event.getCurrent(), event.getArgs());
        newEvent.setTarget(target);
        return world.callEvent(newEvent);
        }
    }
