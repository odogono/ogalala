// $Id: MuaApp.java,v 1.22 1999/09/01 16:07:01 jim Exp $
// The multi-user adventure game application
// James Fryer, 6 Aug 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.server.apps;

import java.util.*;
import java.io.*;
import com.ogalala.server.*;
import com.ogalala.mua.*;

public class MuaApp
    implements Application
    {
    private World world;
    private String paths = null;
    private boolean adminLogEnabled = false;
    private boolean eventLogEnabled = false;

    /** Create a new application. Implementations should avoid overwriting
        existing apps with the same ID.
    */
    public void create(String appID, Enumeration args)
        throws ApplicationOpenException
        {
        try {
        	parseArgs(args);
            world = WorldFactory.createWorld(appID, paths);
            world.setAdminLog( adminLogEnabled );
            world.setEventLog( eventLogEnabled );
            world.runStartupScripts();
            WorldFactory.importState(world, appID);
            world.start();
            }
        catch (AtomException e)
        	{
            throw new ApplicationOpenException(e.toString());
        	}
        catch (WorldException e)
        	{
            throw new ApplicationOpenException(e.toString());
        	}
        catch (IOException e)
        	{
            throw new ApplicationOpenException(e.toString());
        	}
        }

    /** Open an existing application
    */
    public void open(String appID, Enumeration args)
        throws ApplicationOpenException
        {
        try {
            String paths = null;
            if (args.hasMoreElements())
                paths = args.nextElement().toString();
            world = WorldFactory.loadWorld(appID, paths);
            world.start();
            }
        catch (Exception e)
        	{
            throw new ApplicationOpenException(e.toString());
        	}
        }

    /** Close this application. Preserve state, if appropriate.
    */
    public void close()
        {
        try {
            world.stop();

            //### Some potential here to preserve the existing state file!!
            WorldFactory.saveWorld(world);
            WorldFactory.exportState(world, world.getFileName());
            world = null;
            }
        catch (Exception e)
            {
            //### This should be AppOpenException, shirley???
            //### throw new ChannelOpenException(e.getMessage());
            }
        }

    /** Remove an application's persistent state.
        (USE WITH CARE!)
    */
    public void delete(String appID)
        {
        //### Not implemented
        }

    /** Does an application called 'appID' exist?
    */
    public boolean exists(String appID)
        {
        return WorldFactory.exists(appID);
        }

    /** Get the ID of this application
    */
    public String getID()
        {
        // Strip the path from the world filename
        File file = new File(world.getFileName());
        return file.getName();
        }

    /** Create a channel for this application
    */
    public Channel newChannel()
        {
        return new MuaChannel(world);
        }

    /** Get some information about this application
    */
    public String getInfo()
        {
        //### Could have more info
        return "MUA world: " + world.getFileName();
        }

    /** Parse the command-line arguments. There can be any number
        of "switch" arguments, which begin with a hyphen. The following
        switches are supported:
        <ul>
        <li><tt>-adminLog</tt> Log admin watch messages to console
        <li><tt>-eventLog</tt> Log events to event log file
        </ul>
        The first argument that doesn't begin with a hyphen is taken to be
        the paths list. Any other non-switch arguments are ignored.
        <p>
        (This is a bit ugly, but it maintains consistency with
        the old version.)
    */
    private void parseArgs ( Enumeration args )
        {
    	while ( args.hasMoreElements() )
    	    {
    		String arg = args.nextElement().toString();

    		if ( arg.startsWith("-") )
    		    {
    			// process switches
    			if ( arg.equalsIgnoreCase("-adminLog") )
    				adminLogEnabled = true;
    			else if ( arg.equalsIgnoreCase("-eventLog") )
    				eventLogEnabled = true;
    		    }
    		else if ( paths == null )
    			paths = arg;
    	    }
        }
    }

/** MUA channel class
*/
class MuaChannel
    extends Channel
    {
    /** Back-pointer to World
    */
    private World world;

    /** The Watcher
    */
    private UserWatcher watcher;

    /** The parser
    */
    private Parser parser;

    /** The ID of the Thing being controlled
    */
    private String atomID;

    /** The Thing being controlled
    */
    private Atom atom;

    /** Is this a channel to a lead watcher?
    */
    private boolean isLead = true;

    /** Ctor
    */
    public MuaChannel(World world)
        {
        this.world = world;
        parser = world.newParser();
        }

    /** Open the channel.
        <p>
        This function must add the channel to the user's list and to the application's
        list of open channels.
    */
    public void open(String channelID, User user, Enumeration args)
        throws ChannelOpenException
        {
        // Get the character/Thing
        atomID = ChannelUtil.getUserID(channelID);
        if (atomID == null)
            throw new ChannelOpenException("No character name supplied");
        atom = world.getAtom(atomID);
        if (atom == null)
            throw new ChannelOpenException("Character not found: " + atomID);
        if (!(atom instanceof Thing))
            throw new ChannelOpenException("Not a watchable Thing: " + atomID);

        // Check if the user is entitled to watch/play this Thing
        //###

        // Process arguments. '-watch' will make this a watcher channel only. ALso
        //  if the atom is not a Mobile then it must be a watcher only (for the time being)
        if (args.hasMoreElements())
            {
            String s = (String)args.nextElement();
            if ("-watch".equalsIgnoreCase(s))
                isLead = false;
            }
        if (!(atom instanceof Mobile))
            isLead = false;

        super.open(channelID, user, args);
        }

    /** Start the channel. 
    */
    public void start()
        {
        if (isLead)
            watcher.startLead();
        world.parseCommand("LOOK", watcher.getWatchedAtom());
        }
        
    /** Stop the channel 
    */
    public void stop()
        {
        if (isLead)
            watcher.stopLead();
        }
    
    /** Add this channel to the application
    */
    protected void addToApplication()
        {
        // Get the thing to be watched...
        Thing thing = (Thing)atom;

        // Create the Watcher and make the connection
        createWatcher();
        thing.addWatcher(getWatcher());
        }

    /** Remove this channel from the application
    */
    protected void removeFromApplication()
        {
        atom.removeWatcher(getWatcher());
        }

    /** Send input to the application
    */
    public void input(String s)
        {
        Atom actor = watcher.getWatchedAtom();
        world.parseCommand(s, actor, parser);
        }

    /** Get the atom
    */
    public Atom getAtom()
        {
        return atom;
        }

    /** Create the watcher
    */
    public void createWatcher()
        {
        watcher = new UserWatcher(this, (Thing)atom, isLead);
        }

    /** Get the watcher
    */
    public Watcher getWatcher()
        {
        return watcher;
        }

    /** Watcher class
    */
    class UserWatcher
        extends Watcher
        {
        private Channel channel;

        UserWatcher(Channel channel, Thing thing, boolean isLead)
            {
            super(thing, isLead);
            this.channel = channel;
            }

        protected void doOutput(String msg, Event event)
            {
            channel.output(msg);
            }

        /** Called when the watcher is added to a Thing
        */
        public void onAdd()
            {
            }

        /** Called when the watcher is removed from a Thing
        */
        public void onRemove()
            {
            }

        /** Called when lead watcher is started
        */
        public void startLead()
            {
            // Get the character
            Atom character = watcher.getWatchedAtom();

            // Sort out the home and start locations, if necessary
            Atom start = character.getAtom("start_container");
            Atom home = character.getAtom("home_container");

            // If the home container is null, then this isn't a character, so let's leave it to the
            //  player to !GO somewhere.
            if (home == null)
                return;

            // If the start container is null, it needs to be set to the same as the home container
            if (start == null)
                start = home;

            // See if we can enter the 'to' container
        	if (!world.callEvent(character, "on_enter", start))
            	return;

            // Now we can move the character to the start container and notify other players
            // (We cannot use the standard enter msg because we are not in an event)
            world.moveAtom(character, start);
            start.output(character.getString("name") + " has arrived.", character);
            }

        /** Called when lead watcher is removed
        */
        public void stopLead()
            {
            // Get the character and the current location
            Atom character = watcher.getWatchedAtom();
            Atom oldContainer = character.getEnclosingContainer();

            // Get the start location -- defaults to Limbo
            Atom start = character.getAtom("home_container");
            if (start == null)
                start = world.getLimbo();

        	// Notify the container that we are leaving, and give it a chance
        	// to handle stuff. Note that this is different from the way it would
        	// normally be handled in a go command. If the event returned unsatisfactorily,
        	// it would cancel the whole manouvre. However in this situtation, the
        	// character is /always/ going to leave. So in effect, we just ignore any
        	// possible event winging
        	world.callEvent(character, "on_exit", oldContainer);

            // Take the character there
            world.moveAtom(character, start);
            oldContainer.output(character.getString("name") + " has left.", character);
            }
        }
    }

