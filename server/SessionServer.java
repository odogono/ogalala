// $Id: SessionServer.java,v 1.44 1999/08/24 13:46:17 jim Exp $
// A server for multiple user sessions and applications
// James Fryer, 22 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import com.ogalala.server.database.*;
import com.ogalala.util.*;
import java.net.Socket;
import java.util.*;
import java.io.*;

/** The Session Server implements a layer over the Server Framework. The
	 following features are provided:
	 <ul>
	 <li> A User class, which draws information from...
	 <li> The UserDatabase class
	 <li> A Command Interpreter which manages server commands
	 <li> A collection of abstract Applications, and mechanisms to send commands
		  to and receive output from them.
	 <li> Formatting for the three kinds of message the server can send back to
		  the user: Messages, Acknowlegements and Errors.
	 </ul>

	 ### I am not sure about the naming here. Really I'd like to rename the 'Server'
	 ###     class 'BasicServer' so we can call this one 'Server'...
*/
public class SessionServer
    extends Server
	{
    /** Version number
    */
    public static final String VERSION = "0.4";

	private static final String DEFAULT_DB_NAME = "users";

	/** The server owns and maintains the user database object. It is private, a 'get' fn is provided.
	*/
	private UserDatabase userDatabase;

	/** The command interpreter
	*/
	private ServerCommandInterpreter cli = new ServerCommandInterpreter();

	/** Shutdown thread. If this is not null then the system is due to be shut down.
	*/
	private ShutdownThread shutdownThread;

	/** The table of applications
	<p>### Hash not dict because dict doesn't have 'clear'...
	*/
	Hashtable applications = new Hashtable();

	/** The command log file name. If null, we are not logging
	*/
	private String commandLogName = null;

	/** The command log stream
	*/
	private PrintWriter commandLog = null;

	/** Constructor

        @param port The port to listen to
        @param databaseName the name of the user database to open @see com.ogalala.server.database.UserDatabase
	*/
	public SessionServer(int port, String databaseName)
		{
		super(port);

		// Open User database
		try {
			if (databaseName == null || databaseName.trim().equals(""))
				databaseName = DEFAULT_DB_NAME;

			if (UserDatabase.exists(databaseName))
				{
				log("Loading database: " + databaseName);
				userDatabase = new UserDatabase(databaseName);
				}
			else {
				log("Creating database: " + databaseName);
				userDatabase = new UserDatabase();
				userDatabase.create(databaseName, null);
				}
			}
		catch (java.io.IOException e)
			{
			log("Can't open database: " + databaseName);
			Debug.printStackTrace(e);
			System.exit(1);
			}
		}

	/** Execute a script file containing commands as if entered through a connection.
	*/
	public synchronized void execScript(String fileName)
	    {
		// Set a high priority
		//### This may well not be the best way to do what we want, which is
		//###   to make sure the startup script executes quickly
		int oldPriority = Thread.currentThread().getPriority();
  	    Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 1);

	    try {
            // Open the script file
            FileInputStream in = new FileInputStream(fileName);
	        
	        // Create a stream-based User object
	        User user = new User(this, UserDatabase.ADMIN_USER_ID, in, System.out);
            
            // Commands from the input stream will be excuted... Some synching required??? ###
    	    }
    	catch (IOException e)
    	    {
			log("IO error in script: " + e.toString());
			Debug.printStackTrace(e);
    	    }
    	finally
    	    {
    	    // Restore old priority
    	    Thread.currentThread().setPriority(oldPriority);
    	    }
	    }

	/** Start an application
	*/
	public synchronized void startApplication(String appClassName, String appID, Enumeration args)
	    throws ApplicationOpenException
		{
        // Don't start an already active application
        if (getApplication(appID) != null)
            throw new ApplicationOpenException("Application already active: " + appID);

        // Create the application object
        Application app = ApplicationFactory.newApplication(appClassName);
        if (app == null)
            throw new ApplicationOpenException("Application class not found: " + appClassName);

        // If the app doesn't exist, create it. Else, open it.
        boolean created = true;
        if (!app.exists(appID))
            app.create(appID, args);
        else {
            app.open(appID, args);
            created = false;
            }

        // Add the app to the list
        appID = app.getID();
        applications.put(appID, app);

        log((created ? "Created" : "Opened") + " application: " + appID + " (" + appClassName + ")");
		}

	/** Stop an application
	*/
	public synchronized void stopApplication(Application app)
		{
        // Remove the app from the list and close it
        applications.remove(app.getID());
        app.close();
		}

	/** Stop all applications
	*/
	private void stopAllApps()
		{
		Enumeration enum = applications.elements();
		while (enum.hasMoreElements())
			{
			Application app = (Application) enum.nextElement();
			app.close();
			}
	    applications.clear();
		}

	/** Get an application from the list
	*/
	public synchronized Application getApplication(String appID)
		{
		// We might have been passed a channel ID, so get the app ID from it
		appID = ChannelUtil.getApplicationID(appID);
		return (Application)applications.get(appID);
		}

	/** Enumerate applications
	*/
	public final Enumeration getApplications()
		{
		return applications.elements();
		}

	/** Cleanly shut down the server, a param of -1 is used to display the shutdown information
	*/
	synchronized void shutdown(int minutesToShutdown, User user)
		{
		if (minutesToShutdown < 0)
			{
			if (shutdownThread == null)
				user.outputMessage("No shutdown scheduled.");
			else {
				long milliSeconds = shutdownThread.getTimeLeft();
				user.outputMessage("The server will go down in " + shutdownThread.milliSecondsToString(milliSeconds));
				}
			}
		else if (shutdownThread == null)
			{
			shutdownThread = new ShutdownThread(user, minutesToShutdown);
			}
	    else
    		user.outputMessage("SHUTDOWN: The server is already shutting down");
		}

	/** Cancel a shutdown request
	*/
	synchronized void cancelShutdown(User user)
		{
		if (shutdownThread != null)
			{
			shutdownThread.stop();
			shutdownThread = null;
			user.outputMessage("SHUTDOWN: cancelled.");
			log("Shutdown cancelled by user " + user.getUserId());
			}
		else {
			user.outputMessage("SHUTDOWN: There is no shutdown in progress.");
			}
		}

	/** Get the server command interpreter
	*/
	public final ServerCommandInterpreter getCLI()
		{
		return cli;
		}

	/** Get the user database
	*/
	public final UserDatabase getUserDatabase()
		{
		return userDatabase;
		}

	/** Broadcast a message to all users
	*/
	public void broadcast(String msg)
		{
		ConnectionList connections = User.getConnectionList();
		synchronized (connections)
			{
			Enumeration users = connections.elements();
			while (users.hasMoreElements())
				{
				User user = (User)users.nextElement();
				user.outputMessage(msg);
				}
			}
		}

	/** Format a Server Message.
		 <p>
		 Messages are in the general format:
		 <br>
		 &nbsp;&nbsp;  @TAG message
		 <br>
		 Where TAG can be one of: MSG, ACK, ERR.
		 <p>
		 MSG can occur at any time. ACK and ERR occur only in response to
		 server commands. ACKs always have an empty message
	*/
	public static String formatMessage(String msg)
		{
		return formatOutput("MSG", msg);
		}

	/** Format a Server Ack Message.
	*/
	public static String formatAck()
		{
		return formatOutput("ACK", null);
		}

	/** Format a Server Error Message.
	*/
	public static String formatError(String msg)
		{
		return formatOutput("ERR", msg);
		}

	/** Format an output message
	*/
	private static String formatOutput(String tag, String msg)
		{
		StringBuffer buf = new StringBuffer();
		buf.append(ServerCommandInterpreter.COMMAND_PREFIX);
		buf.append(tag);
		if (msg != null)
			{
			buf.append(" ");
			buf.append(msg);
			}
		return buf.toString();
		}

	/** Create the Login object
	*/
	protected void createLogin(Socket socket)
		{
		new PasswordLogin(this,socket);
		}

	/** Create the Connection object
	*/
	public void createConnection(Socket socket, String userName)
		{
		new User(this, userName, socket);
		}

	/** Called just before the server is stopped, we need to kick off all users
		and stop all appilications b4 shutdown.
	*/
	public void stop()
		{
		closeAllConnections();
		stopAllApps();
		super.stop();
		}

	private void closeAllConnections()
		{
		Connection.getConnectionList().removeAll();
		}
    
	/** Activate/deactivate command logging
	*/
	public final void setCommandLog(String commandLogName)
		{
		this.commandLogName = commandLogName;
		}

	/** Log a command
	    @param command The command to log (may be output to the log file)
	    @param commandTime The time when the command was issued
	    @param commandDuration The time it took to perform the command
	*/
	protected synchronized void logCommand(String command, long commandTime, long commandDuration)
		{
        if (commandLogName != null)
            {
            // If the file is not open, open it and write the header
            if (commandLog == null)
                {
                try {
                    commandLog = new PrintWriter(new FileOutputStream(commandLogName), true);
                    }
                catch (IOException e)
                    {
                    // If we can't create the file there's nothing we can do about it...
                    return;
                    }
                commandLog.println("CommandTime\tProcessTime\tUserCount\tCommand");
                }
            
            // Write log data
            StringBuffer buf = new StringBuffer();
            buf.append(Long.toString(commandTime));
            buf.append("\t");
            buf.append(Long.toString(commandDuration));
            buf.append("\t");
            buf.append(Connection.getConnectionList().size());
            buf.append("\t");
            buf.append(command);
            commandLog.println(buf.toString());
            commandLog.flush();
            }
		}

    /** Number of milliseconds in a second
    */
    private static final int SECOND = 1000;
        
    /** Number of milliseconds in a minute
    */
    private static final int MINUTE = SECOND * 60;
        
	/** Manage the shutdown process.
	*/
	class ShutdownThread
    	extends Thread
		{
		/** The ID of the user who requested the shutdown
		*/
		private String userID;

		/** The system time when the server must shut down, in milliseconds. (Note all 
		    times are stored in ms internally although they are displayed in minutes.)
		*/
		private long shutdownTime;

		ShutdownThread(User user, int minutesToShutdown)
			{
			super("Shutdown Thread (" + user.getUserId() + ", " + minutesToShutdown + ")");
			this.userID = user.getUserId();
			shutdownTime = System.currentTimeMillis() + ((long)minutesToShutdown * MINUTE);
			log("Shutdown (" + minutesToShutdown + " minutes) requested by " + user.getUserId());
      	    setPriority(Thread.NORM_PRIORITY - 1);
			this.start();
			}

		public void run()
			{
			broadcastShutdownMessages();

			// Shut down the system
			broadcast("The system is going down NOW");
			SessionServer.this.stop();
			log("Shutdown by " + userID);
			}
        
        /** Broadcast messages to the users about the impending shutdown.
        */
        private void broadcastShutdownMessages()
            {
			// Loop until out of time. Broadcast messages according to the 'nextSleepTime' function
			while (true)
				{
				try {
    				// How long until the shutdown?
    				long timeLeft = getTimeLeft();
//        			log("shutdown: timeLeft: " + timeLeft);
    				
    				// If there is no time left, return
    				if (timeLeft <= 0)
    				    return;
    				    
    				// If there is only one minute left, warn the users and sleep for one more minute
    				//  (We stretch the meaning of "a minute" a little here to tidy up any 
    				//  innacuracy in the sleeps)
    				else if (timeLeft <= (MINUTE * 3) / 2)
    					{
    					broadcast("The system is going down in ONE MINUTE!");
    					sleep(MINUTE);
    					}

    				// Else, warn and calculate the next shutdown time
    				else {
    					broadcast("The system is going down in " + milliSecondsToString(timeLeft));

    					// Get the timeout appropriate for the time left until shutdown
  						sleep(nextSleepTime(timeLeft));
    					}
                    }
				catch (InterruptedException e)
					{
					}
				}
            }
            
		/** Get the time to wait (in millis) until broadcasting another 
		    shutdown warning message.
		    <p>
		    A message is broadcast 30 minutes before the shutdown time, then every 5 minutes
		    until 5 minutes before shutdown time, then every minute.
		*/
		private long nextSleepTime(long timeLeft)
			{
			if (timeLeft > 30 * MINUTE)
				return timeLeft - (30 * MINUTE);
			else if (timeLeft > 5 * MINUTE)
				return 5 * MINUTE;
			else if (timeLeft > MINUTE)
				return MINUTE;
		    else
    			return 0;
			}

		/** returns time left to wait in milli-seconds
		*/
		public long getTimeLeft()
			{
            long result = shutdownTime - System.currentTimeMillis();
            if (result < 0)
                result = 0;
            return result;
			}

		/** Take a millisecond values and convert it to a form that is human readable
		*/
		public final String milliSecondsToString(long millis)
			{
//			log("shutdown: millis: " + millis);
			return Long.toString((millis + MINUTE/2) / MINUTE) + " minutes";
			}
        
/*
        protected void broadcast(String msg)
	        {
	        log(msg);
	        SessionServer.this.broadcast(msg);
	        }
*/	        
		}
	}
