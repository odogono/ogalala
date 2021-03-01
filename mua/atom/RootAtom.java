// $Id: RootAtom.java,v 1.8 1999/03/17 14:37:03 jim Exp $
// The special root atom class
// James Fryer, 8 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Special root atom class. Only one of these exists.
    <p>
    No use currently exists for this root atom, it is here for future expansion...
*/
class RootAtom
    extends Atom
    {
    private static final long serialVersionUID = 1;
    
    /** Create a new Root Atom
    */
    RootAtom(World world)
        {
        super(world, null, AtomDatabase.ROOT_ID);
        // The root atom is always frozen
        frozen = -1;
        }

    /** Is this the root atom? Of course it is
    */
    public boolean isRoot()
        {
        return true;
        }
    }
    