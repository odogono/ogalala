// $Id: PropertyDialog.java,v 1.1 1999/02/23 18:10:33 matt Exp $
// A dialog for creating a new atom property.
// Matthew Caldwell, 22 February 1999
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.ogalala.widgets.*;
import com.ogalala.mua.*;

/**
 *  A modal dialog for creating a new atom property, as used
 *  in the AtomBuilder.
 */
public class PropertyDialog
	extends Dialog
	implements ActionListener
{
	//---------------------------------------------------------------
	//  class variables
	//---------------------------------------------------------------

	private static final int DLG_WIDTH = 350;
	private static final int DLG_HEIGHT = 250;
	private static final String CANCEL = "Cancel".intern();
	private static final String OK = "OK".intern();
	
	//---------------------------------------------------------------
	//  instance variables
	//---------------------------------------------------------------
	
	/** Field for entering the property name. */
	public TextField name = new TextField();
	
	/** Field for entering the property value. */
	public TextField value = new TextField();
	
	/** TextArea for entering comments on this property. */
	public TextArea comments
		= new TextArea( "", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY );
	
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
	public PropertyDialog ( Frame parent )
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
		setTitle ( "Add New Property" );
		
		setLayout ( new BindLayout() );

		add ( ok,
			  new BindConstraints ( 90, 25,
			  						15,
			  						BindConstraints.NONE,
			  						( DLG_WIDTH / 2 ) + 8,
			  						20 ) );
		add ( cancel,
			  new BindConstraints ( 90, 25,
			  						( DLG_WIDTH / 2 ) + 8,
			  						BindConstraints.NONE,
			  						15,
			  						20 ) );
		add ( comments,
			  new BindConstraints ( 20, 20,
			  						15,
			  						100,
			  						15,
			  						60 ) );
			  						
		add ( new Label ( "Documentation comments:" ),
			  new BindConstraints ( 20, 20,
			  						15,
			  						80,
			  						15,
			  						BindConstraints.NONE ) );
		
		add ( name,
			  new BindConstraints ( 90, 25,
			  						15,
			  						50,
			  						( DLG_WIDTH / 2 ) + 8,
			  						BindConstraints.NONE ) );
		
		add ( value,
			  new BindConstraints ( 90, 25,
			  						( DLG_WIDTH / 2 ) + 8,
			  						50,
			  						15,
			  						BindConstraints.NONE ) );
			  						
		add ( new Label ( "Name:" ),
			  new BindConstraints ( 50, 20,
			  						15,
			  						30,
			  						( DLG_WIDTH / 2 ) + 8,
			  						BindConstraints.NONE ) );
			  						
		add ( new Label ( "Value:" ),
			  new BindConstraints ( 50, 20,
			  						( DLG_WIDTH / 2 ) + 8,
			  						30,
			  						15,
			  						BindConstraints.NONE ) );
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
			// name and comments are mandatory
			// value may be left blank (ie, "")
			// name must be a valid ID
			String nameTxt = name.getText();
			
			if ( ! Atom.isValidID ( nameTxt ) )
			{
				Alert.showAlert ( (Frame) getParent(),
								  "Invalid name. Names must be a single word containing only letters, numbers and underscores." );
				name.requestFocus();
				name.selectAll();
				return;
			}
			
			String commentTxt = comments.getText();
			
			if ( commentTxt.equals( "" ) )
			{
				Alert.showAlert ( (Frame) getParent(),
								  "Please enter some explanatory comments for this property!" );
				comments.requestFocus();
				comments.selectAll();
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