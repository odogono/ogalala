// $Id: NewExitDialog.java,v 1.3 1999/03/16 16:12:40 matt Exp $
// A dialog for creating a new exit.
// Matthew Caldwell, 23 February 1999
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.ogalala.widgets.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;

/**
 *  A modal dialog for creating a new exit, as used
 *  in the AtomBuilder.
 */
public class NewExitDialog
	extends Dialog
	implements ActionListener
{
	//---------------------------------------------------------------
	//  class variables
	//---------------------------------------------------------------

	private static final int DLG_WIDTH = 450;
	private static final int DLG_HEIGHT = 350;
	private static final int DIRECTION_ROWS = 6;
	private static final String CANCEL = "Cancel".intern();
	private static final String OK = "OK".intern();
	
	/** A constant indicating the "From" exit. */
	public static final int SOURCE = 0;
	
	/** A constant indication the "To" exit. */
	public static final int DESTINATION = 1;
	
	//---------------------------------------------------------------
	//  instance variables
	//---------------------------------------------------------------
	
	/** List of containers for the source room. */
	public Choice sourceRoom = new Choice ();
	
	/** List of containers for the destination room. */
	public Choice destRoom = new Choice ();
	
	/** List of exit classes for the source exit. */
	public Choice sourceClass = new Choice ();
	
	/** List of exit classes for the destination exit. */
	public Choice destClass = new Choice ();
	
	/** List of directions for the source exit. */
	public List sourceDirections = new List ( DIRECTION_ROWS, true );
	
	/** List of parents the atom may inherit from. */
	public List destDirections = new List ( DIRECTION_ROWS, true );
	
	/** TextArea for entering comments on this atom. */
	public TextArea comments
		= new TextArea( "", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY );
		
	/** List of ODL files the exits may belong to. */
	public Choice odl = new Choice ();
	
	/**
	 *  Whether the dialog was closed with the Cancel button
	 *  (or, equivalently, the close box).
	 */
	public boolean cancelled = true;
	
	//---------------------------------------------------------------
	//  construction
	//---------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public NewExitDialog ( Frame parent )
	{
		super( parent, true );
				
		addWindowListener
		(
			// anonymous inner class to handle closing the window
			// -- the setVisible() should unblock show()
			new WindowAdapter()
			{
				public void windowClosing ( WindowEvent e )
				{
					Window wind = e.getWindow();
					wind.setVisible( false );
					
					// close box is equivalent to cancel
					cancelled = true;
				}
			}
		);
		
		// buttons
		Button ok = new Button ( OK );
		ok.addActionListener ( this );
		ok.setActionCommand ( OK );
		
		Button cancel = new Button ( CANCEL );
		cancel.addActionListener ( this );
		cancel.setActionCommand ( CANCEL );

		setBounds ( 0, 0, DLG_WIDTH, DLG_HEIGHT );
		
		setResizable ( true );
		setTitle ( "Create New Exit" );
		
		populateDirectionList ( sourceDirections );
		populateDirectionList ( destDirections );
		destDirections.add ( "none" );
		
		// lay out the dialog
		Panel btnPanel = new Panel ();
		btnPanel.setLayout ( new FlowLayout ( FlowLayout.CENTER,
											  15,
											  10 ) );
		btnPanel.add ( ok );
		btnPanel.add ( cancel );
		
		Panel srcChoices = new Panel ();
		srcChoices.setLayout ( new GridLayout ( 5, 1 ) );
		srcChoices.add ( new Label ( "From" ) );
		srcChoices.add ( sourceRoom );
		srcChoices.add ( new Label ( "Exit type" ) );
		srcChoices.add ( sourceClass );
		srcChoices.add ( new Label ( "Direction(s)" ) );
		
		Panel srcPanel = new Panel ();
		srcPanel.setLayout ( new BorderLayout() );
		srcPanel.add ( "North", srcChoices );
		srcPanel.add ( "Center", sourceDirections );
		
		Panel dstChoices = new Panel ();
		dstChoices.setLayout ( new GridLayout ( 5, 1 ) );
		dstChoices.add ( new Label ( "To" ) );
		dstChoices.add ( destRoom );
		dstChoices.add ( new Label ( "Exit type" ) );
		dstChoices.add ( destClass );
		dstChoices.add ( new Label ( "Direction(s)" ) );
		
		Panel dstPanel = new Panel ();
		dstPanel.setLayout ( new BorderLayout() );
		dstPanel.add ( "North", dstChoices );
		dstPanel.add ( "Center", destDirections );
		
		Panel miscPanel = new Panel ();
		miscPanel.setLayout ( new GridLayout( 3, 1, 3, 3 ) );
		miscPanel.add ( new Label ( "ODL File:" ) );
		miscPanel.add ( odl );
		miscPanel.add ( new Label ( "Comments:" ) );
		
		Panel commentPanel = new Panel ();
		commentPanel.setLayout ( new BorderLayout() );
		commentPanel.add ( "North", miscPanel );
		commentPanel.add ( "Center", comments );
		
		Panel mainPanel = new Panel ();
		mainPanel.setLayout ( new GridLayout ( 1, 3, 15, 10 ) );
		mainPanel.add ( srcPanel );
		mainPanel.add ( dstPanel );
		mainPanel.add ( commentPanel );
		
		setLayout ( new BorderLayout () );
		add ( "Center", mainPanel );
		add ( "South", btnPanel );
	}

	//---------------------------------------------------------------
	//  usage
	//---------------------------------------------------------------

	/** Set the list of atoms that can be exits. */
	public void setExitList ( StringVector exitList )
	{
		// rebuild the exits lists
		sourceClass.removeAll();
		destClass.removeAll();
		
		Enumeration enum = exitList.elements();
		while ( enum.hasMoreElements() )
		{
			String exitClass = (String) enum.nextElement();
			sourceClass.add ( exitClass );
			destClass.add ( exitClass );
		}
		
		repaint();
	}

	//---------------------------------------------------------------

	/** Set the list of containers that can have exits. */
	public void setContainerList ( StringVector roomList )
	{
		// rebuild the room lists
		sourceRoom.removeAll();
		destRoom.removeAll();
		
		Enumeration enum = roomList.elements();
		while ( enum.hasMoreElements() )
		{
			String room = (String) enum.nextElement();
			sourceRoom.add ( room );
			destRoom.add ( room );
		}
		
		repaint();
	}

	//---------------------------------------------------------------
	
	/** Set the list of ODL files that can be used. */
	public void setODLList ( StringVector odlList )
	{
		// rebuild odl list
		odl.removeAll();
		
		Enumeration enum = odlList.elements();
		while ( enum.hasMoreElements() )
		{
			odl.add ( (String) enum.nextElement() );
		}
		
		repaint();
	}
	
	//---------------------------------------------------------------	

	/** Clear fields for reuse. */
	public void reset ()
	{
		for ( int i = 0; i < sourceDirections.getItemCount(); i++ )
			sourceDirections.deselect ( i );
 		for ( int i = 0; i < destDirections.getItemCount(); i++ )
			destDirections.deselect ( i );

		comments.setText ( "" );
		cancelled = true;
	}

	//---------------------------------------------------------------	

	/** Utility to fill a list with all available directions. */
	public void populateDirectionList ( List list )
	{
		list.add ( "north" );
		list.add ( "south" );
		list.add ( "east" );
		list.add ( "west" );
		list.add ( "northeast" );
		list.add ( "southeast" );
		list.add ( "northwest" );
		list.add ( "southwest" );
		list.add ( "up" );
		list.add ( "down" );
		list.add ( "in" );
		list.add ( "out" );
	}

	//---------------------------------------------------------------
	
	/**
	 *  A utility function to map the directions selected in
	 *  the direction list to the direction numbers used by
	 *  the ExitTable class. If "none" is among the selected
	 *  directions, or no direction is selected, 0 is returned.
	 
	 @param whichSide  Which end of the currently defined
	                   conduit the directions should be obtained
	                   for. Should be either SOURCE or DESTINATION.
	 
	 */
	public int getDirections ( int whichSide )
	{
		String[] dirs;
		
		if ( whichSide == SOURCE )
			dirs = sourceDirections.getSelectedItems();
		else if ( whichSide == DESTINATION )
			dirs = destDirections.getSelectedItems();
		else
			return 0;
		
		if ( dirs.length == 0 )
			return 0;
		
		int result = 0;
		
		for ( int i = 0; i < dirs.length; i++ )
		{
			if ( dirs[i].equals("none") )
				return 0;
			
			result |= ExitTable.toDirection ( dirs[i] );
		}
		
		return result;
	}

	//---------------------------------------------------------------
	//  event handling
	//---------------------------------------------------------------
	
	/**
	 *  Handle button presses.
	 */
	public void actionPerformed ( ActionEvent e )
	{
		if ( e.getActionCommand().equals( OK ) )
		{
			// source direction is mandatory
			if ( getDirections ( SOURCE ) == 0 )
			{
				Alert.showAlert ( (Frame) getParent(),
								  "You must specify a direction for the \"From\" side." );
				return;
			}
			
			cancelled = false;
		}
		else
			cancelled = true;
			
		setVisible ( false );
	}

	//---------------------------------------------------------------
}