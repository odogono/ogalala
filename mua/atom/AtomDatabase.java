// $Id: AtomDatabase.java,v 1.54 1999/07/09 13:29:14 jim Exp $
// Database for storing Atoms
// James Fryer, 8 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import java.io.*;
import com.ogalala.util.*;

/** The Atom Database manages the creation and destruction of atoms. It is a 
    Serializable object which will retain the current state of all the atoms.
*/
public class AtomDatabase
    implements Serializable
    {
    public static final int serialVersionUID = 1;

    /* IDs for the essential atoms
    */
    public static final String ROOT_ID = "root";
    public final static String LIMBO_ID = "limbo";
    public final static String ATOM_ID = "atom";
    public final static String THING_ID = "thing";
    public final static String CONTAINER_ID = "container";
    public final static String MOBILE_ID = "mobile";
    
    /** Back-pointer to World
        <p>
        ### I am not altogether happy about needing this variable... I would
        ###     prefer some other method to enable atoms to know about their 
        ###     context...
    */
    private World world;  //###

    /** A list of all atoms in the database
    */
    protected Dictionary atoms;

    /** The root atom
    */
    private Atom rootAtom;

    /** The limbo container
    */
    private Atom limboContainer;
    
    /** The sequence number used to generate unique item IDs
    */
    private int sequenceNumber;

    /** A placeholder for null property values
    */
    private NullPropertyValue nullPropertyValue;
    
    /** Constants for creating essential atoms
    */
    public final static int ATOM = 0;
    public final static int THING = 1;
    public final static int CONTAINER = 2;
    public final static int MOBILE = 3;
    
    /** Mapping of atom constants to the IDs
    */
    private final static String atomIDs[] = { ATOM_ID, THING_ID, CONTAINER_ID, MOBILE_ID, };
    
    /** Delimiter for export files
    */
    protected static final String EXPORT_DELIMITER = "@@";
    
    /** Create a new database object
    */
    protected AtomDatabase(World world)
        {
        this.world = world;
        sequenceNumber = 0;
        atoms = new Hashtable();
        nullPropertyValue = new NullPropertyValue();
        }

    /** Default constructor, for serialization
    */
    protected AtomDatabase()
        {
        }
        
    /** ### Hack, required to get the system running...
    */
    public void init()
        {
        createCoreAtoms();
        }


    /** Create a new Atom
        <p>
        If ID is null, a unique ID will be generated.
    */
    public final Atom newAtom(String id, Atom parent)
        {
        return _newAtom(id, parent, ATOM);
        }
        
    /** Create a new Atom with multiple parents
        <p>
        If ID is null, a unique ID will be generated.
    */
    public final Atom newAtom(String id, Vector parents)
        {
        Atom result = _newAtom(id, (Atom)parents.elementAt(0), ATOM);
        for (int i = 1; i < parents.size(); i++)
            result.inherit((Atom)parents.elementAt(i));
        return result;
        }
        
    /** Create a new Thing of the appropriate static type for 'parent'
        <p>
        If ID is null, a unique ID will be generated.
    */
    public final Thing newThing(String id, Atom parent)
        {
        // Determine the static type to create
        int atomType = THING;
        if (parent.isDescendantOf(getAtom(AtomDatabase.MOBILE_ID)))
            atomType = MOBILE;
        else if (parent.isDescendantOf(getAtom(AtomDatabase.CONTAINER_ID)))
            atomType = CONTAINER;
            
        return (Thing)_newAtom(id, parent, atomType);
        }
        
    /** Create a new Thing of the of the appropriate static type for the list of parents.
        <p>
        If ID is null, a unique ID will be generated.
    */
    public final Thing newThing(String id, Vector parents)
        {
        // If there is only one parent in the array, shortcut what follows by calling
        //  the single-parent version
        if (parents.size() == 1)
            return newThing(id, (Atom)parents.elementAt(0));
            
        // Find the most demanding parent
        int atomType = THING;
        Atom topParent = null;
		Atom mobile = getAtom(MOBILE_ID);
		Atom container = getAtom(CONTAINER_ID);
		for (int i = 0; i < parents.size(); i++)
    		{
			Atom atom = (Atom)parents.elementAt(i);
			if (atom.isDescendantOf(mobile))
    			{
				topParent = atom;
                atomType = MOBILE;
				
				// this is the most demanding static type,
				// so there's no point in continuing
				break;
    			}
			else if (atom.isDescendantOf(container))
				{
				topParent = atom;
                atomType = CONTAINER;
                }
		    }
        
        Thing result = (Thing)_newAtom(id, topParent, atomType);
		for (int i = 0; i < parents.size(); i++)
            {
			Atom atom = (Atom)parents.elementAt(i);
            if (topParent != atom)
                result.inherit(atom);
            }
            
        return result;
        }
        
    /** Create a new Atom with the static type specified by 'atomType'.
    */
    private Atom _newAtom(String id, Atom parent, int atomType)
        {
        // Get the parent atom if none specified
        if (parent == null)
            parent = getDefaultParent(atomType);
            
        // Ensure that 'parent' is a static Atom
        else if (parent instanceof Thing)
            throw new AtomException("Atom cannot inherit from " + parent.getClassName());

        // If the ID is null, generate a unique ID to use
        if (id == null)
            id = getUniqueID(parent.getID());
    
        // Else, ensure case insensitivity and validity
        else {
            //### I would prefer to be case-preserving -- impossible with Hashtable
            id = id.toLowerCase();
         
            // Check the ID
            if (!Atom.isValidID(id))
                throw new AtomException("Invalid atom ID: " + id);
                
            // Check it doesn't exist
            else if (getAtom(id) != null)
                throw new AtomException("Atom exists: " + id);
            }
            
        // Create the new Atom
        Atom result = null;
        switch (atomType)
            {
        case ATOM:
            result = new Atom(world, parent, id);
            break;
        case THING:
            result = new Thing(world, parent, id);
            break;
        case CONTAINER:
            result = new Container(world, parent, id);
            break;
        case MOBILE:
            result = new Mobile(world, parent, id);
            break;
        default:
            throw new AtomException("AtomDatabase: Bad atom type passed to _newAtom: " + atomType);
            }
            
        // Add the atom to the database
        addAtom(result);

        return result;
        }

    /** Get the default parent for an atom class
    */
    protected Atom getDefaultParent(int atomType)
        {
        if (atomType <= ATOM || atomType > MOBILE)
            return getRoot();
        else
            return getAtom(atomIDs[atomType]);
        }
        
    /** Get the type number for an atom
    */
    protected int getAtomType(Atom atom)
        {
        if (atom instanceof Mobile)
            return MOBILE;
        else if (atom instanceof Container)
            return CONTAINER;
        else if (atom instanceof Thing)
            return THING;
        else
            return ATOM;
        }
        
    /** Get the type number for an atom class name. Defaults to ATOM.
    */
    protected int getAtomType(String className)
        {
        for (int i = 0; i < atomIDs.length; i++)
            {
            if (className.equalsIgnoreCase(atomIDs[i]))
                return i;
            }
        return ATOM;
        }
        
    /** Create a copy of an existing Atom
        <p>
        The new atom has the same fields and inheritance as the existing one,
        but is created in Limbo and if it is a container then it will be created
        empty.
    */
    public Atom cloneAtom(Atom atom)
        {
        // Get the first parent
        Enumeration parents = atom.getParents();
        Atom parent = (Atom)parents.nextElement();
        
        // Make an atom of the same static class
        Atom result = _newAtom(null, parent, getAtomType(atom));
        
        // Inherit from the original's parents
        while (parents.hasMoreElements())
            result.inherit((Atom)parents.nextElement());
        
        // Copy the fields
        Enumeration names = atom.getFieldNames();
        Enumeration values = atom.getFieldValues();
        while (names.hasMoreElements())
            result.setField(names.nextElement().toString(), values.nextElement());
        
        return result;
        }
        
    /** Add an atom to the database
    */
    private void addAtom(Atom atom)
        {
        atoms.put(atom.getID(), atom);
        }

    /** Get an existing Atom
    */
    public Atom getAtom(String id)
        {
        // Ensure case insensitivity
        //### I would prefer to be case-preserving -- impossible with Hashtable
        id = id.toLowerCase();
        
        return (Atom)atoms.get(id);
        }

    /** Delete an atom from the database
    */
    public void deleteAtom(Atom atom)
        {
        // Can't delete root, frozen atoms or limbo
        if (atom.isRoot())
            throw new AtomException("Can't delete root atom");
        else if (atom.isFrozen())
            throw new AtomException("Can't delete frozen atom");
        else if (atom.isLimbo())
            throw new AtomException("Can't delete limbo container");
            
        atom.unlink();
        atoms.remove(atom.getID());
        }

    /** Enumerate the atoms
    */
    public Enumeration getAtoms()
        {
        return atoms.elements();
        }

    /** Get the atom at the root of the inheritance hierarchy
    */
    public Atom getRoot()
        {
        return rootAtom;
        }

    /** Create the "inner core" atoms: root, thing, container, mobile, limbo
    */
    private void createCoreAtoms()
        {
        // Create the root atom
        rootAtom = new RootAtom(world);     //### 'world'...
        addAtom(rootAtom);
        
        // Create thing, container, mobile
        Atom thing = newAtom(THING_ID, rootAtom);
        Atom container = newAtom(CONTAINER_ID, thing);
        newAtom(MOBILE_ID, container);
        
        // Create limbo and set it to contain itself
        limboContainer = newThing(LIMBO_ID, container);
        Container limbo = (Container)limboContainer;
        limbo.setContainer(limbo);
        }
        
    /** Are the required atoms present?
    */
    protected boolean checkCoreAtoms()
        {
        return getAtom(ROOT_ID) != null && 
                getAtom(THING_ID) != null && 
                getAtom(CONTAINER_ID) != null && 
                getAtom(MOBILE_ID) != null && 
                getAtom(LIMBO_ID) != null;
        }
        
    /** Get the "limbo" container
    */
    public Atom getLimbo()
        {
        return limboContainer;
        }

    /** Get the value used to indicate null properties
    */
    public final Object getNullPropertyValue()
        {
        return nullPropertyValue;
        }

    /** Create an ID unique in the database, prefixed with the 'base' argument
    */
    public String getUniqueID(String base)
        {
        while (true)
            {
            String result = base + sequenceNumber;
            sequenceNumber++;
            if (atoms.get(result) == null)
                return result;
            }
        }
        
    /**
	* Returns the number of atoms currently in the atom database
	*/
	public int size()
		{
		return atoms.size();
		}

    /** Freeze all atoms in the database
    */
    public void freeze()
        {
        //### In this implementation we simply set all atoms' frozen
        //###   codes to -1. In future the frozen code will be used as
        //###   an index into a table of parents for all non-leaf atoms.
        Enumeration atoms = getAtoms();
        while (atoms.hasMoreElements())
            {
            Atom atom = (Atom)atoms.nextElement();
            atom.frozen = -1;
            }
        }
    
    /** Export the dynamic state of the database -- all non-frozen atoms
    */
    protected final void exportState(PrintWriter out)
        {
        Enumeration atoms = getRoot().getDescendants();
        while (atoms.hasMoreElements())
            {
            Atom atom = (Atom)atoms.nextElement();
            if (!atom.isFrozen())
                out.println(atom.toExportFormat());
            }
        out.println(EXPORT_DELIMITER);
        }

    /** Import a collection of atoms
    */
    protected final void importState(BufferedReader in)
        throws IOException
        {
        // Create the importer and add the templates
        AtomImporter importer = new AtomImporter(world);
        while (true)
            {
            String s = in.readLine();
            if (s == null || s.equals(EXPORT_DELIMITER))
                break;
            importer.addTemplate(s);
            }
        
        // Create the atoms and resolve the fields
        importer.makeAtoms();
        importer.resolveAtoms();
        }
    }

/** Placeholder class for null property values
*/
class NullPropertyValue
    implements Serializable
    {
    public String toString()
        {
        return "null";
        }
    }
