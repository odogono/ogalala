// $Id: BadRecordException.java,v 1.5 1998/06/24 21:12:52 jim Exp $
// BadRecordException, thrown 
// @author Richard Morgan, 18 May 98 
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.database;

public class BadRecordException extends Exception
{
    public BadRecordException(String reason)
    {
        super(reason);
    }
}
