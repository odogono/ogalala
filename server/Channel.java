// $Id: Channel.java,v 1.11 1999/09/01 16:07:01 jim Exp $
// Abstract channel for server application I/O
// James Fryer, 28 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.util.Enumeration;

/** A Channel enables I/O between a user and an application.
    <ul>
    <li><b>open</b> Creates a channel which is attached to user and 
                    application. May be overridden.                    
    <li><b>close</b> Removes the channel from the user and application. 
    <li><b>start</b> Sends setup information to the client.
    <li><b>stop</b> Sends closedown information to the client
    <li><b>input</b> Process input from the client
    <li><b>output</b> Send outpuit to the client
    </ul>
*/
public abstract class Channel
    {
    /** The channel ID
    */   
    protected String channelID;
    
    /** The user
    */   
    protected User user;
    
    /** Open the channel. 
        <p>
        This function must add the channel to the user's list and to the application's 
        list of open channels.
    */
    public void open(String channelID, User user, Enumeration args)
        throws ChannelOpenException
        {
        // Set up instance variables
        this.channelID = channelID;
        this.user = user;
        
        // Add to user and app's channel lists
        addToApplication();
        user.addChannel(this);
        }
        
    /** Close the channel.
    */
    public void close()
        {
        // Remove this channel from the application and user channel lists
        user.removeChannel(this);
        removeFromApplication();
        }
    
    /** Start the channel. This is where any introductory messages can be sent.
    */
    public void start()
        {
        }
        
    /** Stop the channel 
    */
    public void stop()
        {
        }
    
    /** Called when this channel is opened. Must add the channel to the application
    */
    protected abstract void addToApplication()
        throws ChannelOpenException;

    /** Called when this channel is closed. Removes the channel from the application.
    */
    protected abstract void removeFromApplication();
    
    /** Send input to the application
    */
    public abstract void input(String s);
    
    /** Receive output from the application
    */
    public void output(String s)
        {
        user.outputChannel(this, s);
        }
    

    /** Get the channel ID, e.g. "nile/miss_marple".
    */
    public String getChannelID()
        {
        return channelID;
        }
    
    /** Get the user 
    */
    public User getUser()
        {
        return user;
        }
    }
