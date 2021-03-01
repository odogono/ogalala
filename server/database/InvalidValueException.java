// $Id: InvalidValueException.java,v 1.5 1998/07/06 12:11:11 rich Exp $
// 
// @author Richard Morgan, 18 May 98 
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.database;

public class InvalidValueException 
    extends Exception
{
    public InvalidValueException( String msg )
    {
        super( msg );
    }
}