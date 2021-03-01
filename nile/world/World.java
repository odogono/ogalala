// $Id: World.java,v 1.4 1998/05/02 14:48:31 jim Exp $
// Game World for 1932.com Death on the Nile
// James Fryer, 25 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.world;

import java.io.*;
import java.util.*;
import com.ogalala.server.*;
import com.ogalala.nile.server.*;
import com.ogalala.nile.database.*;

/** The World class implements the 1932.com game world. It is a facade class
    containing the various components that make up the world.
*/
public class World
    // implements Runnable
    {
    // Database
    Database database;

    // The game can be in either 'playing' or 'paused' state. This flag is
    //  true when the game is playing.
    private boolean isPlaying;

    // ADMININSTRATION

    /** Constructor. The database must exist. The game is in paused
        state until 'start' is called.
    */
    public World(String worldName)
        {
        System.out.println("World: Opening database file \"" + worldName + "\"");

        // The game is paused until 'start' is called
        isPlaying = false;

        // Check the database exists
        if (!Database.exists(worldName))
            fatal("World: Database file \" + worldName + \" not found");

        // Create the database object
        database = new Database(worldName);
        }

    /** Create a new database
    */
    public static void initGame(String worldName)
        {
        System.out.println("World: Creating database file \"" + worldName + "\"");

        // Don't overwrite existing databases
        if (Database.exists(worldName))
            fatal("World: Database file \"" + worldName + "\" exists already. I will not overwrite it.");

        // Create the database
        Database database = null;
        try {
            // Create the database on disk, then open it and write it to ensure a valid database
            //  is comitted to disk
            Database.create(worldName);
            database = new Database(worldName);
            database.write();
            }

        // All errors are fatal
        catch (IOException e)
            {
            fatal("World: Error creating database file \"" + worldName + "\": " + e);
            }
        catch (Exception e)
            {
            fatal("World: Error creating database file \"" + worldName + "\": " + e);
            }

        // Add the nouns required to make a valid game world database
        }

    /** Start playing the game
    */
    public void start()
        {
        isPlaying = true;

        // Read the database
        try {
            database.read();
            }

        // Class not found probably means database is incompatible with this version
        catch (ClassNotFoundException e)
            {
            fatal("World: Class not found in database file \"" + database.getName() + "\" :" + e);
            }

        // I/O error
        catch (IOException e)
            {
            fatal("World: Error reading database file \"" + database.getName() + "\": " + e);
            }

        // Other error
        catch (Exception e)
            {
            fatal("World: Error reading database file \"" + database.getName() + "\": " + e);
            }

        // Start all the game objects
        database.start();
        }

    /** Stop playing the game
    */
    public void stop()
        {
        database.stop();
        try {
            database.write();
            }
        catch (IOException e)
            {
            warning("World: Error writing database file \"" + database.getName() + "\": " + e);
            }

        isPlaying = false;
        }

    /** Is the game playing?
    */
    public boolean isPlaying()
        {
        return isPlaying;
        }

    /** Create a Controller for the supplied User and character name.
        @return: the new controller
    */
    public Controller makeController(User user, String characterName)
        {
        return null; //###
        }

    /** Make a Command Interpreter for the user
    */
    CommandInterpreter makeCommandInterpreter(User user)
        { return new CommandInterpreter(this, user); }

    /** Make a Noun
    */
    public Noun makeNoun(User user, String className, String id /* ARGS ###*/)
        {
        // Attempt to construct the noun
        Noun result = null;
        try {
            result = database.makeNoun(className, id, user.getName());
            }

        // Report errors back to the user
        catch (ClassNotFoundException e)
            {
            user.outError("CLI", "Can't create game object: class <" + className + "> not found");
            return null;
            }
        catch (IllegalAccessException e)
            {
            user.outError("CLI", "Can't create game object: class <" + className + "> not found");
            return null;
            }
        catch (InstantiationException e)
            {
            user.outError("CLI", "Can't create game object: class <" + className + "> is abstract");
            return null;
            }
        catch (Exception e)
            {
            user.outError("CLI", "Can't create game object: " + e);
            return null;
            }

        // Process the parameters supplied by the user
        //###

        // Add the new noun to the user's inventory
        //### This is a temporary measure -- where should new nouns go?

        return result;
        }

    /** Shut down the application
    */
    public void shutdown(User user, int minutesToShutdown)
        {
        //### It would probably be wise to keep a pointer to this thread
        //###   which would allow shutdown to be cancelled... I can just imagine the
        //###   horror and impending doom after a @SHUTDOWN 10000 is issued...
        new ShutdownThread(this, user, minutesToShutdown);
        }

    /** Fatal error function
    */
    static public void fatal(String msg)
        {
        System.err.println("Fatal error: " + msg);
        System.exit(1);
        }

    /** Non-fatal error function
    */
    static public void warning(String msg)
        {
        System.err.println("Warning: " + msg);
        }
    }

/** Manage the shutdown process.
*/
class ShutdownThread
    extends Thread
    {
    // The world to shut down
    World world;

    // The user who requested the shutdown
    private User user;

    // Time to shutdown, in minutes
    private int minutesToShutdown;

    // Time to the next shutdown warning, in minutes
	//### Not used ???
    private int sleepTime;

    ShutdownThread(World world, User user, int minutesToShutdown)
        {
        //ASSERT(user.getStatus() == user.STATUS_PROGRAMMER);
        this.user = user;
        this.world = world;
        this.minutesToShutdown = minutesToShutdown;
        sleepTime = nextSleepTime();
        System.out.println("World: Shutdown(" + minutesToShutdown + ") requested by " + user.getName());
        this.start();
        }

    public void run()
        {
        // Loop until out of time. Broadcast a message every 30 minutes, then every 5 minutes,
        //  then every minute until the system goes down.
        while (minutesToShutdown > 0)
            {
            try {
                // If there is only one minute left, warn the users and set the shutdown time to 0
                if (minutesToShutdown == 1)
                    {
                    broadcast("The system is going down in ONE MINUTE!");
                    minutesToShutdown = 0;
                    sleep(60000);
                    }

                // Else, warn and calculate the next shutdown time
                else {
                    broadcast("The system is going down in " + minutesToShutdown + " minutes");

                    // Get the timeout appropriate for the time left until shutdown
                    int sleepMinutes = nextSleepTime();
                    sleepMinutes = Math.min(sleepMinutes, minutesToShutdown - sleepMinutes);

                    // Reduce the shutdown time and go to sleep
                    minutesToShutdown -= sleepMinutes;
                    sleep(sleepMinutes * 60 * 1000);
                    }
                }

            catch (Exception e)
                {
                //### IS ignoring errors the best thing to do???
                }
            }

        // Shut down the system
        broadcast("The system is going down NOW");
        world.stop();
        //### The above isn't enough -- really need a way to tell the SERVER to stop.
        //###   The port MAY be left open in this implementation.
        System.out.println("World: Shutdown by " + user.getName());
        System.exit(0);
        }

    /** Get the time to wait until broadcasting another shutdown warning message
    */
    private int nextSleepTime()
        {
        if (minutesToShutdown > 30)
            return 30;
        else if (minutesToShutdown > 5)
            return 5;
        else if (minutesToShutdown > 1)
            return 1;
        else
            return 0;
        }

    private void broadcast(String msg)
        {
        // Get the user's ID string
        String nameField = new String(user.getName() + " (System Manager)");

        // Construct the packet
        OutputPacket packet = new OutputPacket();
        packet.textPacket("SHOUT");
        packet.addField(nameField);
        packet.addField(msg);

        // Send the message to all logged-in users
        ConnectionList connections = user.getConnectionList();
        synchronized (connections)
            {
            Enumeration users = connections.elements();
            while (users.hasMoreElements())
                {
                User user = (User)users.nextElement();
                user.output(packet);
                }
            }
        }
    }
