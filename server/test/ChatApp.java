// $Id: ChatApp.java,v 1.5 1999/09/01 15:05:32 jim Exp $
// Yet another chat application. This time to test the session server.
// James Fryer, 2 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.apps;

import java.io.*;
import java.util.*;
import com.ogalala.server.*;
//import com.ogalala.server.apps.chat.*;

/** A Session Server application that implements a chat system
*/
public class ChatApp
    implements Application
    {
    /** The ID of this application
    */
    private String appID;
    
    /** There is only one chat room in this version, but multiple rooms could be 
        easily added...
    */
    private ChatRoom chatRoom;
    
    /** Command line options 
        ### Perhaps applications should have a 'getHelp' function...
    */
    private static final String strictOption = "-strict";
    private static final String spoofOption = "-spoof";
    
    /** Option flags
        Strict: Don't allow users to log in under different IDs
    */
    boolean strictFlag = false;
    
    /** Spoof: Allow spoofing
    */
    boolean spoofFlag = false;    
    
    public ChatApp()
        {
        }

    /** Create a new application. Implementations should avoid overwriting 
        existing apps with the same ID.
    */
    public void create(String appID, Enumeration args)
        {
        // Pass on to 'open' as there is no persistent info
        open(appID, args);
        }

    /** Open an existing application
    */
    public void open(String appID, Enumeration args)
        {
        this.appID = appID;
        
        // Process args
        while (args.hasMoreElements())
            {
            String option = args.nextElement().toString();
            if (strictOption.equalsIgnoreCase(option))
                strictFlag = true;
            else if (spoofOption.equalsIgnoreCase(option))
                spoofFlag = true;
            }            

        chatRoom = new ChatRoom("Ogalala test chat");
        }

    /** Close this application. Preserve state, if appropriate.
    */
    public void close()
        {
        chatRoom.broadcast("This chat room is now closing down.");
        
        // Get the people from the chat room and close all the channels
        synchronized (chatRoom)
            {
            Enumeration enum = chatRoom.getPeople();
            while (enum.hasMoreElements())
                {
                ChatPerson person = (ChatPerson)enum.nextElement();
                person.close();
                }
            }
            
        chatRoom = null;
        }

    /** Remove an application's persistent state. 
        (USE WITH CARE!)
    */
    public void delete(String appID)
        {
        // Do nothing
        }

    /** Does an application called 'appID' exist?
    */
    public boolean exists(String appID)
        {
        // Always true because there is no persistent info
        return true;
        }

    /** Get the ID of this application
    */
    public String getID()
        {
        return appID;
        }

    /** Create a new channel
    */
    public Channel newChannel()
        {
        return new ChatPerson(this);
        }
    
    /** Get some information about this application
    */
    public String getInfo()
        {
        StringBuffer result = new StringBuffer("Ogalala Chat application");
        if (strictFlag)
            result.append(" <strict>");
        if (spoofFlag)
            result.append(" <spoofy>");
        return result.toString();
        }
    
    /** Get the chat room
    */
    public final ChatRoom getChatRoom()
        {
        return chatRoom;
        }
        
    /** Execute a user command
    */
    public void execute(ChatPerson person, String s)
        {
        // Remove trailing whitespace from the string
        s = s.trim();

        // If it's an empty string, ignore it
        if (s.length() == 0)
            return;

        // If the string starts with a slash then it's an escaped command
        else if (s.charAt(0) == '/')
            execCommand(person, s);

        // If the string starts with a dash then it's a whisper
        else if (s.charAt(0) == '-')
            execWhisper(person, s);

        // If the string starts with a bang then it's a spoof
        else if (s.charAt(0) == '!')
            spoof(person, s);

        // Else, it is a say
        else
            chatRoom.say(person, s);
        }

    private void help(ChatPerson person)
        {
        person.output("Ogalala Chat Server commands:");
        person.output("Text typed in will be broadcast to all users");
        person.output("/?, /h\tThis help message");
        person.output("/q\tQuit");
        person.output("/w\tWho is online?");
        person.output("-person\tWhisper something to a person");
        }

    // Execute a slash-command
    private void execCommand(ChatPerson person, String s)
        {
        if (s.startsWith("/?") || s.startsWith("/h") || s.startsWith("/H"))
            help(person);
        else if (s.startsWith("/q") || s.startsWith("/Q"))
            quit(person);
        else if (s.startsWith("/w") || s.startsWith("/W"))
            chatRoom.who(person);
        else
            person.output("Unknown command: " + s);
        }

    private void execWhisper(ChatPerson person, String s)
        {
        //### There is a bug in this routine, if the person types "-" or "-name" without a message
        //###   then an exception is raised; I have left this in because it demonstrates that
        //###   the connection can cope with errors.

        // Separate the person's name from the message
        // The target name is everything between the dash and the space.
        // The message is everything after the first space.
        int i = s.indexOf(" ");
        String targetUserName = s.substring(1, i);
        s = s.substring(i + 1);

        // Get the connection that is being whispered to
        ChatPerson target = chatRoom.getPerson(targetUserName);

        // If the target is null, the user probably mistyped
        if (target == null)
            person.output("I can't find person " + targetUserName + " anywhere. Please try again");

        // Don't allow users to whisper to themselves...
        else if (target == person)
            person.output("Trying to talk to yourself?");

        // Do the whisper
        else
            chatRoom.whisper(person, target, s);
        }

    /** Leave the building
    */
    private void quit(ChatPerson person)
        {
        person.close();
        }
        
    /** Enter a command as if it comes from another user
    */
    public synchronized void spoof(ChatPerson person, String s)
        {
        // Is spoofing allowed here?
        if (isSpoof())
            {
            // Separate the person's name from the message
            // The target name is everything between the bang and the space.
            // The spoofed command is everything after the first space.
            int i = s.indexOf(" ");
            String targetUserName = s.substring(1, i);
            s = s.substring(i + 1);

            // Get the victim of the spoof
            ChatPerson target = chatRoom.getPerson(targetUserName);

            // If the target is null, the user probably mistyped
            if (target == null)
                person.output("I can't find person " + targetUserName + " anywhere. Please try again");
                
            // Send the command as if it had come from the lucky spoofee.
            else 
                execute(target, s);
            }
            
        // No spoofing here.            
        else
            person.output("Sorry. Spoofing is not allowed here.");
        }

    public boolean isStrict()
        { return strictFlag; }

    public boolean isSpoof()
        { return spoofFlag; }
    }
