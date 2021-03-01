// $Id: AuthenticationException.java,v 1.2 1998/06/09 11:55:30 matt Exp $
// An exception thrown by password validation functions.
// Matthew Caldwell, 11 May 1998
// Copyright (c) Ogalala Ltd <info@ogalala.com>

package com.ogalala.crypt;

/**
 *  An exception thrown by the PasswordValidator class
 *  and possible elsewhere.
 */
public class AuthenticationException
	extends Exception
{
	public AuthenticationException () {}
	public AuthenticationException ( String s ) { super(s); }
}