// $Id: ContentsEnumeration.java,v 1.3 1998/11/26 10:37:46 jim Exp $
// Traverse all the atoms in a container
// James Fryer, 1 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Traverse all atoms in a container.
*/
public class ContentsEnumeration
    extends ContainerEnumeration
    {
    /** The constructor takes the start atom for the traversal
    */
    public ContentsEnumeration(Atom start)
        {
        super(start);
        }

    /** Should 'atom' be returned as an element of the enumeration?
        <p>
        Always true
    */
    protected boolean acceptForReturn(Atom atom)
        {
        return true;
        }

    /** Should 'atom' be traversed?
        <p>
        Always true
    */
    protected boolean acceptForTraversal(Atom atom)
        {
        return true;
        }
    }
