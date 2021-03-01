// $Id: PasswordConnector.java,v 1.1 1998/06/09 11:54:03 matt Exp $
// A class that handles authenticated logins for the client.
// Matthew Caldwell, 1 June 1998
// Copyright (c) Ogalala Ltd <info@ogalala.com>

package com.ogalala.client;

import java.io.*;
import com.ogalala.crypt.PasswordValidator;
import com.ogalala.crypt.AuthenticationException;
import com.ogalala.util.Eavesdropper;
import com.ogalala.net.Connector;

/**
 *  A Connector that does password-digest authentication for
 *  the client. The actual hashing stuff is done by the
 *  PasswordValidator class. This class deals with the
 *  authentication protocol only (ie, the exchange of messages
 *  leading to a yea or nay).
 *  <p>
 *  The protocol is specified in the document
 *  <a href="./protocol.txt">protocol.txt</a>. The basic exchange
 *  is pretty simple:
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
 *  of the tokens used in the protocol. These are passed to the
 *  <tt>err</tt> Eavesdropper object supplied to the <tt>connect()</tt>
 *  method. Typically, they will be output to the console, but that's
 *  a detail of the client implementation and not dealt with in this
 *  class.
 *  <p>
 *  Although the server supports multiple login attempts on a single
 *  connection, the client does not. The login procedure is entirely
 *  self-contained, and requires all the relevant data up front. If the
 *  attempt fails, it is most likely because of an error in the username
 *  or password, in which case these must be corrected before another
 *  attempt is made.
 *  <p>
 *  NB: at present this implementation diverges somewhat from the
 *  protocol specification document (it is case-sensitive in places
 *  and doesn't properly reset on receiving repeat "LOGIN\n" messages).
 *  This is okay for the moment as the server is matchingly wrong,
 *  but both will be fixed soon!
 
 @see com.ogalala.server.PasswordLogin
 
 */
public class PasswordConnector
	implements Connector
{
	//------------------------------------------------------------------------

	// instance variables
	
	/**
	 *  The ID of the user to log in as. This class does not restrict
	 *  this in any way. If a particular client and/or server imposes
	 *  its own restrictions, they must be enforced before the ID is
	 *  passed to this class.
	 */
	private String user;
	
	/**
	 *  The password with which to log in. This class does not restrict
	 *  this in any way. If a particular client and/or server imposes
	 *  its own restrictions, they must be enforced before the ID is
	 *  passed to this class. In this version, the password is assumed
	 *  to be stored in plaintext form by the server. Later versions
	 *  will probably pre-hash the password with the userID before
	 *  validating.
	 */
	private String pass;
	
	//------------------------------------------------------------------------
	
	/**
	 *  Constructor. The specified userID and password are used to
	 *  actually log in with by the <tt>connect()</tt> method.
	 
	 @param userID    The ID of the user to log in as.
	 @param password  The user's password.
	 
	 */
	public PasswordConnector ( String userID,
							   String password )
	{
		// initialize our instance variables with the
		// supplied values
		user = userID;
		pass = password;
	}

	//------------------------------------------------------------------------

	/**
	 *  Login on the connection for which the input and output
	 *  streams are provided. Unknown messages on these streams
	 *  are reported to the Eavesdropper object. The userID and
	 *  password set in the constructor are used to login with.
	 *  Implements the Connector interface.
	 
	 @param in   Stream from which to read incoming messages.
	 @param out  Stream to which to write outgoing messages.
	 @param err  Object to which unknown messages should be reported.
	 @return  Whether or not the login attempt was successful.
	 
	 @see com.ogalala.net.Connect
	 @see com.ogalala.net.TextConnection
	 
	 */
	public boolean connect ( BufferedReader in,
							 PrintWriter out,
							 Eavesdropper err )
		throws IOException
	{
		// an object we'll use for validation
		PasswordValidator auth = new PasswordValidator();
		
		String incoming;

		try
		{			
			// read lines until we receive the initial challenge
			while ( true )
			{
				incoming = in.readLine().trim();
					
				if ( "LOGIN".equalsIgnoreCase(incoming) )
					break;
				err.hear ( incoming );
			}
			
			// specify that we want to use protocol version 0.1
			// err.hear ( "Sending PROTOCOL 0.1" );
			out.print("PROTOCOL 0.1\n");
			out.flush();
			
			// wait for a response that either accepts or rejects
			// this protocol
			while ( true )
			{
				incoming = in.readLine().trim();
				if ( incoming.startsWith( "PROTOCOL OK" ) )
					break;
				else if ( incoming.startsWith( "PROTOCOL " ) )
					return false;
				
				err.hear ( incoming );
			}
			
			// send the user ID
			// err.hear( "Sending USER " + user );
			out.print( "USER " + user + "\n" );
			out.flush();
			
			// wait for a response that either accepts of rejects
			// this user, and for the former provides the password
			// hash seed
			while ( true )
			{
				incoming = in.readLine().trim();
				if ( "INVALID USER".equalsIgnoreCase( incoming ) )
					return false;
				else if ( incoming.startsWith( "SEED " ) )
				{
					auth.setSeed ( incoming.substring(4).trim() );
					break;
				}
				
				err.hear ( incoming );
			}
			
			// hash the password and send it
			// err.hear ( "Sending PASS " + auth.getHash ( pass ) );
			// out.print ( "PASS " + auth.getHash() + "\n" );
			out.print ( "PASS " + auth.getHash ( pass ) + "\n" );
			out.flush();
			
			// wait for success or failure of login attempt
			while ( true )
			{
				incoming = in.readLine().trim();
				if ( "CONNECT FAILED".equalsIgnoreCase( incoming ) )
					return false;
				else if ( "CONNECT OK".equalsIgnoreCase( incoming ) )
					return true;
					
				err.hear ( incoming );
			}
			
		}
		catch ( NullPointerException e )
		{
			// if the connection fails, readLine() may return null
			// and null.trim() may throw this exception. In this
			// case, the login has obviously failed.
			return false;
		}
		catch ( AuthenticationException e )
		{
			return false;
		}
	}

}