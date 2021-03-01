// $Id: ParserException.java,v 1.4 1999/04/29 10:01:36 jim Exp $
// Exception for parser errors
// James Fryer, 16 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

public class ParserException extends RuntimeException
    {
    public ParserException() { }
    public ParserException(String s) { super(s); }
    }
