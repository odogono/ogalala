// $Id: Mobile.java,v 1.5 1999/03/10 16:23:28 jim Exp $
// A Thing that can follow exits
// James Fryer, 20 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Mobile class is a placeholder -- possibly in the future mobiles might have
    hard-coded implementation.
    <p>
    Also it is sometimes handy to be able to test 'instanceof Mobile'.
*/
public class Mobile
    extends Container
    {
    private static final long serialVersionUID = 1;
    
    /** Create a new Mobile
    */
    Mobile(World world, Atom parent, String id)
        {
        super(world, parent, id);
        }

    /** Get the thing's static class name
    */
    public String getClassName()
        {
        return "Mobile";
        }

    /** Get a system field. 
    */
    protected Object getSystemField(String name)
        {
//###        if ("container".equalsIgnoreCase(name))
//###            return container;
//###        else
            return super.getSystemField(name);
        }

    /** ###
    */
    protected void unlink()
        {
        super.unlink();
        }
    
    /** Test this atom for validity.
        @return true if this is a valid atom.
    */
    public boolean invariant()
        {
        return super.invariant();
        }
    }
