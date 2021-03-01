// $Id: SessionTest.java,v 1.8 1998/08/07 14:47:39 jim Exp $
// Test driver for Session Layer
// James Fryer, 28 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.test;

import com.ogalala.server.*;
import com.ogalala.util.StringUtil;

class SessionTest
    {
	public static final int DEFAULT_PORT = 1932;

  	static final String help_string = 
	  	"-start:<filename>\tSpecify a file containing applications to start when the server is initialised\n" +
	  	"-port:<int>\t\tSpecify what port the server should listen at\n" +
  		"-database:<filename>\tSpecify what user database to open\n" +
  		"-?\t\t\tDisplay this page";
  
    private int port = 0;
    private String databaseName = null;
    private String initFileName = null;
    
	private SessionTest( String args[] )
		{
		parseArgs(args);

    	// Create the server
		SessionServer server = new SessionServer(port, databaseName);
		
		// Run the start file
		if (initFileName != null)
		    server.execInitFile(initFileName);

		// pass thread to server
		server.start();
		}

    private void parseArgs(String args[])
        {
        final String switch_port = "-port:";
        final String switch_help = "-?";
        final String switch_database = "-database:";
        final String switch_init = "-start:";
  
        for (int i = 0; i != args.length; i++)
            {
            if (args[i].equals(switch_help))
                usage();

            // look at the switches in the args list and determines whether the user
            // specified the port he/she wants to connect to. If not use the defualt port
            else if (args[i].startsWith(switch_port))
                {
                String portStr = args[i].substring(switch_port.length(), args[i].length());
                if (StringUtil.isAllNumbers(portStr))
                    {
                    // cool we have extracted the port no!
                    port = Integer.parseInt(portStr);
                    }
                else {
                    System.out.println("The port number should not contain characters.\nUsing default setting");
                    port = DEFAULT_PORT;
                    }
                }

            // Has the user specified the database name?
            else if (args[i].startsWith( switch_database ))
                databaseName = args[i].substring(switch_database.length(), args[i].length());
            
            // Look for init file
            else if (args[i].startsWith( switch_init ))
                initFileName = args[i].substring(switch_init.length(), args[i].length());
            }
        
        // Check we have required args
        if (port == 0)
            usage();
        }
    
    /** Print help and terminate
    */
    private void usage()
        {
		System.out.println(help_string);
		System.exit(0);
        }
  
  
  	//--------------------------------------------------------------------------------
  
    public static void main(String[] args)
    	{
		new SessionTest(args);
	    }
	}