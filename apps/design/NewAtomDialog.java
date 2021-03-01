// $Id: NewAtomDialog.java,v 1.2 1999/02/24 16:56:00 matt Exp $
// A dialog for creating a new atom.
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
 *  A modal dialog for creating a new atom, as used
 *  in the AtomBuilder.
 */
public class NewAtomDialog
	extends Dialog
	implements ActionListener
{
	//---------------------------------------------------------------
	//  class variables
	//---------------------------------------------------------------

	private static final int DLG_WIDTH = 350;
	private static final int DLG_HEIGHT = 200;
	private static final String CANCEL = "Cancel".intern();
	private static final String OK = "OK".intern();
	
	//---------------------------------------------------------------
	//  instance variables
	//---------------------------------------------------------------
	
	/** Field for entering the property name. */
	public TextField id = new TextField();
	
	/** List of parents the atom may inherit from. */
	public List parents = new List ();
	
	/** Checkbox specifying whether the atom is concrete. */
	public Checkbox isThing = new Checkbox ( "Make it a Thing" );
	
	/** TextArea for entering comments on this atom. */
	public TextArea comments
		= new TextArea( "", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY );
		
	/** List of ODL files the atom may belong to. */
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
	public NewAtomDialog ( Frame parent )
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
		
		// set up the parent list
		parents.setMultipleMode ( true );
					
		setResizable ( true );
		setTitle ( "Create New Atom" );
		
		// lay out the dialog
		Panel btnPanel = new Panel ();
		btnPanel.setLayout ( new FlowLayout ( FlowLayout.CENTER,
											  15,
											  10 ) );
		btnPanel.add ( ok );
		btnPanel.add ( cancel );
		
		Panel miscPanel = new Panel ();
		miscPanel.setLayout ( new GridLayout ( 4, 1 ) );
		miscPanel.add ( new Label ("Atom ID") );
		miscPanel.add ( id );
		miscPanel.add ( isThing );
		miscPanel.add ( odl );
		
		Panel parentPanel = new Panel ();
		parentPanel.setLayout ( new BorderLayout() );
		parentPanel.add ( "North", new Label ("Parents") );
		parentPanel.add ( "Center", parents );
		
		Panel commentPanel = new Panel ();
		commentPanel.setLayout ( new BorderLayout() );
		commentPanel.add ( "North", new Label ("Comments") );
		commentPanel.add ( "Center", comments );
		
		Panel mainPanel = new Panel ();
		mainPanel.setLayout ( new GridLayout ( 1, 3, 15, 10 ) );
		mainPanel.add ( miscPanel );
		mainPanel.add ( parentPanel );
		mainPanel.add ( commentPanel );
		
		setLayout ( new BorderLayout () );
		add ( "Center", mainPanel );
		add ( "South", btnPanel );
	}

	//---------------------------------------------------------------

	/** Constructor specifying parents. */
	public NewAtomDialog ( Frame parent,
						   StringVector atomList )
	{
		this ( parent );
		setAtomList ( atomList );
	}	


	//---------------------------------------------------------------
	//  usage
	//---------------------------------------------------------------

	/** Set the list of atoms that can be inherited from. */
	public void setAtomList ( StringVector atomList )
	{
		// rebuild the parents list
		parents.removeAll();
		
		Enumeration enum = atomList.elements();
		while ( enum.hasMoreElements() )
		{
			parents.add ( (String) enum.nextElement() );
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
		for ( int i = 0; i < parents.getItemCount(); i++ )
			parents.deselect ( i );
		id.setText ( "" );
		comments.setText ( "" );
		isThing.setState ( false );	
		cancelled = true;
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
			// validate contents
			// id is mandatory
			String idTxt = id.getText();
			
			if ( ! Atom.isValidID ( idTxt ) )
			{
				Alert.showAlert ( (Frame) getParent(),
								  "Invalid name. Names must be a single word containing only letters, numbers and underscores." );
				id.requestFocus();
				id.selectAll();
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