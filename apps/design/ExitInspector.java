// $Id: ExitInspector.java,v 1.2 1999/03/04 17:42:30 matt Exp $
// A dialog for listing the exits of a container.
// Matthew Caldwell, 1 March 1999
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.ogalala.widgets.*;
import com.ogalala.mua.*;

/**
 *  A simple inspector dialog that displays the exits
 *  leading from a given container. This dialog
 *  is totally passive -- no editing facilities are
 *  provided, and the only event it responds to is
 *  a click in the close box.
 */
public class ExitInspector
	extends Dialog
{
	//---------------------------------------------------------------
	//  class variables
	//---------------------------------------------------------------

	private static final int DLG_WIDTH = 250;
	private static final int DLG_HEIGHT = 300;
	
	//---------------------------------------------------------------
	//  instance variables
	//---------------------------------------------------------------
	
	/** The list of exits. */
	protected InspectorPanel inspector = new InspectorPanel ();
	
	/** A scrolling panel for displaying the inspector. */
	protected ScrollPane scroller = new ScrollPane ();
	
	/** The atom currently being displayed. */
	protected Atom current = null;
	
	//---------------------------------------------------------------
	//  construction
	//---------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public ExitInspector ( Frame parent )
	{
		super( parent, false );
		
		// hide the window when the close box is clicked
		addWindowListener
		(
			new WindowAdapter()
			{
				public void windowClosing ( WindowEvent e )
				{
					Window wind = e.getWindow();
					wind.setVisible( false );
				}
			}
		);
		
		setBounds ( 0, 0, DLG_WIDTH, DLG_HEIGHT );
		setResizable ( true );
		setTitle ( "Exits" );
		setLayout ( new BorderLayout() );
		add ( "Center", scroller );
		scroller.add ( inspector );
	}

	//---------------------------------------------------------------

	/**
	 *  Specify the atom being inspected. Atoms that
	 *  aren't Containers can't have an exit table, so
	 *  they always clear the inspector. For Containers,
	 *  the exit table is enumerated, and the exits added
	 *  to the inspector with their destinations.
	 */
	public void setAtom ( Atom atom )
	{
		inspector.removeAllProperties();
		current = atom;
		
		if ( atom == null
			|| ! (atom instanceof com.ogalala.mua.Container) )
		{
			setTitle ( "Exits" );
			scroller.add ( inspector );
			repaint();
			return;
		}
		
		com.ogalala.mua.Container room = (com.ogalala.mua.Container) atom;
		Hashtable exits = new Hashtable ();
		
		// enumerate the exits -- this has to be done
		// manually (grrr!)
		insertExit ( room, exits, ExitTable.DIR_NORTH );
		insertExit ( room, exits, ExitTable.DIR_SOUTH );
		insertExit ( room, exits, ExitTable.DIR_WEST );
		insertExit ( room, exits, ExitTable.DIR_EAST );
		insertExit ( room, exits, ExitTable.DIR_NORTHWEST );
		insertExit ( room, exits, ExitTable.DIR_SOUTHEAST );
		insertExit ( room, exits, ExitTable.DIR_NORTHEAST );
		insertExit ( room, exits, ExitTable.DIR_SOUTHWEST );
		insertExit ( room, exits, ExitTable.DIR_UP );
		insertExit ( room, exits, ExitTable.DIR_DOWN );
		insertExit ( room, exits, ExitTable.DIR_IN );
		insertExit ( room, exits, ExitTable.DIR_OUT );
		
		// go through the exits list and add the relevant
		// properties to the inspector
		Enumeration enum = exits.keys();
		while ( enum.hasMoreElements() )
		{
			Atom theExit = (Atom) enum.nextElement();
			String label = (String) exits.get ( theExit );
			Object destination = theExit.getField ( "destination" );
			String destName;
			if ( destination instanceof String )
				destName = (String) destination;
			else if ( destination instanceof Atom )
				destName = ((Atom) destination).getID();
			else if ( destination == null )
				destName = "<none>";
			else
				destName = destination.toString();
				
			inspector.addProperty ( label, destName, false );
		}
		
		setTitle ( "Exits: " + atom.getID() );
		scroller.add ( inspector );
		repaint();
	}

	//---------------------------------------------------------------
	
	/**
	 *  Utility function to insert a mapping of a particular
	 *  direction to an exit in a hashtable. If the exit
	 *  is already mapped to a direction, the new direction
	 *  is added to those in the table rather than replacing
	 *  them.
	 */
	protected void insertExit ( com.ogalala.mua.Container room,
								Hashtable table,
								int direction )
	{
		Atom exit = (Atom) room.getExit ( direction );
		if ( exit == null )
			return;
		
		String existing = (String) table.get ( exit );
		if ( existing == null )
			table.put ( exit, ExitTable.toString ( direction ) );
		else
			table.put ( exit, existing + ", " + ExitTable.toString ( direction ) );
	}
	
	//---------------------------------------------------------------
	
	/**
	 *  Accessor for finding the atom whose exits are
	 *  currently displayed by this inspector.
	 */
	public Atom getAtom () { return current; }

	//---------------------------------------------------------------
	
	/**
	 *  Get the direction(s) of the currently selected
	 *  exit (if any).
	 */
	public String getSelectedItem ()
	{
		return inspector.getSelectedItem();
	}

	//---------------------------------------------------------------
}