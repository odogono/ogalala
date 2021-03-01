// $Id: ExitTable.java,v 1.10 1999/04/21 19:21:52 jim Exp $
// Data structure for room exits
// James Fryer, 16 Oct 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;


/** An Exit Table is a collection of exits accessed by the directions
    they travel in. Containers have exit tables if 'Container.addExit'
    is used.
    <p>
    Note that the approved way of adding exits to a container is to used 
    'World.addExit'.
    
*/
public final class ExitTable
    implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;
    
    /** Constants to represent directions
        <p>
        Note that these numbers are allocated so that reversing the bytes
        will give the opposite direction.
    */
    public static final int DIR_NORTH = 0x0001;
    public static final int DIR_SOUTH = 0x0100;
    public static final int DIR_WEST = 0x0002;
    public static final int DIR_EAST = 0x0200;
    public static final int DIR_NORTHWEST = 0x0004;
    public static final int DIR_SOUTHEAST = 0x0400;
    public static final int DIR_NORTHEAST = 0x0008;
    public static final int DIR_SOUTHWEST = 0x0800;
    public static final int DIR_UP = 0x0010;
    public static final int DIR_DOWN = 0x1000;
    public static final int DIR_IN = 0x0020;
    public static final int DIR_OUT = 0x2000;
    public static final int DIR_NDIR = DIR_NORTH | DIR_NORTHWEST | DIR_NORTHEAST;
    public static final int DIR_SDIR = DIR_SOUTH | DIR_SOUTHWEST | DIR_SOUTHEAST;
    public static final int DIR_WDIR = DIR_WEST | DIR_NORTHWEST | DIR_SOUTHWEST;
    public static final int DIR_EDIR = DIR_EAST | DIR_NORTHEAST | DIR_SOUTHEAST;
    public static final int DIR_ALL = 0x3fff;
    
    /** Strings that can be entered by the user to represent directions
        <p>### Should this be part of the Vocabulary???
    */
    public static final String CMD_NORTH = "north";
    public static final String CMD_NORTH_S = "n";
    public static final String CMD_SOUTH = "south";
    public static final String CMD_SOUTH_S = "s";
    public static final String CMD_WEST = "west";
    public static final String CMD_WEST_S = "w";
    public static final String CMD_EAST = "east";
    public static final String CMD_EAST_S = "e";
    public static final String CMD_NORTHWEST = "northwest";
    public static final String CMD_NORTHWEST_S = "nw";
    public static final String CMD_NORTHEAST = "northeast";
    public static final String CMD_NORTHEAST_S = "ne";
    public static final String CMD_SOUTHWEST = "southwest";
    public static final String CMD_SOUTHWEST_S = "sw";
    public static final String CMD_SOUTHEAST = "southeast";
    public static final String CMD_SOUTHEAST_S = "se";
    public static final String CMD_UP = "up";
    public static final String CMD_UP_S = "u";
    public static final String CMD_DOWN = "down";
    public static final String CMD_DOWN_S = "d";
    public static final String CMD_IN = "in";
    public static final String CMD_OUT = "out";
    
    /** Strings representing directions as displayed to the user
    */
    public static final String LABEL_NORTH = "north";
    public static final String LABEL_SOUTH = "south";
    public static final String LABEL_WEST = "west";
    public static final String LABEL_EAST = "east";
    public static final String LABEL_NORTHWEST = "northwest";
    public static final String LABEL_NORTHEAST = "northeast";
    public static final String LABEL_SOUTHWEST = "southwest";
    public static final String LABEL_SOUTHEAST = "southeast";
    public static final String LABEL_UP = "up";
    public static final String LABEL_DOWN = "down";
    public static final String LABEL_IN = "in";
    public static final String LABEL_OUT = "out";
    public static final String LABEL_NDIR = "northwards";
    public static final String LABEL_SDIR = "southwards";
    public static final String LABEL_WDIR = "westwards";
    public static final String LABEL_EDIR = "eastwards";
    public static final String LABEL_UNKNOWN = "unknown";
    
    /** How many exits to allow for in the exits table
    */
    private static final int EXIT_TABLE_SIZE = 14;
    
    /** The active exits 
        <p>
        Note that elements 6 and 7 of this array are not used in order
        to get the byte swapping mapping to reversing direction.
    */
    private Atom exits[] = new Atom[EXIT_TABLE_SIZE];
    
    /** Ctor
    */
    protected ExitTable()
        {
        }
        
    /** Get an exit
    
        @return an exit atom or null if no exit is associated with the direction
    */
    public Atom getExit(int direction)
        {
        int n = getTableIndex(direction);
        if (n >= 0)
            return exits[n];
        else    
            return null;
        }
    
    /** Add an exit object
    */
    public void addExit(int direction, Atom exit)
        {
        // For each set bit in 'direction', set the corresponding entry in the exit table
        for (int i = 0; i < EXIT_TABLE_SIZE; i++)
            {
            if ((direction & 1) != 0)
                exits[i] = exit;
            direction >>= 1;
            }
        }
        
    /** Remove exits by direction
    */
    public void removeExit(int direction)
        {
        // For each set bit in 'direction', clear the corresponding entry in the exit table
        for (int i = 0; i < EXIT_TABLE_SIZE; i++)
            {
            if ((direction & 1) != 0)
                exits[i] = null;
            direction >>= 1;
            }
        }
    
    /** Remove exits by object
    */
    public void removeExit(Atom exit)
        {
        for (int i = 0; i < EXIT_TABLE_SIZE; i++)
            {
            if (exits[i] == exit)
                exits[i] = null;
            }
        }
    
    /** Is this table empty?
    */
    public boolean isEmpty()
        {
        for (int i = 0; i < EXIT_TABLE_SIZE; i++)
            {
            if (exits[i] != null)
                return false;
            }
        return true;
        }
    
    /** Convert to export format.
        <p>
        [ direction=atomID... ]
    */
    protected String toExportFormat()
        {
        StringBuffer result = new StringBuffer("[ ");
        int direction = DIR_NORTH;
        for (int i = 0; i < EXIT_TABLE_SIZE; i++)
            {
            if (exits[i] != null)
                {
                result.append(toString(direction));
                result.append("=");
                result.append(exits[i].getID());
                result.append(" ");
                }
            direction <<= 1;
            }
        result.append("]");
        return result.toString();
        }
        
    /** Translate a command string to a direction flag
    */
    public static int toDirection(String exitCmd)
        {
        //### DOn't like the use of intern here... Spurious entries will be added to the intern table.
        exitCmd = exitCmd.toLowerCase().intern();
        if (exitCmd == CMD_NORTH || exitCmd == CMD_NORTH_S)
            return DIR_NORTH;
        else if (exitCmd == CMD_SOUTH || exitCmd == CMD_SOUTH_S)
            return DIR_SOUTH;
        else if (exitCmd == CMD_WEST || exitCmd == CMD_WEST_S)
            return DIR_WEST;
        else if (exitCmd == CMD_EAST || exitCmd == CMD_EAST_S)
            return DIR_EAST;
        else if (exitCmd == CMD_NORTHWEST || exitCmd == CMD_NORTHWEST_S)
            return DIR_NORTHWEST;
        else if (exitCmd == CMD_NORTHEAST || exitCmd == CMD_NORTHEAST_S)
            return DIR_NORTHEAST;
        else if (exitCmd == CMD_SOUTHWEST || exitCmd == CMD_SOUTHWEST_S)
            return DIR_SOUTHWEST;
        else if (exitCmd == CMD_SOUTHEAST || exitCmd == CMD_SOUTHEAST_S)
            return DIR_SOUTHEAST;
        else if (exitCmd == CMD_UP || exitCmd == CMD_UP_S)
            return DIR_UP;
        else if (exitCmd == CMD_DOWN || exitCmd == CMD_DOWN_S)
            return DIR_DOWN;
        else if (exitCmd == CMD_IN)
            return DIR_IN;
        else if (exitCmd == CMD_OUT)
            return DIR_OUT;
        else
            return 0;
        }
    
    /** Translate a direction flag to a direction label
    */
    public static String toString(int direction)
        {
        if (direction == DIR_NDIR)
            return LABEL_NDIR;
        else if (direction == DIR_SDIR)
            return LABEL_SDIR;
        else if (direction == DIR_WDIR)
            return LABEL_WDIR;
        else if (direction == DIR_EDIR)
            return LABEL_EDIR;
        else if ((direction & DIR_NORTH) != 0)
            return LABEL_NORTH;
        else if ((direction & DIR_SOUTH) != 0)
            return LABEL_SOUTH;
        else if ((direction & DIR_WEST) != 0)
            return LABEL_WEST;
        else if ((direction & DIR_EAST) != 0)
            return LABEL_EAST;
        else if ((direction & DIR_NORTHWEST) != 0)
            return LABEL_NORTHWEST;
        else if ((direction & DIR_SOUTHEAST) != 0)
            return LABEL_SOUTHEAST;
        else if ((direction & DIR_NORTHEAST) != 0)
            return LABEL_NORTHEAST;
        else if ((direction & DIR_SOUTHWEST) != 0)
            return LABEL_SOUTHWEST;
        else if ((direction & DIR_UP) != 0)
            return LABEL_UP;
        else if ((direction & DIR_DOWN) != 0)
            return LABEL_DOWN;
        else if ((direction & DIR_IN) != 0)
            return LABEL_IN;
        else if ((direction & DIR_OUT) != 0)
            return LABEL_OUT;
        else
            return LABEL_UNKNOWN;
        }
        
    /** For a direction, get the corresponding index in the exits table
    */
    private static int getTableIndex(int direction)
        {
        if ((direction & DIR_NORTH) != 0)
            return 0;
        else if ((direction & DIR_SOUTH) != 0)
            return 8;
        else if ((direction & DIR_WEST) != 0)
            return 1;
        else if ((direction & DIR_EAST) != 0)
            return 9;
        else if ((direction & DIR_NORTHWEST) != 0)
            return 2;
        else if ((direction & DIR_SOUTHEAST) != 0)
            return 10;
        else if ((direction & DIR_NORTHEAST) != 0)
            return 3;
        else if ((direction & DIR_SOUTHWEST) != 0)
            return 11;
        else if ((direction & DIR_UP) != 0)
            return 4;
        else if ((direction & DIR_DOWN) != 0)
            return 12;
        else if ((direction & DIR_IN) != 0)
            return 5;
        else if ((direction & DIR_OUT) != 0)
            return 13;
        else
            return -1;
        }

    /** Given a direction, get the direction that is its opposite
    */
    public static int getOppositeDirection(int direction)
        {
        // Swap the bytes of 'direction'
        int hi = (direction << 8) & 0xff00;
        int lo = (direction >> 8) & 0xff;
        return hi | lo;
        }
    }
