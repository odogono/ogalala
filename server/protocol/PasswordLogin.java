// $Id: PasswordLogin.java,v 1.11 1999/11/12 12:13:22 jim Exp $
// A class that handles authenticated logins for the server.
// Matthew Caldwell, 11 May 1998
// Copyright (c) Ogalala Ltd <info@ogalala.com>

package com.ogalala.server;

import java.io.*;
import java.net.Socket;
import com.ogalala.crypt.AuthenticationException;
import com.ogalala.crypt.PasswordValidator;
import com.ogalala.util.*;
import com.ogalala.server.database.*;

/**
 *  A Login manager that does password-digest authentication
 *  for the server. The actual hashing stuff is done by the
 *  PasswordValidator class. This class deals with the
 *  authentication protocol only (ie, the exchange of messages
 *  leading to the yea or nay).
 *  <p>
 *  The protocol is specified in the document
 *  <a href="./protocol.txt">protocol.txt</a>. The basic exchange is
 *  pretty simple:
 *  <ol><tt>
 *  <li>SERVER: LOGIN\n
 *  <li>CLIENT: PROTOCOL <i></tt>version<tt></i>\n
 *  <li>SERVER: PROTOCOL <i></tt>reply<tt></i>\n
 *  <li>CLIENT: USER <i></tt>userID<tt></i>\n
 *  <li>SERVER: INVALID USER\n</tt> <b>or</b> <tt>SEED <i></tt>seed<tt></i>\n
 *  <li>CLIENT: PASS <i></tt>hash<tt></i>\n
 *  <li>SERVER: CONNECT <i></tt>success<tt></i>\n
 *  </tt></ol>
 *  <p>
 *  The argument to each message is described in the protocol document.
 *  <i>userID, seed</i> and <i>hash</i> constitute the actual user
 *  authentication scheme.
 *  <p>
 *  Other messages, terminated by a line feed, may be sent in
 *  either direction at any time, as long as they don't start with any
 *  of the tokens used in the protocol. At the client end these are probably
 *  output directly to the console. At the server end (dealt with in
 *  this class) they are simply ignored. Messages that conform to the
 *  protocol but are in the wrong order are also ignored.
 *  <p>
 *  The server can reject the connection at any time. If the whole
 *  transaction proceeds successfully, the connection is accepted and
 *  the user is logged in.
 *  <p>
 *  The whole login process is aborted after 30 seconds by the Login
 *  superclass.
 *  <p>
 *  The PasswordTransactionServer class needs access to the user
 *  database in order to map usernames and passwords. This is wrapped
 *  by the internal function <tt>getPassword()</tt>. When a sensible
 *  mechanism for reading the database has been implemented, it'll be
 *  used here. In the meantime, it accepts all usernames as valid, and
 *  specifies the password "ogalala" for everyone.
 *  <P>
 *  NB: at present this implementation diverges somewhat from the
 *  protocol specification document (it is case-sensitive in places).
 *  This is okay for the moment as the client is matchingly wrong,
 *  but both will be fixed soon!

 */
public class PasswordLogin
	extends Login
{
	//-----------------------------------------------------------------

	// class & instance variables
    UserDatabase userDatabase;

	/**
	 *  Maximum number of failed login attempts to allow before
	 *  aborting the connection. Note that current clients will
	 *  not take advantage of this, they'll terminate the connection
	 *  after the first failure, but future clients may want to
	 *  drop back to a previous protocol version.
	 */
	private static final int MAX_LOGIN_ATTEMPTS = 3;

	//-----------------------------------------------------------------

	/**
	 *  Constructor.

	 @see com.ogalala.server.Login

	 */
	public PasswordLogin ( Server serverIN, Socket socketIN )
	{
		super ( serverIN, socketIN );
	}

	/** Init function
	*/
	protected void init()
		{
		userDatabase = ((SessionServer)server).getUserDatabase();
		}

	//-----------------------------------------------------------------

	/**
	 *  The actual login negotiation process. Sets userName to
	 *  the correct ID if successful, or to null otherwise.
	 */
	public void negotiateLogin ()
	{
		// System.out.println("Starting login negotiation");

		// start by outputting the banner message
		// this can be anything that doesn't include
		// the word "LOGIN" at the start of a line
		printBannerMessage();

		// make sure the banner got terminated
		out.print("\n");

		// an object we'll use for validation
		PasswordValidator auth = new PasswordValidator();

		// allow a maximum number of login attempts
		for ( int attempts = 0;
			  attempts < MAX_LOGIN_ATTEMPTS && userName == null;
			  attempts++ )
		{
			// validator must start from a virgin state
			auth.reset();

			try
			{
				// System.out.println("Sending LOGIN" );

				// send the initial challenge
				out.print("LOGIN\n");
				out.flush();

				String incoming, version;

				// loop reading until we get a line conforming
				// to the protocol
				while ( true )
				{
					incoming = in.readLine().trim();

					// System.out.println("Received " + incoming);

					if ( incoming.startsWith("PROTOCOL ") )
					{
						version = incoming.substring(8).trim();
						break;
					}
				}

				// if the protocol version is "0.1" it's acceptable
				// otherwise, the login attempt fails
				if ( "0.1".equals(version) )
				{
					// System.out.println("Sending PROTOCOL OK");
					out.print("PROTOCOL OK\n");
					out.flush();
				}
				else
				{
					// System.out.println("Sending PROTOCOL 0.1");
					out.print("PROTOCOL 0.1\n");
					out.flush();
					continue;
				}

				// loop reading until we get a line conforming
				// to the protocol
				while ( true )
				{
					incoming = in.readLine().trim();
					// System.out.println("Received " + incoming);
					if ( incoming.startsWith("USER ") )
					{
						userName = incoming.substring(4).trim();
						break;
					}
				}


				String password = getPassword( userName );

				if ( password == null )
                {
				    // ### not happy that it is possible for remote machines to obtain
				    // ### who isn't valid user accounts on our box, but the following
				    // ### allows this
    				// it's an invalid user, this counts as a failed attempt
    				// System.out.println("Sending INVALID USER");
					out.print("INVALID USER\n");
					System.out.println("INVALID USER");
					out.flush();
					userName = null;
					continue;
                }

				// it's a valid user, so let's authenticate her
				String seed = auth.getSeed();

				// send the seed to the client
				// System.out.println("Sending SEED " + seed);
				out.print("SEED " + seed + "\n" );
				out.flush();

				String clienthash;

				// loop reading until we get a line conforming
				// to the protocol
				while ( true )
				{
					incoming = in.readLine().trim();
					// System.out.println("Received " + incoming);
					if ( incoming.startsWith("PASS ") )
					{
						clienthash = incoming.substring(4).trim();
						break;
					}
				}

				// attempt to authenticate user
				// report success or failure to client
				// and then succeed or fail
				if ( auth.authenticate ( password, clienthash ) )
				{
					// System.out.println("Sending CONNECT OK");
					out.print("CONNECT OK\n");
					out.flush();
					return;
				}

				// System.out.println("Sending CONNECT FAILED");
				out.print("CONNECT FAILED\n");
				out.flush();
				userName = null;
			}
			// IOExceptions cause us to abort
			catch ( IOException e )
			{
				userName = null;
				return;
			}
			// AuthenticationExceptions send us back to square one
			catch ( AuthenticationException e )
			{
				userName = null;
			}
		}
	}

	//-----------------------------------------------------------------

	/**
	 *  Map user ID to password. Returns null for an unknown
	 *  user, otherwise returns the required password as a
	 *  string. In the long term, this will probably be stored
	 *  as a hash value, but for the moment it uses real text.
	 *  (This will actually be a function of the client; the
	 *  server doesn't need to know.)
	 *  <p>

	 @param user  The ID to get the password for.
	 @return      The password for the specified user.

	 */
	public String getPassword ( String user )
	{
		// reject null users
		if ( user.trim().length() == 0 )
			return null;

		// everyone else
        try
        {
            return userDatabase.getPasswordForUser(user);
	    }
        catch (IOException e)
        {
            Debug.printStackTrace( e );
            // ### should we stop the thread/server here? after all the db
            // ### must be broken... how is anyone going to get in?
            // ### for now returning null will send invalid user to the client.
            return null;
        }
        catch (com.ogalala.server.database.UserNotFoundException e)
        {
            Debug.printStackTrace( e );
            return null;
        }
	}

	//-----------------------------------------------------------------
}

