// $Id: BasicPasswordLogin.java,v 1.7 1998/11/05 12:02:10 rich Exp $
// Basic login, no password
// James Fryer, 13 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.net.*;
import java.io.*;
import java.util.*;
import com.ogalala.util.*;
import com.ogalala.server.database.*;

/** This implements a very basic login procedure for the base server classes and the derived
	class such as ChatServer and MuddyServer . <p>It has a time out of X seconds (obtained from
	the base class) and allows n attempts to log in before the connection is torn down.
*/
public class BasicPasswordLogin
    extends Login
    {
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    UserDatabase userDatabase;

	/** @param server The server derived class that handles the network connections in and out of the app.
		@param socket The new socket that requires passing thougth the login process.
	*/
    public BasicPasswordLogin(Server server, Socket socket, UserDatabase userDatabaseIN)
    {
        super(server, socket);
        this.userDatabase = userDatabaseIN;
        try
        {
            if (!userDatabase.isOpen())
            {
                // if the database is not open and valid it will totally mess up the
                // party so we will stop that now.
                System.exit(1);
            }
        }
        catch (IOException e)
            {
            Debug.printStackTrace( e );
            }
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
            newUserName = newUserName.trim();
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

            // lets find that user record password
            UserRecord userRecord = null;
            try
                {
                userRecord = userDatabase.getUserRecord(newUserName);
                }
            catch (IOException e)
                {
                Debug.printStackTrace( e );
                return;
                }
            catch (UserNotFoundException e)
            	{
            	Debug.printStackTrace( e );
            	}

            // lets get a password off the user
            String password = null;

            try {
                out.println("\r\nPlease enter your password");
                password = in.readLine().trim();
                }

            // Got an error? be off with you you'll have to telnet again.
            catch (IOException e)
                {
                return;
                }

            String realPassword;

            if (userRecord != null)
                {
                realPassword = userRecord.getPassword();
                }
            else
                {
                System.out.println("connection attempt to unknown account, quitely rejecting");
                realPassword = "";
                }

            if (realPassword == null)
                {
                System.out.println("Null password for user " + newUserName + " skipping authorization.");
                }
            else if (!(realPassword.equals(password)))
                {
                out.println("\r\nBad password please try again\n");
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

