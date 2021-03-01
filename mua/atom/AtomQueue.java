// $Id: AtomQueue.java,v 1.5 1998/07/28 13:32:24 jim Exp $
// A priority queue for atoms
// James Fryer, 19 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** A priority queue for atoms
*/
final class AtomQueue
    extends PriorityQueue
    {
    /** Constructor. 
    */
    public AtomQueue(int size, boolean reversePriority)
        {
        super(size, reversePriority);
        }

    /** Ctor for normal-priority sorts
    */
    public AtomQueue(int size)
        {
        this(size, false);
        }

    /** Compare atoms
    */
    protected int compare(Object a1, Object a2)
        {
        return Atom.compare((Atom)a1, (Atom)a2);
        }
    }
