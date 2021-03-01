// $Id: ChatPerson.java,v 1.10 1999/09/01 16:07:01 jim Exp $
// Represents a person in a chat session. Implements server channel. 
// James Fryer, 2 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.server.apps;

import java.util.*;
import com.ogalala.server.*;

public class ChatPerson
    extends Channel
    {
    /** back-pointer to application
    */
    private ChatApp app;
    
    /** The chat user's nickname
    */
    private String personID;
    
    /** The current chat room
    */
    private ChatRoom chatRoom;
    
    public ChatPerson(ChatApp app)
        {
        this.app = app;
        }
        
    /** Open the channel 
    */
    public void open(String channelID, User user, Enumeration args)
        throws ChannelOpenException
        {
        // Set up chat room
        chatRoom = app.getChatRoom();
        
        // Get the chat name
        personID = ChannelUtil.getUserID(channelID);
        if (personID == null)
            personID = user.getUserId();

        // If in 'strict' mode, person ID must equal user ID            
        if (app.isStrict() && !personID.equalsIgnoreCase(user.getUserId()))
            throw new ChannelOpenException("Strict chat: Chat ID must be the same as login ID");
            
        // Make sure the person name is not already in use
        if (chatRoom.getPerson(personID) != null)
            throw new ChannelOpenException("Chat: Chat ID \'" + personID + "\' is in use");

        super.open(channelID, user, args);
        }
    
    /** Start the channel. 
    */
    public void start()
        {
        chatRoom.startSession(this);
        }
        
    /** Stop the channel 
    */
    public void stop()
        {
        chatRoom.stopSession(this);
        }
    
    /** Called when this channel is opened. Must add the channel to the application
    */
    protected void addToApplication()
        {
        chatRoom.addPerson(this);
        }

    /** Called when this channel is closed. Removes the channel from the application.
    */
    protected void removeFromApplication()
        {
        chatRoom.removePerson(this);
        }
    
    /** Send input to the application
    */
    public void input(String s)
        {
        app.execute(this, s);
        }
    
    /** Get the person's ID
    */
    public String getPersonID()
        {
        return personID;
        }
    }
