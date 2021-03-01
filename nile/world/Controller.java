// $Id: Controller.java,v 1.4 1998/05/02 14:48:31 jim Exp $
// Controller interface
// James Fryer, 25 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.world;

class Persona extends Object
    {
    //### This is a temporary placeholder class!!
    }

/** The Controller interface provides a means by which characters in the
    game -- both PCs and NPCs -- can communicate with the intelligence,
    human or AI, which operates them.
*/
public abstract class Controller
    {
    // The persona that is controlled by this class
    private Persona persona;

    /** Creator.
    */
    public Controller(Persona persona)
        {
        this.persona = persona;
        }

    /** Get the persona associated with this controller
    */
    public Persona getPersona()
        { return persona; }

    /** Output function -- must be implemented by users of this class
    */
    abstract public void output(String s);

    // Packet output functions

    /** Output a text packet
    */
    public void outputText(String packetName, String packetData)
        {
        output("T " + packetName + "|" + packetData);
        }

    /** Output an error packet
    */
    public void outputError(String packetName, String packetData)
        {
        output("E " + packetName + "|" + packetData);
        }

    /** List packet control constants
    */
    public static final int LIST_DEFINE = 0;    // Define a list
    public static final int LIST_ADD = 1;       // Add to a list
    public static final int LIST_REMOVE = 2;    // Remove from a list

    /** Open a list packet. The list will be built up in 'buf'
    */
    public StringBuffer openList(String packetName, int command)
        {
        // Create a buffer for the list packet
        StringBuffer result = null;

        // Create the packet header
        switch (command)
            {
        case LIST_DEFINE:
            result = new StringBuffer("L " + packetName);
            break;
        case LIST_ADD:
            result = new StringBuffer("L+ " + packetName);
            break;
        case LIST_REMOVE:
            result = new StringBuffer("L- " + packetName);
            break;
            }

        // ASSERT(result != null);

        return result;
        }

    /** Define a new list
    */
    public StringBuffer openList(String packetName)
        {
        return openList(packetName, LIST_DEFINE);
        }

    /** Add to a list packet
    */
    public void addList(StringBuffer buf, String listElement)
        {
        buf.append("|" + listElement);
        }

    /** Output a list packet
    */
    public void closeList(StringBuffer buf)
        {
        output(buf.toString());
        }

    /** Open a variable packet
    */
    public StringBuffer openVar(String packetName)
        {
        return new StringBuffer("V " + packetName);
        }

    /** Add to a variable packet
    */
    public void addVar(StringBuffer buf, String varName, String varValue)
        {
        buf.append("|" + varName + "=" + varValue);
        }

    /** Output a variable packet
    */
    public void closeVar(StringBuffer buf)
        {
        output(buf.toString());
        }
    }
