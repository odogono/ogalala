// $Id: Atom.java,v 1.73 1999/04/21 14:29:25 jim Exp $
// Atoms: fundamental type for multi-user adventure games
// James Fryer, 8 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** The Atom is the basic unit in the type hierarchy.
    <p>
    Atom's public routines fall into the following groups:
    <ul>
    <li><b>Parents and children</b>: Atoms form a multiple-inheritance
        type hierarchy. Each atom has a list of parents and children.
        Parents can be added to at run time. (Children are added as a side-
        effect of having parents.)

    <li><b>Properties and fields</b>: Atoms have (or are) a table of named
        fields. These can be set, got and removed as usual. In addition the
        <code>getProperty</code> function looks for a field first in the
        current object, then in the parents of that object. Additional 
        utility functions are provided to enable different field types
        to be set and retrieved.

    <li><b>Marking</b>: Each atom has a "marked" flag. This is used to
        mark atoms as visited in traversals.

    </ul>
    <p>
    (Thanks to Richard Bartle for many of the algorithms used here.)

*/
public class Atom
    implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;
    
    /** Each Atom has a unique ID
    */
    private String id;

    /** Back-pointer to world
    */
    protected World world;

    /** Table of fields
    */
    private Dictionary fields;

    /** Parents of this atom
    */
    private Object parents;

    /** Children of this atom
    */
    private AtomVector children;
    
    /** The depth and height of this atom, used for inheritance lookup.
        <p>
        An atom's parents are sorted by their depth and height, such that
        the parent with the greatest depth is searched first. If depths
        are equal, the parent with the greatest height is searched first.
        <p>
        Access functions are provided for these variables.
        <p>
        The depth and height of all atoms is recalculated when the inheritance
        hierarchy changes in the following ways:
        <ul>
        <li> When a parent is added to an atom with one or more parents
        <li> ### ...
        </ul>
        So inheritance is a potentially expensive operation.
    */
    private short depth;
    private short height;

    /** Frozen atoms cannot have their inheritance structure modified. If this 
        variable is non-zero, the atom is frozen.
    */
    protected short frozen = 0;

    /** The marked flag for this atom.
    */
    private transient int marked;

    /** This is the value of 'marked' in a marked atom
    */
    private static int markedValue = 0;
    
    /** Has this atom been deleted?
    */
    private boolean deleted = false;

    /** Property name to check if a container is closed
    */
    public static final String IS_CLOSED = "is_closed";

    /** Create a new Atom
    */
    protected Atom(World world, Atom parent, String id)
        {
        this.world = world;
        this.id = id;
        fields = new Hashtable();
        depth = 0;
        height = 0;
        
        // The constructor will throw if 'parent' is not a static Atom. This will
        //  leave the database in an inconsistent state so it is advisable to test
        //  for this before creating the atom.
        inherit(parent);
        }

    /** Get the atom's ID
    */
    public final String getID()
        {
        return id;
        }

    /** Is this the root atom?
    */
    public boolean isRoot()
        {
        return false;
        }

    /** Get the atom's static class name
    */
    public String getClassName()
        {
        return "Atom";
        }
    
    /** Remove the atom from any data structures it belongs to when it is 
        being deleted.
        <p>
        Specifically, the inheritance and containment hierarchies must be 
        adjusted. The 'deleted' flag is set by this function.
        <p>
        Subclasses must call 'super.unlink()'.
    */
    protected void unlink()
        {
        removeParentsAndChildren();
        deleted = true;
        }
        
// Inheritance
    
    /** Enumerate the direct parents of this atom
    */
    public final Enumeration getParents()
        {
        // If the parents are in a vector, return its enum
        if (parents instanceof AtomVector)
            return ((AtomVector)parents).elements();

        // Else, if the parent is not null, return a single element enum
        else if (parents != null)
            return new SingleElementEnumeration(parents);

        // Else, the parent is null, return an iterator on the root atom
        else
            return new SingleElementEnumeration(world.getRoot());
        }

    /** Enumerate all ancestors of this atom
    */
    public final Enumeration getAncestors()
        {
        return new AtomAncestorEnumeration(this);
        }

    /** Has this atom got any children?
    */
    public final boolean hasChildren()
        {
        return children != null;
        }

    /** Enumerate the direct children of this atom
    */
    public final Enumeration getChildren()
        {
        // If the children are in a vector, return the enumeration from that
        if (children instanceof AtomVector)
            return ((AtomVector)children).elements();

        // Else, return a (possibly empty) single element enum
        else
            return new SingleElementEnumeration(children);
        }

    /** Enumerate all descendants of this atom
    */
    public final Enumeration getDescendants()
        {
        return new AtomDescendantEnumeration(this);
        }

    /** Inherit from 'newParent'.
        <p>
        Note that an atom cannot inherit from itself, nor from an atom
        which is one of its descendants.
    */
    public final void inherit(Atom newParent)
        {
        // Ignore null parameters
        if (newParent == null)
            return;
        
        // The root atom never has parents (except itself)
        if (this.isRoot())
            {
            if (newParent.isRoot())
                return;
            else
                throw new AtomException("Root atom cannot have parents");
            }
            
        // Frozen atoms can't inherit
        else if (this.isFrozen())
            throw new AtomException("Frozen atoms cannot inherit");
            
        // Atoms can't inherit themselves
        else if (newParent == this)
            throw new AtomException("Atom cannot inherit from itself");
            
        // Avoid cyclic inheritance
        else if (newParent.isDescendantOf(this))
            throw new AtomException("Atom cannot inherit from its descendant");
        
        // Can only inherit from Atoms
        else if (newParent instanceof Thing)
            throw new AtomException("Atom cannot inherit from " + newParent.getClassName());
        
        // Add the parent to the parents list
        this.addParent(newParent);

        // Add the current atom to the parent's child list
        newParent.addChild(this);

        // Rebalance the DAG, if necessary
        //###
        new AtomSorter(world.getRoot());
        }

    /** Remove the inheritance relationship between this atom and 'parent'.
    */
    public final void uninherit(Atom parent)
        {
        uninherit(parent, true);
        }

    /** Remove the inheritance relationship between this atom and 'parent'.
        <p>
        The 'preventOrphans' flag, when false, enables atoms to be removed from the
        inheritance hierarchy altogether.
    */
    private final void uninherit(Atom parent, boolean preventOrphans)
        {
        // Frozen atoms can't uninherit
        if (this.isFrozen())
            throw new AtomException("Frozen atoms cannot uninherit");
            
        // Ignore null parameters
        if (parent == null)
            return;

        // If this atom is the root, do nothing. The root has no parents (except itself)
        if (this.isRoot())
            return;

        // Remove the current atom from the parent's child list
        parent.removeChild(this);

        // Remove the parent from the parents list
        this.removeParent(parent);

        // If this atom is now an orphan, and orphans are not allowed, it must inherit from the root
        Atom root = world.getRoot();
        if (parents == null && preventOrphans)
            {
            // There should not be an error when inheriting from root, so ignore exceptions
            try {
                this.inherit(root);
                }
            catch (AtomException e)
                { }
            }

        // Reorder the DAG, if necessary
        //###
        new AtomSorter(root);
        }
    
    /** Remove this atom from the inheritanvce hierarchy (used when deleting)
    */
    private final void removeParentsAndChildren()
        {
        // Copy the children to a vector. This prevents the enumeration being
        //  corrupted when we remove elements from its underlying data structure
        Vector v = new Vector();
        Enumeration children = getChildren();
        while (children.hasMoreElements())
            v.addElement(children.nextElement());

        // Remove all children
        for (int i = 0; i < v.size(); i++)
            {
            Atom child = (Atom)v.elementAt(i);
            child.uninherit(this);
            }

        // Copy the parents to a vector
        v.setSize(0);
        Enumeration parents = getParents();
        while (parents.hasMoreElements())
            v.addElement(parents.nextElement());

        // Remove all parents, allowing this atom to become orphaned
        for (int i = 0; i < v.size(); i++)
            {
            Atom parent = (Atom)v.elementAt(i);
            this.uninherit(parent, false);
            }

        // The atom is now an orphan and can be deleted
        //### Is this the best way to handle this?
        }

    /** Add a child to this atom
        <p>
        For efficiency, the 'children' member variable has the following meanings:
        <ul>
        <li> Null: There are no children
        <li> Other: A Vector containing the children
        </ul>
        This is based on the assumption that if an atom has one child, it is
        likely to have more.
    */
    private final void addChild(Atom newChild)
        {
        // If there are no children, create a vector to keep them in
        if (children == null)
            children = new AtomVector();

        // Else, if the new child is already in the list, do nothing
        else if (children.contains(newChild))
            return;

        // Add the child to the vector
        children.addElement(newChild);
        }

    /** Remove a child from this atom
    */
    private final void removeChild(Atom oldChild)
        {
        // If there are no children, return
        if (children == null)
            return;

        // Find the child in the children array
        int i = children.indexOf(oldChild);

        // If it's not present, return
        if (i < 0)
            return;

        // Remove the child from the array
        children.remove(i);

        // If the array is empty, set it to null
        if (children.isEmpty())
            children = null;
        }

    /** Add a parent to this Atom
        <p>
        Note that in a (probably unwise) attempt to be efficient, we store parents
        in a vector only if there is one or more of them. So the meaning of the
        'parents' member variable is as follows:
        <ul>
        <li> Non-Vector: One parent, the object itself
        <li> Vector: Multiple parents, stored in the vector
        </ul>
        The above is based on the assumption that there will be many atoms with only
        one parent, so it is worth optimising this case.
    */
    private final void addParent(Atom newParent)
        {
        // If 'parents' is null, simply add the atom as the only parent
        if (parents == null)
            parents = newParent;

        // Else, if the new parent is already present, do nothing
        else if (parents == newParent)
            return;

        // Else, determine whether the vector needs to be created.
        else {
            // If 'parents' is not a vector, create the vector and add the existing parent to it
            if (!(parents instanceof AtomVector))
                {
                AtomVector v = new AtomVector();
                v.put((Atom)parents);
                parents = v;
                }

            // Add the new element to the vector, if it is not already present.
            AtomVector v = (AtomVector)parents;
            if (!v.contains(newParent))
                v.put(newParent);
            }
        }

    /** Remove a parent from this atom
        <p>
        If the result of this leaves the 'parents' variable with a null value,
        the atom has been "orphaned" and this will have to be fixed up by the
        caller.
    */
    private final void removeParent(Atom oldParent)
        {
        // If there are no parents, do nothing.
        if (parents == null)
            return;

        // Else, if there is only one parent, and it is the one we have been asked to remove,
        //  set the parent to null. The caller will have to ensure that this atom gets
        //  linked back in to the inheritance hierarchy.
        if (!(parents instanceof AtomVector))
            {
            if (parents == oldParent)
                parents = null;
            }

        // Else, this atom has more than one parent, remove 'oldParent' from the vector.
        else {
            // Get the vector and find the parent to remove
            AtomVector v = (AtomVector)parents;
            int i = v.indexOf(oldParent);

            // If the parent to remove is in the vector
            if (i >= 0)
                {
                // Remove the parent
                v.remove(i);

                // If the vector now has one element, replace the vector with the element itself
                if (v.size() == 1)
                    parents = v.get(0);

                // If the vector now has no elements, set it to null. Again, the caller will handle orphans.
                else if (v.isEmpty())
                    parents = null;
                }
            }
        }

    /** Is the current atom a descendant of 'atom'?
    */
    public final boolean isDescendantOf(Atom atom)
        {
        // If the atoms are the same, return true
        if (this == atom)
            return true;
            
        // Else go through the descendants, looking for a match
        Enumeration enum = atom.getDescendants();
        while (enum.hasMoreElements())
            {
            if (this == enum.nextElement())
                return true;
            }
        return false;
        }

    /** Is this atom frozen?
        <p>
        Frozen atoms cannot have parents added or removed.
    */
    public boolean isFrozen()
        {
        return frozen != 0;
        }

// Properties
    
    /** Get a property.
        <p>
        The value returned could be defined in this object, or in one of its 
        ancestors.
        <p>
        If the property value is an Action, it will be called and the result 
        returned.
        <p>
        Null and nonexistent properties will be returned as null values.
    */
    public final Object getProperty(String name)
        {
        // Get the raw property
        Object result = getRawProperty(name);
        
        // Try to cook it
        result = AtomData.cookProperty(this, name, result);
            
        return result;
        }
        
    /** Get an unprocessed property.
        <p>
        Action values will be returned without calling them.
        <p>
        Null properties will be returned as a reference to AtomData.nullValue.
        Use AtomData.isNullProperty to identify such values.
        <p>
        If the property doesn't exist, null is returned.
    */
    public final Object getRawProperty(String name)
        {
        // Look for the property in this atom
        Object result = getField(name);
        if (result != null)
            return result;
        
        // Look for the property in the ancestors
        else
            return getPrecursorProperty(name);
        }
    
    /** Get the "precursor" property, that is the value of the property 
        defined in an ancestor atom.
        <p>
        The property is returned in a "raw" state.
    */
    public final Object getPrecursorProperty(String name)
        {
        // Ensure case insensitivity
        //### I would prefer to be case-preserving -- impossible with Hashtable
        //### Also note there is excessive calling of this function because of the multiple 
        //###   entry points.
        name = name.toLowerCase();

        // If this is the root atom, there is no precursor, so stop here
        if (isRoot())
            return null;

        // Get the ancestors and look for the property there
        Enumeration ancestors = getAncestors();
        while (ancestors.hasMoreElements())
            {
            Atom atom = (Atom)ancestors.nextElement();
            Object result = atom.getDynamicField(name);
            if (result != null)
                return result;
            }

        return null;
        }
    
    /** Get the atom which defines the precursor to a property. That is, if the 
        property 'a.p' is defined on the atom 'a1', this function will return the 
        ancestor 'an' which defines a precursor to 'p'. If the property has no 
        precursor, null is returned.
    */
    public final Atom getPrecursorAtom(String name)
        {
        // Ensure case insensitivity
        //### I would prefer to be case-preserving -- impossible with Hashtable
        name = name.toLowerCase();

        // If this is the root atom, there is no precursor, so stop here
        if (isRoot())
            return null;
            
        // If the property is defined in this atom, the precursor will be the
        //  first atom which defines the property. If it is not defined here,
        //  we need to skip the first definition of the property in the ancestors.
        boolean skipFirst = (getDynamicField(name) == null);

        // Get the ancestors and look for the property there
        Enumeration ancestors = getAncestors();
        while (ancestors.hasMoreElements())
            {
            Atom result = (Atom)ancestors.nextElement();
            Object value = result.getDynamicField(name);
            if (value != null)
                {
                if (skipFirst)
                    skipFirst = false;
                else   
                    return result;
                }
            }
            
        return null;
        }
    
    /** Get a String property
    */
    public final String getString(String name)
    	{
    	Object value = getProperty(name);
    	if (value != null)
    	    return AtomData.toString(value);
    	else 
    	    return null;
    	}
    	
    /** Set a String property
    */
    public final void setString(String name, String value)
    	{
    	setField(name, value);
    	}
    	
    /** Get an integer property
    */
    public final int getInt(String name)
    	{
  	    return AtomData.toInt(getProperty(name));
    	}
    	
    /** Set an integer property
    */
    public final void setInt(String name, int value)
    	{
    	setField(name, new Integer(value));
    	}
    	
    /** Get a boolean property
    */
    public final boolean getBool(String name)
    	{
  	    return AtomData.toBool(getProperty(name));
    	}
    	
    /** Set a boolean property
    */
    public final void setBool(String name, boolean value)
    	{
    	setField(name, new Boolean(value));
    	}
    	
    /** Get an Enumeration property
    */
    public final Enumeration getEnum(String name)
    	{
  	    return AtomData.toEnum(getProperty(name));
    	}
    	
    /** Set an Enumeration property
    */
    public final void setEnum(String name, Enumeration value)
    	{
    	setField(name, value);
    	}
    	
    /** Get a Vector property
    	<p>
    	This function will return a null value if the property is not a Vector.
    */
    public final Vector getVector(String name)
    	{
  	    return AtomData.toVector(getProperty(name));
    	}
    	
    /** Set a Vector property
    */
    public final void setVector(String name, Vector value)
    	{
    	setField(name, value);
    	}
    	
    /** Get a Table property
    	<p>
    	This function will return a null value if the property is not a Table.
    */
    public final Dictionary getTable(String name)
    	{
  	    return AtomData.toTable(getProperty(name));
    	}
    	
    /** Set a Table property
    */
    public final void setTable(String name, Dictionary value)
    	{
    	setField(name, value);
    	}
    	
    /** Get an Atom property
    	<p>
    	This function will return a null value if the property is not an Atom.
    */
    public final Atom getAtom(String name)
    	{
  	    return AtomData.toAtom(getProperty(name));
    	}
    	
    /** Set an Atom property
    */
    public final void setAtom(String name, Atom value)
    	{
    	setField(name, value);
    	}
    	
    /** Get an Action property
    	<p>
    	This function will return a null value if the property is not an Action.
    */
    public final Action getAction(String name)
    	{
  	    return AtomData.toAction(getProperty(name));
    	}
    	
    /** Set an Action property
    */
    public final void setAction(String name, Action value)
    	{
    	setField(name, value);
    	}
    	
    /** Set a property.
        <p>
        This does exactly the same as 'setField'; it is supplied for consistency.
    */
    public final void setProperty(String name, Object value)
        {
        setField(name, value);
        }
        
    /** Traverse this atom and its ancestors, adding all properties to a dictionary.
        <p>
        ### Property cooking is not properly handled here.
    */
    public final void getProperties(Dictionary table)
        {
        // Get this atom's fields
        getFields(table);
        
        // Get ancestors' fields
        Enumeration ancestors = getAncestors();
        while (ancestors.hasMoreElements())
            {
            Atom atom = (Atom)ancestors.nextElement();
            atom.getFields(table);
            }
        }
    
// Fields
    
    /** Get a field.
        <p>
        Fields are always defined in the object being queried. This function
        returns system fields such as "ID".
        <p>
        Note that fields are always "raw" values. Use 'getProperty' if you need
        null values, called actions, etc.
        
        @see getRawProperty
    */
    public final Object getField(String name)
        {
        // Ensure case insensitivity
        //### I would prefer to be case-preserving -- impossible with Hashtable
        name = name.toLowerCase();
        
        // Look in the fields
        Object result = getDynamicField(name);
        
        // Not a defined field -- try the system fields
        if (result == null)
            result = getSystemField(name);
            
        return result;
        }

    /** Convert a string into a valid data type.

        @see com.ogalala.mua.AtomData
    */
    public final Object parseField(String fieldAsString)
        {
        return AtomData.parse(fieldAsString, world);
        }
        
    /** Get a dynamic field (one that is defined in the Dictionary).
    */
    private final Object getDynamicField(String name)
        {
        return fields.get(name);
        }

    /** Get a system field. These are fields such as ID which are defined
        in the Java object.
        <p>
        Subclasses of Atom may redefine this to add system fields of their 
        own -- but they must call this function after looking for their fields.
        <p>
        //### Some (extendible) way to iterate the pre-defined field names would be useful
        
        @return the field value or null if not found
    */
    protected Object getSystemField(String name)
        {
        if ("id".equalsIgnoreCase(name))
            return getID();
        else if ("class".equals(name))
            return getClassName();
        else if ("debug_info".equals(name))
            return this.toString();
        else if ("ancestors".equals(name))
            return AtomData.enumToVector(getAncestors());
        else if ("parents".equals(name))
            return AtomData.enumToVector(getParents());
        else if ("children".equals(name))
            return AtomData.enumToVector(getChildren());
        else if ("descendants".equals(name))
            return AtomData.enumToVector(getDescendants());
        else if ("frozen".equals(name))
            return Integer.toString(frozen);
        else if ("fields".equals(name))
            {
            Dictionary result = new Hashtable();
            getFields(result);
            return result;
            }
        else if ("properties".equals(name))
            {
            Dictionary result = new Hashtable();
            getProperties(result);
            return result;
            }
        else
            return null;        
        }

    /** Set a field.
    */
    public final void setField(String name, Object value)
        {
        // Null values are converted to a placeholder object
        if (value == null)
            value = world.getAtomDatabase().getNullPropertyValue();
            
        Debug.assert(AtomData.isValidType(value), "(Atom/720)");
        
        // Ensure case insensitivity
        //### I would prefer to be case-preserving -- impossible with Hashtable
        name = name.toLowerCase();

        // Check name for validity
        if (!isValidID(name))
            throw new AtomException("Invalid field name: " + name);
        
        // Try to set a system field
        if (setSystemField(name, value))
            return;
            
        // Don't allow redefinition of other system fields
        if (getSystemField(name) != null)
            throw new AtomException("Can't change read-only field: " + name);
            
        // Set the field
        fields.put(name, value);
        }

    /** Set a system field. These are fields which are defined in the Java object.
        <p>
        This function is intended for use by subclasses which may define fields
        in the Java object for efficiency. Subclasses must call 'super.setSystemField'.
        <p>
        'Name' will have been converted to lowercase before this function is called.
        
        @return true if the field has been set.
    */
    protected boolean setSystemField(String name, Object value)
        {
        return false;
        }
        
    /** Remove a field.
        <p>
        Note that calls to 'getProperty' may still succeed for this field name
        if it is defined further up the type hierarchy.
    */
    public final void clearField(String name)
        {
        fields.remove(name);
        }

    /** Add this atom's fields to a dictionary, avoiding overwriting existing values
    */
    public final void getFields(Dictionary table)
        {
        Enumeration names = getFieldNames();
        Enumeration values = getFieldValues();
        while (names.hasMoreElements())
            {
            String name = names.nextElement().toString();
            Object value = values.nextElement();
            if (table.get(name) == null)
                table.put(name, value);
            }
        }
        
    /** Enumerate the field names
    */
    public final Enumeration getFieldNames()
        {
        return fields.keys();
        }

    /** Enumerate the field values
    */
    public final Enumeration getFieldValues()
        {
        return fields.elements();
        }

// Marking
    
    /** Unmark all atoms
    */
    public final static void clearAllMarks()
        {
        // By incrementing 'markedValue', we ensure that no atom will be marked
        markedValue++;
        }

    /** Is this atom marked?
    */
    public final boolean isMarked()
        {
        return marked == markedValue;
        }

    /** Mark or unmark an atom
    */
    public final void setMark(boolean flag)
        {
        // If 'flag' is true, set 'marked' to 'markedValue'. If 'flag'
        //  is false, set 'marked' to any lower value.
        marked = flag ? markedValue : markedValue - 1;
        }

// Utilities
    
    /** Has this atom been deleted?
        <p>
        This is here because deleting an atom from the database does not remove
        all references to it (e.g. in the event queue). So as these references
        come up, they can test if the atom is still available or not.        
    */
    public final boolean isDeleted()
        {
        return deleted;
        }

    /** Is 's' a valid atom ID?
        <p>
        A valid ID contains only alphanumeric characters and underscores.
    */
    public static boolean isValidID(String s)
        {
        if (s == null)
            return false;
        char [] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++)
            {
            if (!(Character.isLetterOrDigit(chars[i]) || chars[i] == '_'))
                return false;
            }
        return true;
        }

    // Utility functions for organising inheritance hierarchy

    /** Sort this atom's parents
    */
    final void sortParents()
        {
        if (parents instanceof AtomVector)
            ((AtomVector)parents).sort();
        }

    /** Get the depth of an atom in the inheritance DAG
    */
    final int getInheritanceDepth()
        {
        return depth;
        }

    /** Set the depth of an atom in the inheritance DAG
    */
    final void setInheritanceDepth(int depth)
        {
        this.depth = (short)depth;
        }

    /** Get the height of an atom in the inheritance DAG
    */
    final int getInheritanceHeight()
        {
        return height;
        }

    /** Set the height of an atom in the inheritance DAG
    */
    final void setInheritanceHeight(int height)
        {
        this.height = (short)height;
        }

    /** Compare two atoms by depth and height
        @return a1 - a2
    */
    public static int compare(Atom a1, Atom a2)
        {
        int result = a1.getInheritanceDepth() - a2.getInheritanceDepth();
        if (result == 0)
            result = a1.getInheritanceHeight() - a2.getInheritanceHeight();
        return result;
        }

    /** Get the world
    */
    public final World getWorld()
        {
        return world;
        }

    /** Set the world variable
        <p>
        Used by AtomDatabase only, when atoms are read in. A bit of a hack...
    */
    final void setWorld(World world)
        {
        this.world = world;
        }

    /** Convert the atom to the export format:
        <ul>
        <li>Type
        <li>ID
        <li>Parents
        <li>Fields
        </ul>
    */
    protected String toExportFormat()
        {
        // Class name 
        StringBuffer result = new StringBuffer(getClassName());
        result.append(AtomDatabase.EXPORT_DELIMITER);

        // ID
        result.append(getID());
        result.append(AtomDatabase.EXPORT_DELIMITER);

        // Parents
        Enumeration parents = getParents();
        result.append("[ ");
        while (parents.hasMoreElements())
            {
            result.append(((Atom)parents.nextElement()).getID());
            result.append(" ");
            }
        result.append("]");
        result.append(AtomDatabase.EXPORT_DELIMITER);

        // Fields
        result.append(AtomData.toString(fields));
        
        return result.toString();
        }
        
    /** Return a string representation of this atom
    */
    public String toString()
        {
        // Atom ID
        StringBuffer result = new StringBuffer(getID());
        
        // Static class
        result.append(" (" + getClassName() + ")");
        
        // Marked
        result.append(isMarked() ? " ' " : "   ");

        // Depth and height
        result.append("[" + Integer.toHexString(depth) + "." + Integer.toHexString(height) + "]");

        return result.toString();
        }
        
    /** Test this atom for validity.
        @return true if this is a valid atom.
    */
    public boolean invariant()
        {
        // ID must be valid, database non-null
        if (!isValidID(id))
            return false;
        if (world == null)
            return false;
            
        // Database must contain this atom (if not deleted)
        if ((!deleted && world.getAtom(id) == null) || (deleted && world.getAtom(id) != null))
            return false;
            
        // All parents must have 'this' as a child (except root)
        Enumeration enum;
        if (!isRoot())
            {
            enum = getParents();
            while (enum.hasMoreElements())
                {
                Atom atom = (Atom)enum.nextElement();
                if (!enumContains(atom.getChildren(), this))
                    return false;
                }
            }
            
        // All children must have 'this' as a parent
        enum = getChildren();
        while (enum.hasMoreElements())
            {
            Atom atom = (Atom)enum.nextElement();
            if (!enumContains(atom.getParents(), this))
                return false;
            }
            
        // No field can have invalid name, be null or invalid data type
        Enumeration fieldNames = getFieldNames();
        Enumeration fieldValues = getFieldValues();
        while (fieldNames.hasMoreElements())
            {
            if (!isValidID(fieldNames.nextElement().toString()))
                return false;
            Object field = fieldValues.nextElement();
            if (field == null)
                return false;
            else if (AtomData.getFieldType(field) == AtomData.UNKNOWN)
                return false;
            }
        
        // If we get here, everything is OK.
        return true;
        }
    
    /** Does 'enum' contain 'object'?
        <p>### Utility function, probably belongs somewhere else
    */
    protected static boolean enumContains(Enumeration enum, Object object)
        {
        while (enum.hasMoreElements())
            {
            if (enum.nextElement() == object)
                return true;            
            }
        return false;
        }
        
// Thing functions (output)
    
    /** Send an Output Packet to the Watchers.
    */
    public void output(OutPkt out)
        {
        throw new AtomException("Attempted to output from Atom (Atom.java/1000)");
        }

    /** Send an Output Packet to the Watchers, except those attached to
        'missOut'.
        <p>
        This function is for use when sending output to the things in a
        container, except the actor.
    */
    public void output(OutPkt out, Atom missOut)
        {
        output(out);
        }

    /** Send an Output Packet to the Watchers, except those attached to
        'missOut1' and 'missOut2'.
        <p>
        This function is for use when sending output to the things in a
        container, except the actor and the second party. For example
        the WHISPER and GIVE commands.
    */
    public void output(OutPkt out, Atom missOut1, Atom missOut2)
        {
        output(out);
        }

    /** Output a string.
        <p>
        This is a convenience function which creates a Misc packet from the 
        string parameter.
    */
    public final void output(String msg)
        {
        output(newMiscPkt(msg));
        }
		
    /** Output string to the Watchers, except those attached to 'missOut'
        <p>
        This is a convenience function which creates a Misc packet from the 
        string parameter.
    */
    public final void output(String msg, Atom missOut)
        {
        output(newMiscPkt(msg), missOut);
        }

    /** Output string to the Watchers, except those attached to
        'missOut1' and 'missOut2'
        <p>
        This is a convenience function which creates a Misc packet from the 
        string parameter.
    */
    public final void output(String msg, Atom missOut1, Atom missOut2)
        {
        output(newMiscPkt(msg), missOut1, missOut2);
        }

	/**
	*	Output a string, using user defined string substitution
	*	Because of this type of substitution, the message effectively
	*	gets formatted twice.
	*
	*	## Perhaps there is someway of marking the packet so it 
	*	doesn't need to be done twice ?
	*
	*	This is a convenience function which creates a Misc packet from the 
    *   string parameter.
    *
	* ## Subject to approval from a higher source (james)
	*/
	public final void output(String msg, String value[])
		{
		output(msg, value, null, null);
		}
	
	/**
	*
	*/
	public final void output(String msg, String value)
		{
		output(msg, new String[]{ value }, null, null);
		}
	/**
	*	Output a string, except those attached to 'missOut1',
	*	using user defined string substitution
	*	Because of this type of substitution, the message effectively
	*	gets formatted twice.
	*
	*	## Perhaps there is someway of marking the packet so it 
	*	doesn't need to be done twice ?
	*
	*	This is a convenience function which creates a Misc packet from the 
    *   string parameter.
    *
	* ## Subject to approval from a higher source (james)
	*/
	public final void output(String msg, String value[], Atom missOut1)
		{
		output(msg, value, missOut1, null);
		}
		
	/**
	*
	*/
	public final void output(String msg, String value, Atom missOut1)
		{
		output(msg, new String[]{ value }, missOut1, null);
		}
	/**
	*	Output a string, except those attached to 'missOut1' and 'missOut2', 
	*	 using user defined string substitution
	*	Because of this type of substitution, the message effectively
	*	gets formatted twice, once here, and then later when the Ouput packet
	*	is processed. 
	*
	*	## Perhaps there is someway of marking the packet so it 
	*	doesn't need to be done twice ?
	*
	*	This is a convenience function which creates a Misc packet from the 
    *   string parameter.
    *
	* ## Subject to approval from a higher source (james)
	*/
	public final void output(String msg, String value[], Atom missOut1, Atom missOut2)
		{
		String result = msg;
		
		//get the current event to wrap around the message
		Event event = world.getCurrentEvent();
        
        //if the 
        if (event != null)
            result = event.formatOutput(result, value);
        
        if( missOut1 != null && missOut2 != null )
			output(newMiscPkt(result), missOut1, missOut2);
			
		else if( missOut1 != null )
			output(newMiscPkt(result), missOut1);
		
		else
			output(newMiscPkt(result));
		}
	
	/**
	*
	*/
	public final void output(String msg, String value, Atom missOut1, Atom missOut2)
		{
		output(msg, new String[]{ value }, missOut1, missOut2);
		}
		
	
    /** Create a misc packet
    */
    private OutPkt newMiscPkt(String msg)
        {
        return new OutPkt("misc", "msg", msg);
        }

	
		
	
// Thing functions (other)
    
    /** Get the name field (convenience function)
    */
    public final String getName()
        {
        return AtomData.toString(getProperty("name"));
        }

    /** Get the description field (convenience function)
    */
    public final String getDescription()
        {
        return AtomData.toString(getProperty("description"));
        }

    /** Get the long description field (convenience function)
    */
    public final String getLongDescription()
        {
        return AtomData.toString(getProperty("long_description"));
        }

    /** Get the container (always returns null)
    */
    public Atom getContainer()
        {
        return null;
        }
    
    /** Find the first closed container that contains this atom (always null for atoms)
    */
    public Atom getEnclosingContainer()
        {
        return null;
        }
        
    /** Add a watcher (stub)
    */
    public void addWatcher(Watcher newWatcher)
        {
        throw new AtomException("Can't add Watcher to Atom");
        }
        
    /** Remove a Watcher (stub)
    */
    public void removeWatcher(Watcher deadWatcher)
        {
        throw new AtomException("Can't remove Watcher from Atom");
        }

// Container functions

    /** Enumerate the contents (stub)
    */
    public Enumeration getContents()
        {
        return null;
        }

    /** Recursively enumerate the contents of this container and all sub-containers (stub)
    */
    public Enumeration getDeepContents()
        {
        return null;
        }

    /** Is 'thing' contained in this container?
    */
    public boolean contains(Atom atom)
        {
        return false;
        }

    /** Is 'thing' contained in this container or one of its sub-containers?
    */
    public boolean containsDeep(Atom atom)
        {
        return false;
        }

    /** Put an atom in the container
    */
    public void putIn(Atom atom)
        {
        throw new AtomException("Can't use " + getClassName() + " as Container");
        }
        
    /** How many items are stored in this container?
    */
    public int getCount()
        {
        return 0;
        }
        
    /** Is the container empty?
    */
    public boolean isEmpty()
        {
        return true;
        }
    
    /** Is the container closed? 
    */
    public final boolean isClosed()
        {
        return getBool(IS_CLOSED);
        }
        
    /** Get an exit
        
        @return An exit atom or a string, or null if no exit corresponds to the direction
    */
    public Atom getExit(int direction)
        {
        return null;
        }
        
    /** Add an exit
    */
    public void addExit(int direction, Atom exit)
        {
        throw new AtomException("Can't add exit to " + getClassName());
        }
        
    /** Remove an exit
    */
    public void removeExit(Atom exit)
        {
        throw new AtomException("Can't remove exit from " + getClassName());
        }
        
    /** Is this the limbo container?
    */
    public final boolean isLimbo()
        {
        return getID().equals(AtomDatabase.LIMBO_ID);
        }
    }
