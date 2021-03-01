// $Id: BasicLogin.java,v 1.10 1999/04/23 07:54:40 jim Exp $
// Basic login, no password
// James Fryer, 13 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.net.*;
import java.io.*;
import java.util.*;
import com.ogalala.util.StringUtil;

/** This implements a very basic login procedure for the base server classes and the derived 
	class such as ChatServer and MuddyServer . <p>It has a time out of X seconds (obtained from 
	the base class) and allows n attempts to log in before the connection is torn down.
*/
public class BasicLogin
    extends Login
    {
    private static final int MAX_LOGIN_ATTEMPTS = 3;

	/** @param server The server derived class that handles the network connections in and out of the app.
		@param socket The new socket that requires passing thougth the login process.
	*/
    public BasicLogin(Server server, Socket socket)
        {
        super(server, socket);
        }

    /** Handle the login process: if the login succeeds set 'userName' to the user's name
    */
    protected void negotiateLogin()
        {
        // Print a welcome message
        printBannerMessage();

        // loop until maxium number of login attempts has been reached or success!
        for (int attempts = 0; attempts < MAX_LOGIN_ATTEMPTS && userName == null; attempts++)
            {
            // Print the login prompt
            printLoginPrompt();

            // Get the user's response
            String newUserName = null;
            try {
                newUserName = in.readLine();
                }

            // Got an error? be off with you you'll have to telnet again.
            catch (IOException e)
                {
                return;
                }

            // If 'newUserName' is null, the login attempt has failed, so return
            if (newUserName == null)
                return;

            // Trim spaces from the string and try again if it was empty
            newUserName.trim();
            if (newUserName.length() == 0)
                {
                out.println("\r\n\nYou didn't enter your name, please try again\n");
                continue;
                }

            // Is it a valid user name? If not, try again
            if (!isValidUserName(newUserName))
                {
                out.println("\r\n\nUser names must contain only letters and numbers\n");
                continue;
                }

            // Do we already have this name logged in? If so, try again
            if (isLoggedIn(newUserName))
                {
                out.println("\r\n\nI already have " + newUserName + " logged in. Please use another name\n");
                continue;
                }

            // Got a valid user name, store it
            userName = newUserName;
            }
        }

    /** Return true if the string is a valid user name. 
		@returns true if the userName contains no whitespace.
    */
    protected boolean isValidUserName(String userName)
        {
        return StringUtil.oneWordOnly(userName);
        }
    }

