// $Id: NileMain.java,v 1.3 1998/05/02 14:48:31 jim Exp $
// Main class for 1932.com: Death onthe Nile
// James Fryer, 25 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile;

import com.ogalala.server.*;
import com.ogalala.nile.server.*;
import com.ogalala.nile.world.*;

class NileMain
    {
	// Version
	public static final String VERSION = "0.1";

	// Default port
	public static final int PORT = 1932;

    // The name of the world used in this game
	private static String worldName = null;

	// Does the world need to be created?
	private static boolean createWorld = false;

    public static void main(String[] args)
    	{
        System.out.println("1932: Death on the Nile server version " + VERSION);
        System.out.println("Copyright (C) 1998 Ogalala Ltd <info@ogalala.com>");
        System.out.println("");

    	// Process the args
        processArgs(args);

    	// Create the database, if required
    	if (createWorld)
    	    World.initGame(worldName);

    	// Create the server
		Server server = new NileServer(PORT, worldName);
		server.start();
    	}

    private static void processArgs(String[] args)
        {
    	if (args.length < 1)
    	    help();
    	int currArg = 0;
    	if (args[currArg].equalsIgnoreCase("-init"))
    	    {
    	    createWorld = true;
    	    currArg++;
    	    }
    	worldName = args[currArg];
    	if (worldName == null)
    	    help();
        }

    private static void help()
        {
	    System.err.println("Usage: NILE [-init] database-name");
	    System.exit(1);
        }
    }
