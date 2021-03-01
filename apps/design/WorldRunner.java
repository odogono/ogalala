// $Id: WorldRunner.java,v 1.14 1999/03/17 17:52:58 matt Exp $
// Test runner for ODL files
// Matthew Caldwell, 10 November 1998
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import com.ogalala.util.*;
import com.ogalala.mua.*;

import com.ogalala.widgets.*;
import com.ogalala.widgets.text.*;
import com.ogalala.client.*;

/**
 *  The WorldRunner class is used to run a game world in
 *  single-player mode for testing purposes. It is similar
 *  to WorldTest, but it has a more complete front-end that
 *  uses the same formatting as the client, and provides
 *  facilities for loading new ODL files into the world to
 *  try them out.
 *  <p>
 *  In the slightly longer term, this will probably play
 *  a central role in a more integrated building tool...
 */
public class WorldRunner
	extends WindowAdapter
	implements ActionListener, Runnable, Eavesdropper, Mapper, ToolWatchReceiver
{
	//----------------------------------------------------------------	
	//  class variables
	//----------------------------------------------------------------	

	/**
	 *  An instance of this class running as an application.
	 *  This is instantiated by a call to <tt>main()</tt>.
	 */
	protected static WorldRunner theApp = null;
	
	/** The number of lines of command history to keep. */
	protected static int HISTORY_LINES = 20;

	/**
	 *  Maximum number of characters the TextArea can safely
	 *  contain.
	 */
	public static final int MAX_CAPACITY = 28000;
	
	/**
	 *  Amount of text to retain when the content of the
	 *  TextArea exceeds <tt>MAX_CAPACITY</tt>.
	 */
	public static final int RETAIN_AMOUNT = 22000;
	
	/**
	 *  Text editor application to be invoked on File->New.
	 */
	public static final String DEFAULT_EDITOR = "notepad";
	
	/**
	 *  A script to check the ODL directory out of CVS.
	 */
	public static final String CHECKIN_SCRIPT = "odl_checkin.bat";
	
	/**
	 *  A script to check a file into CVS.
	 */
	public static final String CHECKOUT_SCRIPT = "odl_checkout.bat";
	
	/**
	 *  A script to update the ODL directory.
	 */
	public static final String UPDATE_SCRIPT = "odl_update.bat";
	
	/**
	 *  Version string.
	 */
	public static final String VERSION = "$Revision: 1.14 $";

	//----------------------------------------------------------------	
	//  instance variables
	//----------------------------------------------------------------

	/** The World we're running. */
	private World world = null;
	
	/** A parser for this world. */
	private Parser parser = null;
	
	/** The administrator of this world. */
	private Atom player = null;
	
	/** The script search path for the world. */
	private String scriptPath = ".";
	
	/** The fileName of the world database. */
	private String dbName = "core.db";
	
	/** Whether to echo outgoing messages to the console. */
	private boolean echoOut = true;

	/** Editor application to invoke (must be on the path). */
	private String editor = DEFAULT_EDITOR;
	
	//----------------------------------------------------------------	
	// interface elements
	//----------------------------------------------------------------	

	/** The main window. */
	private Frame mainWindow = new Frame ( "1932.com Test Environment [" + VERSION + "]" );
	
	/** A dialog to get the names of script files to load. */
	private FileDialog openFileDialog = new FileDialog ( mainWindow, "Select File" );
	
	/** The styled output panel. */
	private ScrollingStyledTextPanel console = new ScrollingStyledTextPanel();
	
	/** The text entry field. */
	private CompletionField entryField = new CompletionField();
	
	/** Command history for the entry field. */
	private CommandHistory cmdHistory
		= new CommandHistory ( entryField, HISTORY_LINES );

	/** Manager for the menu bar. */
	private RunnerMenuManager menu = new RunnerMenuManager ( mainWindow );
	
	/** Text area to log unformatted world output. */
	private TextArea textLog
		= new TextArea( "", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY );


	//----------------------------------------------------------------
	//  construction
	//----------------------------------------------------------------

	/** Default constructor. */
	public WorldRunner ()
	{
	}
	
	//----------------------------------------------------------------	
	//  application shell
	//----------------------------------------------------------------	

	/**
	 *  Create, initialize and run an instance of this
	 *  application.
	 */
	public static void main ( String[] args )
	{
		theApp = new WorldRunner ();
		if ( theApp.init(args) )
			theApp.run();
	}
	
	//----------------------------------------------------------------	

	/**
	 *  Initialize this instance from the command-line arguments
	 *  provided to <tt>main()</tt>.
	 
	 @return <tt>false</tt> if the arguments are erroneous,
	 		 <tt>true</tt> otherwise.
	 */
	public boolean init ( String[] args )
	{
		// -? -h --help : print usage and exit
		// -s scriptPath : set scriptPath
		// -d dbName : start up with given database
		
		if ( args.length == 0 )
			return true;
		
		int argPtr = 0;
		
		while (true)
		{
			if ( argPtr >= args.length )
				break;
			
			// -h: help	
			if ( "-h".equals(args[argPtr])
				 || "--help".equals(args[argPtr])
				 || "-?".equals(args[argPtr]) )
			{
				printUsage();
				argPtr++;
			}
			// -s: script path
			else if ( "-s".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					scriptPath = args[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -s requires an argument" );
					return false;
				}
			}
			// -d: database file
			else if ( "-d".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					dbName = args[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -d requires an argument" );
					return false;
				}
			}
			// t: textEditor
			else if ( "-t".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					editor = args[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -t requires an argument" );
					return false;
				}
			}
			// unknown arguments
			else
			{
				System.err.println ( "ERROR: Unknown option: " + args[argPtr] );
				printUsage();
				return false;
			}
			
		}
		
		return true;
	}
	
	//----------------------------------------------------------------	

	/**
	 *  Do any necessary cleaning up in preparation for the
	 *  application closing.
	 
	 @return  Whether the application can now safely quit.
	 
	 */
	public boolean cleanup ()
	{
		return true;
	}

	//----------------------------------------------------------------

	/**
	 *  Run the application.
	 */
	public void run ()
	{
		// put the interface elements together
		mainWindow.setBounds ( 50, 50, 450, 400 );
		
		// listen for menu events
		menu.addActionListener ( this );
		
		// create the main window's peer so that insets exist
		mainWindow.addNotify();
		Insets mainInsets = mainWindow.getInsets();
		
		// set up the interface elements
		mainWindow.setLayout ( new BindLayout() );
		
		mainWindow.add ( entryField,
						 new BindConstraints ( 200, 20,
						 					   mainInsets.left,
						 					   BindConstraints.NONE,
						 					   mainInsets.right,
						 					   mainInsets.bottom ) );
		mainWindow.add ( textLog,
						 new BindConstraints ( 200, 60,
						 					   mainInsets.left,
						 					   BindConstraints.NONE,
						 					   mainInsets.right,
						 					   mainInsets.bottom + 20 ) );
		mainWindow.add ( console,
						 new BindConstraints ( 200, 100,
						 					   mainInsets.left,
						 					   mainInsets.top,
						 					   mainInsets.right,
						 					   mainInsets.bottom + 80 ) );
		console.setEditable ( false );
		console.setBufferLines ( 400 );
		console.setDiscardLines ( 100 );
		
		// listen for events in the entry field
		entryField.addActionListener ( this );
		
		// listen for window events
		mainWindow.addWindowListener ( this );
		
		// show the window
		mainWindow.show();
		entryField.requestFocus();
		
		// configure the message formatting
		MessageFormatter.showID = true;
		MessageFormatter.debug = true;
		MessageFormatter.owner = this;
		
		// try to check out the ODL files directory
		try
		{
			Runtime.getRuntime().exec( CHECKOUT_SCRIPT );
		}
		catch ( IOException x )
		{
			printErr ( "ERROR: Couldn't launch check out script \""
					   + MarkupTranslator.escape(CHECKOUT_SCRIPT)
					   + "\": "
					   + x );
		}
		
		// if a database was specified on the command line,
		// initialize the world
		if ( dbName != null )
			loadDatabase ( dbName );
	}

	//----------------------------------------------------------------
	//  event handling
	//----------------------------------------------------------------
	
	/**
	 *  Handle action events: menu commands and text entry.
	 */
	public void actionPerformed ( ActionEvent e )
	{
		String cmd = e.getActionCommand();

		// user input
		if ( e.getSource() == entryField )
		{
			send ( entryField.getText() );
			entryField.selectAll();
		}
		// File menu
/*		else if ( cmd.equals("Load") )
		{
			// get the name of the database file to open
			openFileDialog.show();
			
			// load it
			if ( openFileDialog.getFile() != null )
			{
				dbName = openFileDialog.getFile();
				loadDatabase ( dbName );
			}
		} */
		else if ( cmd.equals("Reload") )
		{
			// reload the current database
			loadDatabase( dbName );
		}
		else if ( cmd.equals("New") )
		{
			try
			{
				Runtime.getRuntime().exec( editor );
			}
			catch ( IOException x )
			{
				printErr ( "ERROR: Couldn't launch editor \""
						   + MarkupTranslator.escape(editor)
						   + "\": "
						   + x );
			}
		}
		else if ( cmd.equals("Edit") )
		{
			openFileDialog.show();
			
			if ( openFileDialog.getFile() != null )
			{
				try
				{
					Runtime.getRuntime().exec( editor
											   + " "
											   + openFileDialog.getDirectory()
											   + openFileDialog.getFile() );
				}
				catch ( IOException x )
				{
					printErr ( "ERROR: Couldn't launch editor \""
							   + MarkupTranslator.escape(editor)
							   + "\": "
							   + x );
				}
			}
		}
		else if ( cmd.equals("Import") )
		{
			// get the name of the file to import
			openFileDialog.show();
			
			// import it
			if ( openFileDialog.getFile() != null )
				importODL ( openFileDialog.getDirectory()
							+ openFileDialog.getFile() );
		}
		else if ( cmd.equals("Update") )
		{
			try
			{
				Runtime.getRuntime().exec( UPDATE_SCRIPT );
			}
			catch ( IOException x )
			{
				printErr ( "ERROR: Couldn't launch update script \""
						   + MarkupTranslator.escape(UPDATE_SCRIPT)
						   + "\": "
						   + x );
			}
		}
		else if ( cmd.equals("CheckIn") )
		{
			// get the name of the file to check in
			openFileDialog.show();
			
			// check it in
			if ( openFileDialog.getFile() != null )
			{
				try
				{
					Runtime.getRuntime().exec( CHECKIN_SCRIPT
											   + " "
											   + openFileDialog.getFile() );
				}
				catch ( IOException x )
				{
					printErr ( "ERROR: Couldn't launch check in script \""
							   + MarkupTranslator.escape(CHECKIN_SCRIPT)
							   + "\": "
							   + x );
				}
			}
		}
		else if ( cmd.equals("Quit") )
		{
			// clean up and exit
			if ( cleanup() )
				System.exit(0);
		}
		// Edit menu -- currently unimplemented
		else if ( cmd.equals("Undo") ) ;
		else if ( cmd.equals("Cut") ) ;
		else if ( cmd.equals("Copy") ) ;
		else if ( cmd.equals("Paste") ) ;
		else if ( cmd.equals("Clear") ) ;
		else if ( cmd.equals("Prefs") ) ;
		// View menu
		else if ( cmd.equals("Debug") )
		{
			MessageFormatter.debug = true;
			menu.setItemCommand( "Show Debug Messages", "NoDebug" );
			menu.setItemChecked( "Show Debug Messages", true );
		}
		else if ( cmd.equals("NoDebug") )
		{
			MessageFormatter.debug = false;
			menu.setItemCommand( "Show Debug Messages", "Debug" );
			menu.setItemChecked( "Show Debug Messages", false );
		}
		else if ( cmd.equals("AtomIDs") )
		{
			MessageFormatter.showID = true;
			menu.setItemCommand( "Show Atom IDs", "NoAtomIDs" );
			menu.setItemChecked( "Show Atom IDs", true );
		}
		else if ( cmd.equals("NoAtomIDs") )
		{
			MessageFormatter.showID = false;
			menu.setItemCommand( "Show Atom IDs", "AtomIDs" );
			menu.setItemChecked( "Show Atom IDs", false );
		}
		else if ( cmd.equals("Verbose") )
		{
			MessageFormatter.verbose = true;
			menu.setItemCommand( "Short Descriptions", "Terse" );
			menu.setItemChecked( "Short Descriptions", false );
		}
		else if ( cmd.equals("Terse") )
		{
			MessageFormatter.verbose = false;
			menu.setItemCommand( "Short Descriptions", "Verbose" );
			menu.setItemChecked( "Short Descriptions", true );
		}
		else if ( cmd.equals("EchoOut") )
		{
			echoOut = true;
			menu.setItemCommand( "Echo Outgoing Messages", "NoEchoOut" );
			menu.setItemChecked( "Echo Outgoing Messages", true );
		}
		else if ( cmd.equals("NoEchoOut") )
		{
			echoOut = false;
			menu.setItemCommand( "Echo Outgoing Messages", "EchoOut" );
			menu.setItemChecked( "Echo Outgoing Messages", false );
		}
		// Help menu -- currently unimplemented
		else if ( cmd.equals("About") ) ;
		else if ( cmd.equals("Help") ) ;
	}

	//----------------------------------------------------------------

	/**
	 *  Handle the closing of windows. At present this app only
	 *  has a single window, so closing it is equivalent to
	 *  quitting.
	 */
	public void windowClosing ( WindowEvent e )
	{
		// attempt to cleanup and exit if successful
		if ( e.getWindow() == mainWindow )
		{
			if ( cleanup() )
				System.exit(0);
		}
		else
			e.getWindow().setVisible ( false );
	}

	//----------------------------------------------------------------
	//  world interaction
	//----------------------------------------------------------------

	/**
	 *  (Re-)initialize the world database and extract a player
	 *  and parser to interface with.
	 */
	public void loadDatabase ( String fileName )
	{
		try
		{
			println ( "Loading world..." );
			
			world = WorldFactory.newWorld( fileName, scriptPath );
			world.start();
						
			// get the things we need for communication
			parser = world.newParser();
			player = world.getAtom(world.ADMIN_ID);
			
			// add a watcher to the player that routes
			// messages to our front end
			player.addWatcher ( new ToolWatcher ( player, true, this ) );
			
			println ( "World loaded." );
		}
		catch ( Exception e )
		{
			printErr ( "ERROR: unable to initialize database \""
					   + MarkupTranslator.escape(fileName)
					   + "\": "
					   + e );
		}
	}
	
	//----------------------------------------------------------------

	/**
	 *  Add a path to the world's search path for scripts.
	 */
	public void addPath ( String searchPath )
	{
		world.addPath( searchPath );
	}

	//----------------------------------------------------------------

	/**
	 *  Compile and and run an ODL file.
	 */
	public void importODL ( String pathName )
	{
		ODLCompiler odl = new ODLCompiler ();
		odl.listen ( this );
		
		println ( "<color magenta>Compiling "
				  + MarkupTranslator.escape ( pathName )
				  + "</color>");
		
		Vector scripts = odl.compile( pathName );
		
		if ( scripts.size() == 0 )
		{
			printErr("ERROR: Compilation failed");
		}
		else
		{
			println ( "<color magenta>Executing compiled scripts</color>" );
			
			// really should run the dependency compiler here,
			// but since we only compile a single file there can
			// only be one set of output files and these are added
			// to the vector in the right order by ODLCompiler
			// so for the time being just run them and keep fingers
			// crossed
			Enumeration enum = scripts.elements();

			while ( enum.hasMoreElements() )
			{
				String scriptName = (String) enum.nextElement();
				File srcFile = new File ( scriptName );
				String shortName = srcFile.getName();
				File dstFile = new File ( shortName );
				
				// move script into the script path (".") if somewhere else				
				try
				{
					if ( ! srcFile.getCanonicalPath().equals(dstFile.getCanonicalPath()) )
					{
						try
						{
							FileUtil.copy ( srcFile, dstFile );
							srcFile.delete();
						}
						catch ( IOException e )
						{
							printErr( "ERROR: Couldn't move script "
									  + MarkupTranslator.escape(shortName)
									  + " into the script path" );
							return;
						}
					}
				}
				catch ( IOException e )
				{
				}
				
				send ( "!run " + shortName );
			}
		}
		
	}
	
	//----------------------------------------------------------------
	//  i/o
	//----------------------------------------------------------------
	
	/**
	 *  Send a string to the world as if from the administrator.
	 */
	public void send ( String command )
	{
		if ( echoOut )
			println ( "<font monospaced><colour gray>" + command + "</colour></font>" );
			
		if ( player != null
			 && parser != null
			 && command != null )
			world.parseCommand ( command, player, parser );
	}

	//----------------------------------------------------------------

	/**
	 *  Receive a message from the world.
	 */
	public void receive ( String message )
	{
		log ( message );
		println ( MessageFormatter.format(message) );
	}

	//----------------------------------------------------------------

	/**
	 *  Receive an error message from the ODLCompiler.
	 */
	public boolean hear ( String message )
	{
		printErr ( MarkupTranslator.escape( message ) );
		return true;
	}
	
	//----------------------------------------------------------------

	/**
	 *  Print a message to the log display.
	 */
	public void log ( String msg )
	{
		if ( ! msg.endsWith ( System.getProperty ( "line.separator" ) ) )
			msg += System.getProperty("line.separator");
		
		// make sure that the text area remains within its
		// ~30k limit
		String newText = textLog.getText() + msg;
		
		if ( newText.length() > MAX_CAPACITY )
		{
			newText = newText.substring ( newText.length() - RETAIN_AMOUNT );
			textLog.setText( newText );
			textLog.append("");
		}
		else
		{
			textLog.append(msg);
		}
	}
	
	//----------------------------------------------------------------

	/**
	 *  Print a (styled) message to the main console.
	 */
	public void print ( String str )
	{
		console.append ( new MarkupTranslator(str).getGlyphArray() );
	}
	
	//----------------------------------------------------------------

	/**
	 *  Print a (styled) message to the main console,
	 *  appending a line break.
	 */
	public void println ( String str )
	{
		print ( str + "<br>" );
	}

	//----------------------------------------------------------------

	/**
	 *  Print an error message to the main console.
	 */
	public void printErr ( String str )
	{
		print ( "<color red>" + str + "</color><br>" );
	}

	//----------------------------------------------------------------
	//  utilities
	//----------------------------------------------------------------

	/**
	 *  Print a usage message for this application to
	 *  <tt>System.err</tt>.
	 */
	public void printUsage ()
	{
		System.err.println("WorldRunner [" + VERSION + "] usage:");
		System.err.println("java com.ogalala.tools.WorldRunner [options [args]]");
		System.err.println("  options:");
		System.err.println("  -d dataBase       use the specified database file" );
		System.err.println("  -s scriptPath     search for scriptPath for script files");
		System.err.println("  -t textEditor     use textEditor to edit ODL files");
		System.err.println("  -h | --help | -?  print this usage message");
	}
		
	//----------------------------------------------------------------
	//  Mapper implementation
	//----------------------------------------------------------------

	/** Set the map URL. From Mapper. Not currently implemented. */
	public void setMapImage ( String url ) {}
	
	//----------------------------------------------------------------

	/** Set the map location. From Mapper. Not currently implemented. */
	public void setMapLocation ( Point loc ) {}

	//----------------------------------------------------------------	

}