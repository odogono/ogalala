// $Id: OutPkt.java,v 1.5 1998/12/07 13:41:43 jim Exp $
// Output packet formatter
// James Fryer, 23 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** An Output Packet formatter for events.
*/
public final class OutPkt
    {
    /** Internal buffer
    */
    private StringBuffer buf;
    
    /** Count the depth of lists. Note that nested lists can be 
        generated but will not currently be parsed by the client.
    */
    private int listDepth;
    
    /** General-purpose constructor
    */
    public OutPkt(String packetType)
        {
        buf = new StringBuffer("type=" + packetType);
        listDepth = 0;
        }
        
    /** Convenience constructor for packets with one field
    */
    public OutPkt(String packetType, String name, String value)
        {
        this(packetType);
        addField(name, value);
        }
        
    /** Convenience constructor for packets with two fields
    */
    public OutPkt(String packetType, String name1, String value1, String name2, String value2)
        {
        this(packetType);
        addField(name1, value1);
        addField(name2, value2);
        }
        
    /** Convenience constructor for packets with three fields
    */
    public OutPkt(String packetType, String name1, String value1, String name2, String value2, String name3, String value3)
        {
        this(packetType);
        addField(name1, value1);
        addField(name2, value2);
        addField(name3, value3);
        }
        
    /** Constructor for "misc" packets
    */
    public OutPkt()
        {
        this("misc");
        }
    
    /** Add a field. The value will be quoted if it contains spaces.
    */
    public void addField(String name, String value)
        {
        buf.append(" ");
        buf.append(name);
        buf.append("=");
        buf.append(quoteString(value));
        }
        
    /** Open a list field
    */
    public void openList(String name)
        {
        buf.append(" ");
        buf.append(name);
        buf.append("=[");
        listDepth++;
        }
        
    /** Close a list field
    */
    public void closeList()
        {
        Debug.assert(listDepth > 0, "(OutPkt/96)");
        buf.append(" ]");
        listDepth--;
        }
        
    /** Ensure that no list fields are left open
    */
    public void closeAllLists()
        {
        while (listDepth > 0)
            closeList();
        }
        
    /** Add an item to a list field
    */
    public void addListItem(String value)
        {
        Debug.assert(listDepth > 0, "(OutPkt/113)");
        
        buf.append(" ");
        buf.append(quoteString(value));
        }
        
    /** Convert to string
    */
    public String toString()
        {
        closeAllLists();
        return buf.toString();
        }
        
    /** Wrap 's' in quotes if 'mustBeQuoted(s)' is true.
        @see #mustBeQuoted
    */
    public static String quoteString(String s)
        {
        if (mustBeQuoted(s))
            return "\"" + s + "\"";
        else
            return s;
        }
        
    /** Should 's' be surrounded by quotes to be correctly parsed by 
        StreamTokenizer, TableParser, etc?
        <p>
        ### This fails various sanity checks -- such as is the string already quoted?
        What if the string contains double quotes -- it can't say to wrap with single quotes...
    */
    private static boolean mustBeQuoted(String s)
        {
        // Avoid null or empty strings
        if (s == null)
            return false;
        else if (s.length() == 0)
            return true;
            
        // Look for an open list char. If the string starts with a square bracket
        //  it is a list or table and should not be quoted.
        int openListPos = s.indexOf('[');
        if (openListPos == 0)
            return false;
        
        // Look for spaces
        else if (!StringUtil.isSingleWord(s))
            return true;
            
        // If there's an open list char inside the string
        else if (openListPos > 0)
            return true;

        // Look for close list char
        else if (s.indexOf(']') > 0)
            return true;

        // Look for equals char
        else if (s.indexOf('=') >= 0)
            return true;
            
        else 
            return false;
        }
    }
