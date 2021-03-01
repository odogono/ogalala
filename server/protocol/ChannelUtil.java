// $Id: ChannelUtil.java,v 1.6 1998/06/08 10:56:10 jim Exp $
// Utilities for manipulating channel IDs
// James Fryer, 28 May 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.server;

/** Channel IDs are used to identify the channel that is being input to
    or output from the server. This class contains utilities for manipulating
    the channel IDs.
    <p>
    The channel ID is at the start of the I/O string, in square brackets. The
    function 'containsChannelID' returns true if a channel ID is present. Strings
    without the channel ID (which are also not server commands) are sent to 
    the current channel.
    <p>
    The functions 'getChannelID', 'insertChannelID', and 'removeChannelID'
    are used to manipulate channel IDs within strings.
    <p>
    A channel ID is structured into two sections, the Application ID and the 
    user ID, separated by the forward slash '/'. The user ID may not be the same 
    as the user's actual ID, rather it is a way to identify the user from the 
    application. One user may have several channels into an application 
    using different IDs.
    <p>
    The functions 'getApplicationID' and 'getUserID' are used to parse a 
    channel ID.
    <p>
    Examples: 
    <br>
    [chat/fred]This is going to the chat application's channel, user fred.
    <br>
    [nile/miss_marple]This is going to the Nile game, character Miss Marple
*/
public class ChannelUtil
    {
    private static final char OPEN_CHAR = '[';
    private static final String OPEN = "[";
    private static final String CLOSE = "]";
    private static final String DELIM = "/";        
    
    /** Does 's' contain a channel ID?
    */
    public static boolean containsChannelID(String s)
        {
        return s.length() > 0 && s.charAt(0) == OPEN_CHAR;
        }

    /** Get the channel ID (if any) from a string.
        @return Channel ID, or null if none found
    */
    public static String getChannelID(String s)
        {
        // Return null if no ID is found
        if (!containsChannelID(s))
            return null;
            
        // Else, get the ID and return it
        else {
            // Get the position of the closing bracket
            int closePos = s.indexOf(CLOSE);
            
            // If there is no closing bracket, treat the entire string as a channel ID...
            //  this will probably result in a "channel not found" error, but this is a 
            //  badly formed packet.
            if (closePos < 0)
                return s.substring(1);

            // Else, the result is the text between the brackets
            else
                return s.substring(1, closePos);
            }
        }
    
    /** Insert a channel ID into a string
        @return A new string prefixed with the channel ID
    */
    public static String insertChannelID(String channelID, String s)
        {
        return OPEN + channelID + CLOSE + s;
        }            

    /** Remove a channel ID (if any) from a string
        @return A new string with the channel ID prefix removed
    */
    public static String removeChannelID(String s)
        {
        // If there is no ID prefix, pass the string through
        if (!containsChannelID(s))
            return s;
            
        // Else, find the closing bracket and remove the string
        else {
            // Get the position of the closing bracket
            int closePos = s.indexOf(CLOSE);
            
            // If there is no closing bracket, treat the entire string as a channel ID and return an empty string
            if (closePos < 0)
                return "";

            // Else, the result is the text after the closing bracket
            else
                return s.substring(closePos + 1);
            }        
        }

    /** Get the application ID from 's'. 
        <p>
        Channel ID sections are separated by forward slashes. The app ID is the 
        first section in the channel ID.
        
        @return The app ID from 's'.
    */
    public static String getApplicationID(String s)
        {
        // Find the slash
        int delimPos = s.indexOf(DELIM);
        
        // If there's no slash, return the whole string
        if (delimPos < 0)
            return s;
            
        // Else return the string up to the slash
        else
            return s.substring(0, delimPos);
        }

    /** Get the user ID from 's'.
        <p>
        Channel ID sections are separated by forward slashes. The user ID is the 
        second section in the channel ID.
        
        @return A new string with the first section removed
    */
    public static String getUserID(String s)
        {
        // Find the slash
        int delimPos = s.indexOf(DELIM);
        
        // If there's no slash, return an empty string
        if (delimPos < 0)
            return "";
            
        // Else return the string after the slash
        else
            return s.substring(delimPos + 1);
        }

    /** Is 'channelID' valid?
        <p>
        Validity here means, does it contain a forward slash?
    */
    public static boolean isValidChannelID(String channelID)
        {
        return channelID != null && channelID.indexOf(DELIM) >= 0;
        }

    /** Prevent instances of this class
    */
    private ChannelUtil()
        {
        }
    }
