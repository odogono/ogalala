// $Id: ParseException.java,v 1.1 1998/09/23 16:36:19 matt Exp $
// An exception thrown while parsing object descriptions etc.
// Matthew Caldwell, 23 September 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

/**
 *  A class of exceptions thrown during parsing/compilation.
 */
public class ParseException
	extends Exception
{
	public ParseException() { super(); }
	public ParseException( String str ) { super(str); }
}