// $Id: DependencyNode.java,v 1.2 1998/10/01 11:46:42 matt Exp $
// Object to hold a set of atom dependencies.
// Matthew Caldwell, 30 September 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import java.util.*;
import java.io.*;

/**
 *  A DependencyNode object keeps track of the atoms
 *  imported and exported by a file. It is essentially
 *  a node in a directed graph, but the graph edges are
 *  stored by name rather than in some more efficient
 *  pointer structure. DependencyNodes initialize
 *  themselves by reading the header of a script file.
 *  <p>
 *  DependencyNode objects  are used by the DependencyCompiler
 *  class to generate a topological ordering of a bunch of
 *  script files (or die trying). The implementation is frankly
 *  clumsy, but that can be addressed at another time
 *  or not at all.
 
 @see com.ogalala.tools.DependencyCompiler
 
 */
public class DependencyNode
{
	//-------------------------------------------------------------
	//  instance variables
	//-------------------------------------------------------------

	/**
	 *  A list of the names of the atoms on which this node
	 *  depends. All names are stored as upper case
	 *  <tt>intern()</tt>-ed strings.
	 */
	protected Vector imports = new Vector ( 10, 10 );
	
	/**
	 *  A list of the names of the atoms which this node
	 *  defines. All names are stored as upper case
	 *  <tt>intern()</tt>-ed strings.
	 */
	protected Vector exports = new Vector ( 10, 10 );
	
	/**
	 *  The name of the file whose dependencies this node
	 *  represents.
	 */
	protected String name;
	
	//-------------------------------------------------------------
	//  construction
	//-------------------------------------------------------------

	/**
	 *  Read in the dependencies from the given file. If the
	 *  file header is malformed, warning messages may be
	 *  written to the supplied error stream, but the node
	 *  may still be successfully created. If an error
	 *  occurs opening or reading the file, construction
	 *  will fail and an IOException thrown.
	 
	 @param fileName  The name of the file this node represents.
	 @param err       A stream to which warning messages should be written.
	 
	 */
	public DependencyNode ( String fileName,
							PrintWriter err )
		throws IOException
	{
		name = fileName;
		BufferedReader in = new BufferedReader ( new FileReader ( name ) );
		
		while ( true )
		{
			String line = in.readLine();
			
			if ( line == null )
				break;
				
			line = line.trim().toUpperCase();
			
			// blank and non-comment lines terminate the header
			if ( line.equals("") || (! line.startsWith("#")) )
				break;
			
			if ( line.startsWith("#IMPORT ") )
			{
				// all words in the line after "#IMPORT" should
				// be atom names
				StringTokenizer tokenizer = new StringTokenizer ( line );
				
				// discard "#IMPORT"
				tokenizer.nextToken();
				
				// read in all the atom names the line declares
				while ( tokenizer.hasMoreTokens() )
				{
					String token = tokenizer.nextToken().intern();
					
					if ( isValidName(token) )
						imports.addElement(token);
					else
						err.println ( "WARNING: Invalid import name in file "
									  + fileName
									  + ": " + token );
				}
			}
			else if ( line.startsWith("#EXPORT ") )
			{
				// all words in the line after "#EXPORT" should
				// be atom names
				StringTokenizer tokenizer = new StringTokenizer ( line );
				
				// discard "#EXPORT"
				tokenizer.nextToken();
				
				
				// read in all the atom names the line declares
				while ( tokenizer.hasMoreTokens() )
				{
					String token = tokenizer.nextToken().intern();
					
					if ( isValidName(token) )
						exports.addElement(token);
					else
						err.println ( "WARNING: Invalid export name in file "
									  + fileName
									  + ": " + token );
				}
			}
			
			// other header lines have no bearing on dependencies
		}
		
		// exporting nothing is not illegal, but notify
		// in case it indicates a problem
		if ( exports.size() == 0 )
			err.println ( "WARNING: file "
						  + fileName
						  + " exports no atoms" );
	}

	//-------------------------------------------------------------
	//  usage	
	//-------------------------------------------------------------
	
	/**
	 *  Determine whether all this node's dependencies
	 *  are satisfied by the atoms in the supplied list.
	 
	 @param supplied  A list of atom names for the node
	 				  to check its dependencies against.
	 				  Atom names are expected to have been
	 				  converted to upper case and
	 				  <tt>intern()</tt>-ed, (which they will
	 				  be if supplied by other DependencyNode
	 				  objects) as this function compares them
	 				  by object reference.
	 */
	public boolean isSatisfied ( Vector supplied )
	{
		Enumeration enum = imports.elements();
		
		while ( enum.hasMoreElements() )
		{
			if ( ! supplied.contains( enum.nextElement() ) )
				return false;
		}
		
		return true;
	}
	
	//-------------------------------------------------------------
	//  accessors
	//-------------------------------------------------------------

	/**
	 *  Get a list of the atoms this node depends on.
	 */
	public Vector getImportList ()
	{
		return imports;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get an enumeration of the atoms this node
	 *  depends on.
	 */
	public Enumeration getImports ()
	{
		return imports.elements();
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get a list of the atoms this node defines.
	 */
	public Vector getExportList ()
	{
		return exports;
	}
	
	//-------------------------------------------------------------

	/**
	 *  Get an enumeration of the atoms this node defines.
	 */
	public Enumeration getExports ()
	{
		return exports.elements();
	}

	//-------------------------------------------------------------

	/**
	 *  Get the name of the file whose dependencies this node
	 *  represents.
	 */
	public String getFilename ()
	{
		return name;
	}

	//-------------------------------------------------------------
	//  utilities
	//-------------------------------------------------------------
	
	/**
	 *  Check that a supplied string is a valid property, atom
	 *  or action identifier, meaning it must start with a letter
	 *  or underscore and consist only of letters, numbers and
	 *  underscores.
	 */
	public static boolean isValidName ( String str )
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
}