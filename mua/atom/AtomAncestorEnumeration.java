// $Id: AtomAncestorEnumeration.java,v 1.8 1998/09/24 09:23:27 jim Exp $
// Enumerate ancestors of an atom
// James Fryer, 19 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Traverse all ancestors of an atom.
*/
final class AtomAncestorEnumeration
    implements Enumeration
    {
    /** The queue of atoms to search
    */
    AtomQueue queue;

    Atom nextAtom;

    public AtomAncestorEnumeration(Atom start)
        {
        // Initialise and put the first atom onto the queue
        Atom.clearAllMarks();

        // Create the queue
        //### Initial queue size, we double the height of the root atom and hope for the best...
        int queueSize = start.getWorld().getRoot().getInheritanceHeight() * 2;
        if (queueSize < 1)
            queueSize = 100;
        queue = new AtomQueue(queueSize);

        // Get the first atom and mark it
        nextAtom = start;
        nextAtom.setMark(true);

        // The first element in the enum is 'start', so skip over it
        nextElement();
        }

    public boolean hasMoreElements()
        {
        return nextAtom != null;
        }

    public Object nextElement()
        {
        // The rather convoluted algorithm used here ensures that atoms get
        //  processed only once.

        // Get the next atom
        Atom result = nextAtom;

        // Put its unmarked children onto the queue
        Enumeration e = result.getParents();
        while (e.hasMoreElements())
            {
            Atom atom = (Atom)e.nextElement();
            if (!atom.isMarked())
                queue.put(atom);
            }

        // Assume no more atoms
        nextAtom = null;

        // Look for the next unmarked atom, if any, on the queue
        while (!queue.isEmpty() && nextAtom == null)
            {
            // Get an atom
            Atom atom = (Atom)queue.get();

            // If it's not marked, make it the next one
            if (!atom.isMarked())
                nextAtom = atom;
            }

        // If there's a next atom, mark it
        if (nextAtom != null)
            nextAtom.setMark(true);

        return result;
        }
    }
