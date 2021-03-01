// $Id: ApplicationOpenException.java,v 1.3 1998/06/24 21:12:52 jim Exp $
// Exception thrown when application can't be opened
// James Fryer, 3 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

/** Exception thrown when application can't be opened
*/
public class ApplicationOpenException
	extends Exception
    {
	public ApplicationOpenException() {}
	public ApplicationOpenException(String s) { super(s); }
    }
