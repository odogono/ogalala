// $Id: Connection.java,v 1.29 1999/04/26 12:24:23 jim Exp $
// Represents a connection to the server
// Richard Morgan/James Fryer, 11 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.net.*;
import java.io.*;
import com.ogalala.util.*;

/** Connection class, represents a logged-in user
*/
abstract public class Connection
    implements Runnable
	{
    /** List of active connections
    */
    protected static ConnectionList connectionList = new ConnectionList();

    /** Back-pointer to server
    */
    protected Server server;

    /** Handle for the network socket
    */
    private Socket socket = null;

    /** Input thread
    */
    private Thread inThread;

    /** Priority for input thread
    */
	public static final int INPUT_PRIORITY = Thread.MAX_PRIORITY - 1;

    /** Output queue
    */
    private OutputQueue outQueue;

    /** Input timeout (milliseconds). This is the time that input 
        will block for before throwing InterruptedIOException
    */
	public static final int INPUT_TIMEOUT = 1000;

    /** Output timeout (milliseconds). This is the time that output
        will wait for before checking if the server is alive.
    */
	public static final int OUTPUT_TIMEOUT = 5000;

    /** I/O streams
    */
    protected BufferedReader inStream;
    protected PrintWriter outStream;

    /** User information
    */
    private String userName;
    
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

    /** Create a socket-based connection
    */
    public Connection(Server server, String userName, Socket socket)
    	{
    	this(server, userName);
    	
	    // Set socket and timeout
    	this.socket = socket;
    	try {
        	socket.setSoTimeout(INPUT_TIMEOUT);
            }
        catch (SocketException e)
            {
            log("Set socket timeout exception");
            Debug.printStackTrace(e);
            return;
            }
            
	    // Initialise with streams from socket
	    try {
	        initIntern(socket.getInputStream(), socket.getOutputStream());
    	    }
        catch (IOException e)
            {
            log("Socket IO exception");
            Debug.printStackTrace(e);
            return;
        	}
	    }

    /** Create a stream-based connection
    */
    public Connection(Server server, String userName, InputStream in, OutputStream out)
    	{
    	this(server, userName);
        initIntern(in, out);
	    }

    private Connection(Server server, String userName)
    	{
	    // Set server, and user name
    	this.server = server;
    	this.userName = userName;
	    }

    /** Internal initialise function
    */
    private void initIntern(InputStream in, OutputStream out)
        {
	    // Set up I/O streams
    	inStream = new BufferedReader(new InputStreamReader(in));
    	outStream = new PrintWriter(out, true);

    	// Create the output queue
    	outQueue = new OutputQueue("Output thread for user: " + userName);

	    // Call the inheritable initialise function
	    init();

    	// Add this connection to the connection list
    	connectionList.add(this);

    	// Create/start the threads
    	inThread = new Thread(this, "Input thread for user: " + userName);
        inThread.setPriority(INPUT_PRIORITY);

        // Call the inheritable start function
        start();
        log("Started");
        }

    /** Called after the connection is created, before it is started
    */
    public void init()
        {
        }

    /** Called before the connection is destroyed
    */
    public void destroy()
        {
        }

	/** Called to start the connection running
	*/
	public void start()
	    {
        state = RUNNING;
    	inThread.start();
    	outQueue.start();
	    }

	/** Called to stop the connection
	*/
	public void stop()
	    {
	    // Flag that the connection is no longer active. The cleanup is done in 'disconnect'.
	    state = CLOSING_DOWN;
	    }

	/** Close the connection and remove it from the list
	*/
	private void disconnect()
	    {
	    // If the connection is still running, stop it
	    if (state == RUNNING)
	        stop();

	    // Close the connection down if required
	    if (state == CLOSING_DOWN)
	        {
	        // Call inheritable destroy function
	        destroy();

    	    // Remove from the connection list
    	    connectionList.remove(this);

	        // Clean up resources. If the socket is not null then the streams belong
	        //  to the creator of the channel, so we don't close them.
            if (socket != null)
    	        {
    	        try {
    	            inStream.close();
                    outStream.close();
      	            socket.close();
        	        }
        	    catch (IOException e)
        	        {
        	        }
    			catch (NullPointerException e)
    				{
    				}
                }
                
            // Go into stopped state
            state = STOPPED;
            log("Stopped");

	        // kill threads
            outQueue.stop();
            inThread.stop();    // This must be last, as it can kill the current thread!
	        }
        }

    /*  Implement 'finalize' to ensure that 'disconnect' gets called
    */
    protected void finalize()
        { 
        disconnect(); 
        }

	/** Public output function
	*/
	public void output(String s)
	    {
	    // Add the string to the output queue
        outQueue.put(s);
	    }

	/** Get the user iD.
	*/
	public String getUserId()
	    { 
	    return userName; 
	    }

	/** Get the connection list
	*/
	public static ConnectionList getConnectionList()
	    { 
	    return connectionList; 
	    }

    /** Output a diagnostic message
    */
    protected final void log(String msg)
        {
        Debug.println("Connection (" + userName + "): " + msg);
        }

    /** Get strings from the input stream and dispatch them
    */
    public void run()
    	{
    	while (state == RUNNING)
    	    {
          	// Get a string from the input stream
          	String s = null;
            try {
                s = inStream.readLine();
                }
                
            // This exception indicates that the read has timed out. If 
            //  the connection has been closed while the IO blocked this is 
            //  our opportunity to close down.
            catch (InterruptedIOException e)
                {
                if (state == RUNNING)
                    continue;
                else
                    s = null;
                }
                
            // If there's an error in the communication stream, we can assume that the user has departed
            catch (IOException e)
                {
                s = null;
            	}

    	    // If the string is null, then we've lost the connection
    	    if (s == null)
    	        stop();

    	    // Else, send it off to be processed
    	    else {
    	        try {
        	        processInput(s);
        	        }

    	        // If 'processInput' allows errors through, report them
    	        //  here to prevent the connection dying.
        	    catch (RuntimeException e)
        	        {
        	        log("processInput exception");
        	        Debug.printStackTrace( e );
        	        output("Error processing input: " + e.toString());
        	        }
            	}
            }
            
        // Close down the connection
        disconnect();
        }

    /** This function must be overridden by child classes to process the input
        in an application-specific manner
    */
    protected abstract void processInput(String s);

    /** The default output function, can be overridden if necessary.
    */
    void processOutput(String s)
        { 
        outStream.println(s); 
        }

    /** The output queue waits for output strings to be added and then dispatches
        them to the connection's 'processOutput' function.
        <p>
        Synchronization: The 'put' and 'get' functions are synched. This class uses
            an "Optimistic single-threaded" strategy to wait for items to be added to
            the queue. (See Java Language Reference, O'Reilly, section 8.2.2.)
    */
    class OutputQueue
        extends Thread
        {
        // The queue of output strings
        Queue queue = new Queue();

        OutputQueue(String msg)
            {
            super(msg);
            }

        /** Add a string to the queue. (Called only by 'connection.output')
        */
        synchronized void put(String s)
            {
            // Add the object to the queue
            queue.put(s);

            // Inform waiting processes that something has been added
            notify();
            }

        /* Get a string from the queue. (Called only by 'run')
        */
        synchronized private String get()
            {
            // Wait until something arrives
            while (queue.isEmpty())
                {
                try {
                    wait(OUTPUT_TIMEOUT);
                    }

                catch (InterruptedException e)
                    {
                    }
                }

            // Return the queue item or null if the connection is stopped
            if (state == RUNNING)
                return (String)queue.get();
            else
                return null;
            }

        public void run()
            {
            String s;
            do  {
                // Get the string and dispatch it
                s = get();
                if (s != null)
                    processOutput(s);
                }
            while (s != null);
            }
        }
	}
