// $Id: PropertyDefinition.java,v 1.4 1999/04/22 13:35:32 matt Exp $
// An object to hold a parsed property description.
// Matthew Caldwell, 23 September 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import java.util.*;
import java.io.*;

import com.ogalala.util.TableParser;

/**
 *  A class to hold the elements of a property description,
 *  namely its name, value, comments and any dependencies.
 *  It can be read from script or ODL formats and written to
 *  script or ODL files.
 */
public class PropertyDefinition
{
	//-------------------------------------------------------------
	//  class variables
	//-------------------------------------------------------------
	
	public static final int UNASSIGNED = 0;
	public static final int INTEGER = 1;
	public static final int STRING = 2;
	public static final int BOOLEAN = 3;
	public static final int ATOM = 4;
	public static final int ACTION = 5;
	public static final int LIST = 6;
	public static final int TABLE = 7;
	
	//-------------------------------------------------------------
	//  instance variables
	//-------------------------------------------------------------
	
	/**
	 *  The name of this property.
	 */
	protected String name = "";
	
	/**
	 *  The value string for this property.
	 */
	protected String value = "";
	
	/**
	 *  Any comments applying to this property.
	 */
	protected Vector comments = new Vector ( 5, 5 );
	
	/**
	 *  Any atoms on which this property depends.
	 */
	protected Vector dependencies = new Vector ( 3, 3 );
	
	/**
	 *  An integer indicating the type of the property's value.
	 */
	protected int valueType = UNASSIGNED;
	
	/**
	 *  An object to hold the list or table value, if the
	 *  property's value actually is a list or table.
	 */
	protected Object listValue = null;
	
	//-------------------------------------------------------------
	//  construction
	//-------------------------------------------------------------
	
	/** Default constructor. */
	public PropertyDefinition ()
	{
	}
	
	//-------------------------------------------------------------
	//  reading
	//-------------------------------------------------------------
	
	/**
	 *  Extract the object description from a stream in
	 *  ODL format.
	 
	 @return  Whether the object's closing brace was encountered.
	 
	 */
	public boolean readFromODL ( BufferedReader in )
		throws IOException, ParseException
	{
		// format is:
		// # comments
		// name = value [\]
		// [ value continuation lines ]
		
		// the property name may refer outside the scope of
		// the current atom, in which case it must look like:
		// $atom.property = value [\]
		
		// some interpretation of value may be required if
		// value is not a quoted string, since if it contains
		// atom IDs they are dependencies
		
		String line;
		
		// read in the # comments
		//System.out.println("Reading property comments");
		while ( true )
		{
			line = in.readLine();
			
			if ( line == null )
				throw new EOFException ();
			
			line = line.trim();
			
			// empty lines and comment lines are valid here
			if ( line.startsWith( "#" ) || line.equals("") )
				comments.addElement(line);
			else
				break;
		}
		
		//System.out.println("Got definition line: " + line );
		
		// concatenate any continuation lines
		while ( line.endsWith("\\") )
		{
			//System.out.println("Appending continuation line");
			
			// remove the trailing backslash
			line = line.substring( 0, line.length() - 1 );
			
			// add the next line
			String nextLine = in.readLine();
			
			if ( nextLine == null )
				throw new EOFException ();
			
			line += nextLine.trim();
			
			//System.out.println("Extended definition line: " + line );
		}
		
		// at the end of an object definition, the property will just be '}'
		// in which case we just stop and report back
		if ( line.startsWith("}") )
			return true;
		
		// parse the property declaration
		parseProperty ( line );

		//System.out.println ( "Property type is " + valueType );
		return false;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Parse a property declaration into the internal structures
	 *  of this object. (Extracted from <tt>readFromODL()</tt> so
	 *  as to be available to subclasses.)
	 */
	protected void parseProperty ( String line )
		throws ParseException
	{
		// the property definition must begin with a single-word name
		// and an equals sign -- everything else is the value
		
		// get name		
		try
		{
			name = line.substring( 0, line.indexOf('=') ).trim();
		}
		catch ( IndexOutOfBoundsException x )
		{
			throw new ParseException ( "No '=' in property assignment: \""
									   + line + "\"" );
		}
		
		// setting properties in another atom's scope
		// ###########
		// need to change this to support multiple levels and not require
		// the leading '$' sign
		if ( name.indexOf('.') != -1 )
		{
			try
			{
				// atom name and all nested property names must be valid

				// start with the atom
				String atomName = name.substring ( 0, name.indexOf('.') ).trim();
				if ( atomName.startsWith("$") )
					atomName = atomName.substring(1);
				
				// the atom must already be defined elsewhere,
				// and is therefore a dependency (if there are nested
				// properties then these must have been created already,
				// but ATM there's nothing to be done about that)
				dependencies.addElement ( atomName );
				
				if ( !isValidName( atomName ) )
					throw new ParseException ( "Invalid atom name in external property assignment: \""
											   + name + "\"" );
				
				// loop until we run out of property names
				int dotIndex = name.indexOf('.');
				
				while ( dotIndex != -1 )
				{
					String propertyName;
					int nextDotIndex = name.indexOf('.', dotIndex);
					
					if ( nextDotIndex != -1 )
						propertyName = name.substring ( dotIndex + 1,
														nextDotIndex ).trim();
					else
						propertyName = name.substring ( dotIndex + 1 );
					
					if ( !isValidName( propertyName ) )
						throw new ParseException ( "Invalid property name in external property assignment: \""
												   + name + "\"" );
					dotIndex = nextDotIndex;
				}
				
			}
			catch ( IndexOutOfBoundsException x )
			{
			}
		}
		else if ( !isValidName( name ) )
			throw new ParseException ( "Invalid property name: \"" + name + "\"" );
		
		// get value
		try
		{
			value = line.substring( line.indexOf('=') + 1, line.length() ).trim();
		}
		catch ( IndexOutOfBoundsException x )
		{
			throw new ParseException ( "Property " + name + " has no value" );
		}

		// decide type and search for dependencies if necessary
		if ( value.startsWith("\"") )
		{
			// value is a string
			// check for closing quote -- this is only a crude validity
			// check -- substitute a better one later! ####
			if ( ! value.endsWith("\"") )
				throw new ParseException ( "String value for property "
										   + name
										   + " doesn't end with '\"': "
										   + value );
			valueType = STRING;
		}
		else if ( value.startsWith("$") )
		{
			// value is an atom, and therefore also a dependency
			// check for atom ID validity
			String atomName = value.substring(1, value.length());
			
			if ( isValidName( atomName ) )
			{
				valueType = ATOM;
				dependencies.addElement( atomName );	
			}
			else
				throw new ParseException ( "Atom value for property "
										   + name
										   + " is invalid: "
										   + value );
		}
		else if ( value.startsWith("[") )
		{
			// value is a list or table and we need to
			// interpret it and check for dependencies
			
			// we do a crude syntax check here, but don't attempt
			// to balance the brackets -- if the format is wrong
			// the resulting list may be a bit strange, but it'll
			// still get parsed -- presumably this will be true
			// for the server as well -- may want to add better
			// syntax checking later ####
			if ( ! value.endsWith("]") )
				throw new ParseException ( "List value for property "
										   + name
										   + " doesn't end with ']': "
										   + value );
										   
			// remove the outer brackets and parse into a table
			TableParser tp = new TableParser ( value.substring ( 1, value.length() - 1 ) );
			Hashtable table = new Hashtable ( 10, 10 );
			
			int result = tp.parse ( table,
									false,
									true,
									Integer.MAX_VALUE,
									TableParser.TO_LOWER_CASE );
			
			// do a recursive search for referenced atoms
			findDependencies ( table );
			
			if ( ( result & TableParser.IS_LIST ) != 0 )
			{
				valueType = LIST;
				listValue = table.get("");
			}
			else
			{
				valueType = TABLE;
				listValue = table;
			}
		}
		else if ( value.startsWith("!") )
		{
			// value is an action
			// must be a valid identifier
			if ( isValidName( value.substring(1, value.length()) ) )
				valueType = ACTION;
			else
				throw new ParseException ( "Action value for property "
										   + name
										   + " is invalid: "
										   + value );
		}
		else if ( value.equalsIgnoreCase("true")
				 || value.equalsIgnoreCase("false") )
		{
			// value is a boolean
			valueType = BOOLEAN;
		}
		else
		{
			// value may be an int or unquoted string
			// must be in a valid format
			try
			{
				int blah = Integer.parseInt(value);
				valueType = INTEGER;
			}
			catch ( NumberFormatException x )
			{
				if ( isValidWord( value ) )
					valueType = STRING;
				else
					throw new ParseException ( "Value for property "
											   + name
											   + " is invalid: "
											   + value );
			}
		}
	}
	
	//-------------------------------------------------------------
	//  writing
	//-------------------------------------------------------------
	
	/**
	 *  Write out the property definition in ODL format.
	 */
	public void writeAsODL ( PrintWriter out )
	{
		if ( valueType == UNASSIGNED )
			return;
		
		// print any comments
		Enumeration enum = comments.elements();
		while ( enum.hasMoreElements() )
		{
			out.println( "\t" + enum.nextElement() );
		}
		
		// then the property definition as name = value
		out.println( "\t" + name + " = " + value );
	}
	
	//-------------------------------------------------------------

	/**
	 *  Write out the property definition in script format.
	 */
	public void writeAsScript ( PrintWriter out, String parentID )
	{
		if ( valueType == UNASSIGNED )
			return;
		
		// if there are any comments and they don't start with
		// a blank line, print a newline for clarity
		if ( comments.size() > 0 
			 && !((String) comments.elementAt(0)).equals("") )
			out.println();
		
		// print any comments
		Enumeration enum = comments.elements();
		while ( enum.hasMoreElements() )
		{
			out.println( enum.nextElement() );
		}
		
		// then the property definition
		
		// for external properties, don't prefix the object name,
		// but remove the $
		if ( name.indexOf('.') != -1 )
		{
			if ( name.startsWith("$") )
				name = name.substring(1);
				
			// special case: "$atom._where = $x" -> "!move atom x"
			if ( name.substring( name.indexOf('.') ).equals("._where") )
			{
				String what = name.substring( 0, name.indexOf('.') );
				
				if ( value.startsWith("$") )
					value = value.substring(1);
				
				out.println( "!move " + what + " " + value );
			}
			else
			{
				out.println( "!set " + name
							 + " " + value );
			}
		}
		// otherwise as "!set obj.property value"
		else
			out.println( "!set " + parentID + "." + name + " " + value );
	}
	
	//-------------------------------------------------------------
	//  accessors & info
	//-------------------------------------------------------------
	
	/**
	 *  Determine whether the property has been initialized
	 *  with any content.
	 */
	public boolean hasContent ()
	{
		return valueType != UNASSIGNED;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Add the property's dependencies (if any) to the
	 *  supplied Hashtable. Atom IDs are actually case
	 *  insensitive, but here they are always added
	 *  in upper case to avoid having multiple versions
	 *  in the table.
	 */
	public void getDependencies ( Hashtable depend )
	{
		Enumeration enum = dependencies.elements();
		while ( enum.hasMoreElements() )
		{
			depend.put( ((String) enum.nextElement()).toUpperCase().intern(),
						ObjectDefinition.ATOM );
		}
	}

	//-------------------------------------------------------------

	/**
	 *  Get the property's name.
	 */
	public String getName ()
	{
		return name;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get the property's value string.
	 */
	public String getValue ()
	{
		return value;
	}

	//-------------------------------------------------------------

	/**
	 *  The value of this property as a Vector or Hashtable.
	 *  If the value is not a Vector or Hashtable in the
	 *  first place, this returns <tt>null</tt>.
	 */
	public Object getListValue ()
	{
		return listValue;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get a list of any comments associated with the property.
	 */
	public Vector getComments ()
	{
		return comments;
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Get the type of the property's value, which will be
	 *  one of: UNASSIGNED, INTEGER, STRING, BOOLEAN, ATOM,
	 *  ACTION, LIST, TABLE.
	 */
	public int getType ()
	{
		return valueType;
	}

	//-------------------------------------------------------------
	//  internal utilities
	//-------------------------------------------------------------
	
	/**
	 *  Check that a supplied string is a valid unquoted string
	 *  for the purposes of property assignments. Words must
	 *  contain no whitespace or special characters, but are
	 *  not as restricted as names.
	 */
	protected boolean isValidWord ( String str )
	{
		if ( str == null || str.length() == 0 )
			return false;
		
		for ( int i = 0; i < str.length(); i++ )
		{
			char ch = str.charAt(i);
			
			if ( ch <= 32
				 || ch == '"'
				 || ch == '\''
				 || ch == '['
				 || ch == ']'
				 || ch == '\\'
				 || ch >= 127 )
				return false;
		}
		
		return true;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Check that a supplied string is a valid property, atom
	 *  or action identifier, meaning it must start with a letter
	 *  or underscore and consist only of letters, numbers and
	 *  underscores.
	 */
	protected boolean isValidName ( String str )
	{
		if ( str == null || str.length() == 0 )
			return false;
		
		char ch = str.charAt(0);
		
		if ( ! (( ch >= 'a' && ch <= 'z' )
			 || ( ch >= 'A' && ch <= 'Z' )
			 || ch == '_' ) )
			return false;
		
		if ( str.length() > 1 )
		{
			for ( int i = 1; i < str.length(); i++ )
			{
				ch = str.charAt(i);
				
				if ( ! ( ( ch >= 'a' && ch <= 'z' )
					  || ( ch >= 'A' && ch <= 'Z' )
					  || ( ch >= '0' && ch <= '9' )
					  || ch == '_' ) )
					 return false;
			}
		}
		
		return true;
	}

	//-------------------------------------------------------------
	
	/**
	 *  Recursively search for dependencies in the leaves of
	 *  a tree of nested lists and tables. Any atom identifiers
	 *  found in the search are added to the property's dependency
	 *  list.
	 */
	protected void findDependencies ( Object obj )
	{
		if ( obj instanceof String )
		{
			String str = (String) obj;
			if ( str.startsWith("$")
				 && isValidName ( str.substring(1, str.length()) ) )
				dependencies.addElement( str.substring(1, str.length()) );
		}
		else if ( obj instanceof Hashtable )
		{
			Hashtable table = (Hashtable) obj;
			Enumeration enum = table.elements();
			while ( enum.hasMoreElements() )
				findDependencies( enum.nextElement() );
		}
		else if ( obj instanceof Vector )
		{
			Enumeration enum = ((Vector) obj).elements();
			while ( enum.hasMoreElements() )
				findDependencies( enum.nextElement() );
		}
	}
	
	//-------------------------------------------------------------
}