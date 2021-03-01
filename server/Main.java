// $Id: Main.java,v 1.15 1999/08/24 13:46:17 jim Exp $
// Main program for Session Server
// James Fryer, 28 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import com.ogalala.util.*;

class Main
    {
  	static final String helpMessage =
	  	"-start:<filename>\tSpecify a file containing applications to start when the server is initialised\n" +
	  	"-port:<int>\t\tSpecify what port the server should listen at\n" +
  		"-database:<filename>\tSpecify what user database to open\n" +
  		"-?\t\t\tDisplay this page";

    private int port = 0;
    private String databaseName = null;
    private String initScriptName = null;
    private String commandLogName = null;

	private Main( String args[] )
		{
		// Print banner message
        System.out.println("HotGen Server Version " + SessionServer.VERSION);
        System.out.println("Copyright (C) 1999 HotGen Studios Ltd. <www.hotgen.com>");
        System.out.println("Running java " + System.getProperty("java.version") + 
                " on " + System.getProperty("java.vendor"));

		parseArgs(args);

    	// Create the server
		SessionServer server = new SessionServer(port, databaseName);
		server.setCommandLog(commandLogName);

		// Run the start file
		if (initScriptName != null)
		    server.execScript(initScriptName);

		// pass thread to server
		server.start();
		}


    private void parseArgs(String args[])
        {
        final String switch_port = "-port:";
        final String switch_help = "-?";
        final String switch_database = "-database:";
        final String switch_init = "-start:";
        final String switch_log = "-log:";

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
                    System.out.println("Invalid port number.");
                    System.exit(1);
                    }
                }

            // Has the user specified the database name?
            else if (args[i].startsWith( switch_database ))
                databaseName = args[i].substring(switch_database.length(), args[i].length());

            // Look for init file
            else if (args[i].startsWith( switch_init ))
                initScriptName = args[i].substring(switch_init.length(), args[i].length());

            // Look for performance log file
            else if (args[i].startsWith( switch_log ))
                commandLogName = args[i].substring(switch_log.length(), args[i].length());
            }

        // Check we have required args
        if (port == 0)
            usage();
        }

    /** Print help and terminate
    */
    private void usage()
        {
		System.out.println(helpMessage);
		System.exit(0);
        }

  	//--------------------------------------------------------------------------------

    public static void main(String[] args)
    	{
		new Main(args);
	    }
	}
