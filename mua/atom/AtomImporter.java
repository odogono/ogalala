// $Id: AtomImporter.java,v 1.3 1999/07/09 13:29:14 jim Exp $
// Import atoms from dynamic state file
// James Fryer, 21 April 99
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Utility class for importing dynamic state
*/
final class AtomImporter
    {
    /** Back-pointer to world
    */
    private World world;
    
    /** The list of un-fixed-up atom templates
    */
    private Vector atomTemplates;
    
    AtomImporter(World world)
        {
        this.world = world;
        atomTemplates = new Vector();
        }
    
    /** Add a template to the list
    */
    public void addTemplate(String s)
        {
        atomTemplates.addElement(new AtomTemplate(s));
        }
        
    /** Create the atoms, without setting their properties
    */
    public void makeAtoms()
        {
        Enumeration enum = atomTemplates.elements();
        while (enum.hasMoreElements())
            {
            AtomTemplate template = null;
            try {
                template = (AtomTemplate)enum.nextElement();
                template.makeAtom();
                }
            catch (RuntimeException e)
                {
                //### Should be World.warning
                System.out.println("AtomImporter: Can't make atom: " + template.id);
                Debug.printStackTrace(e);
                }
            }
        }
        
    /** Set the fields of the atoms.
        <p>
        This is done separately so that any atoms referred to in properties will
        be created before they are referenced
    */
    public void resolveAtoms()
        {
        Enumeration enum = atomTemplates.elements();
        while (enum.hasMoreElements())
            {
            AtomTemplate template = null;
            try {
                template = (AtomTemplate)enum.nextElement();
                template.resolveAtom();
                }
            catch (RuntimeException e)
                {
                //### Should be World.warning
                System.out.println("AtomImporter: Can't resolve atom: " + template.id);
                Debug.printStackTrace(e);
                }
            }
        }
        
    /** This inner class represents an atom which has been read from the import file
        
    */
    class AtomTemplate
        {
        /** The class name of the atom
        */
        private String className;

        /** The ID of the atom
        */
        private String id;

        /** The IDs of the atom's parents
        */
        private Vector parentIDs;

        /** The atom's fields, in un-fixed-up form (i.e. each field is a string,
            not a resolved property)
        */
        private Dictionary fields;
        
        /** The IDs of the container -- null for Atoms
        */
        private String containerID;

        /** The exit table -- null if not present
        */
        private Dictionary exits;

        /** The atom created by this template
        */
        private Atom atom;
        
        /** Create a template from a string. The string is in the format:
            <p>
            ClassName ID parents fields [ container ]
            <p>
            The above fields are separated by AtomDatabase.EXPORT_DELIMITER.
        */
        AtomTemplate(String s)
            {
            // Class name, ID
            StringTokenizer tzr = new StringTokenizer(s, AtomDatabase.EXPORT_DELIMITER);
            className = tzr.nextToken();
            id = tzr.nextToken();
            
            // Parents
            // (Note we remove the first character of the string -- the opening bracket --
            //  before sending it to the table parser.)
            String s2 = tzr.nextToken();
            s2 = s2.substring(1);
            TableParser parser = new TableParser(s2);
            parentIDs = (Vector)parser.getParsed(TableParser.NO_CONVERSION);
            
            // Fields
            s2 = tzr.nextToken();
            s2 = s2.substring(1);
            parser = new TableParser(s2);
            fields = new Hashtable();
    		parser.parse(fields, false, false, Integer.MAX_VALUE, TableParser.TO_LOWER_CASE);

    		// Remove the dummy entry from the fields
	        fields.remove("");
            
            // Container
            if (tzr.hasMoreTokens())
                containerID = tzr.nextToken();
            else
                containerID = null;
            
            // Exit table -- optionally present in containers
            if (tzr.hasMoreTokens())
                {
                s2 = tzr.nextToken();
                s2 = s2.substring(1);
                parser = new TableParser(s2);
                exits = new Hashtable();
        		parser.parse(exits, false, false, Integer.MAX_VALUE, TableParser.TO_LOWER_CASE);
                }
            else
                exits = null;
            }
            
        /** Make the atom
        */
        void makeAtom()
            {
            // Get the database, the integer atom type and the parents
            AtomDatabase database = world.getAtomDatabase();
            int atomType = database.getAtomType(className);
            Vector parents = getParents();
            
            // Create the Atom/Thing to return. Note that we create with the 
            //  database because we don't want ON_CREATE to be sent to the atom;
            //  it has been created already, we are reconstituting it here.
            if (atomType == database.ATOM)
                atom = database.newAtom(id, parents);
            else 
                atom = database.newThing(id, parents);
            }
        
        /** Convert the parent IDs into atoms
        */
        private Vector getParents()
            {
            Vector result = new Vector();
            Enumeration enum = parentIDs.elements();
            while (enum.hasMoreElements())
                {
                String parentID = enum.nextElement().toString();
                result.addElement(world.getAtom(parentID));
                }                
            return result;
            }
            
        /** Resolve the atom's containment, fields and exits.
            <p>
            This is done separately from atom creation to ensure atoms 
            exist before they are referenced as properties.
        */
        void resolveAtom()
            {
            resolveFields();
            resolveContainment();
            resolveExits();
            }
        
        private void resolveFields()
            {
            Enumeration keys = fields.keys();
            Enumeration values = fields.elements();
            while (keys.hasMoreElements())
                {
                String key = keys.nextElement().toString();
                Object value = resolveField(values.nextElement());
                atom.setField(key, value);
                }
            }
        
        /** Resolve an individual field as read in from the state file. This can
            be a string, a Dictionary or a Vector.
        */
        private Object resolveField(Object field)
            {
            // If the field is a dictionary or vector, we recursively parse its contents
            if (field instanceof Dictionary)
                return resolveDictionary((Dictionary)field);
            else if (field instanceof Vector)
                return resolveVector((Vector)field);
                
            // Any other field is treated as a string and parsed with AtomData
            else
                return AtomData.parse(field.toString(), world);
            }
        
        /** Create a new Dictionary with resolved fields
        */
        private Dictionary resolveDictionary(Dictionary d)
            {
            Dictionary result = new Hashtable();
            Enumeration keys = d.keys();
            Enumeration values = exits.elements();
            while (keys.hasMoreElements())
                {
                String key = keys.nextElement().toString();
                Object field = values.nextElement();
                field = resolveField(field);
                result.put(key, field);
                }
            return result;
            }
        
        /** Create a new vector with resolved fields
        */
        private Vector resolveVector(Vector v)
            {
            Vector result = new Vector();
            result.setSize(v.size());
            for (int i = 0; i < v.size(); i++)
                {
                Object field = v.elementAt(i);
                field = resolveField(field);
                result.setElementAt(field, i);
                }
            return result;
            }

        private void resolveContainment()
            {
            if (containerID != null)
                {
                Atom container = world.getAtom(containerID);

                // If the container is frozen and the atom is an exit, we need special handling.
                //  (this is why we do containment after fields!)
                if (container.isFrozen() && atom.isDescendantOf(world.getAtom("exit")))
                    {
                    String directionLabel = atom.getString("direction");
                    Atom destination = atom.getAtom("destination");
                    container.addExit(ExitTable.toDirection(directionLabel), atom);
                    }
                else
                    container.putIn(atom);
                }
            }

        private void resolveExits()
            {
            if (exits != null)
                {
                Enumeration keys = exits.keys();
                Enumeration values = exits.elements();
                while (keys.hasMoreElements())
                    {
                    String directionLabel = keys.nextElement().toString();
                    String destinationID = values.nextElement().toString();
                    atom.addExit(ExitTable.toDirection(directionLabel), world.getAtom(destinationID));
                    }
                }
            }
        }
    }
