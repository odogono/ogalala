// $Id: ConsoleWatcher.java,v 1.9 1998/09/14 14:05:10 jim Exp $
// A Watcher that outputs to the console
// James Fryer, 13 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import com.ogalala.util.*;

/** Watcher that sends output to the console.
*/
public class ConsoleWatcher
    extends Watcher
    {
    public ConsoleWatcher(Atom atom, boolean isLead)
        {
        super(atom, isLead);
        }
        
    /** Perform the output.
        <p>
        This function will be overriden in implementing classes. The message 
        must be present, and formatted for human readers. The event can be 
        null, it is intended to be the event that precipitated the output
        and is present for the benefit of NPC intelligences.
    */
    protected void doOutput(String msg, Event event)
        {
        if (msg != null)
            System.out.println("[" + atom.getID() + "]" + msg);
        }
    }
