// $Id: AtomPrinter.java,v 1.4 1999/03/11 10:41:09 alex Exp $
// Abstract class for outputting Atoms to character streams
// James Fryer, 22 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.mua;

import java.io.*;
import java.util.*;
import com.ogalala.util.*;

/** This is an abstract class to assist in writing atoms in the
    general format:
    <ul>
    <li> Atom header comment
    <li> Atom declaration
    <li> Parents
    <li> Children
    <li> Fields
    </ul>

    It is intended to output atoms in any format required by atom input tools.
*/
abstract public class AtomPrinter
    {
    protected PrintWriter out;

    public AtomPrinter(Writer out)
        {
        this.out = new PrintWriter(out);
        }

    /** Write an atom
    */
    public void printAtom(Atom atom)
        throws IOException
        {
        printHeader(atom);
        printFields(atom);
        out.println();
        }

    /** Write an atom's descendants
    */
    public void printDescendants(Atom atom)
        throws IOException
        {
        Enumeration enum = atom.getDescendants();
        while (enum.hasMoreElements())
            printAtom((Atom)enum.nextElement());
        }

    /** Write an atom's header
    */
    protected void printHeader(Atom atom)
        throws IOException
        {
        printHeaderComment(atom);
        
        // Avoid writing root atom's declaration and parents
        if (!atom.isRoot())
            {
            printDeclaration(atom);
            printParents(atom);
            }
        printChildren(atom);
        }

    /** Write an atom's fields
    */
    protected void printFields(Atom atom)
        throws IOException
        {
        Enumeration names = atom.getFieldNames();
        Enumeration values = atom.getFieldValues();
        while (names.hasMoreElements())
            printField(atom, names.nextElement().toString(), values.nextElement());
        }

    /** Format an atom's ID so it is understood as an ID by reading programs
    */
   protected String formatID(Atom atom) 
        {
        return atom.getID();
        }
        
    /** Write an atom header comment
    */
    protected void printHeaderComment(Atom atom)
        throws IOException
        {
        }

    /** Write an atom declaration
    */
    protected abstract void printDeclaration(Atom atom) throws IOException;

    /** Write an atom's parents
    */
    protected abstract void printParents(Atom atom) throws IOException;

    /** Write an atom's children
        <p>
        This function will normally generate output for informative purposes only.
    */
    protected void printChildren(Atom atom)
        throws IOException
        {
        }

    /** Write one field
    */
    protected abstract void printField(Atom atom, String fieldName, Object fieldValue) throws IOException;
    }
