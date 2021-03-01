// $Id: WorldWindow.java,v 1.3 1999/03/17 17:52:58 matt Exp $
// Test runner for ODL, as a service window.
// Matthew Caldwell, 8 March 1999
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
 *  The WorldWindow class is used to run a game world in
 *  single-player mode for testing purposes. It is based
 *  on the WorldRunner application, but is restricted to
 *  a single window for use by other apps (specifically
 *  the AtomBuilder).
 */
public class WorldWindow
	extends Frame
	implements ActionListener, Mapper, ToolWatchReceiver
{
	//----------------------------------------------------------------	
	//  class variables
	//----------------------------------------------------------------	

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
	
	/** Version string. */
	public static final String VERSION = "$Revision: 1.3 $";

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
	private String dbName;
	
	/** Whether to echo outgoing messages to the console. */
	private boolean echoOut = true;
	
	//----------------------------------------------------------------	
	// interface elements
	//----------------------------------------------------------------	

	/** The styled output panel. */
	private ScrollingStyledTextPanel console = new ScrollingStyledTextPanel();
	
	/** The text entry field. */
	private CompletionField entryField = new CompletionField();
	
	/** Command history for the entry field. */
	private CommandHistory cmdHistory
		= new CommandHistory ( entryField, HISTORY_LINES );

	/** Text area to log unformatted world output. */
	private TextArea textLog
		= new TextArea( "", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY );

	//----------------------------------------------------------------
	//  construction
	//----------------------------------------------------------------

	/** Default constructor. */
	public WorldWindow ()
	{
		this ( AtomBuilder.WORK_DB );
	}
	
	//----------------------------------------------------------------	

	/** Constructor specifying a database file. */
	public WorldWindow ( String database )
	{
		super ( "World Tester: " + database );
		dbName = database;
		init ();
	}

	//----------------------------------------------------------------
	//  initialization
	//----------------------------------------------------------------

	/**
	 *  Run the application.
	 */
	protected void init ()
	{
		// put the interface elements together
		setBounds ( 50, 50, 450, 400 );
		Insets mainInsets = getInsets();
		
		// lay it out
		Panel panel = new Panel ();
		panel.setLayout ( new BindLayout() );
		
		panel.add ( entryField,
				    new BindConstraints ( 200, 20,
				 					      mainInsets.left,
				 					      BindConstraints.NONE,
				 					      mainInsets.right,
				 					      mainInsets.bottom ) );
		panel.add ( textLog,
				    new BindConstraints ( 200, 60,
				 					      mainInsets.left,
				 					      BindConstraints.NONE,
				 					      mainInsets.right,
				 					      mainInsets.bottom + 20 ) );
		panel.add ( console,
				    new BindConstraints ( 200, 100,
				 					      mainInsets.left,
				 					      mainInsets.top,
				 					      mainInsets.right,
				 					      mainInsets.bottom + 80 ) );
			 					    
		console.setEditable ( false );
		console.setBufferLines ( 400 );
		console.setDiscardLines ( 100 );
		
		setLayout ( new BorderLayout() );
		add ( "Center", panel );
		
		// listen for events in the entry field
		entryField.addActionListener ( this );
		
		// vanish in response to a click in the close box
		addWindowListener
		(
			// anonymous inner class to handle disposing of the window
			new WindowAdapter()
			{
				public void windowClosing ( WindowEvent e )
				{					
					world = null;
					parser = null;
					player = null;
					
					// dispose of the window
					Window wind = e.getWindow();
					wind.setVisible( false );
					wind.dispose();
				}
			}
		);
		
		// show the window
		setVisible( true );
		entryField.requestFocus();
		
		// configure the message formatting
		MessageFormatter.showID = true;
		MessageFormatter.debug = true;
		MessageFormatter.owner = this;
				
		// load the world
		loadDatabase ( dbName );
	}

	//----------------------------------------------------------------
	//  event handling
	//----------------------------------------------------------------
	
	/**
	 *  Handle action events: text entry only.
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
	}

	//----------------------------------------------------------------
	//  world interaction
	//----------------------------------------------------------------

	/**
	 *  Initialize the world database and extract a player
	 *  and parser to interface with.
	 */
	public void loadDatabase ( String fileName )
	{
		try
		{
			println ( "Loading world..." );
			
			world = WorldFactory.newWorld( fileName, scriptPath );
			
			// the world needs to process timer events
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
	//  Mapper implementation
	//----------------------------------------------------------------

	/** Set the map URL. From Mapper. Not currently implemented. */
	public void setMapImage ( String url ) {}
	
	//----------------------------------------------------------------

	/** Set the map location. From Mapper. Not currently implemented. */
	public void setMapLocation ( Point loc ) {}

	//----------------------------------------------------------------	
}