// $Id: LoginInterface.java,v 1.4 1998/06/24 21:12:53 jim Exp $
// LoginInterface
// @author Richard Morgan, 10 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;
import com.ogalala.server.database.UserNotFoundException;

public interface LoginInterface
{
    String getPasswordForUser( String userId ) throws UserNotFoundException, java.io.IOException;
    int getAccountStatusForUser( String userId ) throws UserNotFoundException, java.io.IOException;
}