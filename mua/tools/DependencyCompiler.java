// $Id: DependencyCompiler.java,v 1.4 1998/11/16 18:22:49 matt Exp $
// Compiler to create a dependency script.
// Matthew Caldwell, 29 September 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import com.ogalala.util.*;
import java.util.*;
import java.io.*;

/**
 *  A translator that processes a list of script files
 *  and generates a single script that runs them all in the
 *  correct order to satisfy their declared dependencies.
 *  The resulting script file will itself declare any
 *  dependencies that are not internally satisfied, and
 *  may in turn be fed into the compiler to produce a
 *  higher-level dependency script. However, since this
 *  may lead to awkward cyclic dependencies, it is
 *  probably better to process all script files en masse,
 *  or at least in wholly self-contained modules.
 *  <p>
 *  Script dependencies are declared in each file's
 *  header (which is everything preceding the
 *  first blank line and generally consists entirely
 *  of comments), via the <tt>#import</tt> and <tt>#export</tt>
 *  directives. These keywords are followed by a space-separated
 *  list of atom and thing IDs. There can be any number of
 *  #import and #export directives in each file's header.
 *  Automatically-generated #import and #export lines
 *  will always list atom names entirely in upper case, but
 *  the names themselves are not case sensitive.
 *  <p>
 *  If the files to be processed contain cyclic dependencies,
 *  (ie, if two scripts each require the other to load first),
 *  the compilation will fail with an appropriate error
 *  message. In such cases, it is necessary to hand-code
 *  the script files to remove such dependencies.
 */
public class DependencyCompiler
	implements Runnable
{
	//-------------------------------------------------------------
	//  instance variables
	//-------------------------------------------------------------

	/**
	 *  The thread with which the <tt>run()</tt> method is
	 *  executed when the class is run as an application.
	 */
	protected Thread runThread = null;
	
	/**
	 *  The list of files to be processed, as provided on the
	 *  command line.
	 */
	protected String[] sources = null;

	/**
	 *  A stream to which error messages are written. This will
	 *  commonly be System.err (or a PrintWriter wrapper for it),
	 *  but can optionally be a log file.
	 */
	protected PrintWriter err = null;
	
	/**
	 *  A flag indicating that the error stream has already
	 *  been created so this class does not need to.
	 */
	protected boolean createErr = true;
	
	/**
	 *  A public flag which is set when compilation
	 *  is successful.
	 */
	public boolean succeeded = false;
	
	//-------------------------------------------------------------

	/**
	 *  A list of DependencyNode objects detailing the
	 *  dependencies of each script file.
	 */
	protected Vector dependencies = new Vector ( 10, 10 );
	
	/**
	 *  A list of atoms that are imported by one or more
	 *  scripts in the file list and not defined anywhere.
	 */
	protected Vector imports = new Vector ( 10, 10 );
	
	/**
	 *  A list of the atoms that are exported by scripts
	 *  in the file list.
	 */
	protected Vector exports = new Vector ( 10, 10 );
	
	/**
	 *  A list of all the script file names in dependency
	 *  order.
	 */
	protected Vector orderedFileList = new Vector ( 10, 10 );
	
	//-------------------------------------------------------------
	//  class variables
	//-------------------------------------------------------------
	
	/**
	 *  A reference to the application object if the class
	 *  is being run as an application. This is instantiated
	 *  by <tt>main()</tt>.
	 */
	protected static DependencyCompiler theApp = null;
	
	/** Version string. */
	public static final String VERSION = "$Revision: 1.4 $";
	
	//-------------------------------------------------------------
	//  construction
	//-------------------------------------------------------------
	
	/**
	 *  The default constructor does nothing, on the assumption
	 *  that any initialization will be done by <tt>init()</tt>.
	 *  If the class is to be instantiated other than via the
	 *  standard invocation of <tt>main()</tt>, it may be
	 *  useful to provide other constructors.
	 */
	public DependencyCompiler ()
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
		theApp = new DependencyCompiler();
		theApp.init(args);
			
		theApp.runThread = new Thread ( theApp );
		theApp.runThread.start();
	}

	//-------------------------------------------------------------
	
	/**
	 *  Do any necessary initialization from the command-line
	 *  arguments.
	 */
	public void init ( String[] args )
	{
		succeeded = false;
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
		// nothing comes of nothing
		if ( sources.length == 0 )
		{
			printUsage();
			return;
		}
		
		// process command-line options
		int argPtr = 0;
		String outName = null;
		String errName = null;
		
		while ( true )
		{
			// handle any options
			if ( argPtr >= sources.length )
				return;
				
			if ( "-h".equals(sources[argPtr])
				 || "--help".equals(sources[argPtr])
				 || "-?".equals(sources[argPtr]) )
			{
				printUsage();
				argPtr++;
			}
			else if ( "-o".equals(sources[argPtr]) )
			{
				if ( argPtr + 1 < sources.length )
				{
					outName = sources[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "Not enough arguments" );
					return;
				}
			}
			else if ( "-e".equals(sources[argPtr]) )
			{
				if ( argPtr + 1 < sources.length )
				{
					errName = sources[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "Not enough arguments" );
					return;
				}
			}
			else if ( sources[argPtr].startsWith("-") )
			{
				System.err.println ( "Unknown option: " + sources[argPtr] );
				printUsage();
				return;
			}
			else
				// we'll assume it's a fileName and proceed
				break;
		}
		
		// create the error file, if any
		if ( createErr || err == null )
		{	
			try
			{
				if ( errName == null )
					err = new PrintWriter ( System.err, true );
				else
					err = new PrintWriter ( new FileWriter ( errName ), true );
			}
			catch ( IOException e )
			{
				System.err.println ( "WARNING: Unable to create error file " + errName + ": " + e );
				System.err.println ( "Errors will be written to System.err" );
				err = new PrintWriter ( System.err );
			}
		}
		
		// read dependencies from each file in turn
		for ( ; argPtr < sources.length; argPtr++ )
		{
			readDependencies ( sources[argPtr] );
		}
		
		// sort the files into order
		// and if successful write them to the output file
		if ( sortDependencies() )
			writeDependencies ( outName );
		
		if ( errName != null )
		{
			err.flush();
			err.close();
		}
		
		// set the success flag so that clients know
		succeeded = true;
	}
		
	//-------------------------------------------------------------
	
	/**
	 *  Read the imports and exports from a given file.
	 */
	protected void readDependencies ( String fileName )
	{
		try
		{
			DependencyNode node = new DependencyNode ( fileName, err );
			
			dependencies.addElement ( node );
			
			// record all imports and exports
			Enumeration enum = node.getExports();
			while ( enum.hasMoreElements() )
			{
				Object obj = enum.nextElement();
				
				if ( exports.contains( obj ) )
				{
					err.println( "WARNING: file "
								 + fileName
								 + " exports already-defined atom "
								 + obj );
				}
				else
					exports.addElement(obj);
			}
			
			enum = node.getImports();
			while ( enum.hasMoreElements() )
			{
				Object obj = enum.nextElement();
				
				if ( ! imports.contains( obj ) )
					imports.addElement(obj);
			}

		}
		catch ( IOException e )
		{
			err.println( "ERROR: Exception reading file "
						 + fileName + ": " + e );
		}
	}
	
	//-------------------------------------------------------------

	/**
	 *  Generate the list of script files in dependency order.
	 *  If this is not possible (because of a cyclic dependency
	 *  or some other reason), the sort is abandoned and the
	 *  method returns <tt>false</tt>.
	 
	 @return  Whether the sort completed successfully.
	 
	 */
	protected boolean sortDependencies ()
	{
		// first, prune the list of imports to those not
		// also exported
		Enumeration enum = exports.elements();
		while ( enum.hasMoreElements() )
		{
			imports.removeElement( enum.nextElement() );
		}
		
		// next, duplicate the list to a temporary one that
		// we'll use to store all atoms that are defined as
		// we go along
		Vector atoms = (Vector) imports.clone();
		
		// we also need a temporary list of atoms that have
		// all their dependencies satisfied
		Vector satisfiedNodes = new Vector ( 10, 10 );
		
		// now, repeatedly search through the list of
		// dependency nodes and check whether they are
		// satisfied by what has been defined so far
		// if so, they can be moved from the dependencies
		// list to the satisfiedNodes list and subsequently
		// into the orderedFileList
		while ( true )
		{
			enum = dependencies.elements();
			
			while ( enum.hasMoreElements() )
			{
				DependencyNode node = (DependencyNode) enum.nextElement();
				
				if ( node.isSatisfied ( atoms ) )
					satisfiedNodes.addElement ( node );
			}
			
			// if no nodes were satisfied and there are still
			// dependencies, we're fucked -- there must be
			// a cyclic dependency
			if ( satisfiedNodes.size() == 0 )
			{
				err.println ( "ERROR: The following files have one or more cyclic dependencies:" );
				enum = dependencies.elements();
				while ( enum.hasMoreElements() )
					err.println("\t" + ((DependencyNode)enum.nextElement()).getFilename() );
				return false;
			}
			
			// otherwise, remove the satisfied dependencies from
			// the dependency list, add their names to the ordered
			// file list and add their exports to the atoms list
			enum = satisfiedNodes.elements();
			
			while ( enum.hasMoreElements() )
			{
				DependencyNode node = (DependencyNode) enum.nextElement();
				
				dependencies.removeElement ( node );
				
				orderedFileList.addElement( node.getFilename() );
				
				Enumeration exportEnum = node.getExports();
				
				while ( exportEnum.hasMoreElements() )
					atoms.addElement ( exportEnum.nextElement() );
			}
			
			// if there are no more dependencies, we're done
			if ( dependencies.size() == 0 )
				return true;
			
			// forget this round and repeat
			satisfiedNodes.removeAllElements();
		}
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Write a dependency script based on the dependencies
	 *  read in from the file list. The dependency script
	 *  has the usual header declaring its own dependencies,
	 *  and then simply runs each of the specified files
	 *  in turn, in dependency order.
	 *  <p>
	 *  Note that files in the dependency script have any
	 *  path components removed. If there are multiple files
	 *  with the same names in different directories, this
	 *  will probably lead to grief.
	 
	 @param fileName  The name of the output file. If this is
	                  <tt>null<tt>, the script is written to
	                  <tt>System.out</tt>.
	 */
	protected void writeDependencies ( String fileName )
	{
		try
		{
			PrintWriter script = new PrintWriter ( new FileWriter ( fileName ) );
			
			// write the file header
			script.println ( "# auto-generated dependency script" );
			script.println ( "#date " + new Date() );
			script.println ( "#copyright Ogalala Ltd" );
			
			// write imports
			if ( imports.size() > 0 )
			{
				script.print ( "#import" );
				
				Enumeration enum = imports.elements();
				while ( enum.hasMoreElements() )
					script.print ( " " + enum.nextElement() );
				script.println();
			}
			
			// write exports (this is likely to be seriously unwieldy!)
			if ( exports.size() > 0 )
			{
				script.print ( "#export" );
				
				Enumeration enum = exports.elements();
				while ( enum.hasMoreElements() )
					script.print ( " " + enum.nextElement() );
				script.println();
			}
			
			// a couple of blank lines end the header
			script.println();
			script.println();
			
			
			// write the ordered sequence of !run commands
			Enumeration enum = orderedFileList.elements();
			
			while ( enum.hasMoreElements() )
			{
				File pathStripper = new File ( (String) enum.nextElement() );
				script.println ( "!run " + pathStripper.getName() );
			}
			
			// voila!
			script.flush();
			script.close();
		}
		catch ( IOException e )
		{
			err.println ( "ERROR: Exception writing script file "
						  + fileName
						  + ": "
						  + e );
		}
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Print a usage message for this application to
	 *  <tt>System.err</tt>.
	 */
	public void printUsage ()
	{
		// the command-line structure is:
		// [--help | -h | -? | ] : print usage message
		// [-o outputFile] [-e errorFile] file1 file2 ...
		// if no output file is specified, the output will be
		// written to System.out
		// if no errorFile is specified, the output will be
		// written to System.err
		System.err.println("DependencyCompiler [" + VERSION + "] usage:");
		System.err.println("java com.ogalala.tools.DependencyCompiler [options] files...");
		System.err.println("  options:");
		System.err.println("  -e logFile        write errors to logFile");
		System.err.println("                    (default is System.err)");
		System.err.println("  -o outFile        write dependency script to outFile");
		System.err.println("                    (default is System.out)" );
		System.err.println("  -h | --help | -?  print this usage message");
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Specify an output stream that errors should be sent to.
	 *  This should be called before <tt>run()</tt>.
	 */
	public void setErr ( PrintWriter err )
	{
		this.err = err;
		createErr = false;
	}
	
	//-------------------------------------------------------------
}
