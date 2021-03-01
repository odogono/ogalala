// $Id: User.java,v 1.4 1998/05/02 14:48:31 jim Exp $
// User class for Death on the Nile
// James Fryer, 25 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.world;

import java.net.*;

import com.ogalala.server.*;
import com.ogalala.nile.server.*;

/** The User class is a connection with additional user information
*/
public class User
    extends Connection
	{
	// Back-pointer to world
    private World world;

    // The command interpreter
    //### (I don't usually go in for abbreviations but I'm going to try this one)
    private CommandInterpreter cli;

    // User status constants (not to be confused with social status &c.)
    //### Perhaps a better name for the user status would be appropriate?
    static public final int STATUS_GUEST = 0;
    static public final int STATUS_PLAYER = 1;
    static public final int STATUS_MODERATOR = 2;
    static public final int STATUS_PROGRAMMER = 3;
    static public final int STATUS_DEFAULT = STATUS_PROGRAMMER;

    // Current status
    protected int status = STATUS_DEFAULT;

    // The active character controller
    protected Controller controller;

    public User(Server server, Socket socket, String userName)
    	{
    	super(server, socket, userName);
    	}

    public void init()
        {
        super.init();
        world = ((NileServer)server).getWorld();
        cli = world.makeCommandInterpreter(this);
        }

    protected void processInput(String s)
        {
        cli.execute(s);
        }

    public void start()
        {
        super.start();
        }

    public void stop()
        {
        super.stop();
        }

    /** Get the current active controller
    */
    public Controller getController()
        { return controller; }

    /** Set the active controller
    */
    public void setController(Controller controller)
        {
        this.controller = controller;
        if (controller != null)
            {
            //### controller.setConnection(this);
            }
        }

    /** Get the user name.
        ### Really this function should be renamed in 'Connection' but I
            haven't done this to avoid breaking existing code.
    */
    public String getName()
        { return getUserName(); }

    /** Get the user status level
    */
    public int getStatus()
        { return status; }

    /** Set the user status level
    */
    public void setStatus(int newStatus)
        {
        // ASSERT(newStatus >= STATUS_GUEST && newStatus <= STATUS_PROGRAMMER);
        status = newStatus;
        }

//### Note this will probably become some sort of "NounWatcher" interface

    /** Output a packet
    */
    public void output(OutputPacket packet)
        {
        output(packet.toString());
        }

    /** Output a text packet
    */
    public void outText(String tag, String s)
        {
        OutputPacket packet = new OutputPacket();
        packet.textPacket(tag, s);
        output(packet.toString());
        }

    /** Output an error packet
    */
    public void outError(String tag, String s)
        {
        OutputPacket packet = new OutputPacket();
        packet.errorPacket(tag, s);
        output(packet.toString());
        }

	}
