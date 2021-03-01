// $Id: ObjectDefinition.java,v 1.13 1998/10/28 14:42:16 matt Exp $
// An object to hold a parsed object description.
// Matthew Caldwell, 15 September 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import java.util.*;
import java.io.*;

/**
 *  A class to hold the various bits of an object definition,
 *  ready for further processing. At present, an object
 *  definition consists of an id, some (or no) comments, some
 *  (or no) parents and some (or no) property definitions.
 *  Property definitions are encapsulated by the class of
 *  the same name.
 */
public class ObjectDefinition
{
	//-------------------------------------------------------------
	//  class variables
	//-------------------------------------------------------------
	
	/**
	 *  A constant used to identify this object block as a
	 *  thing definition.
	 */
	public static final String THING = "thing".intern();
	
	/**
	 *  A constant used to identify this object block as an
	 *  atom definition.
	 */
	public static final String ATOM = "atom".intern();
	
	/**
	 *  A constant used to identify this object block as an
	 *  exits table.
	 */
	public static final String EXITS = "exits".intern();
	
	/**
	 *  A constant Vector used to represent any unassigned
	 *  vector variables.
	 */
	public static final Vector EMPTY_VECTOR = new Vector();
	
	//-------------------------------------------------------------

	// states
	
	protected static final int EXPECTING_SPECIFIER = 0;
	protected static final int EXPECTING_ID = 1;
	protected static final int EXPECTING_COLON = 2;
	protected static final int EXPECTING_PARENT = 3;
	
	//-------------------------------------------------------------
	//  instance variables
	//-------------------------------------------------------------
	
	/**
	 *  The type of object can be THING, ATOM or EXITS.
	 *  Different types are exported to different script files.
	 */
	protected String objectType;
	
	/**
	 *  A list of PropertyDefinition objects containing the
	 *  properties defined or overridden by this object.
	 */
	protected Vector properties = new Vector ( 10, 5 );
	
	/**
	 *  A list of any exits defined within this object.
	 *  (This is only used if the object is in fact an
	 *  exit table.)
	 */
	protected Vector exits = new Vector();
	
	/**
	 *  A list of comment strings applying to this object.
	 */
	protected Vector comments = new Vector ( 10, 5 );
	
	/**
	 *  A list of atoms from which this object directly
	 *  inherits.
	 */
	protected Vector parents = new Vector ( 5, 5 );
	
	/**
	 *  The id of this object.
	 */
	protected String id = null;
	
	/**
	 *  The object's <tt>_where</tt> property, which defines
	 *  its location, isn't a "real" property and gets
	 *  special treatment.
	 */
	protected String where = null;
	
	/**
	 *  Any comments associated with the <tt>_where</tt> property.
	 */
	protected Vector whereComments = null;
	
	/**
	 *  The object's <tt>_nouns</tt> property, which defines the
	 *  nouns that can be used to refer to it and its
	 *  descendants, isn't a "real" property and gets
	 *  special treatment.
	 */
	protected Vector nouns = EMPTY_VECTOR;
	
	/**
	 *  Any comments associated with the <tt>_nouns</tt>
	 *  declaration.
	 */
	protected Vector nounComments = null;
	
	//-------------------------------------------------------------
	//  constructors
	//-------------------------------------------------------------

	/**
	 *  Default constructor.
	 */
	public ObjectDefinition ()
	{
	}
	
	//-------------------------------------------------------------
	//  parsing
	//-------------------------------------------------------------
	
	/**
	 *  Extract the object description from a stream in
	 *  ODL format.
	 *  Much of the work is done by the PropertyDefinition
	 *  class. Most of the object definition is a matter
	 *  of string patterns -- not much real interpretation
	 *  is done.
	 
	 @param instr  A buffered text input stream to read the
	 			   definition from.
	 
	 */
	public void readFromODL ( BufferedReader in )
		throws IOException, ParseException
	{
		// format is:
		// # comments
		// atom : parent parent ... {
		// properties
		// }
		
		// properties read themselves and contain their own comments
		// a property will read the closing brace and terminate
		// the parse
		
		String line;
		
		// read in the # comments
		//System.out.println("Reading comments");
		while ( true )
		{
			line = in.readLine();
			
			if ( line == null )
				throw new EOFException ();
			
			line = line.trim();
			
			// empty lines and comment lines are valid here
			if ( line.startsWith ( "#" ) || line.equals("") )
				comments.addElement(line);
			else
				break;
		}
		
		// parse the object header
		//System.out.println("Reading header");
		StreamTokenizer tokenizer = getTokenizer ( line );
		int tokenType = 0;
		int state = EXPECTING_SPECIFIER;
		
		while ( tokenType != '{' )
		{
			tokenType = tokenizer.nextToken();
			
			switch ( tokenType )
			{
				case StreamTokenizer.TT_EOL:
				case StreamTokenizer.TT_EOF:
					// go on to the next line
					line = in.readLine();
					
					if ( line == null )
						throw new ParseException ( "Unexpected end of file" );
					
					tokenizer = getTokenizer ( line );
					break;
				
				case StreamTokenizer.TT_WORD:
					// process a word token
					switch ( state )
					{
						case EXPECTING_SPECIFIER:
							if ( tokenizer.sval.equalsIgnoreCase(THING) )
								objectType = THING;
							else if ( tokenizer.sval.equalsIgnoreCase(ATOM) )
								objectType = ATOM;
							else if ( tokenizer.sval.equalsIgnoreCase(EXITS) )
							{
								// exits blocks have no other header info
								// so set the state to EXPECTING_PARENT, which
								// is what we end up in at the end of the header,
								// and wait for the '{' to arrive
								objectType = EXITS;
								state = EXPECTING_PARENT;
								break;
							}
							else
								throw new ParseException ( "Unknown object specifier: " + tokenizer.sval );
							
							state = EXPECTING_ID;
							break;
						
						case EXPECTING_ID:
							id = tokenizer.sval;
							state = EXPECTING_COLON;
							break;
						
						case EXPECTING_COLON:
							throw new ParseException ( "Expecting ':' after object ID, found \""
													   + tokenizer.sval + "\"" );
								
						case EXPECTING_PARENT:
							// actually getting a parent is an error for an exit table
							if ( objectType != EXITS )
								parents.addElement ( tokenizer.sval );
							else
								throw new ParseException ( "Unexpected token in exits table header: \""
														   + tokenizer.sval + "\"" );
					}
					
					break;
					
				case ':':
					// a colon in anything other than the expected place in an error
					switch ( state )
					{
						case EXPECTING_SPECIFIER:
							throw new ParseException ( "Got ':' when expecting object specifier" );
						case EXPECTING_ID:
							throw new ParseException ( "Got ':' when expecting object ID" );
						case EXPECTING_PARENT:
							if ( objectType == EXITS )
								throw new ParseException ( "Found colon in exits table header" );
							else
								throw new ParseException ( "Multiple colons in object header" );
						
						case EXPECTING_COLON:
							state = EXPECTING_PARENT;
							//System.out.print(": ");
					}
					
					break;
				
				case '{':
					// proceed to the next stage
					//System.out.println("{");
					break;
					
				default:
					// all other token characters are wrong
					throw new ParseException ( "Illegal character in object header: " + ((char)tokenType) );
			}
		}
		
		if ( state != EXPECTING_PARENT )
			throw new ParseException ( "Unexpected '{'" );
		
		// repeatedly parse properties until one reports reaching
		// the definition end (or throws an EOFException)
		//System.out.println("Reading properties");
		
		// we use a slightly different procedure for exits tables
		// since they have different kinds of entries
		
		if ( objectType == EXITS )
		{
			while ( true )
			{
				ExitTableEntry entry = new ExitTableEntry();
				
				boolean result = entry.readFromODL( in );
				
				if ( entry.hasContent() )
				{
					// exits are added to the exits list,
					// property assignments to the properties
					// so they can be written in order
					if ( entry.isExit() )
						exits.addElement( entry );
					else
						properties.addElement( entry );
				}
				
				if ( result )
					return;
			}
		}
		else
		{
			while ( true )
			{
				PropertyDefinition prop = new PropertyDefinition();
				
				boolean result = prop.readFromODL( in );
				
				if ( prop.hasContent() )
				{
					// special properties need special treatment
					
					// _where
					if ( prop.getName().equalsIgnoreCase("_where") )
					{
						where = prop.getValue();
						// trim leading $ sign
						if ( where.startsWith("$") )
							where = where.substring(1);
						whereComments = prop.getComments();
					}
					// _nouns
					else if ( prop.getName().equalsIgnoreCase("_nouns") )
					{
						Object nounList = prop.getListValue();
						if ( nounList != null
							 && nounList instanceof Vector )
							nouns = (Vector) nounList;
						nounComments = prop.getComments();
					}
					// all others
					else
						properties.addElement ( prop );
				}
				
				if ( result )
					return;
			}
		}
		
	}
		
	//-------------------------------------------------------------

	/**
	 *  Extract the object description from a stream
	 *  in script file format. A script file may
	 *  contain multiple interleaved definitions, so
	 *  it is necessary to specify which particular
	 *  object you want to extract.
	 *  <p>
	 *  Since definitions in script files need not
	 *  be contiguous (although they usually will,
	 *  especially if generated automatically), this
	 *  method must always read the whole stream in
	 *  search of relevant commands.
	 
	 ########################################
	 Is this a daft way to do things? It's
	 fairly straightforward, but it does mean
	 that for a script defining 50 objects
	 the file will need to be scanned 51
	 times, which is pretty inefficient --
	 the alternative is to despatch each line
	 of the script to the relevant object as
	 encountered. Must think on't...
	 ########################################
	 
	 @param instr  The input stream to read the definition from.
	 @param ID     The ID of the object to be extracted.
	 
	 */
	public void readFromScript ( BufferedReader in, String ID )
	{
	}
	
	//-------------------------------------------------------------

	/**
	 *  Extract a list of all the atom IDs defined in
	 *  a script stream. See #### hash comment ### for
	 *  parseFromScript...
	 */
	public static Vector getIDs ( InputStream instr )
	{
		return null;
	}
	
	//-------------------------------------------------------------
	//  writing to different formats
	//-------------------------------------------------------------
	
	/**
	 *  Write the object description in script format. This
	 *  does not work properly for exits tables.
	 */
	public void writeAsScript ( PrintWriter out )
		throws IOException
	{
		// every object starts with a separator line just to make
		// things a bit more readable -- even the final empty
		// object at the end of the file
		out.println ( "# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #");
		
		if ( id == null || objectType == EXITS )
			return;
		
		// put a newline in to space things out
		out.println();
		
		Enumeration enum;
		
		// if the object has a location, go there first so
		// it gets created in the right place
		//System.out.println("Going to container");
		if ( where != null )
		{
			// print location comments, if any
			if ( whereComments != null )
			{
				enum = whereComments.elements();
				while ( enum.hasMoreElements() )
					out.println( enum.nextElement() );
			}
			
			// go to location
			out.println ( "!go " + where );
		}
		
		// print the comments relating to the object itself
		enum = comments.elements();
		while ( enum.hasMoreElements() )
		{
			out.println( enum.nextElement() );
		}
		
		// create the object, specifying its type
		// and all its parents	
		if ( objectType == THING )
			out.print( "!new " + id );
		else
			out.print( "!atom " + id );
		
		enum = parents.elements();
		while ( enum.hasMoreElements() )
		{
			out.print( " " + enum.nextElement() );
		}
		
		out.println();

			
		// next, dump all its property definitions
		enum = properties.elements();
		while ( enum.hasMoreElements() )
		{
			PropertyDefinition prop = (PropertyDefinition) enum.nextElement();
			prop.writeAsScript( out, id );
		}
		
		// finally, define its nouns
		
		// print any noun comments
		if ( nounComments != null )
		{
			enum = nounComments.elements();
			while ( enum.hasMoreElements() )
			{
				out.println( enum.nextElement() );
			}
		}
		
		// print nouns
		// nouns are now all combined into a single command
		enum = nouns.elements();
		
		if ( enum.hasMoreElements() )
		{
			out.print("!noun ");
			
			while ( enum.hasMoreElements() )
			{
				out.print( "" + enum.nextElement() );
				if ( enum.hasMoreElements() )
					out.print( ";" );
			}
			
			out.println( " " + id );
		}
		
		// finish with another blank line for clarity
		out.println();
		
		out.flush();
	}
	
	//-------------------------------------------------------------

	/**
	 *  Write the exits table in script format.
	 */
	public void writeExitsAsScript ( PrintWriter out )
		throws IOException
	{
		if ( ( objectType != EXITS ) )
			return;
		
		if ( exits.size() == 0 )
			return;
			
		// put a newline in to space things out
		out.println();
		
		Enumeration enum;
		
		// print the comments relating to the object itself
		enum = comments.elements();
		while ( enum.hasMoreElements() )
		{
			out.println( enum.nextElement() );
		}
		
		// next, dump all the exit definitions
		enum = exits.elements();
		while ( enum.hasMoreElements() )
		{
			ExitTableEntry exit = (ExitTableEntry) enum.nextElement();
			exit.writeAsScript( out, id );
		}
				
		// finish with another blank line for clarity
		out.println();
		
		out.flush();
	}
	
	//-------------------------------------------------------------

	/**
	 *  Write the property assignments in script format.
	 *  (This is only used for exits tables.)
	 */
	public void writePropertiesAsScript ( PrintWriter out )
		throws IOException
	{
		if ( ( objectType != EXITS ) )
			return;
		
		if ( properties.size() == 0 )
			return;
			
		// put a newline in to space things out
		out.println();
		
		Enumeration enum;
		
		// dump all the property definitions
		enum = properties.elements();
		while ( enum.hasMoreElements() )
		{
			PropertyDefinition prop = (PropertyDefinition) enum.nextElement();
			prop.writeAsScript( out, id );
		}

		// finish with another blank line for clarity
		out.println();
		
		out.flush();
	}
	
	//-------------------------------------------------------------

	/**
	 *  Write the object description in ODL format.
	 */
	public void writeAsODL ( PrintWriter out )
		throws IOException
	{
		// first thing printed for ODL is comments
		Enumeration enum = comments.elements();
		while ( enum.hasMoreElements() )
		{
			out.println( enum.nextElement() );
		}
		
		// then start the object definition
		out.print( objectType + " " + id + " :" );
		
		// list the parents
		enum = parents.elements();
		while ( enum.hasMoreElements() )
		{
			out.print( " " + enum.nextElement() );
		}
		
		out.println();
		
		// start the body of the object definition
		out.println("{");
		
		// list fancy properties
		// -- location
		if ( where != null )
		{
			// print any location comments
			if ( whereComments != null )
			{
				enum = whereComments.elements();
				while ( enum.hasMoreElements() )
					out.println( "\t" + enum.nextElement() );
			}
			
			// print _where
			out.println("\t_where = " + where );
		}
		
		// -- nouns
		if ( nouns != null )
		{
			// print any noun comments
			if ( nounComments != null )
			{
				enum = nounComments.elements();
				while ( enum.hasMoreElements() )
					out.println( "\t" + enum.nextElement() );
			}
			
			// print _nouns
			out.print("\t_nouns = [ " );
			
			enum = nouns.elements();
			while ( enum.hasMoreElements() )
			{
				out.print( enum.nextElement() + " " );
			}
			
			out.println("]");
		}
		
		// list normal properties
		enum = properties.elements();
		while ( enum.hasMoreElements() )
		{
			PropertyDefinition prop = (PropertyDefinition) enum.nextElement();
			prop.writeAsODL( out );
		}
		
		// close the body definition
		out.println("}");
		out.println();
		
	}

	//-------------------------------------------------------------
	//  internal utilities
	//-------------------------------------------------------------
	
	/**
	 *  Construct and initialize a StreamTokenizer to parse
	 *  the supplied string. The tokenizer is set up to parse
	 *  the ODL object header.
	 */
	protected StreamTokenizer getTokenizer ( String str )
	{
		StreamTokenizer tokenizer = new StreamTokenizer ( new StringReader ( str ) );
		
		// start from scratch
		tokenizer.resetSyntax();
		
		// ignore all whitespace
		tokenizer.whitespaceChars ( 0, ' ' );
		
		// valid identifier constituents
		tokenizer.wordChars(48, 57);  // digits
		tokenizer.wordChars(65, 90);  // A-Z
		tokenizer.wordChars(95, 95);  // underscore
		tokenizer.wordChars(97, 122); // a-z

		// everything else is significant on its own, and in
		// almost all cases is a syntax error
		
		return tokenizer;
	}
	
	//-------------------------------------------------------------
	//  accessors
	//-------------------------------------------------------------

	/**
	 *  Determine the type of this object.
	 */
	public String getType ()
	{
		return objectType;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get the id of this object.
	 */
	public String getID ()
	{
		return id;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get a list of the parent atoms of this object.
	 */
	public Vector getParents()
	{
		return parents;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get a list of the comment strings for this object.
	 */
	public Vector getComments()
	{
		return comments;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get a list of the properties defined or overridden on
	 *  this object.
	 */
	public Vector getProperties()
	{
		return properties;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get a list of all atoms on which this object depends,
	 *  which is the union of its parents, its location and
	 *  any atoms referenced in its properties.
	 */
	public Vector getDependencies()
	{
		// #################################################
		// unlike most of the other lists we deal with here,
		// the dependencies are significant and we need to
		// be sure they are unique and exclusive -- the
		// following method is nice and lazy for me but
		// inelegant and very inefficient -- I'm sure JGL
		// has an appropriate collection class, but I'd have
		// to find it and learn how to use it. Maybe later.
		// #################################################
		
		Hashtable depend = new Hashtable ( 10, 0.5f );
		
		Enumeration enum = parents.elements();
		while ( enum.hasMoreElements() )
		{
			String dependAtom = ((String) enum.nextElement()).toUpperCase().intern();
			depend.put( dependAtom, ATOM );
		}
		
		// add location, if any
		if ( where != null )
		{
			if ( where.startsWith("$") )
				depend.put( where.substring(1, where.length()).toUpperCase().intern(), ATOM );
			else		
				depend.put( where.toUpperCase().intern(), ATOM );
		}
		
		// add any property dependencies
		// (PropertyDefinition.getDependencies(Hashtable) uses
		// the same nasty hack as here...)
		enum = properties.elements();
		while ( enum.hasMoreElements() )
		{
			((PropertyDefinition) enum.nextElement()).getDependencies(depend);
		}
		
		// add any exit dependencies
		// (ditto)
		enum = exits.elements();
		while ( enum.hasMoreElements() )
		{
			((PropertyDefinition) enum.nextElement()).getDependencies(depend);
		}
		
		// copy the key list -- which is guaranteed to have only
		// one copy of each key -- into a new vector and return it
		Vector result = new Vector ( depend.size() );
		enum = depend.keys();
		while ( enum.hasMoreElements() )
			result.addElement(enum.nextElement());
		
		return result;
	}

	//-------------------------------------------------------------
}