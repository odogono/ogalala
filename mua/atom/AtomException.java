// $Id: AtomException.java,v 1.3 1998/07/21 19:43:44 jim Exp $
// Exception for atom errors
// James Fryer, 8 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

public class AtomException
    extends RuntimeException
    {
    public AtomException() { }
    public AtomException(String s) { super(s); }
    }
