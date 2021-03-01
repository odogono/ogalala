// $Id: Container.java,v 1.36 1999/04/21 19:21:52 jim Exp $
// An container for Things
// James Fryer, 1 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** A Container is a Thing that can contain other Things.
    <p>
*/
public class Container
    extends Thing
    {
    private static final long serialVersionUID = 1;
    
    /** The list of contained objects
    */
    private Vector contents;
    
    /** The exit table
    */
    private ExitTable exitTable;
    
    /** Property names
    */
    public static final String DIRECTION = "direction";
    
    /** Create a new Container
    */
    Container(World world, Atom parent, String id)
        {
        super(world, parent, id);
        
        // Create contents list
        contents = new Vector();
        }

    /** Send an Output Packet to all accessible contained atoms
    */
    public void output(OutPkt out)
        {
        // Get the expanded string
        String outMsg = expandOutPkt(out);

        // Send output to the container itself
        this.doOutput(outMsg);
        
        // Send the message to the contained atoms
        Enumeration enum = newOutputEnumeration();
        while (enum.hasMoreElements())  
            {
            Thing thing = (Thing)enum.nextElement();
            thing.doOutput(outMsg);
            }
        }

    /** Send an Output Packet to all accessible contained atoms except 'missOut'.
        <p>
        This function is for use when sending output to the things in a
        container, except the actor.
    */
    public void output(OutPkt out, Atom missOut)
        {
        // Get the expanded string
        String outMsg = expandOutPkt(out);

        // Send output to the container itself
        this.doOutput(outMsg);
        
        // Send the message to the contained atoms
        Enumeration enum = newOutputEnumeration();
        while (enum.hasMoreElements())  
            {
            Thing thing = (Thing)enum.nextElement();
            if (thing != missOut)
                thing.doOutput(outMsg);
            }
        }

    /** Send an Output Packet to all accessible contained atoms except 
        'missOut1' and 'missOut2'.
        <p>
        This function is for use when sending output to the things in a
    *    container, except the actor and the second party. For example
    *    the WHISPER and GIVE commands.
    */
    public void output(OutPkt out, Atom missOut1, Atom missOut2)
        {
        // Get the expanded string
        String outMsg = expandOutPkt(out);

        // Send output to the container itself
        this.doOutput(outMsg);
        
        // Send the message to the contained atoms
        Enumeration enum = newOutputEnumeration();
        while (enum.hasMoreElements())  
            {
            Thing thing = (Thing)enum.nextElement();
            Atom atom = (Atom)enum.nextElement();
            if (thing != missOut1 && thing != missOut2)
                thing.doOutput(outMsg);
            }
        }
        
    /** 
    *	Get an enumeration of all accessible atoms that will receive output
    *   when it is sent to this container.
    *	The atoms include all items that exist within a container, along with
    *	any items that are in open containers.
    */
    private Enumeration newOutputEnumeration()
        {
        return new ListenEnumeration( this );
        }
        
    /** Get the thing's static class name
    */
    public String getClassName()
        {
        return "Container";
        }

    /** Get a system field. 
    */
    protected Object getSystemField(String name)
        {
        if ("contents".equals(name))
            return contents;
        if ("deep_contents".equals(name))
            return AtomData.enumToVector(getDeepContents());
        else if ("is_empty".equals(name))
            return new Boolean(isEmpty());
        else if ("count".equals(name))
            return new Integer(getCount());
        else {
            // Look for a direction, else pass to super
            int direction = ExitTable.toDirection(name);
            Atom exit;
            if (exitTable != null && direction != 0 && 
                    (exit = exitTable.getExit(direction)) != null)
                return exit;
            else
                return super.getSystemField(name);
            }
        }
        
    /** Enumerate the contents
    */
    public Enumeration getContents()
        {
        return contents.elements();
        }

    /** Recursively enumerate the contents of this container and all sub-containers
    */
    public Enumeration getDeepContents()
        {
        return new ContentsEnumeration(this);
        }

    /** Is 'atom' contained in this container?
    */
    public boolean contains(Atom atom)
        {
        return Atom.enumContains(getContents(), atom);
        }

    /** Is 'atom' contained in this container or one of its sub-containers?
    */
    public boolean containsDeep(Atom atom)
        {
        return Atom.enumContains(getDeepContents(), atom);
        }

    /** Put an atom in the container
    */
    public void putIn(Atom atom)
        {
        add(atom);
        }

    /** Add a thing to the contents
    */
    private void add(Atom atom)
        {
        // Only Things can be added
        if (!(atom instanceof Thing))
            throw new AtomException("Can't add Atoms to container.");
        Thing thing = (Thing)atom;
            
        // Avoid containing null objects
        if (thing == null)
            throw new NullPointerException("Can't place <null> in container.");
            
        // Can't contain itself
        else if (this == thing)
            throw new AtomException("Container can't be placed inside itself.");
        
        // If 'thing' is also a container, extra error possibilities arise
        else if (thing instanceof Container)
            {
            Container container = (Container)thing;
            
            // Can't move Limbo to a different container
            if (container.isLimbo())
                throw new AtomException("Limbo container cannot be moved");
        
            // Can't contain cyclically. If the contents is null, 
            //  the container is just being created, so ignore
            else if (container.contents != null && container.containsDeep(this))
                throw new AtomException("Can't contain cyclically");
            }
            
        // Remove the thing from its current container
        Container oldContainer = (Container)thing.getContainer();
        if (oldContainer != null)
            oldContainer.removeThing(thing);
        thing.setContainer(null);
        
        // Add the thing to this container
        addThing(thing);
        thing.setContainer(this);
        }
        
    /** Low-level add function
    */
    private void addThing(Thing thing)
        {
        contents.addElement(thing);
        }
        
    /** Low-level remove function
        <p>
        Called (1) when a Thing is moved between containers; (2) when a Thing
        is deleted.
    */
    void removeThing(Thing thing)
        {
        contents.removeElement(thing);
        }
        
    /** How many items are stored in this container?
    */
    public int getCount()
        {
        return contents.size();
        }
        
    /** Is the container empty?
    */
    public boolean isEmpty()
        {
        return contents.isEmpty();
        }
        
    /** Get an exit
        
        @return An exit atom or null if no exit corresponds to the direction
    */
    public Atom getExit(int direction)
        {
        if (exitTable == null)
            return null;
        else
            return exitTable.getExit(direction);
        }
        
    /** Add an exit to the container and exit table
    */
    public void addExit(int direction, Atom exit)
        {
        // Check the exit object is an Atom descended from 'exit'.
        if (exit instanceof Thing)
            {
            if (!exit.isDescendantOf(world.getAtom("exit")))
                throw new AtomException("Exits must be descendants of the exit atom");
            }
        else
            throw new AtomException("Not a valid exit object");
        
        // If this container doesn't have an ET, create one
        if (exitTable == null)
            exitTable = new ExitTable();

        // Set the exit's 'direction' property if it is not already set as a field
        Object directionField = exit.getField(DIRECTION);
        if (directionField == null || AtomData.isNullProperty(directionField))
            exit.setProperty(DIRECTION, ExitTable.toString(direction));

        // Add the exit to the container and the exit table
        putIn(exit);
        exitTable.addExit(direction, exit);
        }
        
    /** Remove an exit
    */
    public void removeExit(Atom exit)
        {
        if (exitTable != null)
            {
            // Remove the exit from the exit table
            exitTable.removeExit(exit);
            
            // If the exit table is now empty, remove it
            if (exitTable.isEmpty())
                exitTable = null;
                
            // If the exit object was an atom, move it to limbo
            //###??? I need to think about this... This is handled properly
            //###       in World, so it is probably OK to leave it here.
            }
        }
        
    /** Convert to export format.
        <p>
        Adds the exit table if there is one.
    */
    protected String toExportFormat()
        {
        String result = super.toExportFormat();
        if (exitTable != null)
            {
            StringBuffer buf = new StringBuffer(result);
            buf.append(AtomDatabase.EXPORT_DELIMITER);
            buf.append(exitTable.toExportFormat());
            result = buf.toString();
            }
        return result;
        }
        
    /** Move the container's contents to Limbo when it is being deleted.
    */
    protected void unlink()
        {
        // Make a copy of 'contents' so we don't have problems as we delete items from it
        Vector v = (Vector)contents.clone();
        
        // Get Limbo and move the contents there
        Atom limbo = world.getLimbo();
        Enumeration enum = getContents();
        try {
            //### Exits???
            while (enum.hasMoreElements())
                limbo.putIn((Atom)enum.nextElement());
            }
        catch (AtomException e)
            {
            //### Not sure what to do about errors here...
            }
            
        super.unlink();
        }

    /** Test this atom for validity.
        @return true if this is a valid atom.
    */
    public boolean invariant()
        {
        // All contained items must have 'this' as container
        Enumeration enum = getContents();
        while (enum.hasMoreElements())
            {
            Thing thing = (Thing)enum.nextElement();
            if (thing.getContainer() != this)
                return false;
            }
            
        return super.invariant();
        }
     }
