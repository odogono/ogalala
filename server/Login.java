// $Id: Login.java,v 1.20 1999/04/26 12:24:23 jim Exp $
// Controls the login process
// James Fryer, 13 March 98, Richard Morgan, 24 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.net.*;
import java.util.*;
import java.io.*;
import com.ogalala.util.*;

/** The Login class handles the user name/password protocol (which must be defined
    by subclassing this class to an application-specific Login class) and creates
    the Connection.<p>

    A connection will timeout after a period time, this is controlled by the inner class LoginTimer.
    <p>
	Once the connection has been successfully negoated the socket is then passed to the
	server who turns it in to a 'proper connection' for the server app. To denote a failed
	negoation for the server to reconize the username is set to null.
*/

abstract public class Login 
    implements Runnable
    {
    /** Timeout value in milliseconds
    */
    final long TIMEOUT = 1000 * 60 * 2;

    /** The user name. If the login is successful, this should be set to a non-
        null value by 'negotiateLogin'.
    */
    protected String userName;

    /** The network socket
    */
    protected Socket socket = null;

    /** Input stream
    */
    protected BufferedReader in = null;

    /** Output stream
    */
    protected PrintWriter out = null;

    /** The internet address that the login originates from
    */
    protected InetAddress netAddress;

    /** Information about the login
    */
    protected String loginInfo;

    /** The thread
    */
    protected Thread thread = null;

    /** Login banner message
    */
    static private String bannerMsg = null;

    /** Login prompt
    */
    static private String loginPrompt = "login: ";
    
    /** Timeout message
    */
    static private String timeoutMsg = "Your allowed connection time has expired, please try to connect again.";

    /** Timeout object
    */
    private LoginTimer loginTimer;

    /** Back-pointer to server
    */
    protected Server server;

	/** This constructor cannot be called on its own as this class is abstract.
		@param server The server derived class that handles the network connections in and out of the app.
		@param socket The new socket that requires passing thougth the login process.
	*/
    public Login(Server server, Socket socket)
        {
        // Initialise object
        this.server = server;
        this.socket = socket;
        netAddress = socket.getInetAddress();
        loginInfo = netAddress.toString() + " " + Debug.getTimeStamp();
        init();

        // Start the timeout
        loginTimer = new LoginTimer();
        loginTimer.start();

        // Start the thread
        thread = new Thread(this, "Login Thread (" + loginInfo + ")");
        thread.start();
        thread.setPriority(Connection.INPUT_PRIORITY);
        }

    /** Handle the login process.
        <p>
        This function is always entered with 'userName' set to null. If the login
        succeeds, 'userName' should be set appropriately. If 'userName' is null
        when this function terminates then the login has failed.
    */
    protected abstract void negotiateLogin();

    /** Set the login messages
    	@param bannerMsgArg Sets the banner message displayed when the user connects to the server
    	@param loginPrompt Sets the login prompt, typically "login:"
    */
    public static void setMessages(String bannerMsgArg, String loginPromptArg)
        {
        bannerMsg = bannerMsgArg;
        loginPrompt = loginPromptArg;
        }

	/** Init function for subclasses
	*/
	protected void init()
		{
		}

	/** The login thread, either this will come to a natural end if the user manages to log in
		within the allowed number of attempts or he goes over the number of allowed attempts.
		Otherwise this thread can be killed by external observers via the stop() function.
	*/
    public void run()
        {
        log("Start");

        // Create streams
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            }
        catch (IOException e)
            {
            log("Error getting streams");
            return;
            }

        // Assume failure
		userName = null;

        // Call the login process, defined in the child class
        negotiateLogin();

        // the login phase has finished so kill the timer
        loginTimer.stop();

        // If the user name is not null, we have a valid login so create the connection
        if (userName != null)
            {
            log("Accepted: " + userName);
            createConnection();
            }

        // Else the login failed
        else {
            log("Failed");
            }
        }

    /** Kill the login process
    */
    public void stop()
        {
        // kill the input/output
        try {
            socket.close();
            }
        catch (IOException e)
            {
            log("Error closing socket: " + e);
            }

        // this thread should be stopped last as if it is the thread of execution running this
        // function it will not get to the end!!
        thread.stop();
        }

    /** Create a Connection of the appropriate type for the application
        and whatever login information has been gathered.
    */
    protected void createConnection()
        {
        // ASSERT(userName != null);
        server.createConnection(socket, userName);
        }

    /** Print the banner message (if any) when the login is started
    */
    protected void printBannerMessage()
        {
        if (bannerMsg != null)
            out.println(bannerMsg);
        }

    /** Print the login prompt
    */
    protected void printLoginPrompt()
        {
        out.print(loginPrompt);

        // flush because there may not be a newline at the end
        out.flush();
        }

    /** Return true if the string is a valid user name.
    	(In this version any string is valid, but heirs of this class could redefine this)
    */
    protected boolean isValidUserName(String userName)
        {
        return true;
        }

    /** Return true if the user name is in use
    */
    protected boolean isLoggedIn(String userName)
        {
        ConnectionList connections = Connection.getConnectionList();
        return connections.find(userName) != null;
        }

    /** Output a diagnostic message
    */
    protected final void log(String msg)
        {
        Debug.println("Login (" + loginInfo + "): " + msg);
        }

    /** Timeout thread waits for the specified time then kills the login process.
    */
    class LoginTimer 
        extends Thread
        {
        public LoginTimer()
            {
            super("Login timeout thread");
            }

    	/** Waits then kills the login process
    	*/
        public void run()
            {
            try {
                // Sleep until timeout
                sleep(TIMEOUT);
                }
            catch (InterruptedException e)
                {
                }

            log("Timed out");

    		// inform the user that they have timed out
    		out.println(timeoutMsg);

            // stop the login process
            Login.this.stop();
            }
        }
    }
