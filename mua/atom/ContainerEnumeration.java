// $Id: ContainerEnumeration.java,v 1.9 1999/03/31 11:33:34 alex Exp $
// Base class for container traversal
// James Fryer, 23 Sept 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Traverse a container.
    <p>
    This is the base class from which container traversals are defined. 
*/
public abstract class ContainerEnumeration
    implements Enumeration
    {
    
    /** Stack of containers
    */
    private Stack stack = new Stack();

    /** The current enumeration
    */
    private Enumeration contents = null;
    
    /** The next object to return
    */
    private Atom next = null;
    
    /** The constructor takes the start atom for the traversal
    */
    public ContainerEnumeration(Atom start)
        {
        init(start);
        }
    
    /** 
    *	Initialise the enumeration.
    *   <p>
    *   This is provided so heirs of this class can initialise internal variables
    *   before calling this function.
    *
    *	@param start		the atom whose contents will make up the elements of the enumeration 
    */
    protected final void init(Atom start)
        {
        // If the container is not empty, start traversing it
        if (!start.isEmpty())
            {
            contents = start.getContents();
            gotoNext();
            }
        }
    
    /** Dummy constructor, implicitly called by heirs which call 'init' rather
        that 'super'.
    */
    protected ContainerEnumeration()
        {
        }
    
    /** Should 'atom' be returned as an element of the enumeration?
    */
    protected abstract boolean acceptForReturn(Atom atom);
        
    /** Should 'atom' be traversed?
        <p>
        A precondition of this function is that 'atom.hasContents' and 
        'acceptForReturn' are both true.
    */
    protected abstract boolean acceptForTraversal(Atom atom);
    
    public boolean hasMoreElements()
        {
        return next != null;
        }

    public Object nextElement()
        {
        Atom result = next;
        gotoNext();
        return result;
        }
        
    private void gotoNext()
        {
        next = null;
        boolean finished = !contents.hasMoreElements();
        while (next == null && !finished)
            {
            // Get the next atom and consider it for the next return value
            Atom atom = (Atom)contents.nextElement();
            if (acceptForReturn(atom))
                next = atom;

            // If the atom has contents, consider traversing them
            if (next != null && !next.isEmpty() && acceptForTraversal(next))
                {
                // Push the current list
                stack.push(contents);

                // Start traversing contents
                contents = atom.getContents();
                }

            // If the current contents list is finished, unwind the stack until we
            //  find a contents list that has elements left or the stack is empty
            while (!contents.hasMoreElements() && !finished)
                {
                // If the stack is empty, we're done.
                if (stack.empty())
                    finished = true;

                // Else, pop the next child list from the stack
                else
                    contents = (Enumeration)stack.pop();
                }
            }
        }
    }
