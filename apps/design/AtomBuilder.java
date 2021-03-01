// $Id: AtomBuilder.java,v 1.24 1999/03/17 18:41:36 matt Exp $
// Form-based ODL constructor.
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
 *  An application to provide GUI creation and editing of
 *  object description (ODL) files.
 *  <p>
 *  Or at least it might be, one day.
 */
public class AtomBuilder
	extends WindowAdapter
	implements ActionListener, ItemListener, Eavesdropper, Runnable
{
	//----------------------------------------------------------------
	//  class variables
	//----------------------------------------------------------------

	/**
	 *  An instance of this class running as an application.
	 *  This is instantiated by a call to <tt>main()</tt>.
	 */
	protected static AtomBuilder theApp = null;

	/**  Version string. */
	public static final String VERSION = "$Revision: 1.24 $";

	/** Name of an atom database containing atom documentation. */
	public static final String DOC_DB = "doc.db";

	/** Name of the atom database to use as the working database. */
	public static final String WORK_DB = "work.db";

	/** A script to check a file into CVS. */
	public static final String CHECKIN_SCRIPT = "odl_checkin.bat";

	/**  A script to check the ODL directory out of CVS. */
	public static final String CHECKOUT_SCRIPT = "odl_checkout.bat";

	/** A script to update the ODL directory. */
	public static final String UPDATE_SCRIPT = "odl_update.bat";

	/** Directory in which to place compiled scripts. */
	public static final String SCRIPT_DIR = "scripts";

	/** Dependency script generated when importing multiple files. */
	public static final String DEPENDENCY_SCRIPT = "dependencies.script";
	
	/** Default preferences file name. */
	public static final String DEFAULT_PREFS = "builder.prefs";
	
	/** Default property attributes file name. */
	public static final String DEFAULT_ATTRIB = "property.attributes";

	/**
	 *  The minimum amount of a window's saved location that
	 *  must be onscreen in each direction for the window not
	 *  to be forcibly relocated on startup.
	 */
	private static final int ONSCREEN_MARGIN = 20;

	//----------------------------------------------------------------
	//  instance variables
	//----------------------------------------------------------------

	/**
	 *  The name of the original database containing the atom
	 *  documentation.
	 */
	protected String docDB = DOC_DB;

	/**
	 *  The name of the working database containing the atom
	 *  documentation. Typically, this will be a copy of the
	 *  original docDB.
	 */
	protected String workDB = WORK_DB;

	/**
	 *  Path to search for scripts. This is only needed if the
	 *  docs database does not already exist.
	 */
	protected String scriptPath = "."
								  + System.getProperty("path.separator")
								  + SCRIPT_DIR;

	/** Whether to recreate the database from scratch. */
	protected boolean createDB = false;

	/** Whether to overwrite and existing working database. */
	protected boolean overwriteDB = false;

	/** A reference to the atom database. */
	protected AtomDatabase db = null;

	/** A reference to the world. */
	protected World world = null;

	/** An object to export atoms to an ODL file. */
	protected ODLExporter odlExport = new ODLExporter ();

	/** A list of opened, non-final ODL files. */
	protected StringVector odlFiles = new StringVector ();

	/** A list of newly-created ODL files. */
	protected StringVector newODLs = new StringVector ();

	/** A list of the names of all available (abstract) Atoms. */
	protected StringVector atomNames = new StringVector ();

	/** A list of the names of all available containers. */
	protected StringVector containers = new StringVector ();

	/** A list of the names of all available exit atoms. */
	protected StringVector exitList = new StringVector ();
	
	/** An object to keep track of application preferences. */
	protected BuilderPrefs prefs = new BuilderPrefs();
	
	/** Name of preferences file to use. */
	protected String prefsFile = DEFAULT_PREFS;
	
	/** Name of attributes file to use. */
	protected String attribFile = DEFAULT_ATTRIB;

	//----------------------------------------------------------------
	// interface elements
	//----------------------------------------------------------------

	/** The main window. */
	private BrowserFrame mainWindow
		= new BrowserFrame ( "1932.com Atom Builder [" + VERSION + "]",
							 null );

	/** A window to display the contents of the selected atom. */
	private AtomInspector inspector = new AtomInspector ( mainWindow );

	/** Manager for the menu bar. */
	private BuilderMenuManager menu = new BuilderMenuManager ( mainWindow );

	/** Directory selection dialog box. */
	private FileDialog folderDlg = new FileDialog ( mainWindow,
													"Select directory",
													FileDialog.SAVE );

	/** File open dialog box. */
	private FileDialog openDlg = new FileDialog ( mainWindow,
												  "Select file",
												  FileDialog.LOAD );

	/** File creation dialog box. */
	private FileDialog createDlg = new FileDialog ( mainWindow,
													"New file",
													FileDialog.SAVE );

	/** Dialog for creating a new atom. */
	private NewAtomDialog newAtomDlg = new NewAtomDialog ( mainWindow );

	/** Dialog to list the exits of a room. */
	private ExitInspector exitInspector = new ExitInspector ( mainWindow );

	/** Dialog for digging new exits. */
	private NewExitDialog newExitDlg = new NewExitDialog ( mainWindow );

	//----------------------------------------------------------------
	//  construction
	//----------------------------------------------------------------

	/** Default constructor. */
	public AtomBuilder ()
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
		theApp = new AtomBuilder ();
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
		// -d dbName : get docs from given database

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
			// -d: database file
			else if ( "-d".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					docDB = args[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -d requires an argument" );
					return false;
				}
			}
			// -w: working database file
			else if ( "-w".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					workDB = args[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -w requires an argument" );
					return false;
				}
			}
			// -p: prefs file
			else if ( "-p".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					prefsFile = args[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -p requires an argument" );
					return false;
				}
			}
			// -a: attributes file
			else if ( "-a".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					attribFile = args[argPtr + 1];
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -a requires an argument" );
					return false;
				}
			}
			// -o: overwrite existing workDB
			else if ( "-o".equals(args[argPtr]) )
			{
				overwriteDB = true;
				argPtr++;
			}
			// -c: create database
			else if ( "-c".equals(args[argPtr]) )
			{
				createDB = true;
				argPtr++;
			}
			// -s: scriptPath
			else if ( "-s".equals(args[argPtr]) )
			{
				if ( argPtr + 1 < args.length )
				{
					scriptPath = args[argPtr + 1]
								 + System.getProperty ( "path.separator" )
								 + SCRIPT_DIR;
					argPtr += 2;
				}
				else
				{
					System.err.println ( "ERROR: option -s requires an argument" );
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
		// close the world
		try
		{
			if ( world != null )
				WorldFactory.saveWorld(world);
		}
		catch ( IOException e )
		{
			System.out.println ( "AtomBuilder.cleanup(): exception closing world: "
								 + e );
		}

		// write the preferences
		try
		{
			flushPrefs ();
			prefs.write ( prefsFile );
		}
		catch ( IOException e )
		{
			System.out.println ( "AtomBuilder.cleanup(): exception writing prefs file: "
								 + e );
		}

		return true;
	}

	//----------------------------------------------------------------

	/**
	 *  Run the application.
	 */
	public void run ()
	{
		// put the interface elements together
		mainWindow.setBounds ( 50, 50, 400, 300 );

		// listen for menu events
		menu.addActionListener ( this );

		// create the atom tree for the main window
		createBrowser ();

		// listen for window events
		mainWindow.addWindowListener ( this );
		mainWindow.addItemListener ( this );

		// derive the lists of loaded ODL files,
		// atoms and containers
		findInfo();

		// set up the inspector
		setPropertyTraits();
		inspector.addWindowListener ( this );
		
		// read the preferences and set the window positions
		try
		{
			prefs.read ( prefsFile );
		}
		catch ( IOException e ) {}
		doPrefs();
		
		// show the window
		mainWindow.show();
	}

	//----------------------------------------------------------------

	/**
	 *  Create the outline in the atom browser from the
	 *  contents of the supplied atom database.
	 */
	public void createBrowser ()
	{
		// if we're expected to create the database from scripts
		// invoke the documentation compiler
		if ( createDB )
		{
			DocCompiler dcom = new DocCompiler();
			String[] args = { "-s", scriptPath, "-f", "main", "-d", docDB, "-z" };
			dcom.init(args);
			dcom.run();
		}

		try
		{
			// copy the docDB to the workDB, overwriting if required
			if ( overwriteDB )
			{
				FileUtil.copy ( docDB, workDB );
			}
			else
			{
				try
				{
					FileUtil.safeCopy ( docDB, workDB );
				}
				catch ( IOException x ) {}
			}

			world = WorldFactory.newWorld ( workDB, scriptPath );
			db = world.getAtomDatabase ();

			OpenClosedTree atomHierarchy = generateAtomTree ( db );
			mainWindow.setContents ( atomHierarchy );
		}
		catch ( IOException e )
		{
			printErr ( "ERROR: couldn't open docs database \""
					   + workDB
					   + "\": "
					   + e );
		}
		catch ( WorldException e )
		{
			printErr ( "ERROR: couldn't open docs database \""
					   + workDB
					   + "\": "
					   + e );
		}
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

		if ( cmd.equals("Quit") )
		{
			// clean up and exit
			// #### may want to change this when running
			// #### as a subsidiary of another program
			if ( cleanup() )
				System.exit(0);
		}
		else if ( cmd.equals("NewODL") )
		{
			doNewODL();
		}
		else if ( cmd.equals("Save") )
		{
			doSaveODL();
		}
		else if ( cmd.equals("SaveAll") )
		{
			doSaveAll();
		}
		else if ( cmd.equals("Import") )
		{
			doImport();
		}
		else if ( cmd.equals("Run") )
		{
			doRun();
		}
		else if ( cmd.equals("NewAtom") )
		{
			doNewAtom();
		}
		else if ( cmd.equals("DelAtom") )
		{
			doDeleteAtom();
		}
		else if ( cmd.equals("FindAtom") )
		{
			doFindAtom();
		}
		else if ( cmd.equals("ShowInspector") )
		{
			inspector.setVisible ( true );
			inspector.toFront();
		}
		else if ( cmd.equals("ShowExits") )
		{
			exitInspector.setVisible ( true );
			exitInspector.toFront();
		}
		else if ( cmd.equals("NewExit") )
		{
			doNewExit();
		}
		else if ( cmd.equals("DelExit") )
		{
			doDeleteExit();
		}
		else if ( cmd.equals("Revert") )
		{
			doRevert();
		}
		else if ( cmd.equals("CheckIn") )
		{
			doCheckIn();
		}
		else if ( cmd.equals("Update") )
		{
			doUpdate();
		}
		else if ( cmd.equals("TestWorld") )
		{
			doTestWorld();
		}
		else if ( cmd.equals("Refresh") )
		{
			refresh();
		}
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
			// #### may want to change this when running
			// #### as a subsidiary of another program
			if ( cleanup() )
				System.exit(0);
		}
		else
			e.getWindow().setVisible ( false );
	}

	//----------------------------------------------------------------

	/**
	 *  Receive notification of item events in the browser
	 *  window (possibly elsewhere as well, eventually).
	 */
	public void itemStateChanged ( ItemEvent e )
	{
		if ( e.getSource() == mainWindow.getOutline() )
		{
			// identify the name of the atom associated
			// with the selected item
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				OpenClosedTree node = (OpenClosedTree) e.getItem();
				Object data = node.getData();

				if ( data instanceof OutlineNodeData )
				{
					String atomName = ((OutlineNodeData) data).name;
					Atom atom;

					// names of exits may be given in the form
					// room.direction, in which case we have to map
					// back to the actual exit object
					if ( atomName.indexOf(".") != -1 )
					{
						String roomName = atomName.substring ( 0, atomName.indexOf(".") );
						String direction = atomName.substring ( atomName.indexOf(".") + 1 );
						Atom room = db.getAtom ( roomName );
						atom = (Atom) room.getExit ( ExitTable.toDirection ( direction ) );
					}
					else
					{
						atom = db.getAtom ( atomName );
					}

					inspector.setAtom ( atom );
					inspector.setVisible ( true );
					exitInspector.setAtom ( atom );
					if ( atom instanceof com.ogalala.mua.Container )
						exitInspector.setVisible ( true );
				}
				else
				{
					System.out.println ( "WARNING: browser item is not OutlineNodeData: "
										 + data );
				}
			}
		}
	}

	//----------------------------------------------------------------
	//  i/o
	//----------------------------------------------------------------

	/**
	 *  Receive an error message from the ODLCompiler.
	 */
	public boolean hear ( String message )
	{
		printErr ( message );
		return true;
	}

	//----------------------------------------------------------------

	/**
	 *  Print a message to the log display.
	 */
	public void log ( String msg )
	{
		System.out.println ( msg );
	}

	//----------------------------------------------------------------

	/**
	 *  Print a message.
	 */
	public void print ( String str )
	{
		System.out.print ( str );
	}

	//----------------------------------------------------------------

	/**
	 *  Print a message to the main console,
	 *  appending a line break.
	 */
	public void println ( String str )
	{
		System.out.println ( str );
	}

	//----------------------------------------------------------------

	/**
	 *  Print an error message to the main console.
	 */
	public void printErr ( String str )
	{
		System.out.println ( str );
	}

	//----------------------------------------------------------------
	//  utilities
	//----------------------------------------------------------------

	/**
	 *  Generate a complete OpenClosedTree with OutlineNodeData
	 *  data values containing atom names and documentation
	 *  in a form suitable for display with an Outline component.
	 *  Note that atoms may appear multiple times in this tree,
	 *  but their children are only expanded the first time.
	 *  This may be changed if it turns out to be confusing.
	 *  Note also that the outermost tree node is empty, since
	 *  it represents the containing Outline view.
	 *  <p>
	 *  This method is static since it may be of more general use
	 *  but I haven't got a sensible place to keep it. It will
	 *  probably move somewhere else eventually.
	 */
	public static OpenClosedTree generateAtomTree ( AtomDatabase db )
	{
		OpenClosedTree treeRoot = new OpenClosedTree ();
		Atom startAtom = db.getRoot();
		Stack enumStack = new Stack ();
		Stack nodeStack = new Stack ();

		Enumeration enum = startAtom.getChildren();

		// start traversal
		// originally this used atom marks to determine whether
		// an atom's children had already been traversed, but
		// this conflicts with AtomDocumenter's calls to
		// isDescendantOf(), so now we have a local Hashtable
		// and use that instead
		Hashtable marker = new Hashtable();

		// add the root atom
		OpenClosedTree node = AtomDocumenter.getAtomTreeNode ( startAtom );
		treeRoot.addChild ( node );

		boolean finished = ! enum.hasMoreElements();

		while ( !finished )
		{
			// get the next atom
			Atom atom = (Atom) enum.nextElement();

			OpenClosedTree childNode = AtomDocumenter.getAtomTreeNode ( atom );

			node.addChild ( childNode );

			// if the atom isn't marked, we need to
			// process it and iterate its children
			if ( marker.get(atom) == null )
			{
				// mark the atom (only traverse children once)
				marker.put ( atom, atom );

				if ( atom.hasChildren() )
				{
					// push the current list and node
					enumStack.push ( enum );
					nodeStack.push ( node );

					node = childNode;
					enum = atom.getChildren();
				}
			}

			// if the current enum is finished, unwind the
			// stack until we find an enum that has elements
			// left or the stack is empty
			while ( !enum.hasMoreElements() && !finished )
			{
				if ( enumStack.empty() )
					finished = true;
				else
				{
					enum = (Enumeration) enumStack.pop();
					node = (OpenClosedTree) nodeStack.pop();
				}
			}
		}

		return treeRoot;
	}

	//----------------------------------------------------------------

	/**
	 *  Extract the names of containers, atoms and ODL files
	 *  from the atom database and flush the new lists to
	 *  the dialogs that use them. This method combines
	 *  the functionality of the old <tt>findODLs()</tt>,
	 *  <tt>findAtoms()</tt> and <tt>findContainers()</tt>
	 *  methods, so they now only require a single pass
	 *  through the atom database.
	 */
	public void findInfo ()
	{
		// the atom database must have been successfully
		// initialized
		if ( db == null )
			return;

		// clear the existing lists
		odlFiles.removeAllElements();
		atomNames.removeAllElements();
		containers.removeAllElements();
		exitList.removeAllElements();

		// get the exit atom so we can check inheritance against it
		Atom exitAtom = db.getAtom ( "exit" );

		// iterate through the atom database
		Enumeration atoms = db.getAtoms();
		while ( atoms.hasMoreElements() )
		{
			Atom atom = (Atom) atoms.nextElement();

			// get source odl from non-final atoms
			if ( atom.getField("__final__") == null
				 && atom.getField("__odl__") != null )
			{
				String source = ((String) atom.getField ( "__odl__" )).intern();

				if ( ! odlFiles.contains( source ) )
					odlFiles.addElement ( source );
			}

			// get abstract atoms
			if ( ! ( atom instanceof Thing ) )
			{
				atomNames.put ( atom.getID() );

				// get exits
				if ( atom.isDescendantOf ( exitAtom ) )
					exitList.put ( atom.getID() );
			}

			// get containers
			if ( ( atom instanceof com.ogalala.mua.Container )
				 && ! ( atom instanceof Mobile ) )
				containers.put ( atom.getID() );
		}

		// add in any odl files that have been
		// created but not yet assigned any atoms
		Enumeration newNames = newODLs.elements();
		while ( newNames.hasMoreElements() )
		{
			String newName = ((String) newNames.nextElement()).intern();
			if ( ! odlFiles.contains ( newName ) )
				odlFiles.addElement ( newName );
		}

		// sort the lists into alpha order
		odlFiles.sort();
		atomNames.sort();
		containers.sort();
		exitList.sort();

		// having populated the lists, set them in the dialogs
		inspector.setODLList ( odlFiles );
		inspector.setAtomList ( atomNames );
		inspector.setContainerList ( containers );

		newAtomDlg.setODLList ( odlFiles );
		newAtomDlg.setAtomList ( atomNames );

		newExitDlg.setODLList ( odlFiles );
		newExitDlg.setContainerList ( containers );
		newExitDlg.setExitList ( exitList );
	}

	//----------------------------------------------------------------

	/**
	 *  Ask the user to select one of the loaded ODL files,
	 *  and then write to it all the atoms currently
	 *  specified as belonging in it.
	 */
	public void doSaveODL ()
	{
		// choose the ODL file to save
		String target = Chooser.choose ( mainWindow,
										 "Select ODL file to save",
										 odlFiles );

		// check to see whether user cancelled
		if ( target == null )
		{
			log ( "AtomBuilder.doSaveODL(): No target ODL selected" );
			return;
		}
		else
			log ( "AtomBuilder.doSaveODL(): Selected target " + target );

		// ask the user to specify a destination directory
		folderDlg.setFile( target );
		folderDlg.show();

		if ( folderDlg.getFile() == null )
		{
			log ( "AtomBuilder.doSaveODL(): No save folder specified" );
			return;
		}

		// #### should ask for a file comment, but I'll deal with
		// #### that a little later

		saveODL ( target,
				  folderDlg.getDirectory(),
				  "[No descriptive comment available]" );
	}


	//----------------------------------------------------------------

	/**
	 *  Export all non-final odl files to a chosen directory.
	 */
	public void doSaveAll ()
	{
		// if there are no odl files, this is a waste of time
		if ( odlFiles.size() == 0 )
			return;

		// ask the user to specify a destination directory
		folderDlg.setFile( "all ODL files" );
		folderDlg.show();

		if ( folderDlg.getFile() == null )
		{
			log ( "AtomBuilder.doSaveAll(): No save folder specified" );
			return;
		}

		String path = folderDlg.getDirectory();

		// #### should ask for a file comment, but I'll deal with
		// #### that a little later

		// save each ODL file in turn
		Enumeration enum = odlFiles.elements();
		while ( enum.hasMoreElements() )
		{
			String target = (String) enum.nextElement();
			saveODL ( target, path, "[No descriptive comment available]" );
		}

	}

	//----------------------------------------------------------------

	/**
	 *  Save a specified ODL file to the specified directory.
	 */
	public void saveODL ( String target,
						  String path,
						  String fileComment )
	{
		if ( target == null )
			return;

		// scan the atom database for all atoms belonging
		// in the selected ODL file
		Enumeration atoms = db.getAtoms();
		AtomVector exportAtoms = new AtomVector();

		log ( "AtomBuilder.saveODL(): scanning for atoms belonging to " + target );

		while ( atoms.hasMoreElements() )
		{
			Atom atom = (Atom) atoms.nextElement();

			if ( target.equals ((String) atom.getField ( "__odl__" )) )
				exportAtoms.put( atom );
		}

		log ( "AtomBuilder.saveODL(): scan complete" );

		if ( exportAtoms.size() == 0 )
		{
			log ( "AtomBuilder.saveODL(): Specified ODL contains no atoms" );
			return;
		}

		try
		{
			odlExport.export ( exportAtoms,
							   prefs.user,
							   fileComment,
							   path + target );
		}
		catch ( IOException e )
		{
			Alert.showAlert ( mainWindow,
							  "An exception was thrown writing to "
							  + path + target
							  + ": "
							  + e );
		}
	}

	//----------------------------------------------------------------

	/** Ask the user to specify an ODL file, then import it. */
	public void doImport ()
	{
		Vector files = MultiFileSelect.select ( mainWindow );
		if ( files == null || files.size() == 0 )
		{
			return;
		}

		System.out.println ( "Importing files " + files );
		importODLs ( files );
	}

	//----------------------------------------------------------------

	/** Ask the user to specify a script file, then run it. */
	public synchronized void doRun ()
	{
		openDlg.show();

		if ( openDlg.getFile() == null )
		{
			log ( "AtomBuilder.doRun(): No script file selected" );
			return;
		}

		close();
		runScript ( openDlg.getDirectory() + openDlg.getFile() );
		reload();
	}

	//----------------------------------------------------------------

	/**
	 *  Compile and run a bunch of ODL files.
	 */
	public synchronized void importODLs ( Vector pathNames )
	{
		ODLCompiler odl = new ODLCompiler ();
		odl.listen ( this );

		Vector scripts = new Vector();

		// compile each ODL file in turn
		Enumeration enum = pathNames.elements();
		while ( enum.hasMoreElements() )
		{
			String pathName = (String) enum.nextElement();
			Vector someScripts = odl.compile( pathName );

			Enumeration scriptEnum = someScripts.elements();
			while ( scriptEnum.hasMoreElements() )
			{
				String scriptName = (String) scriptEnum.nextElement();
				File srcFile = new File ( scriptName );
				String shortName = srcFile.getName();
				File dstFile = new File ( SCRIPT_DIR
										  + System.getProperty("file.separator")
										  + shortName );
				scripts.addElement( shortName );

				// move script into the compiled scripts directory
				// if somewhere else
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
									  + shortName
									  + " into the script path" );
							return;
						}
					}
				}
				catch ( IOException e )
				{
				}
			}
		}

		// we should now have a list of scripts that need to be run
		if ( scripts.size() == 0 )
			return;
		else
		{
			close();

			if ( scripts.size() == 1 )
				runScript ( (String) scripts.elementAt(0) );
			else
			{
				// we must first invoke the dependency compiler
				// and then run the dependency script

				// create the dependency compiler arg list
				String[] args = new String [ scripts.size() + 2 ];
				args[0] = "-o";
				args[1] = SCRIPT_DIR
						  + System.getProperty("file.separator")
						  + DEPENDENCY_SCRIPT;

				for ( int i = 0; i < scripts.size(); i++ )
					args[i+2] = SCRIPT_DIR
								+ System.getProperty("file.separator")
								+(String) scripts.elementAt(i);

				DependencyCompiler dep = new DependencyCompiler();
				dep.init(args);
				dep.run();

				// if compilation failed, we can't go on
				if ( ! dep.succeeded )
				{
					printErr ( "ERROR: dependency failure -- compilation aborted" );
					return;
				}

				// run the dependency script
				runScript ( DEPENDENCY_SCRIPT );
			}

			reload();
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Compile and run an ODL file.
	 */
	public synchronized void importODL ( String pathName )
	{
		ODLCompiler odl = new ODLCompiler ();
		odl.listen ( this );

		println ( "AtomBuilder.importODL(): importing "
				  + pathName );

		Vector scripts = odl.compile( pathName );

		if ( scripts.size() == 0 )
		{
			printErr("ERROR: Compilation failed");
		}
		else
		{
			// really should run the dependency compiler here,
			// but since we only compile a single file there can
			// only be one set of output files and these are added
			// to the vector in the right order by ODLCompiler
			// so for the time being just run them and keep fingers
			// crossed
			Enumeration enum = scripts.elements();

			if ( !enum.hasMoreElements() )
				return;

			// flush any changes back to the disk database
			close();

			while ( enum.hasMoreElements() )
			{
				String scriptName = (String) enum.nextElement();
				File srcFile = new File ( scriptName );
				String shortName = srcFile.getName();
				File dstFile = new File ( SCRIPT_DIR
										  + System.getProperty("file.separator")
										  + shortName );

				// move script into the script path if somewhere else
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
									  + shortName
									  + " into the script path" );
							return;
						}
					}
				}
				catch ( IOException e )
				{
				}

				// run the script
				println ( "AtomBuilder.importODL(): executing compiled script "
						  + shortName );
				runScript ( shortName );
			}

			// reload the in-memory world and refresh the UI
			reload();
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Execute a script file. This uses the DocCompiler to
	 *  generate the new contents, but unfortunately that compiles
	 *  the stuff into the database without updating the
	 *  in-memory version, so that has to be taken care of
	 *  as well. It would be possible to wrap that in this
	 *  method, but in many cases this method is called by
	 *  others that are running several scripts at once and
	 *  it would be rather inefficient to do it every time.
	 *  Instead, the caller is responsible for calling either
	 *  <tt>close()</tt> before running any scripts, and
	 *  <tt>reload</tt> afterwards.
	 */
	public void runScript ( String name )
	{
		// scripts are run by feeding them to the
		// DocCompiler rather than trying to use the
		// World, since (i) that probably wouldn't work
		// anyway and (ii) we need the doc comments etc
		DocCompiler dcom = new DocCompiler();
		String[] args = { "-s", scriptPath, "-f", name, "-d", workDB, "-z" };
		dcom.init(args);
		dcom.run();
	}

	//----------------------------------------------------------------

	/**
	 *  Flush any changes back to the on-disk database, leaving
	 *  the world running.
	 */
	public synchronized void close ()
	{
		try
		{
			WorldFactory.saveWorld(world);
		}
		catch ( IOException e )
		{
			printErr ( "AtomBuilder.close(): " + e );
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Import any changes to the on-disk database into the
	 *  world and refresh the UI.
	 */
	public synchronized void reload ()
	{
		// reload the world
		try
		{
			world = WorldFactory.newWorld ( workDB, scriptPath );
			db = world.getAtomDatabase ();
		}
		catch ( IOException e )
		{
			printErr ( "AtomBuilder.reload(): " + e );
		}
		catch ( WorldException e )
		{
			printErr ( "AtomBuilder.reload(): " + e );
		}

		refresh();
	}

	//----------------------------------------------------------------

	/**
	 *  Refresh the UI. Specifically, the browser window
	 *  needs to be rebuilt to account for changes in the
	 *  inheritance hierarchy, and the atom and container
	 *  lists need to be updated and flushed through to
	 *  the dialogs that use them.
	 */
	public void refresh ()
	{
		// regenerate the atom browser
		OpenClosedTree atomHierarchy = generateAtomTree ( db );
		mainWindow.setContents ( atomHierarchy );
		mainWindow.addItemListener ( this );

		// regenerate the details lists
		findInfo();

		// reset the contents of the exit inspector
		exitInspector.setAtom ( exitInspector.getAtom() );
	}

	//----------------------------------------------------------------

	/**
	 *  Allow the user to create a new ODL file. Note that this
	 *  doesn't actually create a file, it just records the file
	 *  name as a possible repository for atoms. The file itself
	 *  is not actually created until it is saved.
	 */
	public void doNewODL ()
	{
		createDlg.show();

		if ( createDlg.getFile() == null )
		{
			log ( "AtomBuilder.doNewODL(): No ODL file selected" );
			return;
		}

		String newName = createDlg.getFile().intern();

		// record it in the newODLs list so it'll be remember
		// next time findInfo() is called
		newODLs.put ( newName );

		// flush it directly to the ODL files list and send
		// it to the dependent dialog so we don't have to
		// call findInfo() this time
		odlFiles.put ( newName );
		odlFiles.sort ();

		inspector.setODLList ( odlFiles );
		newAtomDlg.setODLList ( odlFiles );
		newExitDlg.setODLList ( odlFiles );
	}

	//----------------------------------------------------------------

	/** Allow the user to create a new atom or thing. */
	public void doNewAtom ()
	{
		// show a dialog allowing the user to specify
		// the details of the atom to create
		newAtomDlg.reset();
		newAtomDlg.show();

		if ( newAtomDlg.cancelled )
			return;

		// get the parents
		String[] parentNames = newAtomDlg.parents.getSelectedItems();
		AtomVector parentList = new AtomVector();

		if ( parentNames.length == 0 )
		{
			parentList.put ( db.getRoot() );
		}
		else
		{
			for ( int i = 0; i < parentNames.length; i++ )
			{
				Atom anAtom = db.getAtom ( parentNames[i] );
				if ( anAtom != null )
					parentList.put ( anAtom );
			}

			if ( parentList.size() == 0 )
				parentList.put ( db.getRoot() );
		}

		// if the new atom is a thing, we need to identify
		// the most demanding parent, but for atoms we don't
		Atom creation;

		if ( newAtomDlg.isThing.getState() )
		{
			// this is hard-coded and kind of hideous,
			// but at present is the best I can do
			Atom MOBILE = db.getAtom ( AtomDatabase.MOBILE_ID );
			Atom CONTAINER = db.getAtom ( AtomDatabase.CONTAINER_ID );
			Atom required = db.getAtom ( AtomDatabase.THING_ID );

			for ( int i = 0; i < parentList.size(); i++ )
			{
				Atom aParent = parentList.get ( i );
				if ( aParent.isDescendantOf ( MOBILE ) )
				{
					required = aParent;

					// this is the most demanding static type,
					// so there's no point in continuing
					break;
				}
				else if ( aParent.isDescendantOf ( CONTAINER ) )
				{
					required = aParent;
				}
			}

			creation = world.newThing ( newAtomDlg.id.getText(), required );
		}
		else
		{
			creation = world.newAtom ( newAtomDlg.id.getText(), parentList.get ( 0 ) );
		}

		// inherit from all parents
		Enumeration enum = parentList.elements();
		while ( enum.hasMoreElements() )
		{
			creation.inherit ( (Atom) enum.nextElement() );
		}

		// new things must have an ODL file and a source file
		// specified so as to be editable
		creation.setField ( "__", "Unknown" );
		creation.setField ( "__odl__", newAtomDlg.odl.getSelectedItem() );

		// add the doc comments, if any
		if ( ! newAtomDlg.comments.getText().trim().equals("") )
			creation.setField ( "__" + creation.getID(),
								newAtomDlg.comments.getText() );

		// refresh the UI
		refresh();

		// set the atom in the browser (which will in turn
		// set it in the inspector etc)
		mainWindow.select ( creation.getID() );
	}

	//----------------------------------------------------------------

	/** Allow the user to delete an atom. */
	public void doDeleteAtom ()
	{
		// get the name of the currently selected atom
		// from the browser
		String name = mainWindow.getSelectedItem();
		if ( name == null )
		{
			Alert.showAlert ( mainWindow,
							  "No atom is selected." );
			return;
		}

		// ask the user to confirm the deletion
		if ( Confirmation.confirm ( mainWindow,
									"Are you sure you want to delete the atom " + name + "?" ) )
		{
			// delete the atom and flush changes to the UI
			try
			{
				world.deleteAtom ( name );
			}
			catch ( AtomException e )
			{
				log ( "AtomBuilder.doDeleteAtom(): " + e );
			}

			refresh();

			// the deleted atom will be set in the inspector, so
			// set it to root instead
			inspector.setVisible( false );
			inspector.setAtom ( world.getRoot() );
			exitInspector.setAtom ( null );
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Attempt to locate a given atom in the browser.
	 *  At present finding is exact or not at all. It isn't
	 *  (yet?) possible to find an atom by a partial name
	 *  or grep pattern.
	 */
	public void doFindAtom ()
	{
		String name = InputDialog.get( mainWindow,
									   "Find Atom",
									   "Enter the name of the atom to be found" );
		if ( name == null )
			return;
		
		name = name.toLowerCase().trim();
		if ( name.equals("") )
			return;

		mainWindow.select ( name );
	}

	//----------------------------------------------------------------

	/**
	 *  Allow the user to create a new exit.
	 */
	public void doNewExit ()
	{
		// show the new exit dialog
		newExitDlg.reset();
		newExitDlg.show();

		if ( newExitDlg.cancelled )
			return;

		// get the details and attempt to construct the new exit
		String sourceName = newExitDlg.sourceRoom.getSelectedItem();
		String destName = newExitDlg.destRoom.getSelectedItem();
		String sourceClassName = newExitDlg.sourceClass.getSelectedItem();
		String destClassName = newExitDlg.destClass.getSelectedItem();
		int srcDirection = newExitDlg.getDirections ( NewExitDialog.SOURCE );
		int dstDirection = newExitDlg.getDirections ( NewExitDialog.DESTINATION );

		// get the relevant atoms
		Atom sourceRoom = db.getAtom ( sourceName );
		Atom destRoom = db.getAtom ( destName );
		Atom sourceTemplate = db.getAtom ( sourceClassName );
		Atom destTemplate = db.getAtom ( destClassName );

		// check if there are exits already defined
		// in any of the directions
		if ( sourceRoom.getExit ( srcDirection ) != null
			 || destRoom.getExit ( srcDirection) != null )
		{
			// ask user to confirm deletion of existing exit(s)
			if ( ! Confirmation.confirm ( mainWindow,
										  "Confirm exit changes",
										  "Exits already exist for one or more of the specified directions. "
										  + "Are you sure you want to replace these?" ) )
				return;

			// remove existing exits
			// #### note that if multiple exits are covered by the new
			// #### directions this won't work correctly -- this should
			// #### be corrected later...
			world.removeExit ( srcDirection, sourceRoom );
			world.removeExit ( dstDirection, destRoom );
		}

		// create the exit Things
		Atom srcExit = world.newThing ( world.getUniqueID(sourceClassName),
										sourceTemplate );
		Atom dstExit = null;

		// add the new exit(s) to the world
		if ( dstDirection == 0 )
		{
			world.addExit ( srcDirection,
							sourceRoom,
							srcExit,
							destRoom );
		}
		else
		{
			dstExit = world.newThing ( world.getUniqueID(destClassName),
									   destTemplate );
			world.addExit ( srcDirection,
							sourceRoom,
							srcExit,
							dstDirection,
							destRoom,
							dstExit );
		}

		// add documentation fields to the atoms
		if ( ! newExitDlg.comments.getText().equals("") )
		{
			srcExit.setField ( "__" + srcExit.getID(),
							   newExitDlg.comments.getText() );
			if ( dstExit != null )
				dstExit.setField ( "__" + dstExit.getID(),
								   newExitDlg.comments.getText() );
		}

		srcExit.setField ( "__", "unknown" );
		srcExit.setField ( "__odl__", newExitDlg.odl.getSelectedItem() );

		if ( dstExit != null )
		{
			dstExit.setField ( "__", "unknown" );
			dstExit.setField ( "__odl__", newExitDlg.odl.getSelectedItem() );
		}

		// refresh the UI to include the new exits
		refresh();
		
		// set the source exit in the browser
		mainWindow.select ( AtomDocumenter.getAtomName(srcExit) );
	}

	//----------------------------------------------------------------

	/**
	 *  Allow the user to remove an existing exit.
	 */
	public void doDeleteExit ()
	{
		// delete the exit currently selected in the
		// ExitInspector
		Atom room = exitInspector.getAtom();
		if ( ! ( room instanceof com.ogalala.mua.Container ) )
		{
			Alert.showAlert ( mainWindow,
							  "The current atom is not a room." );
			return;
		}

		String direction = exitInspector.getSelectedItem();
		if ( direction == null
			 || direction.length() == 0 )
		{
			Alert.showAlert ( mainWindow,
							  "No exit is currently selected." );
			return;
		}

		// request confirmation
		if ( ! Confirmation.confirm ( mainWindow,
									  "Confirm exit deletion",
									  "Are you sure you want to remove the exit leading "
									  + direction
									  + " from room "
									  + room.getID()
									  + "?" ) )
			return;

		// reduce the string to a single direction
		if ( direction.indexOf(",") != -1 )
			direction = direction.substring ( 0, direction.indexOf(",") );

		// convert the direction into an int
		int dirInt = ExitTable.toDirection ( direction );

		// remove
		world.removeExit ( dirInt, room );

		// refresh the UI
		refresh();
	}

	//----------------------------------------------------------------

	/**
	 *  Revert to the core version of the world, forgetting
	 *  any changes made since.
	 */
	public void doRevert ()
	{
		// check that the user really wants to do this
		if ( ! Confirmation.confirm ( mainWindow,
									  "Warning: this will forget any changes you have made. "
									  + "Are you sure you want to revert to the core database?" ) )
			return;

		// copy the core database over the working one
		try
		{
			FileUtil.copy ( docDB, workDB );
		}
		catch ( IOException e )
		{
			System.out.println ( "Exception when attempting to revert working database: " + e );
		}

		// load the world anew
		reload();
		
		// select the root atom in the main window
		// (since the current selection will be referring to
		// an atom in the now defunct world)
		mainWindow.select ( "root" );
	}

	//----------------------------------------------------------------

	/**
	 *  Perform a CVS check in operation. This is actually
	 *  delegated to a batch file or shell script.
	 */
	public void doCheckIn ()
	{
		// get the name of the file to check in
		openDlg.show();

		// check it in
		if ( openDlg.getFile() != null )
		{
			try
			{
				Runtime.getRuntime().exec( CHECKIN_SCRIPT
										   + " "
										   + openDlg.getFile() );
			}
			catch ( IOException x )
			{
				Alert.showAlert ( mainWindow,
								  "Couldn't launch checkin script \""
						   		  + UPDATE_SCRIPT
						   		  + "\": "
						   		  + x );
			}
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Perform a CVS update operation. This is actually
	 *  delegated to a batch file or shell script.
	 */
	public void doUpdate ()
	{
		try
		{
			Runtime.getRuntime().exec( UPDATE_SCRIPT );
		}
		catch ( IOException x )
		{
			Alert.showAlert ( mainWindow,
							  "Couldn't launch update script \""
					   		  + UPDATE_SCRIPT
					   		  + "\": "
					   		  + x );
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Create a test version of the world that the designer
	 *  can execute commands in and generally try out. The
	 *  test version is loaded read-only, so any actions
	 *  performed or commands executed do not alter the
	 *  world on disk.
	 */
	public void doTestWorld ()
	{
		// make sure the current version of the world is
		// saved to disk
		try { WorldFactory.saveWorld(world); }
		catch ( IOException e )
		{
			System.out.println ( "Exception saving world: " + e );
		}

		// create a new world runner from the same db
		WorldWindow whirlwind = new WorldWindow ( workDB );
	}

	//----------------------------------------------------------------

	/**
	 *  Set up the priorities for particular properties in the
	 *  the inspector. Eventually this will use a configuration
	 *  file, but for now just declare a few simple ones directly
	 *  for testing purposes.
	 */
	protected void setPropertyTraits ()
	{
		Properties props = new Properties();
		try
		{
			props.load ( new BufferedInputStream ( new FileInputStream( attribFile ) ) );
			
			Enumeration tags = props.keys();
			while ( tags.hasMoreElements() )
			{
				String tag = (String) tags.nextElement();
				
				if ( tag.indexOf ( "." ) == -1 )
					continue;
				
				String propName = tag.substring ( 0, tag.indexOf ( "." ) );
				String what = tag.substring ( tag.indexOf ( "." ) );
				if ( what.equalsIgnoreCase ( ".priority" ) )
				{
					String value = props.getProperty ( tag ).trim();
					if ( value.length() != 0 )
						AtomInspector.setPriority ( propName, value.charAt(0) );
				}
				else if ( what.equalsIgnoreCase ( ".immutable" ) )
				{
					String value = props.getProperty ( tag ).trim();
					if ( value.equalsIgnoreCase("true") )
						AtomInspector.setImmutable ( propName, true );
				}
			}
		}
		catch ( IOException e )
		{
			printErr ( "AtomBuilder.setPropertyTraits: error reading attributes file \""
					   + attribFile
					   + "\": "
					   + e );
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Synchronize the application state with any preferences.
	 *  For the time being this simply involves positioning all
	 *  the windows correctly.
	 */
	protected void doPrefs ()
	{
		if ( prefs.mainBounds != null )
			mainWindow.setBounds ( onscreen ( prefs.mainBounds ) );
		if ( prefs.atomBounds != null )
			inspector.setBounds ( onscreen ( prefs.atomBounds ) );
		if ( prefs.exitBounds != null )
			exitInspector.setBounds ( onscreen ( prefs.exitBounds ) );
		if ( prefs.openFileBounds != null )
			openDlg.setBounds ( onscreen ( prefs.openFileBounds ) );
		if ( prefs.createFileBounds != null )
			createDlg.setBounds ( onscreen ( prefs.createFileBounds ) );
		if ( prefs.folderBounds != null )
			folderDlg.setBounds ( onscreen ( prefs.folderBounds ) );
		if ( prefs.newAtomBounds != null )
			newAtomDlg.setBounds ( onscreen ( prefs.newAtomBounds ) );
		if ( prefs.newExitBounds != null )
			newExitDlg.setBounds ( onscreen ( prefs.newExitBounds ) );
		
		inspector.setVisible ( prefs.atomVisible );
		exitInspector.setVisible ( prefs.exitVisible );	
	}

	//----------------------------------------------------------------

	/**
	 *  Copy the current application state to the preferences
	 *  object for saving to a file.
	 */
	protected void flushPrefs ()
	{
		prefs.mainBounds = mainWindow.getBounds();
		prefs.atomBounds = inspector.getBounds();
		prefs.exitBounds = exitInspector.getBounds();
		prefs.openFileBounds = openDlg.getBounds();
		prefs.createFileBounds = createDlg.getBounds();
		prefs.folderBounds = folderDlg.getBounds();
		prefs.newAtomBounds = newAtomDlg.getBounds();
		prefs.newExitBounds = newExitDlg.getBounds();
		
		prefs.atomVisible = inspector.isVisible();
		prefs.exitVisible = exitInspector.isVisible();
	}
		
	//----------------------------------------------------------------

	/**
	 *  Utility to ensure that a window is not inadvertently
	 *  set to anoffscreen location.
	 */
	protected Rectangle onscreen ( Rectangle rect )
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle result = new Rectangle ( rect.x, rect.y, rect.width, rect.height );
		
		// the top of the window must be onscreen, because that
		// may be the only draggable part
		if ( result.y > screenSize.height - ONSCREEN_MARGIN )
			result.y = screenSize.height - rect.height;
		if ( result.y < 0 )
			result.y = 0;
		
		// at least ONSCREEN_MARGIN pixels of the window must be
		// visible horizontally, but we don't care which
		if ( result.x + rect.width < ONSCREEN_MARGIN )
			result.x = 0;
		else if ( result.x > screenSize.width - ONSCREEN_MARGIN )
			result.x = screenSize.width - rect.width;
		
		return result;
	}

	//----------------------------------------------------------------

	/**
	 *  Print a usage message for this application to
	 *  <tt>System.err</tt>.
	 */
	public void printUsage ()
	{
		System.err.println("AtomBuilder [" + VERSION + "] usage:");
		System.err.println("java com.ogalala.tools.AtomBuilder [options [args]]");
		System.err.println("  options:");
		System.err.println("  -d dataBase       get atom documentation from" );
		System.err.println("                    specified database file" );
		System.err.println("  -w workDB         copy atom database to new file");
		System.err.println("                    workDB before editing");
		System.err.println("  -o                overwrite workDB if it exists");
		System.err.println("  -s scriptPath     search for scripts on specified" );
		System.err.println("                    path if creating database" );
		System.err.println("  -c                create documentation database" );
		System.err.println("                    from scripts" );
		System.err.println("  -p prefsFile      name of a preferences file to use" );
		System.err.println("  -a attribFile     name of an attribute file defining" );
		System.err.println("                    special property attributes" );
		System.err.println("  -h | --help | -?  print this usage message");
	}

	//----------------------------------------------------------------
}