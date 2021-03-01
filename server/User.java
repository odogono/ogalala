// $Id: User.java,v 1.28 1999/04/29 15:57:30 jim Exp $
// Representation of a user in the session server
// James Fryer, 28 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.util.*;
import java.io.*;
import com.ogalala.server.database.*;
import com.ogalala.util.*;

/** The User class represents a user session.
    <p>
    This class provides the following varied services:
    <ul>
    <li> Basic user details, culled from the database: User ID, Privilege level,
        probably more to come. ### (However, no app-specific information belongs
        in this generic class.)
    <li> Access to the user's database record, e.g. for applications to check if
        a user is entitled to open a channel.
    <li> Connection functions 'start' and 'stop'. Also 'getServer'.
    <li> Input: 'processInput' is defined and routes input to the correct channel or
        to the server CLI.
    <li> Output: Channel output ('outputChannel') and server output ('outputMessage',
        'outputAck', 'outputError'). The 'output' function should not be used by
        clients.
    <li>Channel management: The list of open channels, the current channel, find a
        channel given its ID, functions to add channel information to an output
        string and get the channel from an input string. Some of these functions
        will eventually find their way into shared client/server code.
    </ul>
    The User class is a bit of a mish-mash (or a work in progress). It may be that
    future versions of the server framework break it up into smaller classes.<p>
*/
public class User
    extends Connection
    {
    /** Persistant object that hold the user details
    */
    private UserRecord userRecord = null;

	/** The current privilege value
	*/
	private int privilege = Privilege.GUEST;

    public User( Server server, String userID, java.net.Socket socket )
        {
        super( server, userID, socket );
        }

    public User(Server server, String userName, InputStream in, OutputStream out)
        {
        super(server, userName, in, out);
        }
        
    public void init()
        {
        channels = new Hashtable();
		try {
			userRecord = getServer().getUserDatabase().getUserRecord( getUserId() );
			}
		catch (Exception e)
			{
			// bad error how did they login if the database is closed OR the user doesn't exist?
			log("User object error (User.java/66)");
			Debug.printStackTrace( e );
			}
		initDatabaseInfo( userRecord );
        }

    /** Read information from the user's database entry and set up the appropriate
        variables
    */
    private void initDatabaseInfo( UserRecord userRecord )
        {
        privilege = userRecord.getPrivilege();
        //### What other database fields are required in the User class???
        }

    /** Get the user's privilege level
    */
    public final int getPrivilege()
        {
        return userRecord.getPrivilege();
        }

    /** Get this user's privilege level as a string
    */
    public final String getPrivilegeString()
        {
        return Privilege.getDescription(userRecord.getPrivilege());
        }

    /** Get a privilege level as a string
    */
    public final String getPrivilegeString(int n)
        {
        return Privilege.getDescription(n);
        }

    /** Get the user's database record.
        <p>
        This is provided to allow applications access to user details from the
        database which are not stored in the User class.
    */
    public final com.ogalala.server.database.UserRecord getUserRecord()
        {
		return userRecord;
        }

    /** Called when the session starts
    */
    public void start()
        {
        super.start();
        outputMessage("Hello");
        }

    /** Called to end the session
    */
    public void stop()
        {
        closeAllChannels();
        outputMessage("Bye");
        super.stop();
        }

    /** Get the session server
    */
    public final SessionServer getServer()
        {
        return (SessionServer)server;
        }

    /** Process input from the user
    */
    public void processInput(String command)
        {
        // Remember the time for command logging
        long startTime = System.currentTimeMillis();

        // Do the command
        SessionServer server = getServer();
        internalProcessInput(command, server, server.getCLI());
        
        // Log the command
        long timeTaken = System.currentTimeMillis() - startTime;
        server.logCommand(command, startTime, timeTaken);
        }
        
    /** Internal do input function
    */
    private void internalProcessInput(String command, SessionServer server, ServerCommandInterpreter cli)
        {
        // If it's a server command, send it off to the cli
        if (cli.isServerCommand(command))
            {
            // Remove the first character of the command
            command = command.substring(1, command.length());

            // If it is still prefixed with the server command char, then we allow it to pass through.
            //  I.e. "@@HELLO" will be passed to the channel as "@HELLO". Otherwise, execute
            //  the command and return.
            if (!cli.isServerCommand(command))
                {
                cli.execute(command, this);
                return;
                }
            }

        // If we get here, the output is intended for a channel, so determine which one
        Channel channel = whichChannel(command);

        // Remove the channel ID from the command line
        command = ChannelUtil.removeChannelID(command);

        // If the channel is null, there is an error
        if (channel == null)
            outputError("Channel not found");

        // Else, send the command to the channel
        else
            channel.input(command);
        }
	
    /** Output to a channel
    */
    public final void outputChannel(Channel channel, String msg)
        {
        // If the output is not to the current channel, qualify it
        if (channel != currChannel)
            msg = ChannelUtil.insertChannelID(channel.getChannelID(), msg);
        output(msg);
        }

    /** Server output functions. These are for convenience -- used by server layer only.
    */
    final void outputMessage(String msg)
        {
        output(getServer().formatMessage(msg));
        }

    final void outputAck()
        {
        output(getServer().formatAck());
        }

    final void outputError(String msg)
        {
        output(getServer().formatError(msg));
        }

    /** The list of open channels
    */
    private Dictionary channels;

    /** The current channel
    */
    private Channel currChannel = null;

    /** Add a channel
    */
    public final void addChannel(Channel channel)
        {
        // ASSERT(!channels.contains(channel));

        // Add the channel to the list
        channels.put(channel.getChannelID(), channel);

        // If there is no current channel, make the new channel current
        if (currChannel == null)
            currChannel = channel;
        }

    /** Remove a channel
    */
    public final void removeChannel(Channel channel)
        {
        // Remember if we need to reset the current channel
        boolean mustChangeCurrChannel = (channel == currChannel);

        // Remove the channel from the list
        channels.remove(channel.getChannelID());

        // Change the current channel if necessary
        //### I am not sure if this is the best strategy as it breaks the rule that
        //###   the client asks for (and knows of) the current channel. However it is
        //###   hard to work out what to do instead...
        if (mustChangeCurrChannel)
            {
            // Get the channels list
            Enumeration enum = channels.elements();

            // If the list is empty, set current channel to null
            if (!enum.hasMoreElements())
                currChannel = null;

            // Else set it to the first channel in the list
            else
                setCurrentChannel((Channel)enum.nextElement());
            }
        }

    /** Get an open channel
    */
    public final Channel getChannel(String channelID)
        {
        return (Channel)channels.get(channelID);
        }

    /** Enumerate channels
    */
    public final Enumeration getChannels()
        {
        return channels.elements();
        }

    /** Get the current channel
    */
    public final Channel getCurrentChannel()
        {
        return currChannel;
        }

    /** Set the current channel
    */
    public final void setCurrentChannel(Channel channel)
        {
        // ASSERT(channels.contains(channel));

        currChannel = channel;
        }

    /** Which channel does the string 's' refer to?
        @return The channel specified by 's'; the current channel if no channel is specified; or null.
    */
    public final Channel whichChannel(String s)
        {
        // If there is no channel ID specified, return the current channel
        if (!ChannelUtil.containsChannelID(s))
            return currChannel;

        // Else, get the channel ID from the string and return the corresponding channel
        //  (Possibly null if the channel is not found)
        else {
            String channelID = ChannelUtil.getChannelID(s);
            return getChannel(channelID);
            }
        }

    /** Close all channels
    */
    private void closeAllChannels()
        {
        Enumeration enum = channels.elements();
        while (enum.hasMoreElements())
            {
            Channel channel = (Channel)enum.nextElement();
            channel.close();
            }
        currChannel = null;
        }
    }
