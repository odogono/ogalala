// $Id: AtomSorter.java,v 1.8 1999/03/11 10:41:09 alex Exp $
// Calculate the partial sort order for all atoms
// James Fryer, 8 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** This class provides utilities for reordering the atom inheritance hierarchy.
    <p>
    The atom type hierarchy is a Directed Acyclic Graph (DAG). ###
    <p>
    The sorting process has three passes:
    <ol>
    <li> Calculate the depth of each atom from the root;
    <li> Calculate the height of each atom from its furthest leaf;
    <li> Sort each atom's parent lists by depth first, then height.
    </ul>
*/
final class AtomSorter
    {
    /** The root atom
    */
    private Atom root;

    /** The leaves of the graph (i.e. atoms without children)
    */
    private Vector leaves = new Vector();

    /** The actual maximum depth of the graph
    */
    //### May not be needed
    private int maxDepth = 0;

    /** The maximum permitted depth and height
    */
    public static final int MAX_DEPTH = 255;

    /** The maximum parmitted number of atoms
    */
    public static final int MAX_ATOMS = 0x7fffffff;

    /** The constructor initialises the object and reorders the atom graph
    */
    AtomSorter(Atom root)
        {
        this.root = root;
        reorderAtoms();
        }

    /** Starting from the root atom, calculate the height and depth of each atom
        and then convert this to a sort code. Sort all the parent arrays by
        the sort code.
    */
    private void reorderAtoms()
        {
        // Calculate the depth, calculate the maximum depth, and find all the leaf nodes
        calculateDepths();

        // Calculate the height from all leaf nodes
        calculateHeights();

        // Sort all parent lists
        sortParentLists();
        }

    /** Starting from the root atom, mark the depth of all atoms.
        <p>
        Has two side effects: calculates the maximum depth and stores all the
        leaf nodes in the 'leaves' vector.
    */
    private void calculateDepths()
        {
        // We clear the marks, to avoid having to set all the depths to 0.
        Atom.clearAllMarks();
        setDepth(root, 1);
        }

    /** Recursively set the depth of 'atom' and its descendants
    */
    private void setDepth(Atom atom, int depth)
        {
//        debug("Marking " + atom.toString() + " depth=" + depth);

        // If this atom is not marked, then we have not visited it yet and we
        //  must always set its depth. Otherwise we only change the depth if
        //  the depth parameter is greater than the atom's depth, i.e. we have
        //  reached here by a longer path than last time.
        if (atom.isMarked() && atom.getInheritanceDepth() >= depth)
            return;

        // Mark the current atom and set its depth
        atom.setMark(true);
        atom.setInheritanceDepth(depth);

        // If the next tier of children will be too deep, indicate an error
        if (depth == MAX_DEPTH)
            {
            //### Need a better way to handle this.
            //### The problem is that by the time we have detected this, the atom
            //###  has been added and the damage is done. Need to detect this
            //###  situation *before* it arises, while adding the atom...
            throw new RuntimeException("Atom hierarchy too deep");
            }

        // If this atom has children, set their depths
        if (atom.hasChildren())
            {
            // Mark the depth of the children as one greater than current atom
            depth++;
            Enumeration children = atom.getChildren();
            while (children.hasMoreElements())
                setDepth((Atom)children.nextElement(), depth);
            }

        // Else, no children
        else {
            // Add the current atom to the leaf list
            leaves.addElement(atom);

            // Calculate maximum depth
            if (depth > maxDepth)
                maxDepth = depth;
            }
        }

    /** Calculate the height starting from the atoms in the 'leaves' vector
    */
    private void calculateHeights()
        {
        // Clear marks to avoid having to set all the heights to 0.
        Atom.clearAllMarks();

        // Calculate height from each leaf
        Enumeration e = leaves.elements();
        while (e.hasMoreElements())
            setHeight((Atom)e.nextElement(), 1);
        }

    /** Recursively set the height of 'atom' and its ancestors
    */
    private void setHeight(Atom atom, int height)
        {
        // If the current atom is marked and has a greater height than the current height, do nothing
        if (atom.isMarked() && atom.getInheritanceHeight() > height)
            return;

        // Mark and set the height
        atom.setMark(true);
        atom.setInheritanceHeight(height);

        // If this is the root atom, return
        if (atom == root)
            return;

        // Set the parents' height
        height++;
        Enumeration parents = atom.getParents();
        while (parents.hasMoreElements())
            setHeight((Atom)parents.nextElement(), height);
        }

    /** Sort the parents of each atom
    */
    private void sortParentLists()
        {
        // Clear the marks
        Atom.clearAllMarks();

        // Sort from each leaf
        Enumeration e = leaves.elements();
        while (e.hasMoreElements())
            sortParents((Atom)e.nextElement());
        }

    private void sortParents(Atom atom)
        {
        // If this is the root atom, or it is marked, do nothing
        if (atom == root || atom.isMarked())
            return;

        // Mark the atom and sort its parents
        atom.setMark(true);
        atom.sortParents();

        // Recursively sort the atom's parents parents...
        Enumeration e = atom.getParents();
        while (e.hasMoreElements())
            sortParents((Atom)e.nextElement());
        }

    /** Print a debug message
    */
    private static void debug(String s)
        {
        Debug.println("AtomSorter: " + s);
        }
    }
