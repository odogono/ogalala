// $Id: ListenEnumeration.java,v 1.1 1999/03/31 11:28:51 alex Exp $
// Traverse all relevant atoms in a container
// Alexander Veenendaal, 30 March 1999
// Copyright (C) HotGen Ltd <www.hotgen.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

public class ListenEnumeration 
    extends ContainerEnumeration
    {
    
    /** Property to check if a container is closed
    */
    static final String IS_CLOSED = "is_closed";
    
    /** All atoms returned from this enumeration must descend from this atom
    */
    Atom descendedFrom;
    
    //===========================================================================//
    //-------------------------------- Constructors------------------------------//
    //===========================================================================//
    public ListenEnumeration(Atom start)
        {
        this(start, null);
        }
        
    public ListenEnumeration(Atom start, Atom descendedFrom)
        {
        this.descendedFrom = descendedFrom;
        init(start);
        }
        
    
    //===========================================================================//
    //----------------------------- Protected Methods ---------------------------//
    //===========================================================================//
    /** 
    *	Should 'atom' be returned as an element of the enumeration?
    *	<p>
    *	Always true
    */
    protected boolean acceptForReturn(Atom atom)
        {
        	if( descendedFrom == null )
        		return true;
        	else
        		return atom.isDescendantOf(descendedFrom);	
        }

    /** Should 'atom' be traversed?
        <p>
        if the atom is in the same container as the 
    */
    protected boolean acceptForTraversal(Atom atom)
        {
        return !atom.getBool(IS_CLOSED);
        }

    }