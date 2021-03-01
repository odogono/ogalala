// $Id: NounEditor.java,v 1.1 1999/03/10 16:46:21 matt Exp $
// A noun inspector window for atoms
// Matthew Caldwell, 10 March 1999
// Copyright (c) Ogalala Ltd <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.ogalala.util.*;
import com.ogalala.widgets.*;
import com.ogalala.widgets.text.*;
import com.ogalala.mua.*;

/**
 *  A modal dialog for editing the nouns attached to an atom.
 *  The static method <tt>edit()</tt> can be used to invoke
 *  an editor without the fuss of creating and disposing of it.
 */
public class NounEditor
	extends Dialog
	implements ItemListener, ActionListener, TextListener
{
	//----------------------------------------------------------------
	//  class variables
	//----------------------------------------------------------------

	private static final int DLG_WIDTH = 200;
	private static final int DLG_HEIGHT = 200;
	private static final String OK = "OK".intern();
	private static final String CANCEL = "Cancel".intern();
	private static final String ADD = "Add Noun".intern();
	private static final String REMOVE = "Remove Selected".intern();

	//----------------------------------------------------------------
	//  instance variables
	//----------------------------------------------------------------

	/** The atom being inspected. */
	protected Atom atom = null;
	
	/** A list component to list the nouns. */
	protected List list = new List ( 6, true );
	
	/** Textfield to specify a new noun. */
	protected TextField nounField = new TextField();
	
	/**
	 *  A button to remove the selected nouns. A reference
	 *  is kept to this so it can be disabled when nothing
	 *  is selected in the list.
	 */
	protected Button removeBtn = new Button ( REMOVE );
	
	/**
	 *  A button to add a new noun. A reference is kept to
	 *  this so it can be disabled when there is nothing
	 *  in the textfield.
	 */
	protected Button addBtn = new Button ( ADD );
	
	//----------------------------------------------------------------
	//  construction
	//----------------------------------------------------------------

	/** Constructor. */
	public NounEditor ( Frame parent,
						Atom atom )
	{
		super ( parent, true );
		this.atom = atom;
		
		// hide the window when the close box is clicked
		addWindowListener
		(
			new WindowAdapter ()
			{
				public void windowClosing ( WindowEvent e )
				{
					Window wind = e.getWindow();
					wind.setVisible( false );
				}
			}
		);
		
		// centre the dialog on the parent window
		Rectangle parentBounds = parent.getBounds();
		int hCentre = parentBounds.x + ( parentBounds.width / 2 );
		int vCentre = parentBounds.y + ( parentBounds.height / 2 );
		setBounds ( Math.max (0, hCentre - DLG_WIDTH / 2),
					Math.max (0, vCentre - DLG_HEIGHT / 2),
					DLG_WIDTH,
					DLG_HEIGHT );
		
		// controls
		Button okBtn = new Button ( OK );
		okBtn.setActionCommand ( OK );
		okBtn.addActionListener ( this );
		
		Button cancelBtn = new Button ( CANCEL );
		cancelBtn.setActionCommand ( CANCEL );
		cancelBtn.addActionListener ( this );
		
		addBtn.setActionCommand ( ADD );
		addBtn.addActionListener ( this );
		addBtn.setEnabled ( false );
		
		removeBtn.setActionCommand ( REMOVE );
		removeBtn.addActionListener ( this );
		removeBtn.setEnabled ( false );
		
		// enter in the noun field is equivalent to
		// pressing the add button
		nounField.addActionListener ( this );
		
		// we need to know when to enable and disable
		// the add button
		nounField.addTextListener ( this );
		
		// ditto the remove button
		list.addItemListener ( this );
		
		setResizable ( true );
		setTitle ( "Nouns: " + atom.getID() );
		
		// populate the list
		Vector nouns = atom.getWorld().getVocabulary().getAtomNouns( atom );
		if ( nouns != null )
		{
			Enumeration nounEnum = nouns.elements();
			while ( nounEnum.hasMoreElements() )
				list.add ( (String) nounEnum.nextElement() );
		}
		
		// lay out the container
		Panel btnPanel = new Panel ();
		btnPanel.setLayout ( new GridLayout ( 6, 1, 3, 3 ) );
		btnPanel.add ( nounField );
		btnPanel.add ( addBtn );
		btnPanel.add ( removeBtn );
		btnPanel.add ( new Label (" ") );
		btnPanel.add ( okBtn );
		btnPanel.add ( cancelBtn );
		
		setLayout ( new BorderLayout() );
		add ( "Center", list );
		add ( "East", btnPanel );
		
	}

	//----------------------------------------------------------------
	//  usage
	//----------------------------------------------------------------
	
	/** Static function to edit the nouns of an atom. */
	public static void edit ( Frame parent,
							  Atom atom )
	{
		NounEditor editor = new NounEditor ( parent, atom );
		editor.show();
		editor.dispose();
	}
	
	//----------------------------------------------------------------
	//  event handling
	//----------------------------------------------------------------

	/**
	 *  Respond to button presses and the enter key being
	 *  pressed in the new noun text field.
	 */
	public void actionPerformed ( ActionEvent e )
	{
		if ( e.getActionCommand().equals(CANCEL) )
		{
			setVisible(false);
		}
		else if ( e.getActionCommand().equals(REMOVE) )
		{
			// remove all the selected items from the list
			String[] items = list.getSelectedItems();
			
			if ( items != null && items.length > 0 )
			{
				for ( int i = 0; i < items.length; i++ )
					list.remove ( items[i] );
			}
			
			removeBtn.setEnabled ( false );
		}
		else if ( e.getActionCommand().equals(ADD) )
		{
			// validate the text field and add to the list if OK
			String newValue = nounField.getText().trim().toLowerCase();
			if (  (! newValue.equals(""))
				  && newValue.indexOf(".") == -1
				  && newValue.indexOf(",") == -1
				  && newValue.indexOf("\"") == -1
				  && newValue.indexOf("'") == -1
				  && newValue.indexOf(" ") == -1
				  && (! contains( newValue )) )
				list.add ( newValue );
			
			nounField.setText ( "" );
			addBtn.setEnabled ( false );
		}
		else if ( e.getActionCommand().equals(OK) )
		{
			// flush any changes to the list through
			// to the vocabulary for the current atom
			Vocabulary vocab = atom.getWorld().getVocabulary();
			
			// first check for nouns on the atom which
			// are not in the list, and remove them
			Vector nouns = vocab.getAtomNouns ( atom );
			Enumeration nounEnum = nouns.elements();
			while ( nounEnum.hasMoreElements() )
			{
				String oldNoun = (String) nounEnum.nextElement();
				if ( ! contains ( oldNoun ) )
					vocab.removeNoun ( null, oldNoun );
			}
			
			// then go through the nouns list and add any
			// which are not already defined (noting any failures
			// to report at the end)
			String[] items = list.getItems();
			if ( items != null || items.length > 0 )
			{
				String failNotice = "";
				for ( int i = 0; i < items.length; i++ )
				{
					Atom where = vocab.getNoun ( items[i] );
					if ( where == null )
						vocab.addNoun ( null, atom, items[i] );
					else if ( where != atom )
						failNotice += System.getProperty("line.separator")
									  + items[i]
									  + " ("
									  + where.getID()
									  + ")";
				}
				
				if ( ! failNotice.equals("") )
				{
					Alert.showAlert ( (Frame) getParent(),
									  "The following nouns could not be added because they are already defined:"
									  + failNotice );
				}
			}
			
			setVisible ( false );
		}
	}
	
	//----------------------------------------------------------------

	/**
	 *  Respond to item events in the nouns list to disable
	 *  and enable the remove button depending on whether any
	 *  nouns are selected.
	 */
	public void itemStateChanged ( ItemEvent e )
	{
		// enable or disable the remove button depending
		// on whether any items are selected in the list
		String[] selectedItems = list.getSelectedItems();
		if ( selectedItems == null
			 || selectedItems.length == 0 )
			removeBtn.setEnabled( false );
		else
			removeBtn.setEnabled( true );
	}

	//----------------------------------------------------------------

	/**
	 *  Respond to text events in the new noun field to enable
	 *  and disable to add button depending on whether there
	 *  is valid noun text in the box.
	 */
	public void textValueChanged ( TextEvent e )
	{
		String newValue = nounField.getText().trim().toLowerCase();
		
		// there must be some text
		if ( newValue.equals("") )
			addBtn.setEnabled( false );
		// it must not contain synonym delimiter characters
		else if ( newValue.indexOf(".") != -1
				 || newValue.indexOf(",") != -1
				 || newValue.indexOf("\"") != -1
				 || newValue.indexOf("'") != -1
				 || newValue.indexOf(" ") != -1 )
			addBtn.setEnabled( false );
		else
		{
			// it must not already be in the list
			addBtn.setEnabled ( ! contains( newValue ) );
		}
	}

	//----------------------------------------------------------------
	//  utilities
	//----------------------------------------------------------------
	
	/**
	 *  Check whether a given string is already in the list.
	 */
	protected boolean contains ( String name )
	{
		String[] items = list.getItems();
		if ( items == null || items.length == 0 )
			return false;
		
		for ( int i = 0; i < items.length; i++ )
			if ( items[i].equalsIgnoreCase( name ) )
				return true;
		
		return false;
	}

	//----------------------------------------------------------------
}