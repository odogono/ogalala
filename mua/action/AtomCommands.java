// $Id: AtomCommands.java,v 1.23 1999/07/09 13:29:14 jim Exp $
// Create and manipulate Atoms, Things, etc.
// James Fryer, 17 Sept 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.util.*;
import com.ogalala.util.*;
import com.ogalala.mua.*;

/** Base class for atom creation actions
*/
abstract class ModNewBase
    extends JavaAction
    {
    /** Tell atom factory to invent a unique ID
    */
    public static final String UNSPECIFIED_ID = "*";

    protected Vector parents;
    protected String atomID;

    /** Convert the arguments into an atom ID and an array of parents
    */
    protected void getArgs()
        {
        if (event.getArgCount() < 1)
            throw new AtomException("!ATOM/!NEW atomID/* [parentID...]");
        parseParentArgs();
        parseAtomIDArg();
        }

    /** Collect the parents listed on the command line into the sorted 
        'parents' vector.
    */
    private void parseParentArgs()
        {
        // If no parents are supplied, do nothing
        if (event.getArgCount() < 2)
            {
            parents = null;
            return;
            }
            
        // Create parents array
        parents = new Vector();
        for (int i = 1; i < event.getArgCount(); i++)
            {
            // Get the parents and add them to the list
            String parentID = event.getArg(i).toString();
            Atom parent = world.getAtom(parentID);
            if (parent == null)
                throw new AtomException("Atom not found: " + parentID);
            parents.addElement(parent);
            }
        }

    /** Get the atom ID, either the first argument or a generated ID.
    */
    private void parseAtomIDArg()
        {
        // Get the first argument
        atomID = event.getArg(0).toString();

        // If the atom ID is '*', set it to null so a new ID will be created 
        if (atomID.equals(UNSPECIFIED_ID))
            atomID = null;

        // Else, ensure it's a valid ID and the atom doesn't exist already
        else {
            if (!Atom.isValidID(atomID))
                throw new AtomException("Invalid atom ID: " + atomID);
            else if (world.getAtom(atomID) != null)
                throw new AtomException("Atom already exists: " + atomID);
            }
        }
    }

/** Create new Atom
*/
public class ModNewAtom
    extends ModNewBase
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Get the arguments
        getArgs();

        // Create the atom
        Atom atom;
        if (parents == null)
            atom = world.newAtom(atomID, world.getRoot());
        else
            atom = world.newAtom(atomID, parents);

        // Report to the user
        actor.output("Atom created: " + atom.getID());

        return true;
        }
    }

/** Create new Thing
*/
public class ModNewThing
    extends ModNewBase
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Get the arguments
        getArgs();
        
        // Create the Thing. 
        Atom atom;
        if (parents == null)
            atom = world.newThing(atomID, world.getAtom(AtomDatabase.THING_ID));
        else
            atom = world.newThing(atomID, parents);

        // Move the new Thing to the actor's current location
        //### (Should this be the actor's inventory?)
        world.moveAtom(atom, container);

        // Report to the user
        actor.output(atom.getClassName() + " created: " + atom.getID());

        return true;
        }
    }

/** Duplicate an existing atom
*/
public class ModClone extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
    	// The deepClone flag signifies that we also wish to clone every*thing*
        //  within the given atom.
    	boolean deepClone = false;
    	
        // Check the args
        if(event.getArgCount() < 1)
            throw new AtomException("!CLONE atomID");
        if(event.getArgCount() > 1 && ((String)event.getArg(2)).toLowerCase().equals("deep"))
        	deepClone = true;
        
        // Get the atom to clone
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);
        
        // Check it's a Thing
        if (!(atom instanceof Thing))
            throw new AtomException("Only Things can be cloned");
            
        // Clone it
        Atom newAtom;
        
        if( deepClone )
        	newAtom = world.deepCloneAtom(atom);
        else 
        	newAtom = world.cloneAtom(atom);
        	

        // Move the new Thing to the actor's current location
        //### (Should this be the actor's inventory?)
        world.moveAtom(newAtom, container);

        // Report to the user
        actor.output(atom.getClassName() + " cloned as: " + newAtom.getID());

        return true;
    }
}
    
/** Delete an atom
*/
public class ModDelete
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check the args
        if (event.getArgCount() != 1)
            throw new AtomException("!DEL atomID");
        
        // Get the atom
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);

        // Delete the atom
        world.deleteAtom(atom);

        // Report to the user
        actor.output("Atom deleted: " + atomID);
        
        return true;
        }
    }
    
/** Inherit from an atom
*/
public class ModInherit
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        if (event.getArgCount() < 2)
            throw new AtomException("!INHERIT atomID parent");

        // Get the atom
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);

        // Get the parent atom
        String parentID = event.getArg(1).toString();
        Atom parent = world.getAtom(parentID);
        if (parent == null)
            throw new AtomException("Atom not found: " + parentID);

        // Add the parent to the atom
        atom.inherit(parent);

        // Report to the user
        actor.output(atomID + " inherits from " + parentID);

        return true;
        }
    }
    
/** Uninherit from an atom
*/
public class ModUninherit
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 2)
            throw new AtomException("!UNINHERIT atomID parent");
        
        // Get the atom
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);

        // Get the parent atom
        String parentID = event.getArg(1).toString();
        Atom parent = world.getAtom(parentID);
        if (parent == null)
            throw new AtomException("Atom not found: " + parentID);

        // Remove the parent to the atom
        atom.uninherit(parent);

        // Report to the user
        actor.output(atomID + " uninherited from " + parentID);

        return true;
        }
    }
    
/** Move a Thing
*/
public class ModMove
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        if (event.getArgCount() < 2)
            throw new AtomException("!MOVE atomID destinationID");

        // Get the atom
        String atomID = event.getArg(0).toString();
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);

        // Get the destination
        String destinationID = event.getArg(1).toString();
        Atom destination = world.getAtom(destinationID);
        if (destination == null)
            throw new AtomException("Atom not found: " + destinationID);

        // Move the atom
        world.moveAtom(atom, destination);

        // Report to the user
        actor.output(atomID + " moved to " + destinationID);

        return true;
        }
    }
    
/** Set atom property
*/
public class ModSet
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    /** The atom being set
    */
    private Atom atom;
    
    /** The field being set
    */
    private String fieldID;
    
    /** The value entered by the user
    */
    private String value;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 2)
            throw new AtomException("!SET [atom.]field value");
        
        // Get the args
        parseArgs();
        
        // Set the field value
        atom.setField(fieldID, atom.parseField(value));
            
        // Report to the user
        event.setExpandFormats(false);
        String actualValue = AtomData.toString(atom.getField(fieldID));
        if (actualValue.length() == 0)
            actualValue = "(empty string)";
        actor.output("Value set: " + atom.getID() + "." + fieldID + " (set to): " + actualValue);

        return true;
        }
        
    /** Get the arguments
    */
    private void parseArgs()
        {
        // Collect the args
        fieldID = event.getArg(0).toString();
        value = event.getArg(1).toString();
        
        // If the value is an aggregate type, we have to jam all the arguments together
        //  so that it will be parsed properly.
        if (value.length() > 0 && value.charAt(0) == '[')
            value = concatenateArgs();
        
        // Default to operating on actor
        LValue lvalue = new LValue(actor);
        
        // Parse the lvalue to get the atom
        lvalue.parse(fieldID);
        fieldID = lvalue.getFieldID();
        atom = lvalue.getAtom();

        // Look for a dot within the field ID. If there is a dot, it's a 
        //  compound field, look for an exit atom.
        int dotPos = fieldID.indexOf('.');
        if (dotPos >= 0)
            {
            // The exit field is in the form "direction.fieldID". So we get the 
            //  direction, then the new field ID.
            String directionLabel = fieldID.substring(0, dotPos);
            fieldID = fieldID.substring(dotPos + 1);
            
            // Get the exit that goes in that direction
            int direction = ExitTable.toDirection(directionLabel);
            if (direction == 0)
                throw new ParserException("Bad direction: " + directionLabel);
            Object exitObject = atom.getExit(direction);
            if (exitObject == null)
                throw new AtomException("Exit not found: " + atom.getID() + "." + directionLabel);
            if (!(exitObject instanceof Atom))
                throw new AtomException("Exit not an atom: " + atom.getID() + "." + directionLabel);
                
            // Finally, we can assign the exit atom
            atom = (Atom)exitObject;
            }
        }

    /** This function is a hack to join together all the arguments, undoing all the 
        careful work of the parser, in order to reparse the string as a list or table.
        The first argument 'event.getArg(1)' should begin with an open square bracket.
    */
    private String concatenateArgs()
        {
        StringBuffer result = new StringBuffer();
        for (int i = 1; i < event.getArgCount(); i++)
            {
            // Get the argument string, escaping with quotes if necessary
            String s = event.getArg(i).toString();
            s = quoteString(s);
            result.append(s);
            
            //  Append a space to all but the last argument
            if (i < (event.getArgCount() - 1))
                result.append(" ");
            }
        return result.toString();
        }

    /** Wrap 's' in quotes if 'mustBeQuoted(s)' is true.
        @see #mustBeQuoted
    */
    protected static String quoteString(String s)
        {
        if (mustBeQuoted(s))
            return "\"" + s + "\"";
        else
            return s;
        }
        
    /** Should 's' be surrounded by quotes to be correctly parsed by 
        StreamTokenizer, TableParser, etc? This is an attempt to restore the 
        information removed by the parser before reparsing the string differently...
    */
    protected static boolean mustBeQuoted(String s)
        {
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

        else 
            return false;
        }
    }
    
/** Clear atom property
*/
public class ModClear
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!CLEAR [atom.]field");
        
        // Get the args
        String fieldName = event.getArg(0).toString();
        
        // Default to operating on actor
        LValue lvalue = new LValue(actor);
        
        // Parse the lvalue and clear the field
        lvalue.parse(fieldName);
        Atom atom = lvalue.getAtom();
        atom.clearField(lvalue.getFieldID());
        
        // Report to the user
		actor.output("Value cleared: " + atom.getID() + "." + lvalue.getFieldID());

        return true;
        }
    }
    
/** Examine atom properties
*/
public class ModExam
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Note that this command reverses the LValue parser's priority:
        //  if there is no dot in the field, the whole atom is examined.
        
        // We don't want to expand the format strings within the result
        event.setExpandFormats(false);

        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!EXAM atom[.field]");
        
        // Get args
        String fieldName = event.getArg(0).toString();

        // If there is no dot in the field, dump all the atom properties
        if (fieldName.indexOf('.') < 0)
            {
            // Get the atom
            Atom atom = world.getAtom(fieldName);
            if (atom == null)
                throw new AtomException("Atom not found: " + fieldName);

            // Print a header
            OutPkt output = new OutPkt("atom", "id", atom.getID());
            output.addField("class", atom.getClassName());
            
            // Get the "raw" properties
            Dictionary properties = new Hashtable();
            atom.getProperties(properties);

            // Output the properties
            Enumeration names = properties.keys();
            Enumeration values = properties.elements();
            while (names.hasMoreElements())
                {
                // Get this property's name and value.
               	// Note this is a legitimate use of AtomData, as you have the
                //	value without calling a 'get' function.
                String name = names.nextElement().toString();
                String value = AtomData.toString(values.nextElement());

                // If the name starts with an underscore, ignore it
                //### There should be an option to display these
                if (name.charAt(0) != '_')
                    output.addField(name, value);
                }
            actor.output(output);
            }
            
        // Else there is a property ID, so output just that one property
        else {
            // Parse the atom 
            LValue lvalue = new LValue(actor);
            lvalue.parse(fieldName);
            Atom atom = lvalue.getAtom();
            String fieldID = lvalue.getFieldID();
            
            // Get the property value. Here we use AtomData because we want the raw
            //  property, so we can output !-escaped actions etc.
            Object value = atom.getRawProperty(fieldID);
            String valueType = AtomData.getFieldTypeString(value);
            if (value == null)
                value = "null";
            String valueString = AtomData.toString(value);
            
            // Send to the user
            OutPkt output = new OutPkt("atom", "id", atom.getID());
            output.addField("field_type", valueType);
            output.addField(fieldID, valueString);
            actor.output(output);
            }
        return true;
        }
    }

/** Call an action
*/
public class ModCall
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!CALL atom.property [args...]");
            
        // Get the atom and property ID
        String fieldID = event.getArg(0).toString();
        LValue lvalue = new LValue(actor);
        lvalue.parse(fieldID);
        fieldID = lvalue.getFieldID();
        Atom atom = lvalue.getAtom();
        
        // Get the rest of the args
        Object args[] = null;
        if (event.getArgCount() > 1)
            {
            args = new Object[event.getArgCount() - 1];
            for (int i = 1; i < event.getArgCount(); i++)
                {
                String value = event.getArg(i).toString();
                args[i - 1] = current.parseField(value);
                }
            }
            
        // Call the action
        return world.callEvent(actor, fieldID, atom, args);
        }
    }

/** Freeze database
*/
public class FreezeDatabase
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!FREEZE ALL or atomID");
        String id = event.getArg(0).toString().toLowerCase();
            
        // Look for FREEZE ALL
        if (id.equals("all"))
            {
            world.freeze();
            actor.output("" + world.size() + " atoms frozen.");
            }
    
        // Else freeze one atom
        else {
            actor.output("Freezing individual atoms is not supported yet.");
            }
        return true;
        }
    }
