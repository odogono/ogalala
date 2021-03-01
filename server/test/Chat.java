// $Id: Chat.java,v 1.5 1998/06/18 16:07:31 rich Exp $
// Chat application -- test class for the server
// James Fryer, 13 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.test;

import java.io.*;
import java.util.*;
import com.ogalala.server.*;

/** This is a very simple test application using the Server classes.

    Note that this app uses the ConnectionList directly. It is likely that
    more complex apps will maintain their own user list -- this allows multiple
    apps per server, or multiple locations within an app.
*/
class Chat
    {
    public Chat()
        {
        }

    public void execute(Connection user, String s)
        {
        // Remove trailing whitespace from the string
        s = s.trim();

        // If it's an empty string, ignore it
        if (s.length() == 0)
            return;

        // If the string starts with a slash then it's an escaped command
        else if (s.charAt(0) == '/')
            execCommand(user, s);

        // If the string starts with a dash then it's a whisper
        else if (s.charAt(0) == '-')
            execWhisper(user, s);

        // If the string starts with a bang then it's a spoof
        else if (s.charAt(0) == '!')
            {
            spoof(user, s);
            }

        // Else, it is a say
        else
            say(user, s);
        }

    private void help(Connection user)
        {
        user.output("Ogalala Chat Server commands:\r\n" +
            "Text typed in will be broadcast to all users\r\n" +
            "/?, /h\tThis help message\r\n" +
            "/q\tQuit\r\n" +
            "/w\tWho is online?\r\n" +
            "-user\tWhisper something to a user\r\n"
            );
        }

    /** Announce a user's arrival
    */
    public void announceArrival(Connection user)
        {
        ConnectionList connections = Connection.getConnectionList();
        synchronized (connections)
            {
            Enumeration enum = connections.elements();
            while (enum.hasMoreElements())
                {
                Connection connection = (Connection)enum.nextElement();
                if (connection != user)
                    connection.output(user.getUserId() + " has arrived");
                }
            }
        }

    /** Announce a user's departure
    */
    public void announceDeparture(Connection user)
        {
        ConnectionList connections = Connection.getConnectionList();
        synchronized (connections)
            {
            Enumeration enum = connections.elements();
            while (enum.hasMoreElements())
                {
                Connection connection = (Connection)enum.nextElement();
                if (connection != user)
                    connection.output(user.getUserId() + " has left");
                }
            }
        }

    // Execute a slash-command
    private void execCommand(Connection user, String s)
        {
        if (s.startsWith("/?") || s.startsWith("/h") || s.startsWith("/H"))
            help(user);
        else if (s.startsWith("/q") || s.startsWith("/Q"))
            quit(user);
        else if (s.startsWith("/w") || s.startsWith("/W"))
            who(user);
        else
            user.output("Unknown command: " + s);
        }

    private void say(Connection user, String s)
        {
        String userName = user.getUserId();
        ConnectionList connections = Connection.getConnectionList();
        synchronized (connections)
            {
            Enumeration enum = connections.elements();
            while (enum.hasMoreElements())
                {
                Connection connection = (Connection)enum.nextElement();
                if (connection != user)
                    connection.output(userName + " says: " + s);
                else
                    connection.output("You say: " + s);
                }
            }
        }

    private void execWhisper(Connection user, String s)
        {
        //### There is a bug in this routine, if the user types "-" or "-name" without a message
        //###   then an exception is raised; I have left this in because it demonstrates that
        //###   the connection can cope with errors.

        // Separate the user's name from the message
        int i;
        for (i = 0; i < s.length(); i++)
            {
            // The first space in the string delimits the target user name from the message
            if (s.charAt(i) == ' ')
                break;
            }

        // The target name is everything between the dash and the space.
        // The message is everything after the first space.
        String targetUserId = s.substring(1, i);
        s = s.substring(i + 1);

        // Get the connection that is being whispered to
        Connection target = findUser(targetUserId);

        // If the target is null, the user probably mistyped
        if (target == null)
            user.output("I can't find user " + targetUserId + " anywhere. Please try again");

        // Don't allow users to whisper to themselves...
        else if (target == user)
            user.output("Trying to talk to yourself?");

        // Do the whisper
        else
            whisper(target, user, s);
        }

    private void whisper(Connection target, Connection user, String s)
        {
        target.output(user.getUserId() + " whispers: " + s);
        user.output("You whisper to " + target.getUserId() +": " + s);
        }

    private void spoof(Connection user, String s)
        {
        user.output("Sorry. No spoofing yet.");
        }

    private void quit(Connection user)
        {
        user.stop();
        }

    private void who(Connection user)
        {
        StringBuffer buf = new StringBuffer();
        ConnectionList connections = Connection.getConnectionList();
        synchronized (connections)
            {
            Enumeration enum = connections.elements();
            while (enum.hasMoreElements())
                {
                Connection connection = (Connection)enum.nextElement();
                buf.append(connection.getUserId() + "\r\n");
                }
            }
        user.output(buf.toString());
        }

    // Given a user name, get the user connection. Return null if user not found
    private Connection findUser(String userName)
        {
        ConnectionList connections = Connection.getConnectionList();
        return connections.find(userName);
        }
    }
