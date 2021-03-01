// $Id: CommandInterpreter.java,v 1.4 1998/05/02 14:48:31 jim Exp $
// Command Interpreter for "1932.com Death on the Nile"
// James Fryer, 1 April 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.world;

import java.util.*;
import com.ogalala.util.*;
import com.ogalala.server.*;
import com.ogalala.nile.database.*;

/** The CommandInterpreter is responsible for the execution of commands
    from the user. Commands starting with "@" are user-level commands.
    Other commands are sent to the parser.

    Each user has their own CommandInterpreter. This allows the future
    storage of some state in the cli or parser.
*/
public class CommandInterpreter
    {
    // Back-pointer to world
    protected World world;

    // Back-pointer to user
    protected User user;

    // Convenience pointer to database
    protected Database database;

    // Parser
    //###

    /** Constructor
    */
    public CommandInterpreter(World world, User user)
        {
        this.world = world;
        this.user = user;
        database = world.database;
        //### PArser
        }

    /** Execute a command
    */
    public void execute(String commandLine)
        {
        // Remove leading/trailing spaces from the command line
        commandLine.trim();

        // If the command line is empty, return
        if (commandLine.length() == 0)
            return;

        // If the command line starts with '@' then it's a user-level command
        else if (commandLine.charAt(0) == '@')
            execUserCommand(commandLine);

        // Else, it's a game-level command
        else
            execGameCommand(commandLine);
        }

    /** Execute a command which acts on the user (these commands start with '@')
        All commands are handled in functions named 'execXxx' where 'Xxx' is the
            name of the command. Parameters are passed as an enumeration containing
            strings.
    */
    private void execUserCommand(String commandLine)
        {
        // Parse the command, removing the initial '@'.
        CommandLineTokenizer tokenizer = new CommandLineTokenizer(commandLine.substring(1));
        String command = tokenizer.nextToken();

        // Quit
        if (command.equalsIgnoreCase("QUIT"))
            execQuit();

        // New
        else if (command.equalsIgnoreCase("NEW"))
            execNew(tokenizer);

        // Delete
        else if (command.equalsIgnoreCase("DELETE") || command.equalsIgnoreCase("DEL"))
            execDelete(tokenizer);

        // Set
        else if (command.equalsIgnoreCase("SET"))
            execSet(tokenizer);

        // Shout
        else if (command.equalsIgnoreCase("SHOUT"))
            execShout(tokenizer);

        // Play
        else if (command.equalsIgnoreCase("PLAY"))
            execPlay(tokenizer);

        // Stop
        else if (command.equalsIgnoreCase("STOP"))
            execStop(tokenizer);

        // Shutdown
        else if (command.equalsIgnoreCase("SHUTDOWN"))
            execShutdown(tokenizer);

        //### The following are temporary commands, included for debugging purposes

        // Echo ###
        else if (command.equalsIgnoreCase("ECHO"))
            execEcho(tokenizer);

        // List ###
        else if (command.equalsIgnoreCase("LIST"))
            execList(tokenizer);

        // Save ###
        else if (command.equalsIgnoreCase("SAVE"))
            execSave(tokenizer);

        // Unrecognised command
        else
            user.outError("CLI", "Unknown command <@" + command + ">");
        }

    /** Execute a game command (commands not starting with '@')
    */
    private void execGameCommand(String commandLine)
        {
        //###
        user.outText("MISC", commandLine);
        }

    // Handlers for @ commands

    /** @QUIT -- leave the system
    */
    void execQuit()
        {
        user.stop();
        }

    /** @NEW -- Create a new game object
        @NEW <Class> <ID> <params...>
        If the ID is "?" (advised) then a unique ID will be generated
        Parameters are in the format NAME=VALUE
    */
    void execNew(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_MODERATOR)
            {
            user.outError("CLI", "<@NEW> is a moderator command");
            return;
            }

        // Get the class name and ID
        String className = (String)params.nextElement();
        String id = (String)params.nextElement();

        // Collect the parameters
        //###

        // Check required parameters are present
        if (className == null || id == null)
            {
            user.outError("CLI", "Usage: <@NEW class-name id/? params>");
            return;
            }

        // If the params are OK, create the noun
        world.makeNoun(user, className, id); //###Params
        }

    /** @DELETE -- Remove a game object
    */
    void execDelete(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_MODERATOR)
            {
            user.outError("CLI", "<@DELETE> is a moderator command");
            return;
            }

        user.outError("CLI", "Command <@DELETE> has not been implemented");
        }

    /** @SET -- Set object properties
    */
    void execSet(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_MODERATOR)
            {
            user.outError("CLI", "<@SET> is a moderator command");
            return;
            }

        user.outError("CLI", "Command <@SET> has not been implemented");
        }

    /** @SHOUT -- Send a message to all users
    */
    void execShout(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_MODERATOR)
            {
            user.outError("CLI", "<@SHOUT> is a moderator command");
            return;
            }

        // Get the user's ID string
        StringBuffer nameField = new StringBuffer(user.getName());
        if (user.getStatus() == user.STATUS_MODERATOR)
            nameField.append(" (Moderator)");
        else
            nameField.append(" (System Manager)");

        // Construct the packet
        OutputPacket packet = new OutputPacket();
        packet.textPacket("SHOUT");
        packet.addField(nameField.toString());
        packet.addField((String)params.nextElement());

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

    /** @PLAY -- Start playing the game
    */
    void execPlay(Enumeration params)
        {
        user.outError("CLI", "Command <@PLAY> has not been implemented");
        }

    /** @STOP -- Stop playing the game
    */
    void execStop(Enumeration params)
        {
        user.outError("CLI", "Command <@STOP> has not been implemented");
        }

    /** @SHUTDOWN -- Stop the server
    */
    void execShutdown(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_PROGRAMMER)
            {
            user.outError("CLI", "<@SHUTDOWN> is a system administrator command");
            return;
            }

        // Get the time to shutdown as a string
        String timeAsString = (String)params.nextElement();

        // Check required parameters are present
        if (timeAsString == null)
            {
            user.outError("CLI", "Usage: <@SHUTDOWN minutes/NOW>");
            return;
            }

        // Get time to shutdown
        //### This is a Double because numbers are supplied in the format "5.0"... I would like to fix this.
        Double minutesToShutdown = null;
        try {
            minutesToShutdown = new Double(timeAsString);
            }
        catch (NumberFormatException e)
            {
            // If it's SHUTDOWN NOW, treat as zero, otherwise it's an error
            if (!timeAsString.equalsIgnoreCase("NOW"))
                {
                user.outError("CLI", "Usage: <@SHUTDOWN minutes/NOW>");
                return;
                }
            }

        // Ask the world to die
        world.shutdown(user, minutesToShutdown.intValue());
        }

    /** @LIST -- List game objects
        ### (This is a temporary command)
    */
    void execList(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_PROGRAMMER)
            {
            user.outError("CLI", "<@LIST> is a system administrator command");
            return;
            }

        Enumeration enum = database.elements();
        while (enum.hasMoreElements())
            user.output(((Noun)enum.nextElement()).toString());
        }

    /** @ECHO -- Write parameters back to user
        ### (This is a temporary command)
    */
    void execEcho(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_PROGRAMMER)
            {
            user.outError("CLI", "<@ECHO> is a system administrator command");
            return;
            }

        OutputPacket packet = new OutputPacket();
        packet.textPacket("ECHO");
        while (params.hasMoreElements())
            packet.addField((String)params.nextElement());
        user.output(packet);
        }

    /** @SAVE -- Save the database
        ### (This is a temporary command)
    */
    void execSave(Enumeration params)
        {
        // Ensure user has required status
        if (user.getStatus() < user.STATUS_PROGRAMMER)
            {
            user.outError("CLI", "<@SAVE> is a system administrator command");
            return;
            }

        try {
            database.write();
            }
        catch (Exception e)
            {
            user.outError("CLI", "Error writing database: " + e);
            }
        }

    }
