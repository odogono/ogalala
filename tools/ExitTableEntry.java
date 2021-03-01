// $Id: ExitTableEntry.java,v 1.2 1998/10/22 11:44:31 matt Exp $
// An object to hold a parsed property or exit definition
// Matthew Caldwell, 21 October 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import java.util.*;
import java.io.*;

import com.ogalala.util.TableParser;

/**
 *  ExitTableEntry is a special kind of PropertyDefinition
 *  that, in addition to the usual property assignments
 *  (which are required to include the atom name, since
 *  exit tables aren't in a particular atom's scope)
 *  can contain (and export) exit declarations.
 *  <p>
 *  Note that this whole mechanism is a pretty cheesy hack
 *  to allow the new exits syntax to be bolted on to the
 *  old ODLCompiler structure. The result isn't pretty.
 *  At some stage it'll be necessary to rewrite all of this
 *  in a more sensible way using a parser-generator like
 *  JavaCUP, to make the language more formalized and
 *  extensible, but in the meantime this is quicker.
 */
public class ExitTableEntry
	extends PropertyDefinition
{
	
	//-------------------------------------------------------------
	//  class variables
	//-------------------------------------------------------------

	/**
	 *  This entry is an exit declaration.
	 */
	public static final int EXIT = 10;
	
	/**
	 *  This entry is a property assignment.
	 */
	public static final int PROPERTY = 1;
	
	/**
	 *  The exit has no defined type.
	 */
	public static final int EXIT_UNDEFINED = 0;
	
	/**
	 *  The exit is represented by an atom.
	 */
	public static final int EXIT_ATOM = 1;
	
	/**
	 *  The exit is represented by a string.
	 */
	public static final int EXIT_STRING = 2;
	
	/**
	 *  Default type for unspecified exits.
	 */
	public static final String DEFAULT_ATOM = "exit";
	
	//-------------------------------------------------------------
	//  instance variables
	//-------------------------------------------------------------

	/**
	 *  The kind of declaration this wraps -- is it a normal
	 *  property assignment, or is it an exit?
	 */
	protected int kind;
	
	/**
	 *  Is the exit represented by an atom or just by a string?
	 */
	protected int exitType = EXIT_UNDEFINED;
	
	/**
	 *  The atom ID of the source room.
	 */
	protected String source = null;
	
	/**
	 *  The atom ID of the destination room.
	 */
	protected String destination = null;
	
	/**
	 *  The direction of the exit from the source room.
	 */
	protected String srcDirection = null;
	
	/**
	 *  The direction of the exit from the destination room
	 *  (if not specified, this is the reverse of the
	 *  <tt>srcDirection</tt>).
	 */
	protected String dstDirection = null;
	
	/**
	 *  The atom ID to use as a template for the exit handler.
	 *  If the exitType is EXIT_STRING, this will be the quoted
	 *  string that should be printed when a user attempts to
	 *  use the exit.
	 */
	protected String srcAtom = DEFAULT_ATOM;
	
	/**
	 *  The atom ID to use as a template for the exit handler
	 *  in the destination room. If this is not specified, it
	 *  will be the same as the <tt>srcAtom</tt>.
	 */
	protected String dstAtom = null;
	
	//-------------------------------------------------------------
	//  construction
	//-------------------------------------------------------------
	
	/**
	 *  Default constructor.
	 */
	public ExitTableEntry () {}

	//-------------------------------------------------------------
	//  usage
	//-------------------------------------------------------------

	/**
	 *  Extract the exit or property definition from a
	 *  stream in ODL format.
	 
	 @return  Whether the table's closing brace was encountered.
	 
	 */
	public boolean readFromODL ( BufferedReader in )
		throws IOException, ParseException
	{
		// format is:
		// # comments
		// multipart.id = value [\]
		// [ value continuation lines ]
		// -- OR --
		// # comments
		// room direction [(type|"quoted string")] [= room [direction] [type]]
		// [ continuation lines ]
		
		// property names, values, room names and room types
		// all contain potential dependencies
		// atom IDs need only be preceded by a '$' sign on
		// the right hand side of a property assignment
		
		String line;
		
		// read in the # comments
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
		
		// concatenate any continuation lines
		while ( line.endsWith("\\") )
		{
			// remove trailing backslash
			line = line.substring( 0, line.length() - 1 );
			
			// add the next line
			String nextLine = in.readLine();
			
			if ( nextLine == null )
				throw new EOFException ();
			
			line += nextLine.trim();
		}
		
		// at the end of the exits block the entry will just
		// be '}', in which case we just stop and report back
		if ( line.startsWith("}") )
			return true;
		
		// use a string tokenizer to break down the declaration
		// this will either be multipart.id = value or
		// exit direction ...
		// if the former, we can't use the tokenizer to parse the
		// value, but at least we'll know where we stand
		StringTokenizer tokenizer = new StringTokenizer ( line );
		
		// get the first word -- we don't know what to do with
		// this until later, so just stash it
		String firstWord = tokenizer.nextToken();
		
		if ( ! tokenizer.hasMoreTokens() )
			throw new ParseException ( "Invalid exits table entry: \""
									   + line + "\"" );
		
		String secondWord = tokenizer.nextToken();
		
		// the second word must either start with "=" or
		// be a direction
		if ( secondWord.startsWith("=") )
		{
			// call the superclass method
			parseProperty( line );
			
			// check the property name is multipart
			if ( name.indexOf('.') == -1 )
				throw new ParseException ( "Exits property assignment doesn't specify atom: \""
										   + name + "\"" );
										   
			// note that this is a property definition
			kind = PROPERTY;
		}
		else
		{
			// call the local method
			parseExit( line );
			collectDependencies();
			
			// note that this is an exit declaration
			kind = EXIT;
		}
		
		// and the beat goes on
		return false;
	}

	//-------------------------------------------------------------

	/**
	 *  Parse an exit definition into this object's internal
	 *  structures.
	 */
	protected void parseExit ( String line )
		throws ParseException, IOException
	{
		// format is:
		// room direction (type) = room direction (type)
		// however: types are optional, as is remote direction
		// and, if the type is a quoted string, there is no RHS
		
		// set up the tokenizer
		StreamTokenizer tokenizer = new StreamTokenizer ( new StringReader ( line ) );
		tokenizer.resetSyntax();
		
		// few of the whitespace chars will make it this far, but
		tokenizer.whitespaceChars( 0, ' ' );
		
		// token components
		tokenizer.wordChars( '_', '_' );
		tokenizer.wordChars( 'a', 'z' );
		tokenizer.wordChars( 'A', 'Z' );
		tokenizer.wordChars( '0', '9' );
		tokenizer.wordChars( ',', ',' );
		tokenizer.wordChars( '$', '$' );
		
		// double quotes only
		tokenizer.quoteChar( '"' );
		
		// source
		int tokenType = tokenizer.nextToken();
		
		if ( tokenType != StreamTokenizer.TT_WORD )
			throw new ParseException ( "Invalid source room in exit declaration: \"" + line + "\"" );
		
		source = tokenizer.sval;
		if ( source.startsWith("$") )
			source = source.substring(1);
		if ( ! isValidName ( source ) )
			throw new ParseException ( "Invalid source name in exit declaration: \"" + source + "\"" );
		
		// direction
		tokenType = tokenizer.nextToken();
		
		if ( tokenType != StreamTokenizer.TT_WORD )
			throw new ParseException ( "Invalid direction in exit declaration: \"" + line + "\"" );
		
		srcDirection = tokenizer.sval;
		
		if ( ! isValidDirection(srcDirection) )
			throw new ParseException ( "Invalid direction in exit declaration: \"" + srcDirection + "\"" );
		
		// this is where things start to diverge: optional atom declaration
		tokenType = tokenizer.nextToken();
		
		if ( tokenType == '(' )
		{
			tokenType = tokenizer.nextToken();
			
			// may be a quoted string, in which case that should be everything
			if ( tokenType == '"' )
			{
				srcAtom = "\"" + tokenizer.sval + "\"";
				
				// string exits can't have direction "none"
				if ( srcDirection.toLowerCase().indexOf("none") != -1 )
					throw new ParseException ( "String exits can't have direction 'none': \""
											   + line + "\"" );
				exitType = EXIT_STRING;
			}
			// or an atom id
			else if ( tokenType == StreamTokenizer.TT_WORD )
			{
				srcAtom = tokenizer.sval;
				if ( srcAtom.startsWith("$") )
					srcAtom = srcAtom.substring(1);
				if ( ! isValidName ( srcAtom ) )
					throw new ParseException ( "Invalid atom ID in exit declaration: \"" + srcAtom + "\"" );
			}
			else
				throw new ParseException ( "Invalid atom syntax in exit declaration: \"" + line + "\"" );
			
			// consume the closing bracket
			tokenType = tokenizer.nextToken();
			
			if ( tokenType != ')' )
				throw new ParseException ( "Missing or misplaced ')' exit declaration: \"" + line + "\"" );
			
			// move on
			tokenType = tokenizer.nextToken();
			
			// if the atom type was a quoted string, there should be nothing else
			if ( exitType == EXIT_STRING )
			{
				if ( tokenType == StreamTokenizer.TT_EOF || tokenType == StreamTokenizer.TT_EOL )
				{
					valueType = EXIT;
					return;
				}
				else
					throw new ParseException ( "Invalid extra tokens in string exit declaration: \"" + line + "\"" );
			}
		}
		
		// should have an equals next
		if ( tokenType != '=' )
			throw new ParseException ( "Missing or misplace '=' in exit declaration: \"" + line + "\"" );
		
		// destination
		tokenType = tokenizer.nextToken();
		
		if ( tokenType != StreamTokenizer.TT_WORD )
			throw new ParseException ( "Invalid destination in exit declaration: \"" + line + "\"" );
		
		destination = tokenizer.sval;
		if ( destination.startsWith("$") )
			destination = destination.substring(1);
		if ( ! isValidName ( destination ) )
			throw new ParseException ( "Invalid destination atom ID in exit declaration: \"" + destination + "\"" );
		
		tokenType = tokenizer.nextToken();
		
		// divergence again: optional direction
		if ( tokenType == StreamTokenizer.TT_WORD )
		{
			dstDirection = tokenizer.sval;
			if ( ! isValidDirection(dstDirection) )
				throw new ParseException ( "Invalid direction in exit declaration: \"" + dstDirection + "\"" );
			
			// the direction "none" must at most appear on one side of exit
			if ( (srcDirection.toLowerCase().indexOf("none") != -1)
				 && (dstDirection.toLowerCase().indexOf("none") != -1) )
				throw new ParseException ( "Direction 'none' must appear on one side only: \"" + line + "\"" );
			
			tokenType = tokenizer.nextToken();
		}
		
		// optional atom type
		if ( tokenType == '(' )
		{
			tokenType = tokenizer.nextToken();
			
			// may be a quoted string or atom type
			if ( tokenType == '"' )
			{
				dstAtom = "\"" + tokenizer.sval + "\"";
			}
			else if ( tokenType == StreamTokenizer.TT_WORD )
			{
				dstAtom = tokenizer.sval;
				if ( dstAtom.startsWith("$") )
					dstAtom = dstAtom.substring(1);
				if ( ! isValidName( dstAtom ) )
					throw new ParseException ( "Invalid atom ID in exit declaration: \"" + dstAtom + "\"" );
			}
			else
				throw new ParseException ( "Invalid atom syntax in exit declaration: \"" + line + "\"" );
			
			// consume the closing bracket
			tokenType = tokenizer.nextToken();
			
			if ( tokenType != ')' )
				throw new ParseException ( "Missing or misplaced ')' in exit declaration: \"" + line + "\"" );
		
			// there should be nothing left
			tokenType = tokenizer.nextToken();
		}

		
		if ( tokenType != StreamTokenizer.TT_EOF && tokenType != StreamTokenizer.TT_EOL )
			throw new ParseException ( "Unexpected token at end of exit declaration: \"" + line + "\"" );
		
		// if the direction 'none' appears at all, it must be on the RHS
		// if not, everything must be swapped around
		if ( srcDirection.toLowerCase().indexOf("none") != -1 )
		{
			String temp;
			
			temp = source; source = destination; destination = temp;
			
			if ( dstAtom != null )
			{
				srcAtom = dstAtom;
				
				// 'none' destinations have no type
				dstAtom = null;
			}
			
			if ( dstDirection == null )
				throw new ParseException ( "Invalid exit declaration: 'none' cannot be the only specified direction: \""
										   + line + "\"" );

			temp = srcDirection; srcDirection = dstDirection; dstDirection = temp;
		}
		
		exitType = EXIT_ATOM;
		valueType = EXIT;
	}

	//-------------------------------------------------------------

	/**
	 *  Update the dependency list with the parsed properties
	 *  of an exit declaration.
	 */
	protected void collectDependencies ()
	{
		if ( source != null )
			dependencies.addElement( source );
		
		if ( srcAtom != null && exitType == EXIT_ATOM )
			dependencies.addElement( srcAtom );
		
		if ( destination != null )
			dependencies.addElement( destination );
		
		if ( dstAtom != null && ! dstAtom.startsWith("\"") )
			dependencies.addElement( dstAtom );
	}

	//-------------------------------------------------------------

	/**
	 *  Write out the property or exit definition in script
	 *  format.
	 */
	public void writeAsScript ( PrintWriter out, String parentID )
	{
		if ( kind == PROPERTY )
			super.writeAsScript ( out, parentID );
		else
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
				out.println( enum.nextElement() );
			
			// then the !DIG command
			String direction = srcDirection;
			if ( dstDirection != null )
				direction += "-" + dstDirection;
			
			if ( exitType == EXIT_STRING )
			{
				out.println ( "# **** WARNING: STRING EXITS ARE NOT YET SUPPORTED BY !DIG ****" );
				out.println ( "!dig "
							  + direction
							  + " "
							  + source
							  + " "
							  + srcAtom );
			}
			else
			{
				out.print ( "!dig "
							+ direction
							+ " "
							+ source
							+ " "
							+ destination
							+ " "
							+ srcAtom );
				if ( dstAtom != null )
					out.print ( " " + dstAtom );
				out.println();
			}
				
		}
	}

	//-------------------------------------------------------------
	//  information functions
	//-------------------------------------------------------------

	/**
	 *  Is this entry an exit or a property assigment?
	 */
	public boolean isExit ()
	{
		return ( kind == EXIT );
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Determine whether a given string is a valid single
	 *  or compound direction specifier.
	 */
	public static boolean isValidDirection ( String dir )
	{
		StringTokenizer tokenizer = new StringTokenizer ( dir, ",", true );
		
		if ( !tokenizer.hasMoreTokens() )
			return false;
		
		if ( ! isDirectionName( tokenizer.nextToken() ) )
			return false;
		
		while ( tokenizer.hasMoreTokens() )
		{
			// must have a comma and a token name
			if ( ! tokenizer.nextToken().equals(",") )
				return false;
			
			if ( ! isDirectionName( tokenizer.nextToken() ) )
				return false;
		}
		
		// 'none' must be on its own
		if ( dir.toLowerCase().indexOf("none") != -1
			 && ! dir.toLowerCase().equals("none") )
			return false;
		
		return true;
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Determine whether a given string is a valid direction name.
	 */
	public static boolean isDirectionName ( String directionName )
	{
		String dir = directionName.toLowerCase();
		
		return (    dir.equals("n")  || dir.equals("north")
				 || dir.equals("ne") || dir.equals("northeast")
				 || dir.equals("e")  || dir.equals("east")
				 || dir.equals("se") || dir.equals("southeast")
				 || dir.equals("s")  || dir.equals("south")
				 || dir.equals("sw") || dir.equals("southwest")
				 || dir.equals("w")  || dir.equals("west")
				 || dir.equals("nw") || dir.equals("northwest")
				 || dir.equals("u")  || dir.equals("up")
				 || dir.equals("d")  || dir.equals("down")
				 || dir.equals("i")  || dir.equals("in")
				 || dir.equals("o")  || dir.equals("out")
				 || dir.equals("none") );
	}
	
	//-------------------------------------------------------------
}