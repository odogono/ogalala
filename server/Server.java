// $Id: Server.java,v 1.24 1999/04/26 12:24:23 jim Exp $
// Server class
// Richard Morgan/James Fryer, 11 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.io.*;
import java.net.*;
import com.ogalala.util.*;

/** The Server class is the main class for the server framework.<p>

    The basic concept is that a Server manages input and output to an
    application. (The test application is a chat system.)<p>

    Each user logging in creates a Connection, which encapsulates input
    and output to the network socket. Input to the application is handled
    by overriding the 'Connection.processInput' function. Output is handled
    by calling the 'Connection.output' function from the application.<p>

    The Login process is kept separate from the Connection and the application
    does not have to be concerned with logging in. It is hoped that a future
    version of this framework will implement various login procedures that
    can be used "off the peg" rather than requiring an app to implement its
    own login handler.<p>

    To use the framework you will need to extend the following classes:<p>
  <UL>
  <LI>  Server: Add application-specific data (usually a reference to the
        application class); implement 'createLogin' to create a login of the
        appropriate kind; implement 'createConnection' to create the
        connection type required by the application. (There may be more than
        one connection type for an app, or there may be more than one app per
        server.)

  <LI>  Login: Implement 'negotiateLogin' to perform whatever login process
        is appropriate for the application.

  <LI>  Connection: Implement 'processInput' to direct input to your
        application (it is probably a good idea to keep a back-pointer to
        the app in a Connection); optionally implement 'init', 'destroy',
        'start' and 'stop' as required. (Note: these functions, unlike the
        equivalent Applet functions, require that their 'super' versions are
        called; this restriction will hopefully be lifted in the future.)
    </UL>

    The Chat application and others demonstrates how to do all this.
    <br>Note that the
    list of users in Chat is the same as the connection list. Later apps
    will probably need their own lists of logged in users, as they may
    have multiple locations, or several apps may be running on the same
    server, each requiring its own list of active users.<p>

    Synchronization: You must synch on the connection list whenever you use
    it, to prevent other threads from changing it behind your back.<p>

*/
abstract public class Server
	{
	/** Port on which we will listen
	*/
	private int port = 0;

    /** handle for the server socket
    */
    private ServerSocket serverSocket = null;

	/** Time out value (in milliseconds) for the listen on the server socket
	*/
	private static final int TIMEOUT = 5 * 1000; 

    /** The connection has been closed down
    */
    private static final int STOPPED = 0;

    /** The connection is running
    */
    private static final int RUNNING = 1;
    
    /** The connection is waiting to be closed down
    */
    private static final int CLOSING_DOWN = 2;

    /** Current state of the connection: RUNNING, CLOSING_DOWN or STOPPED.
    */
    private int state = STOPPED;

	/** Create the server socket and other server-level objects
	*/
	public Server(int port)
		{
        log("Initialising port " + port);

		// Set the port
		this.port = port;

        // Create the server socket
        createServerSocket();
      	}

    /** Create the server socket connection
    */
    private void createServerSocket()
        {
		Debug.assert(port != 0, "(Server/105)");
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(TIMEOUT);
	        }
	    catch (IOException e)
	    	{
            log("Could not create ServerSocket on port " + port);
	    	Debug.printStackTrace(e);
            System.exit(-1);
	        }
        }

	/** Listen for client connections
	*/
	public void start()
		{
		Debug.assert(serverSocket != null, "(Server/123)");

    	// flag the server as running
    	state = RUNNING;

        // Loop while still listening, accepting new connections from the server socket
        while (state == RUNNING)
        	{
            // Get a connection from the server socket
			Socket socket = null;
			try {
                socket = serverSocket.accept();
            	if (state == RUNNING) 
            	    createLogin(socket);
                }
                
            // The socket timeout allows us to check if the server has been shut down.
            //  In this case the state will not be RUNNING and the loop will end.
		    catch ( InterruptedIOException e )
		    	{
		    	// timeout happened
		    	}
		    
		    // This shouldn't happen...
    	    catch ( IOException e )
    	    	{
                log("Server: Error creating Socket");
    	    	Debug.printStackTrace(e);
    	        }
	        }
	    
	    // We've finished, clean up
        disconnect();
		}

	/** Request the server stops
	*/
	public synchronized void stop()
		{
		state = CLOSING_DOWN;
		}

	/** Clean up the server
	*/
	public synchronized void disconnect()
		{
		if (state == RUNNING)
		    stop();
		if (state == CLOSING_DOWN)
		    {
            // Close the server socket		    
            try {
        		serverSocket.close();
            	}
            catch (IOException e)
                {
                }
                
    		// flag the server as stopped
    		state = STOPPED;
		    }
		}

    /** Output a diagnostic message
    */
    protected final void log(String msg)
        {
        Debug.println("Server: " + msg);
        }

	/** Make a login connection.
	    This function should be overridden to create the type of login that an application requires.
	*/
	protected abstract void createLogin(Socket socket);

    /** Create a Connection of the appropriate type for the application
        and whatever login information has been gathered.<p>

        Note that it may be neccessary to get the user's information from the
        database in order to determine what kind of connection to create.
    */
    public abstract void createConnection(Socket socket, String userName);
	}
