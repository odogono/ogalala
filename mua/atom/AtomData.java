// $Id: AtomData.java,v 1.22 1999/07/09 13:29:14 jim Exp $
// Data manipulation routines for Atom fields
// James Fryer, 25 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** This class is used to ensure that fields are in some meaningful format
    before they are stored in an Atom, and to convert them to useful
    formats when they are retrieved from an Atom.
    <p>
    'GetfieldType' and 'isValidType' should be used before a field is 
    stored in an Atom.This will ensure that an object is one of the 
    following types:
    <ul>
    <li>An Atom
    <li>A String object
    <li>An Integer object
    <li>A Boolean object
    <li>A Vector object (List -- to be replaced with Java 1.2 List ###)
    <li>A Dictionary object (Table -- to be replaced with Java 1.2 Map ###)
    <li>An Action
    </ul>
    The 'to' functions should be used after retrieving a field, to convert it 
    to some useful type. The following functions will always return an object
    for a non-null field:
    <ul>
    <li>toString: Returns a string representation.
    <li>toInt: Returns an integer. If the field is an Integer, the return value
        will be the value of the integer. If the field is a Boolean, the return 
        value will be 0 or 1. If the field is any other type, the return value 
        will be 0.
    <li>toBool: Returns a boolean. If the field is a Boolean, the value of 
        the object will be returned. If the field is an Integer, true will
        be returned for any non-zero value. If the field is any other type,
        false will be returned. 
    <li>toEnum: an enumeration is always returned containing zero, one, or more 
        objects.
    </ul>
    The following functions return null unless the field can be converted to
    the appropriate type:
    <ul>
    <li>toList
    <li>toTable
    <li>toAtom
    <li>toAction
    </ul>
    If a field is an Action, these functions will not call it. The client must
    call the Action and use its result as the argument to one of these functions.
*/
public final class AtomData
    {
    /** Permitted field types
    */
    public static final int UNKNOWN = 0;
    public static final int ATOM = 1;
    public static final int STRING = 2;
    public static final int INTEGER = 3;
    public static final int BOOLEAN = 4;
    public static final int LIST = 5;
    public static final int TABLE = 6;
    public static final int ACTION = 7;
    public static final int NULL = 8;
    
    /** Prefix chars for atom ID, action name, list
    */
    public static final char ATOM_PREFIX = '$';
    public static final char ACTION_PREFIX = '!';
    public static final char ESCAPE_PREFIX = '~';
    public static final char DICE_PREFIX = '%';
    public static final char LIST_PREFIX = '[';
    public static final char LIST_SUFFIX = ']';
    
    /** Field types as strings
    */
    private static final String fieldTypeStrings[] = 
        {
        "unknown", "atom", "string", "integer", "boolean", "list", "table", "action", "null", 
        };
        
    /** Get the type of a field
    */
    public static int getFieldType(Object field)
        {
        int result = UNKNOWN;
        if (field instanceof Atom)
            result = ATOM;
        else if (field instanceof String)
            result = STRING;
        else if (field instanceof Integer)
            result = INTEGER;
        else if (field instanceof Boolean)
            result = BOOLEAN;
        else if (field instanceof Vector || field instanceof Enumeration)
            result = LIST;
        else if (field instanceof Dictionary)
            result = TABLE;
        else if (field instanceof Action)
            result = ACTION;
        else if (field instanceof NullPropertyValue)
            result = NULL;
        return result;
        }
        
    /** Get the type of a field as a string
    */
    public static String getFieldTypeString(Object field)
        {
        return fieldTypeStrings[getFieldType(field)];
        }
        
    /** Is 'field' a valid type that can be stored in an Atom?
    */
    public static boolean isValidType(Object field)
        {
        return getFieldType(field) != UNKNOWN;
        }
    
    /** Convert an enumeration into a vector
    */
    public static Object enumToVector(Enumeration enum)
        {
        Vector result = new Vector();
        while (enum.hasMoreElements())
            result.addElement(enum.nextElement());
        return result;
        }
        
    /** Translate 'fieldAsString' to a field.
        <br>
        ### IS there a better way to handle the world so we don't have to 
        pass it to this function? Making this class non-static might be the 
        answer...
    */
    public static Object parse(String fieldAsString, World world)
        {
        // Disallow null values
        if (fieldAsString == null)
            throw new NullPointerException("AtomData.parse: null field value");
            
        // If 'fieldAsString' is empty, return it as is
        if (fieldAsString.length() == 0)
            return fieldAsString;
            
        // Get the first char of the string
        char c = fieldAsString.charAt(0);
        
        // If the string is escaped, returh the rest of the string without further checking
        if (c == ESCAPE_PREFIX)
            return fieldAsString.substring(1);

        // Is it an atom?
        else if (c == ATOM_PREFIX)
            {
            // Get the atom 
            String atomID = fieldAsString.substring(1);
            Atom result = world.getAtom(atomID);
            
            // If it wasn't found, throw
            if (result == null)
                throw new AtomException("Atom '" + atomID + "' not found");
                
            return result;
            }
        
        // Is it an action?
        else if (c == ACTION_PREFIX)
            {
            // Get the action class name
            String actionName = fieldAsString.substring(1);
            
            // Attempt to load the class
            Action result = JavaAction.loadAction(actionName);
            
            // If we couldn't load the action, throw
            if (result == null)
                throw new AtomException("Action '" + actionName + "' not found");

            return result;
            }        
            
        // Lists and tables
        else if (c == LIST_PREFIX)
            {
            // Remove the leading '[' and parse with the aggregate parser class
            fieldAsString = fieldAsString.substring(1);
            AggregateParser parser = new AggregateParser(fieldAsString, world);
            return parser.parse();
            }
        
        // Try converting to a number
        else if (Character.isDigit(c) || c == '-')
            {
            try {
                int result = Integer.parseInt(fieldAsString);
                return new Integer(result);
                }
            catch (NumberFormatException e) 
                {
                // Give up and treat as a string
                return fieldAsString;
                }
            }
            
        // Try for a Boolean
        else if (fieldAsString.equalsIgnoreCase("true"))
            return Boolean.TRUE;
        else if (fieldAsString.equalsIgnoreCase("false"))
            return Boolean.FALSE;
        
        // Look for null
        else if (fieldAsString.equalsIgnoreCase("null"))
            return null;

        // Else, it must be a string
        else
            return fieldAsString;
        }
    
    /** Inner class to parse lists and tables
    */
    private static class AggregateParser
        extends TableParser
        {
        /** Back-pointer to World
        */
        private World world;
        
        /** Ctor -- requires a World to operate on, but 
            creates the Dictionary
        */
        AggregateParser(String s, World world)
            {
            super(s);
            this.world = world;
            }
        
        /** Parse the string and return a Vector or Dictionary
        */
        Object parse()
            {
            // Parse the string
            Dictionary dictionary = new Hashtable();
            int parseFlags = parse(dictionary, true, true, Integer.MAX_VALUE, TO_LOWER_CASE);
            
            // If the parser found a list, return the "lost and found" section
            if ((parseFlags & IS_LIST) != 0)
                return dictionary.get("");
                
            // Else, return the dictionary
            else {
                // Remove the "lost and found" section
                //### This may be removed for us in a future version of 'TableParser'
                dictionary.remove("");
                return dictionary;
                }
            }

    	/** Do additional parsing of a string before it gets added
    	    to the dictionary. 
    	 */
    	protected Object postProcessToken(String token, boolean isKey)
        	{
        	if (!isKey)
        	    return AtomData.parse(token, world);
        	else
        		return token;
    	    }
        }
    
    /** Return a string representation of 'field'
    */
    public static String toString(Object field)
        {
        // Check for null values
        if (field == null)
            return "null";
            
        // If it's an Atom, encode the ID
        else if (field instanceof Atom)
            return ATOM_PREFIX + ((Atom)field).getID();
            
        // If it's an Action, encode the action name
        else if (field instanceof Action)
            {
            // Get the last section of the action's class name
            String s = field.getClass().getName();
            int dotPos = s.lastIndexOf('.');
            if (dotPos < 0)
                return "Unknown action";
            else
                return ACTION_PREFIX + s.substring(dotPos + 1);
            }
            
        // Vectors and Dictionaries have their own conversion functions
        //### Vector -> 1.2 List
        else if (field instanceof Vector)
            return vectorToString((Vector)field);
        else if (field instanceof Dictionary)
            return dictionaryToString((Dictionary)field);
            
        // Else, use the default 'toString' behaviour
        else
            return field.toString();
        }
    
    /** Convert a Vector to a string of space-separated tokens wrapped in
        square brackets.
    */
    private static String vectorToString(Vector field)
        {
        StringBuffer result = new StringBuffer("[ ");
        Enumeration values = field.elements();
        while (values.hasMoreElements())
            {
            // Convert the property to a string and append it to the result
            String s = AtomData.toString(values.nextElement());
            s = OutPkt.quoteString(s);
            result.append(s);
            result.append(" ");
            }
        result.append("]");
        return result.toString();
        }
        
    private static String dictionaryToString(Dictionary field)
        {
        StringBuffer result = new StringBuffer("[ ");
        Enumeration keys = field.keys();
        Enumeration values = field.elements();
        while (values.hasMoreElements())
            {
            result.append(keys.nextElement().toString());
            result.append("=");
            // convert the value to a string and append it
            String s = AtomData.toString(values.nextElement());
            s = OutPkt.quoteString(s);
            result.append(s);
            result.append(" ");
            }
        result.append("]");
        return result.toString();
        }

    /** Return an integer representation of 'field', or 0
    */
    public static int toInt(Object field)
        {
        if (field instanceof Integer)
            return ((Integer)field).intValue();
        else if (field instanceof Boolean)
            return (((Boolean)field).booleanValue() == false) ? 0 : 1;
        else
            return toDice(field);
        }
    
    /** Return a boolean representation of 'field'. 
        <p>
        If the field is Boolean, return the value. If it's Integer, 
        return false for 0, true otherwise. Any other type returns false.
    */
    public static boolean toBool(Object field)
        {
        if (field instanceof Boolean)
            return ((Boolean)field).booleanValue();
        else if (field instanceof Integer)
            return ((Integer)field).intValue() != 0;
        else
            return toDice(field) != 0;
        }
        
    /** If the field is a string starting with DICE_PREFIX then send the 
        rest of the string to the Dice class. Otherwise return 0.
    */
    private static int toDice(Object field)
        {
        String s = field.toString();
        if (s.length() > 1 && s.charAt(0) == DICE_PREFIX)
            {
            Dice dice = new Dice(s.substring(1));
            return dice.roll();
            }
        else
            return 0;
        }
    
    /** Return an Enumeration representation of 'field'. 
        <p>
        If the field is a Vector, return its enumeration. If it is an 
        Enumeration, pass it through. If it is any other type, return an
        Enumeration with one element.
    */
    public static Enumeration toEnum(Object field)
        {
        if (field instanceof Enumeration)
            return (Enumeration)field;
        else if (field instanceof Vector)
            return ((Vector)field).elements();
        else
            return new SingleElementEnumeration(field);
        }
    
    /** Return aVector representation of 'field'. 
        <p>
        This function will return null if 'field' is not a Vector.
    */
    public static Vector toVector(Object field)
        {
        if (field instanceof Vector)
            return (Vector)field;
        else
            return null;
        }
    
    /** Return a Dictionary representation of field.
        <p>
        This function will return null if 'field' is not a Dictionary.
    */
    public static Dictionary toTable(Object field)
        {
        if (field instanceof Dictionary)
            return (Dictionary)field;
        else
            return null;
        }
    
    /** Return an Atom representation of field
        <p>
        This function will return null if 'field' is not an Atom.
    */
    public static Atom toAtom(Object field)
        {
        if (field instanceof Atom)
            return (Atom)field;
        else
            return null;
        }
    
    /** Return an Action representation of field
        <p>
        This function will return null if 'field' is not an Action.
    */
    public static Action toAction(Object field)
        {
        if (field instanceof Action)
            return (Action)field;
        else
            return null;
        }
    
    /** Process a property that has been retrieved with 'Atom.getRawProperty'.
        <p>
        If the property is an Action, it will be called. This function will never 
        return an Action.
        <p>
        If the value is null, null is returned
        <p>
        If the property satisfies the test 'AtomDatabase.isNullProperty', null will
        be returned. This allows properties to have null values without breaking the 
        underlying hash tables which store properties.
    */
    public static Object cookProperty(Atom current, String propertyID, Object value)
        {
        World world = current.getWorld();
        
        // Is it an Action? If so, call it
        while (value instanceof Action)
            {
            // This is rather complex because we have to create an event context
            //  where the current event's current atom is the same as the atom whose
            //  property we are retrieving. 
            //### Ideally this would be handled in a World function and 'pushEvent' would be private.
            Action action = (Action)value;
            Event currentEvent = world.getCurrentEvent();
            Event event = world.newEvent(currentEvent.getActor(), propertyID, current);
            world.pushEvent(event);
            action.execute(event);
            value = event.getResult();
            world.popEvent();
            }
            
        // Is it a null property? If so, convert the result to null
        if (isNullProperty(value))
            value = null;
            
        return value;
        }
        
    /** Is 'property' set to the value used for null properties?
    */
    public static boolean isNullProperty(Object property)
        {
        return property instanceof NullPropertyValue;
        }

    // Prevent instantiation
    private AtomData() { }
    }
