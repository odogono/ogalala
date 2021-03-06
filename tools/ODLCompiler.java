// $Id: ODLCompiler.java,v 1.14 1998/10/22 11:44:31 matt Exp $
// A compiler to translate object descriptions to script files
// Matthew Caldwell, 15 September 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import com.ogalala.util.*;
import java.util.*;
import java.io.*;

/**
 *  A simple translator that processes one or more object
 *  description files and outputs .atom, .thing and/or .exit
 *  script files as appropriate.
 *  <p>
 *  An object description file contains (obviously) descriptions
 *  of objects, which can be either <i>things</i> or <i>atoms</i>.
 *  In addition, it can contain <i>exit tables</i>, which define
 *  connections between things.
 *  <p>
 *  <b>Atoms</b> are roughly the equivalent of classes in
 *  traditional OO terms. Atoms may not be manifest in the game
 *  world.
 *  <p>
 *  <b>Things</b> are atom instances which exist in the game
 *  world. They may inherit from atoms but not from other
 *  things. Unlike atoms, things have a location (another thing),
 *  which is defined  using the pseudo-property <tt>_where</tt>.
 *  <p>
 *  The description for a thing or atom defines its ID, its
 *  inheritance and the names and values of any desired
 *  properties. The description may also contain comments,
 *  which are completely ignored by the server, but some
 *  of which may have significance for the design tools.
 *  A simple object description might look something like this:
 
 <pre>
    # Lines starting with a #-sign are "private" comments
    # that may be used for any purpose at all.
    
    ## Lines starting with two #-signs are documentation
    ## comments, which should describe the purpose and
    ## requirements of an atom or property. They can be
    ## processed into documentation in much the same way
    ## as JavaDoc comments in Java source files. As with
    ## JavaDoc, they may contain markup tags; however,
    ## these should ideally be restricted to those tags
    ## which are the same for both HTML and our own
    ## Styled Text markup, since they may wind up in both
    ## forms. At present this means tags for BOLD, ITALIC
    ## and UNDERLINE only.
    
    ## Documentation comments refer to whatever is defined
    ## immediately after them, so the doc comment for an
    ## atom should precede the atom definition, and that for
    ## a property should precede the property definition.
    
    ## This whole collection of doc comments would become
    ## the documentation for the <b>Dummy</b> atom.
    
    # There are other special comment types, which have
    # some special significance for the design tools but
    # do not have any impact on the objects designed, 
    # but for the most part these will be automatically
    # generated by the tools and are not relevant to the
    # present specification.
    
    # The following defines an atom called Dummy that
    # inherits from other atoms called Parent1 and Parent2.
    atom Dummy : Parent1 Parent2
    {
    	# Each line inside the braces defines a property
    	# of the atom and assigns it a value. Atoms and
    	# things automatically have all the properties of
    	# all their ancestors, so properties only need to be
    	# specified if either (a) they are new to this atom,
    	# not inherited from any of its parents or (b) if the
    	# inherited value needs to be customized.
    	
    	# The value of a property can be any of the following
    	# types:
    	#
    	#   String      An ordinary text string, enclosed in
    	#               double quotation marks, eg "a string."
    	#   Integer     Any whole number, eg 12345.
    	#   Boolean     A truth value, represented by the
    	#               unquoted strings TRUE and FALSE (case
    	#               insensitive), eg True.
    	#   Action      The name of an Action class to be executed,
    	#               prefixed with an exclamation mark, eg !Eat.
    	#   Atom        The ID of a previously-defined atom or
    	#               thing, prefixed with a dollar-sign,
    	#               eg $Erics_Bicycle.
    	#   List        A list of items, which may themselves
    	#               be any of these types, enclosed in
    	#               square brackets and delimited by whitespace,
    	#               eg [1 2 3]
    	#   Dictionary  A list of key=value pairs, where the keys
    	#               are strings and the values can be any of
    	#               these types, enclosed in square brackets
    	#               and delimited by whitespace,
    	#               eg [ "name"="Kenny" "action"=!Explode ]
    	aString = "Blah blah blah"
    	anInteger = 9907
    	aBoolean = true
    	anAction = !Explode
    	anAtom = $South_Park_Savings_and_Loan
    	aList = [ "Strike one!" "Strike two!" "Strike three and out!" ]
    	 
    	
    	# A property definition may continue over multiple lines
    	# using a backslash character '\' to indicate continuation.
    	# Note that the first line ends immediately before the backslash,
    	# but all leading whitespace is trimmed from the following line.
    	# So in this case a space is left before the backslash to separate
    	# "that" from "continues".
    	aLongProperty = "This is a long string value that \
    	  continues onto the next line."
    	
    	# Some special pseudo-properties exist that are used
    	# by the tools to represent features of the created
    	# objects without actually directly equating to named
    	# "properties" in the resulting object. These are
    	# identified by a leading underscore character '_'.
    	
    	# The "_nouns" pseudo-property defines words that can
    	# be used to refer to the object or its descendants.
    	# Both atoms and things can define nouns, although in
    	# general a noun should be defined as high up the
    	# inheritance hierarchy as possible. A noun can only
    	# be defined once anywhere in the world, so it is
    	# important that it be accessible to all objects that
    	# might need it. Nouns should usually be single words,
    	# since at present that's what the parser expects.
    	# Multiple nouns can be defined for a single atom by
    	# giving _noun a list value. 
    	_nouns = [ "dummy" "pacifier" ]
    }
    
    # The following defines a thing called Erics_Dummy that
    # inherits from the Dummy atom.
    thing Erics_Dummy : Dummy
    {
    	# The main distinguishing feature of things is that
    	# they can exist in the world, which generally means
    	# they'll have a location. That is, they'll be contained
    	# inside some other thing. The "_where" pseudo-property
    	# is used to specify the name of the thing which this
    	# thing is inside.
    	_where = $Erics_Mouth
    	
    	# Certain properties may be added to things (and atoms)
    	# to give them distinguishing descriptive features. These
    	# properties represent adjectives, and are defined in
    	# the adjectives script. To give a thing or atom these
    	# features, just define the relevant properties on them.
    	# (The tools, when they exist, should be able to provide
    	# a quick list of these properties and what they
    	# represent.) Eg, the "isRed" property might define whether
    	# an object can be distinguished as "red", "crimson",
    	# "vermilion", "carmine", etc.
    	isRed = true
    } 
 </pre>
 
 */
public class ODLCompiler
	implements Runnable
{
	//-------------------------------------------------------------
	//  instance variables
	//-------------------------------------------------------------

	/**
	 *  A list of the things defined in the input stream.
	 *  Each thing is a ObjectDefinition object containing
	 *  some number of comments and property definitions.
	 */
	protected Vector things = new Vector ( 10, 5 );
	
	/**
	 *  A list of the atoms defined in the input stream.
	 *  Each atom is an ObjectDefinition object containing
	 *  some number of comments and property definitions.
	 */
	protected Vector atoms = new Vector ( 10, 5 );
	
	/**
	 *  A list of the exit blocks defined in the input
	 *  stream. Each exit block is an ObjectDefinition object
	 *  containing some number of comments and exit table
	 *  entries.
	 */
	protected Vector exits = new Vector ( 10, 5 );

	/**
	 *  A list of the atoms exported by this file. These
	 *  will become the #exports of the .atom script.
	 */
	protected Vector atomExports = new Vector ( 10, 5 );
	
	/**
	 *  A list of the atoms imported by the atom definitions
	 *  in this file. These will become the #imports of the
	 *  .atom script.
	 */
	protected Vector atomImports = new Vector ( 10, 5 );
	
	/**
	 *  A list of the things exported by this file. These
	 *  will become the #exports of the .thing script.
	 */
	protected Vector thingExports = new Vector ( 10, 5 );
	
	/**
	 *  A list of the atoms and things imported by the thing
	 *  definitions in this file. These will become the
	 *  #imports of the .thing script.
	 */
	protected Vector thingImports = new Vector ( 10, 5 );

	/**
	 *  A list of the atoms and things imported by the
	 *  exit definitions in this file. These will become
	 *  the #imports of the .exit script. (NB: .exit
	 *  scripts define no atoms, so there is no
	 *  corresponding <tt>exitExports</tt> member.
	 */
	protected Vector exitImports = new Vector ( 10, 5 );

	/**
	 *  The list of files to be processed, as provided on
	 *  the command line.
	 */
	protected String[] sources = null;

	//-------------------------------------------------------------
	//  class variables
	//-------------------------------------------------------------
	
	/**
	 *  A reference to the application object if the class
	 *  is being run as an application. This is instantiated
	 *  by <tt>main()</tt>.
	 */
	protected static ODLCompiler theApp = null;
	
	/**
	 *  A thread used to run the main method if the class
	 *  is being run as an application. This is instantiated
	 *  by <tt>main()</tt>.
	 */
	protected static Thread thread = null;
	
	//-------------------------------------------------------------
	//  construction
	//-------------------------------------------------------------
	
	/**
	 *  The default constructor does nothing, on the assumption
	 *  that any initialization will be done by <tt>init()</tt>.
	 *  If the class is to be instantiated other than via the
	 *  standard invocation of <tt>main()</tt>, add appropriate
	 *  custom constructors.
	 */
	public ODLCompiler ()
	{
	}

	//-------------------------------------------------------------
	//  application shell
	//-------------------------------------------------------------
	
	/**
	 *  Create an object of the compiler class and run it.
	 */
	public static void main ( String[] args )
	{
		theApp = new ODLCompiler();
		theApp.init(args);
				
		thread = new Thread ( theApp );
		thread.start();
		
//		try
//		{
//			System.in.read();
//		}
//		catch ( Exception x )
//		{
//		}

	}

	//-------------------------------------------------------------
	
	/**
	 *  Do any necessary initialization from the command-line
	 *  arguments.
	 */
	public void init ( String[] args )
	{
		// at present, all arguments are assumed to be
		// source files to be processed; eventually
		// there may be some other command line options
		// that will need to be processed here first
		sources = args;
	}
		
	//-------------------------------------------------------------

	/**
	 *  The main application procedure. When this exits, the
	 *  initial thread will end. The application itself may
	 *  continue, however, if other threads have been started
	 *  (notably the AWT event handling thread).
	 */
	public void run ()
	{
		// process the source files one by one
		if ( sources.length == 0
			 || "-h".equals(sources[0])
			 || "--help".equals(sources[0])
			 || "-?".equals(sources[0]) )
		{
			printUsage();
			return;
		}
		
		for ( int i = 0; i < sources.length; i++ )
		{
			readODL ( sources[i] );
			writeScripts ( sources[i] );
			reset();
		}
	}

	//-------------------------------------------------------------
	
	/**
	 *  Read a named source file and extract all atom and
	 *  thing definitions and dependencies.
	 */
	public void readODL ( String fileName )
	{
		BufferedReader in = null;
		
		System.out.println( "Reading file " + fileName );
		
		try
		{
			in = new BufferedReader ( new FileReader ( fileName ) );
			
			ObjectDefinition obj = new ObjectDefinition ();
			
			while ( true )
			{
				obj.readFromODL ( in );
				
				if ( obj.getType() == obj.ATOM )
				{
					atoms.addElement ( obj );
					atomExports.addElement ( obj.getID().toUpperCase().intern() );
					
					Vector objImports = obj.getDependencies();
					Enumeration enum = objImports.elements();
					
					while ( enum.hasMoreElements() )
					{
						Object item = enum.nextElement();
						
						// dependencies are always intern()-ed,
						// so we can check for object matches here
						if ( ! atomImports.contains( item ) )
							atomImports.addElement( item );
					}
				}
				else if ( obj.getType() == obj.THING )
				{
					things.addElement ( obj );
					thingExports.addElement ( obj.getID().toUpperCase().intern() );
					
					Vector objImports = obj.getDependencies();
					Enumeration enum = objImports.elements();
					
					while ( enum.hasMoreElements() )
					{
						Object item = enum.nextElement();
						
						// dependencies intern()-ed as above
						if ( ! thingImports.contains( item ) )
							thingImports.addElement( item );
					}
				}
				else
				{
					exits.addElement ( obj );
					// exits have no exports
					
					Vector objImports = obj.getDependencies();
					Enumeration enum = objImports.elements();
					
					while ( enum.hasMoreElements() )
					{
						Object item = enum.nextElement();
						
						// blah blah blah
						if ( ! exitImports.contains( item ) )
							exitImports.addElement( item );
					}
				}

				obj = new ObjectDefinition ();
			}
		}
		catch ( EOFException x )
		{
			System.out.println ( "End of file " + fileName );
		}
		catch ( IOException x )
		{
			System.err.println ( "Exception processing file "
								 + fileName + ": " + x );
		}
		catch ( ParseException x )
		{
			System.err.println ( "Exception processing file "
								 + fileName + ": " + x );
		}
		
		if ( in != null )
		{
			try { in.close(); }
			catch ( IOException e ) {}
		}
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Write the current things and atoms to scripts with
	 *  the given base name. If the base name ends ".odl",
	 *  the suffix will be removed. The output files will use
	 *  the base name with ".atom" and ".thing" appended, and
	 *  will be created in the same directory as the source
	 *  file.
	 */
	public void writeScripts ( String fileName )
	{
		// strip off .odl if present
		String baseName;
		Date today = new Date ();
		
		if ( fileName.substring( fileName.length() - 4,
								 fileName.length() ).equalsIgnoreCase(".odl") )
			baseName = fileName.substring( 0, fileName.length() - 4 );
		else
			baseName = fileName;
		
		// output atoms, if any
		if ( atoms.size() > 0 )
		{
			PrintWriter atomFile = null;
			
			try
			{
				atomFile = new PrintWriter ( new FileWriter ( baseName + ".atom" ) );
				
				// exclude exports from the dependencies
				// all use intern() so object comparison is fine
				Enumeration enum = atomExports.elements();
				while ( enum.hasMoreElements() )
					atomImports.removeElement( enum.nextElement() );
				
				// write file header
				atomFile.println ( "#source " + fileName );
				atomFile.println ( "#date " + today );
				atomFile.println ( "#copyright Ogalala Ltd" );
				
				// write imports
				atomFile.print ( "#import" );
				enum = atomImports.elements();
				while ( enum.hasMoreElements() )
					atomFile.print ( " " + enum.nextElement() );
				atomFile.println();
				
				// write exports
				atomFile.print ( "#export" );
				enum = atomExports.elements();
				while ( enum.hasMoreElements() )
					atomFile.print ( " " + enum.nextElement() );
				atomFile.println();
				
				// a couple of blank lines for separation
				atomFile.println();
				atomFile.println();
				
				// write all atoms
				enum = atoms.elements();
				while ( enum.hasMoreElements() )
				{
					ObjectDefinition def = (ObjectDefinition) enum.nextElement();
					def.writeAsScript( atomFile );
				}
				
				// close the file
				atomFile.flush();
				atomFile.close();
			}
			catch ( IOException e )
			{
				System.err.println ( "Exception writing atom file "
									 + baseName + ".atom :" + e );
			}
		}
		
		
		// output things, if any
		if ( things.size() > 0 )
		{
			PrintWriter thingFile = null;
			
			try
			{
				thingFile = new PrintWriter ( new FileWriter ( baseName + ".thing" ) );
				
				// exclude exports from the dependencies
				// all use intern() so object comparison is fine
				Enumeration enum = thingExports.elements();
				while ( enum.hasMoreElements() )
					thingImports.removeElement( enum.nextElement() );
				
				// write file header
				thingFile.println ( "#source " + fileName );
				thingFile.println ( "#date " + today );
				thingFile.println ( "#copyright Ogalala Ltd" );
				
				// write imports
				thingFile.print ( "#import" );
				enum = thingImports.elements();
				while ( enum.hasMoreElements() )
					thingFile.print ( " " + enum.nextElement() );
				thingFile.println();
				
				// write exports
				thingFile.print ( "#export" );
				enum = thingExports.elements();
				while ( enum.hasMoreElements() )
					thingFile.print ( " " + enum.nextElement() );
				thingFile.println();
				
				// a couple of blank lines for separation
				thingFile.println();
				thingFile.println();
				
				// write all things
				enum = things.elements();
				while ( enum.hasMoreElements() )
				{
					ObjectDefinition def = (ObjectDefinition) enum.nextElement();
					def.writeAsScript( thingFile );
				}
				
				// close the file
				thingFile.flush();
				thingFile.close();
			}
			catch ( IOException e )
			{
				System.err.println ( "Exception writing thing file "
									 + baseName + ".thing :" + e );
			}
		}

		// output exits, if any
		if ( exits.size() > 0 )
		{
			PrintWriter exitFile = null;
			
			try
			{
				exitFile = new PrintWriter ( new FileWriter ( baseName + ".exit" ) );
				
				// write file header
				exitFile.println ( "#source " + fileName );
				exitFile.println ( "#date " + today );
				exitFile.println ( "#copyright Ogalala Ltd" );
				
				// write imports
				exitFile.print ( "#import" );
				Enumeration enum = exitImports.elements();
				while ( enum.hasMoreElements() )
					exitFile.print ( " " + enum.nextElement() );
				exitFile.println();
				
				// a couple of blank lines for separation
				exitFile.println();
				exitFile.println();
				
				// write all exits first
				enum = exits.elements();
				while ( enum.hasMoreElements() )
				{
					ObjectDefinition def = (ObjectDefinition) enum.nextElement();
					def.writeExitsAsScript( exitFile );
				}
				
				// then write all the property assignments
				enum = exits.elements();
				while ( enum.hasMoreElements() )
				{
					ObjectDefinition def = (ObjectDefinition) enum.nextElement();
					def.writePropertiesAsScript ( exitFile );
				}
				
				// close the file
				exitFile.flush();
				exitFile.close();
			}
			catch ( IOException e )
			{
				System.err.println ( "Exception writing exit file "
									 + baseName + ".exit :" + e );
			}
		}
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Dump existing atom and thing definitions and dependencies,
	 *  ready to process another file.
	 */
	public void reset()
	{
		things.removeAllElements();
		thingImports.removeAllElements();
		thingExports.removeAllElements();
		atoms.removeAllElements();
		atomImports.removeAllElements();
		atomExports.removeAllElements();
	}

	//-------------------------------------------------------------
	
	/**
	 *  Print a usage message for this application to
	 *  <tt>System.err</tt>.
	 */
	public void printUsage ()
	{
		System.err.println("ODLCompiler usage:");
		System.err.println("java com.ogalala.tools.ODLCompiler [ -h | --help | -? | files ... ]");
		System.err.println("  -h --help -?      print this usage message");
		System.err.println("  files ...         translate one or more ODL files into" );
		System.err.println("                    .atom and/or .thing scripts" );
	}
	
	//-------------------------------------------------------------
}
