// $Id: Watcher.java,v 1.4 1998/09/14 14:05:12 jim Exp $
// A chain of objects which can receive output from a Thing
// James Fryer, 13 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

/** The Watcher is an abstract class which implements a chain of objects
    which can be attached to any Thing in the database.
    <p>
    Output sent to a Thing is directed to the first Watcher in the chain;
    the output then cascades down the chain.
    <p>
    This is a variant on the Observer (Publish/Subscribe) pattern.
    <p>
    Note that Watchers are transient objects which are not themselves 
    stored in the database.
*/
abstract public class Watcher
    {
    /** The atom that is being watched
    */
    protected Atom atom;

    /** Is this watcher the 'lead' watcher?
        <p>
        The Lead watcher is considered to be "in control" of the Thing 
        being watched. It is always first in the chain. There can be 
        0 or 1 Lead watchers.
    */
    protected boolean isLead;

    /** The next Watcher in the chain
    */
    protected Watcher next;
    
    /** Constructor. 
    */
    public Watcher(Atom atom, boolean isLead)
        {
        this.atom = atom;
        this.isLead = isLead;
        }
        
    /** Output the message and event to the chain of Watchers
    */
    public void output(String msg, Event event)
        {
        doOutput(msg, event);
        if (next != null)
            next.output(msg, event);
        }

    /** Output a message
    */
    public final void output(String msg)
        {
        output(msg, (Event)null);
        }
    
    /** Perform the output.
        <p>
        This function will be overriden in implementing classes. The message 
        must be present, and formatted for human readers. The event can be 
        null, it is intended to be the event that precipitated the output
        and is present for the benefit of NPC intelligences.
    */
    abstract protected void doOutput(String msg, Event event);
    
    /** Get the atom being watched
    */
    public final Atom getWatchedAtom()
        { 
        return atom;
        }
        
    /** Set the next Watcher in the chain
    */
    public final void setNext(Watcher newNext)
        {
        next = newNext;
        }

    /** Get the next Watcher in the chain
    */
    public final Watcher getNext()
        {
        return next;
        }

    /** Is this the Lead watcher?
    */
    public final boolean isLead()
        {
        return isLead;
        }
        
    /** Called when the watcher is added to a Thing
    */
    public void onAdd()
        {
        }

    /** Called when the watcher is removed from a Thing
    */
    public void onRemove()
        {
        }
    }
