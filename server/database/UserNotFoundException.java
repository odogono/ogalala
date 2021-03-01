// $Id: UserNotFoundException.java,v 1.4 1998/07/06 12:11:11 rich Exp $
// UserNotFoundException,
// @author Richard Morgan, 10 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.database;

public class UserNotFoundException
    extends Exception
{
    public UserNotFoundException( String s )
    {
        super( s );
    }
}