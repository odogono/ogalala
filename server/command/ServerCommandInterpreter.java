// $Id: ServerCommandInterpreter.java,v 1.27 1999/08/24 13:46:17 jim Exp $
// Process server commands
// James Fryer, 22 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import com.ogalala.util.*;
import java.util.*;

/** The command interpreter for the server. 
    <p>
    Note that the "@" is removed from the command line before it is 
    sent to this class.
    <p>
    All commands result in either an @ERR or @ACK response.
*/
public class ServerCommandInterpreter
    extends CommandInterpreter
    {
    /** The character which prefixes all server commands
    */
    public static final char COMMAND_PREFIX = '@';

    public ServerCommandInterpreter()
        {
        // Default command
        new DefaultCommand(this, Privilege.GUEST, Command.LOG_OFF);
        
        // Misc commands
        new HelpCommand(this, Privilege.GUEST, Command.LOG_OFF);
        new BroadcastCommand(this, Privilege.MODERATOR, Command.LOG_OFF);
        new WhisperCommand(this, Privilege.MODERATOR, Command.LOG_OFF);
        new EncryptCommand(this, Privilege.USER, Command.LOG_OFF);
		new AddUserCommand(this, Privilege.MODERATOR, Command.LOG_OFF);

        // Session commands
        new QuitCommand(this, Privilege.GUEST, Command.LOG_OFF);
        new PasswordCommand(this, Privilege.USER, Command.LOG_OFF);
        
        // User commands
        new WhoCommand(this, Privilege.MODERATOR, Command.LOG_OFF);
        new ExamineCommand(this, Privilege.MODERATOR, Command.LOG_OFF);
        new SetCommand(this, Privilege.MODERATOR, Command.LOG_OFF);
        new FullSetCommand(this, Privilege.SYSOP, Command.LOG_OFF);
        
        // Channel commands
        new ChannelsCommand(this, Privilege.USER, Command.LOG_OFF);
        new OpenCommand(this, Privilege.GUEST, Command.LOG_OFF);
        new CloseCommand(this, Privilege.GUEST, Command.LOG_OFF);
        new SwitchCommand(this, Privilege.USER, Command.LOG_OFF);
        
        // Application commands
        new AppsCommand(this, Privilege.USER, Command.LOG_OFF);
        new StartCommand(this, Privilege.SYSOP, Command.LOG_OFF);
        new StopCommand(this, Privilege.SYSOP, Command.LOG_OFF);
        new DeleteAppCommand(this, Privilege.SYSOP, Command.LOG_OFF);
        
        // Server control commands
        new ShutdownCommand(this, Privilege.SYSOP, Command.LOG_OFF);
        }

    /** Is 's' a server command?
        @return true if 's' is a server command
    */
    public static boolean isServerCommand(String s)
        {
        // A server command is one that starts with the command prefix
        return s.length() > 0 && s.charAt(0) == COMMAND_PREFIX;
        }
    }

/** Default command, called when command line not recognised.
*/
class DefaultCommand
    extends ServerCommand
    {
    public DefaultCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "DEFAULT\tOnly called on error", privilegeLevel, logLevel);
        cli.addDefaultCommand(this);
        }

    public void execute(Enumeration args, User user)
        {
        user.outputError("Unrecognised command");
        }
    }

//-------------------------------------------------------------------
// Misc commands

// @HELP -- print help message
class HelpCommand
    extends ServerCommand
    {
    public HelpCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "HELP\tPrint this message", privilegeLevel, logLevel);
        cli.addCommand("HELP", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Print help info sorted by privilege
        for (int i = 0; i <= user.getPrivilege(); i++)
            help(user, i);

        // Acknowledge
        user.outputAck();
        }
    
    /** Print out the help items for a given privilege level
    */
    private void help(User user, int privilege)
        {
        user.outputMessage(user.getPrivilegeString(privilege) + " commands");
        Enumeration enum = cli.getCommands(); 
        while (enum.hasMoreElements())
            {
            ServerCommand command = (ServerCommand)enum.nextElement();
            if (command.privilegeLevel == privilege)
                user.outputMessage("  " + ServerCommandInterpreter.COMMAND_PREFIX + command.getShortHelp());
            }
        }
    }


// @BROADCAST -- Send message to all users
class BroadcastCommand
    extends ServerCommand
    {
    public BroadcastCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "BROADCAST message\tSend a message to all users", privilegeLevel, logLevel);
        cli.addCommand("BROADCAST", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Check args
        if (!args.hasMoreElements())
            {
            user.outputError("BROADCAST: No message");
            return;
            }
            
        // Build the message
        String msg = buildMessage("BROADCAST: from ", user, args);
        
        // Broadcast the message
        user.getServer().broadcast(msg);
        
        // Acknowledge
        user.outputAck();
        }
        
    static String buildMessage(String prefix, User user, Enumeration args)
        {
        StringBuffer buf = new StringBuffer(prefix);
        buf.append(user.getUserId());
        buf.append(" (");
        buf.append(user.getPrivilegeString());
        buf.append("): ");
        while (args.hasMoreElements())
            {
            buf.append(args.nextElement().toString());
            if (args.hasMoreElements())
                buf.append(" ");
            }
        return buf.toString();
        }
    }

// @WHISPER -- Send message to a user
class WhisperCommand
    extends ServerCommand
    {
    public WhisperCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "WHISPER userID message\tSend a message to a user", privilegeLevel, logLevel);
        cli.addCommand("WHISPER", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Check if a user name is supplied
        if (!args.hasMoreElements())
            {
            user.outputError("WHISPER: No user ID");
            return;
            }
            
        // Get the connection list and synch on it so the user can't disappear 
        ConnectionList connections = User.getConnectionList();
        synchronized (connections)
            {
            // Get the user
            String otherUserID = (String)args.nextElement();
            User otherUser = (User)connections.find(otherUserID);
            if (otherUser == null)
                {
                user.outputError("WHISPER: Can't find user: " + otherUserID);
                return;
                }
                        
            // Check there is a message to send
            if (!args.hasMoreElements())
                {
                user.outputError("WHISPER: No message");
                return;
                }
                
            // Build the message
            String msg = BroadcastCommand.buildMessage("WHISPER: from ", user, args);
            
            // Send the message
            otherUser.outputMessage(msg);
            
            // Acknowledge
            user.outputAck();
            }
        }
    }
    
// @ENCRYPT -- Process encrypted command line
class EncryptCommand
    extends ServerCommand
    {
    public EncryptCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "ENCRYPT encrypted-text\tProcess encrypted command line", privilegeLevel, logLevel);
        cli.addCommand("ENCRYPT", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Decrypt the message with the user's session key
        //###
        
        // Send the message back to the user's input function
        notImplemented(user); //###
        }
    }


//-------------------------------------------------------------------
// Session commands

// @QUIT -- End session
class QuitCommand
    extends ServerCommand
    {
    public QuitCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "QUIT\tEnd your session", privilegeLevel, logLevel);
        cli.addCommand("QUIT", this);
        }

    public void execute(Enumeration args, User user)
        {
        user.outputAck();
        user.stop();
        }
    }


//-------------------------------------------------------------------
// User commands

// @WHO -- List users
class WhoCommand
    extends ServerCommand
    {
    public WhoCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "WHO\tList users on line", privilegeLevel, logLevel);
        cli.addCommand("WHO", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the connection list
        ConnectionList connections = User.getConnectionList();
        synchronized (connections)
            {
            // Send each user ID as a message
            Enumeration users = connections.elements();
            while (users.hasMoreElements())
                {
                User otherUser = (User)users.nextElement();
                user.outputMessage("WHO: " + otherUser.getUserId());
                }
            }
            
        // Acknowledge
        user.outputAck();
        }
    }
    

//-------------------------------------------------------------------
// Channel commands

// @CHANNELS -- List channels
class ChannelsCommand
    extends ServerCommand
    {
    public ChannelsCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "CHANNELS\tList open channels", privilegeLevel, logLevel);
        cli.addCommand("CHANNELS", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the channels and print them out
        Enumeration channels = user.getChannels();
        while (channels.hasMoreElements())
            {
            Channel channel = (Channel)channels.nextElement();
            String message = "CHANNELS: " + channel.getChannelID();
            
            // Mark the current channel with a star
            if (channel == user.getCurrentChannel())
                message = message + " *";
                
            user.outputMessage(message);
            }
            
        // Acknowledge
        user.outputAck();
        }
    }

// @OPEN -- Open a channel
class OpenCommand
    extends ServerCommand
    {
    public OpenCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "OPEN channelID\tOpen a channel", privilegeLevel, logLevel);
        cli.addCommand("OPEN", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the channel ID from the args
        if (!args.hasMoreElements())
            {
            user.outputError("OPEN: No channel ID");
            return;
            }
        String channelID = args.nextElement().toString();
        
        // Check the ID is valid
        if (!ChannelUtil.isValidChannelID(channelID))
            {
            user.outputError("OPEN: Invalid channel ID: " + channelID);
            return;
            }
        
        // See if the user already has this channel open
        if (user.getChannel(channelID) != null)
            {
            user.outputError("OPEN: Channel already open: " + channelID);
            return;
            }
                
        // Get the application
        Application app = user.getServer().getApplication(channelID);
        if (app == null)
            {
            user.outputError("OPEN: Application not found: " + channelID);
            return;
            }
            
        // Create the channel object
        Channel channel = app.newChannel();
        if (channel == null)
            {
            user.outputError("OPEN: Can't create channel: " + channelID);
            return;
            }
            
        // Open the channel
        try {
            channel.open(channelID, user, args);
            }
        catch (Exception e)
            {
            user.outputError("OPEN: Can't open channel: " + channelID + ": " + e.toString());
            return;
            }
        
        // Acknowledge
        user.outputAck();
        
        // Start the channel
        channel.start();
        }
    }

// @CLOSE -- Close a channel
class CloseCommand
    extends ServerCommand
    {
    public CloseCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "CLOSE channelID\tClose a channel", privilegeLevel, logLevel);
        cli.addCommand("CLOSE", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the channel ID from the args
        if (!args.hasMoreElements())
            {
            user.outputError("CLOSE: No channel ID");
            return;
            }
        String channelID = args.nextElement().toString();

        // Get the channel
        Channel channel = user.getChannel(channelID);
        if (channel == null)
            {
            user.outputError("CLOSE: Channel not found: " + channelID);
            return;
            }
        
        // Stop and close the channel.
        channel.stop();
        channel.close();

        // Acknowledge
        user.outputAck();
        }
    }

// @SWITCH -- Change channel
class SwitchCommand
    extends ServerCommand
    {
    public SwitchCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "SWITCH channelID\tChange current channel", privilegeLevel, logLevel);
        cli.addCommand("SWITCH", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the channel ID from the args
        if (!args.hasMoreElements())
            {
            user.outputError("SWITCH: No channel ID");
            return;
            }
        String channelID = args.nextElement().toString();

        // Get the channel
        Channel channel = user.getChannel(channelID);
        if (channel == null)
            {
            user.outputError("SWITCH: Channel not found: " + channelID);
            return;
            }
        
        // Switch to that channel
        user.setCurrentChannel(channel);

        // Acknowledge
        user.outputAck();
        }
    }


//-------------------------------------------------------------------
// Application commands

// @APPS -- List open applications
class AppsCommand
    extends ServerCommand
    {
    public AppsCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "APPS\tList active applications", privilegeLevel, logLevel);
        cli.addCommand("APPS", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the apps and print them out.
        // The format is APPS: appID app-class-name app-info
        SessionServer server = user.getServer();
        synchronized (server)
            {
            Enumeration apps = server.getApplications();
            while (apps.hasMoreElements())
                {
                Application app = (Application)apps.nextElement();
                String className = app.getClass().getName();
                user.outputMessage("APPS: " + app.getID() + "\t" + 
                        className + "\t" + app.getInfo());
                }
            }
            
        // Acknowledge
        user.outputAck();
        }
    }

// @START -- Start running an application
class StartCommand
    extends ServerCommand
    {
    public StartCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "START app-class appID\tCreate and open an application", privilegeLevel, logLevel);
        cli.addCommand("START", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the class name and app ID from the args
        if (!args.hasMoreElements())
            {
            user.outputError("START: No class name");
            return;
            }
        String appClassName = args.nextElement().toString();
        if (!args.hasMoreElements())
            {
            user.outputError("START: No application ID");
            return;
            }
        String appID = args.nextElement().toString();
        
        // Start the application
        SessionServer server = user.getServer();
        try {
            server.startApplication(appClassName, appID, args);
            }
        catch (Exception e)
            {
            user.outputError("START: Can't create application: " + appID + ": " + e.getMessage());
            return;
            }
            
        // Acknowledge
        user.outputAck();
        }
    }

// @STOP -- Close an application
class StopCommand
    extends ServerCommand
    {
    public StopCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "STOP appID\tClose an application", privilegeLevel, logLevel);
        cli.addCommand("STOP", this);
        }

    public void execute(Enumeration args, User user)
        {
        // Get the app ID from the args
        if (!args.hasMoreElements())
            {
            user.outputError("STOP: No application ID");
            return;
            }
        String appID = args.nextElement().toString();
        
        // Get the application
        SessionServer server = user.getServer();
        Application app = server.getApplication(appID);
        if (app == null)
            {
            user.outputError("STOP: Application not found: " + appID);
            return;
            }
        
        // Remove the app from the server
        server.stopApplication(app);
            
        // Acknowledge
        user.outputAck();
        }
    }

// @DELETE -- Delete an application
class DeleteAppCommand
    extends ServerCommand
    {
    public DeleteAppCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "DELETE appID\tDelete an application (USE WITH CARE!)", privilegeLevel, logLevel);
        cli.addCommand("DELETE", this);
        }

    public void execute(Enumeration args, User user)
        {
        //### Left this one for now!!
        notImplemented(user); //###
        }
    }


//-------------------------------------------------------------------
// Server control commands

// @SHUTDOWN -- Shut down the server
class ShutdownCommand
    extends ServerCommand
    {
    public ShutdownCommand( ServerCommandInterpreter cli, int privilegeLevel, int logLevel )
        {
        super( cli, "SHUTDOWN\tShut down the server", privilegeLevel, logLevel );
        cli.addCommand( "SHUTDOWN", this );
        }
    
    private User user;
    private Enumeration args;
    
    public void execute( Enumeration args, User user )
        {
        this.user = user;
        this.args = args;
        if ( !args.hasMoreElements() )
            {
            shutdownInfo();
            return;
            }
            
		// get the first arg
		String s = args.nextElement().toString();
		if ( s.equalsIgnoreCase( "NOW" ) )
		    shutdownNow();
		else if ( s.equalsIgnoreCase( "CANCEL" ) )
		    shutdownCancel();
		else if ( s.equalsIgnoreCase( "AT" ) )
		    shutdownAt();
        else 
            shutdownMinutes(s);
        }

    private void shutdownInfo()
        {
        user.getServer().shutdown( -1, user ); 
        user.outputAck();
        }
    
    private void shutdownNow()
		{
		user.getServer().shutdown( 0, user );
        user.outputAck();
		}
    
    private void shutdownCancel()
        {
        user.getServer().cancelShutdown( user );
        user.outputAck();
        }
    
    private void shutdownAt()
        {
        if (!args.hasMoreElements() )
            user.outputError( "SHUTDOWN: AT requires time (HH:MM)" );
        else {
            // Get the time for shutdown in minutes
    		int requestTime = parseTime(args.nextElement().toString());
    		if (requestTime < 0)
    		    {
                user.outputError( "SHUTDOWN: AT time format invalid" );                
                return;
    		    }
    		    
            // Get the current time in minutes
    		int nowTime = getNow();
    		
    		// Ensure that the requested time is later than the current time
    		if (nowTime > requestTime)
    		    requestTime += (24 * 60);
    		    
    		// Request the shutdown
			user.getServer().shutdown(requestTime - nowTime, user);
            user.outputAck();
            }
        }
    
    /** Convert a string in the format HH:MM to minutes from 00:00
        @return number of minutes or -1 on error
    */
    private int parseTime(String time)
        {
		StringTokenizer tzr = new StringTokenizer(time, ":");
		if (!tzr.hasMoreTokens())
		    return -1;
		String hoursStr = tzr.nextToken();
		if (!tzr.hasMoreTokens())
		    return -1;
		String minutesStr = tzr.nextToken();
        try {
            int hours = Integer.parseInt(hoursStr);
            int minutes = Integer.parseInt(minutesStr);
            return (hours * 60) + minutes;
    		}
        catch ( NumberFormatException e )
            {
            return -1;
            }
        }
        
    /** Get the present time of day in minutes
    */
    private int getNow()
        {
        Calendar cal = Calendar.getInstance();
        return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE);
        }
        
    private void shutdownMinutes(String timeStr)
        {
        try {
		    int time = Integer.parseInt( timeStr );
			user.getServer().shutdown( time, user );    		    
            user.outputAck();
    		}
        catch ( NumberFormatException e )
            {
            user.outputError( "SHUTDOWN: time format invalid" );                
            }
        }
    
    }
