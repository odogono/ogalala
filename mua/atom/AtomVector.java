// $Id: AtomVector.java,v 1.6 1999/03/10 16:23:28 jim Exp $
// A vector of atoms
// James Fryer, 17 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** This class is used to store atoms in the parent and child lists.
    <p>
    This class extends Vector rather than using it so that we can access
    the underlying array to implement the 'sort' function.
*/
public final class AtomVector
    extends Vector
    {
    private static final long serialVersionUID = 1;
    
    /** Add an atom
    */
    public void put(Atom atom)
        {
        addElement(atom);
        }

    /** Get the i'th atom
    */
    public Atom get(int i)
        {
        return (Atom)elementAt(i);
        }

    /** Remove an atom
    */
    public void remove(Atom atom)
        {
        removeElement(atom);
        }

    /** Remove i'th atom
    */
    public void remove(int i)
        {
        removeElementAt(i);
        }

    /** Sort the atoms by their depth and height fields
        <p>
        This uses insertion sort, which is pretty crap. This should be
        replaced if there are likely to be more than a few parents in a
        typical atom... This code was culled from Java 1.2 SDK, "Arrays.java".
    */
    public void sort()
        {
        int len = elementCount;

    	// Insertion sort on smallest arrays
	    for (int i = 0; i < len; i++)
	        {
    		for (int j = i;
    		        j > 0 && Atom.compare((Atom)elementData[j - 1], (Atom)elementData[j]) > 0;
    		        j--)
    		    swap(j, j-1);
    		}
        }

    /**
     * Swaps array[a] with array[b].
     */
    private void swap(int a, int b)
        {
    	Object t = elementData[a];
    	elementData[a] = elementData[b];
    	elementData[b] = t;
        }
    }
