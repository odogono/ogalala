// $Id: OutputPacket.java,v 1.4 1998/05/02 14:48:31 jim Exp $
// OutputPacket class
// James Fryer, 31 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.world;

/** The OutputPacket is used to build packets for output from the
    World layer.
*/
public class OutputPacket
    {
    // The string buffer containing the formatted packet
    private StringBuffer buf;

    /** List packet control constants
    */
    public static final int LIST_DEFINE = 0;    // Define a list
    public static final int LIST_ADD = 1;       // Add to a list
    public static final int LIST_REMOVE = 2;    // Remove from a list

    // Delimiter character
    private static final String DELIMITER = "|";

    /** Make a text packet
    */
    public final void textPacket(String tag)
        {
        buf = new StringBuffer(100);
        buf.append("T " + tag);
        }

    /** Make a text packet with a field
    */
    public final void textPacket(String tag, String s)
        {
        textPacket(tag);
        addField(s);
        }

    /** Make an Error packet
    */
    public final void errorPacket(String tag)
        {
        buf = new StringBuffer(100);
        buf.append("E " + tag);
        }

    /** Make an Error packet with a field
    */
    public final void errorPacket(String tag, String s)
        {
        errorPacket(tag);
        addField(s);
        }

    /** Make a List packet
    */
    public final void listPacket(String tag, int command)
        {
        // Create the packet header
        switch (command)
            {
        case LIST_DEFINE:
            buf = new StringBuffer("L " + tag);
            break;
        case LIST_ADD:
            buf = new StringBuffer("L+ " + tag);
            break;
        case LIST_REMOVE:
            buf = new StringBuffer("L- " + tag);
            break;
        default:
            buf = null;
            break;
            }
        }

    /** Make a List Define packet
    */
    public final void listPacket(String tag)
        {
        listPacket(tag, LIST_DEFINE);
        }

    /** Make a Variable packet
    */
    public final void varPacket(String tag)
        {
        buf = new StringBuffer(100);
        buf.append("V " + tag);
        }

    /** Add a normal field to a packet
    */
    public final void addField(String s)
        {
        buf.append(DELIMITER);
        buf.append(s);
        }

    /** Add a variable field to a packet (in the format NAME=VALUE)
    */
    public final void addVarField(String name, String value)
        {
        buf.append(DELIMITER);
        buf.append(name);
        buf.append("=");
        buf.append(value);
        }

    /** Get the packet as a string
    */
    public final String toString()
        {
        return buf.toString();
        }
    }
