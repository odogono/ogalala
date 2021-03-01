// $Id: Thing.java,v 1.22 1999/04/07 13:46:19 jim Exp $
// An atom that can be placed in a container
// James Fryer, 1 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** A Thing is an Atom which can be placed in a Container.
    <p>
    A Thing is the base class for all objects which can be picked up,
    dropped, examined etc. in the world model.
*/
public class Thing
    extends Atom
    {
    private static final long serialVersionUID = 1;
    
    /** The container that holds this Thing
    */
    private Container container;
    
    /** The chain of Watchers which receive output from the Thing
    */
    private transient Watcher watcher;
    
    /** Create a new Thing
    */
    Thing(World world, Atom parent, String id)
        {
        super(world, parent, id);

        // Ensure that we inherit from the default atom for this class
        Atom defaultParent = world.getAtom(getClassName());
        if (!this.isDescendantOf(defaultParent))
            inherit(defaultParent);

        // Set container to Limbo
        try {
            Atom limbo = world.getLimbo();
            if (this.isLimbo())
                this.container = (Container)limbo;
            else
                limbo.putIn(this);
            }
        
        // Ignore errors from the add operation
        catch (AtomException e)
            { }
        }

    /** Send an Output Packet to the Watchers.
    */
    public void output(OutPkt out)
        {
        doOutput(expandOutPkt(out));
        }

    /** Send an Output Packet to the Watchers, except those attached to
        'missOut'.
        <p>
        This function is for use when sending output to the things in a
        container, except the actor.
    */
    public void output(OutPkt out, Atom missOut)
        {
        if (this != missOut)
            output(out);
        }

    /** Send an Output Packet to the Watchers, except those attached to
        'missOut1' and 'missOut2'.
        <p>
        This function is for use when sending output to the things in a
        container, except the actor and the second party. For example
        the WHISPER and GIVE commands.
    */
    public void output(OutPkt out, Atom missOut1, Atom missOut2)
        {
        if (this != missOut1 && this != missOut2)
            output(out);
        }
    
    /** Convert an output packet into an expanded string
    */
    protected String expandOutPkt(OutPkt out)
        {
        Event event = world.getCurrentEvent();
        String result = out.toString();
        if (event != null)
            result = event.formatOutput(result);
        return result;        
        }
        
    /** Low-level output function, sends a string and event to the Watchers
        attached to this atom.
    */
    protected final void doOutput(String msg)
        {
        if (watcher != null)
            watcher.output(msg, world.getCurrentEvent());
        }

    /** Get the thing's static class name
    */
    public String getClassName()
        {
        return "Thing";
        }

    /** Get a system field. 
    */
    protected Object getSystemField(String name)
        {
        if ("container".equalsIgnoreCase(name))
            return container;
        else
            return super.getSystemField(name);
        }

    /** Set the container (called by Container, and AtomDatabase when Limbo is being created)
    */
    protected void setContainer(Container container)
        {
        this.container = container;
        }
        
    /** Get the container
    */
    public Atom getContainer()
        {
        return container;
        }
        
    /** Find the first closed container that contains this atom
    */
    public Atom getEnclosingContainer()
        {
        Atom result = container;
        while (!result.isClosed() && !result.isLimbo())
            result = result.getContainer();
        return result;
        }
        
    /** Remove the thing from its container when it is being deleted.
    */
    protected void unlink()
        {
        container.removeThing(this);
        this.container = null;
        removeAllWatchers();
        super.unlink();
        }
    
    /** Add a Watcher to the chain.
        <p>
        A Lead watcher will always be first in the chain. There can only be one lead
        watcher. 
    */
    public void addWatcher(Watcher newWatcher)
        {
        // If there are no watchers, this is the first one
        if (watcher == null)
            watcher = newWatcher;
            
        // If the new watcher is lead
        else if (newWatcher.isLead())
            {
            // If there is already a lead watcher, throw
            if (watcher.isLead())
                throw new AtomException("Only one lead watcher permitted");
                
            // Add the lead at the head of the list
            newWatcher.setNext(this.watcher);
            watcher = newWatcher;
            }
            
        // Else, not a lead watcher
        else {
            // Add the new watcher second in the list
            Watcher next = watcher.getNext();
            watcher.setNext(newWatcher);
            newWatcher.setNext(next);
            }
        
        // Notify the watcher that the addition is successful
        newWatcher.onAdd();
        }
        
    /** Remove a Watcher
    */
    public void removeWatcher(Watcher deadWatcher)
        {
        // Find the watcher in the list, and keep track of the previous watcher
        boolean found = false;
        Watcher prev = null;
        for (Watcher w = watcher; w != null; w = w.getNext())
            {
            if (w == deadWatcher)
                {
                found = true;
                break;
                }
            prev = w;
            }
        
        // Ignore if the watcher was not found
        if (!found)
            return;
            
        // Notify the watcher that it is about to be removed (note this is
        //  done *before* removal)
        deadWatcher.onRemove();
        
        // Remove it from the list
        if (prev == null)
            watcher = watcher.getNext();
        else
            prev.setNext(deadWatcher.getNext());
        }

    /** Remove all watchers
    */
    private void removeAllWatchers()
        {
        // Go through the list notifying all watchers that they have been thrown off
        for (Watcher w = watcher; w != null; w = w.getNext())
            w.onRemove();
            
        // Set the list to null
        watcher = null;
        }
        
    /** Convert to export format. This has the container ID added to the 
        atom export format.
    */
    protected String toExportFormat()
        {
        StringBuffer result = new StringBuffer(super.toExportFormat());
        result.append(AtomDatabase.EXPORT_DELIMITER);
        result.append(container.getID());
        return result.toString();
        }
        
    /** Test this atom for validity.
        @return true if this is a valid atom.
    */
    public boolean invariant()
        {
        // All watchers must refer to 'this'
        for (Watcher w = watcher; w != null; w = w.getNext())
            {
            if (w.getWatchedAtom() != this)
                return false;
            }
            
        // Container must contain 'this', except Limbo
        boolean isLimbo = this instanceof Container && ((Container)this).isLimbo();
        if (!isLimbo && !enumContains(container.getContents(), this))
            return false;
            
        return super.invariant();
        }
    }
