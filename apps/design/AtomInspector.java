// $Id: AtomInspector.java,v 1.14 1999/03/11 10:42:42 alex Exp $
// A property inspector window for atoms
// Matthew Caldwell, 25 January 1999
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
 *  An inspector window which displays the properties of
 *  an atom and can be used to edit them.
 */
public class AtomInspector
	extends Dialog
	implements ItemListener, ActionListener
{
	//----------------------------------------------------------------
	//  instance variables
	//----------------------------------------------------------------

	/** The atom being inspected. */
	protected Atom atom = null;

	/** Whether the current atom is mutable. */
	protected boolean mutable = true;

	//----------------------------------------------------------------
	//  UI elements
	//----------------------------------------------------------------

	/** The inspector component. */
	protected InspectorPanel inspector = new InspectorPanel();

	/** A pane to allow the inspector panel to be scrolled. */
	protected ScrollPane scroller
		= new ScrollPane ( ScrollPane.SCROLLBARS_ALWAYS );

	/** Button to add a new property. */
	protected Button addPropBtn = new Button ( "Add Property" );

	/** Button to clear a property. */
	protected Button clearPropBtn = new Button ( "Clear Property" );

	/** Button to edit nouns. */
	protected Button nounsBtn = new Button ( "Edit Nouns" );

	/** Button to invoke multi-line editor for property. */
	protected Button editMultiBtn = new Button ( "Edit Multiline" );

	/**
	 *  Button to invoke styled-text editor for property.
	 *  (NOT YET IMPLEMENTED)
	 */
	protected Button editStyledBtn = new Button ( "Edit Styled" );

	/** Button to inherit a new parent. */
	protected Button inheritBtn = new Button ( "Inherit" );

	/** Button to uninherit an existing parent. */
	protected Button uninheritBtn = new Button ( "Uninherit" );

	/** List of parents. */
	protected List parentList = new List ();

	/** List of atoms that could be parents. */
	protected List atomList = new List ();

	/** Choice box to select from available ODL files. */
	protected Choice odlChoice = new Choice ();

	/** Choice box to select from available containers. */
	protected Choice whereChoice = new Choice ();

	/** Styled text area to display property comments. */
	protected ScrollingStyledTextPanel docs
		= new ScrollingStyledTextPanel ();

	/** Dialog for creating a new property. */
	protected PropertyDialog propDlg = null;

	//----------------------------------------------------------------
	//  class variables
	//----------------------------------------------------------------

	/** Default window size. */
	public static final Dimension DEFAULT_SIZE = new Dimension ( 400, 400 );

	/** Display traits for local properties. */
	public static final InspectorPanel.PropertyTraits LOCAL
		= 	new InspectorPanel.PropertyTraits ( new Font ( "Dialog",
															Font.BOLD,
															12 ) );

	/** Display traits for inherited properties. */
	public static final InspectorPanel.PropertyTraits INHERITED
		= 	new InspectorPanel.PropertyTraits ( new Font ( "Dialog",
															Font.ITALIC,
															12 ) );

	/** Priorities for standard property names. */
	protected static Hashtable priorities = new Hashtable ();

	/** Properties that are immutable even in mutable atoms. */
	protected static Hashtable immutables = new Hashtable ();

	//----------------------------------------------------------------
	//  construction
	//----------------------------------------------------------------

	/** Constructor. */
	public AtomInspector ( Frame parent )
	{
		// create window
		super ( parent, "Atom Inspector", false );
		setSize ( DEFAULT_SIZE );

		// create UI
		// -- set up the various pieces
		scroller.add ( inspector );

		// property buttons panel
		Panel btnPanel = new Panel ();
		btnPanel.setLayout ( new GridLayout ( 5, 1, 4, 4 ) );
		btnPanel.add ( addPropBtn );
		btnPanel.add ( clearPropBtn );
		btnPanel.add ( nounsBtn );
		btnPanel.add ( editMultiBtn );
		btnPanel.add ( editStyledBtn );

		// properties panel
		Panel propsPanel = new Panel ();
		propsPanel.setLayout ( new BorderLayout() );
		propsPanel.add ( "Center", docs );
		propsPanel.add ( "South", btnPanel );

		// parents panel
		Panel parentPanel = new Panel ();
		parentPanel.setLayout ( new BorderLayout() );
		parentPanel.add ( "Center", parentList );
		parentPanel.add ( "North", new Label ( "Parents" ) );
		parentPanel.add ( "South", uninheritBtn );

		// potential parents panel
		Panel atomsPanel = new Panel ();
		atomsPanel.setLayout ( new BorderLayout() );
		atomsPanel.add ( "Center", atomList );
		atomsPanel.add ( "North", new Label ( "Atoms" ) );
		atomsPanel.add ( "South", inheritBtn );

		// miscellaneous items panel
		Panel miscPanel = new Panel ();
		miscPanel.setLayout ( new FlowLayout() );
		miscPanel.add ( new Label ( "ODL file" ) );
		miscPanel.add ( odlChoice );
		miscPanel.add ( new Label ( "Location" ) );
		miscPanel.add ( whereChoice );

		// lower portion
		Panel lowerPanel = new Panel ();
		lowerPanel.setLayout ( new GridLayout ( 1, 3, 4, 4 ) );
		lowerPanel.add ( parentPanel );
		lowerPanel.add ( atomsPanel );
		lowerPanel.add ( miscPanel );

		// put it all together in the whole window
		setLayout ( new BindLayout() );

		// we really need the insets here, but that requires
		// the peer to have been created and blah-de-blah-de-blah
		// so just use constants instead

		add ( scroller,
			  new BindConstraints ( 100, 100,
			  						5,
			  						25,
			  						150,
			  						150 ) );
		add ( lowerPanel,
			  new BindConstraints ( 100, 140,
			  						5,
			  						BindConstraints.NONE,
			  						5,
			  						5 ) );

		add ( propsPanel,
			  new BindConstraints ( 140, 100,
			  						BindConstraints.NONE,
			  						25,
			  						5,
			  						150 ) );

		// set up action commands etc
		addPropBtn.setActionCommand ( "AddProperty" );
		clearPropBtn.setActionCommand ( "ClearProperty" );
		nounsBtn.setActionCommand ( "EditNouns" );
		editMultiBtn.setActionCommand ( "EditMulti" );
		editStyledBtn.setActionCommand ( "EditStyled" );
		editStyledBtn.setEnabled ( false );
		inheritBtn.setActionCommand ( "Inherit" );
		uninheritBtn.setActionCommand ( "Uninherit" );

		// listen for events in the controls
		inspector.addItemListener ( this );
		addPropBtn.addActionListener ( this );
		clearPropBtn.addActionListener ( this );
		nounsBtn.addActionListener ( this );
		editMultiBtn.addActionListener ( this );
		editStyledBtn.addActionListener ( this );
		inheritBtn.addActionListener ( this );
		uninheritBtn.addActionListener ( this );
		odlChoice.addItemListener ( this );
		whereChoice.addItemListener ( this );

		// create new property dialog
		propDlg = new PropertyDialog ( parent );
	}

	//----------------------------------------------------------------

	/** Constructor specifying an atom to edit. */
	public AtomInspector ( Frame parent,
						   Atom subject )
	{
		this ( parent );
		setAtom ( subject );
	}

	//----------------------------------------------------------------
	//  usage
	//----------------------------------------------------------------

	/**
	 *  Specify the atom this dialog should be editing.
	 */
	public void setAtom ( Atom subject )
	{
		inspector.removeAllProperties();
		atom = subject;


		// iterate through the properties of the atom,
		// adding them to the inspector -- after some
		// discussion with Alex I've concluded that all
		// fields need to be plain edit-boxes, since the
		// type may need to change dynamically
		Hashtable properties = new Hashtable ();
		atom.getProperties( properties );

		AtomDatabase db = atom.getWorld().getAtomDatabase();

		// if the atom is a core atom or marked as final
		// then the properties can't be modified
		if ( atom.getField("__") == null
			|| atom.getField("__final__") != null )
			mutable = false;
		else
			mutable = true;

		String title = AtomDocumenter.getAtomName(atom);
		if ( ! title.equals ( atom.getID() ) )
			title += " (" + atom.getID() + ")";
		if ( ! mutable )
			title += " [read only]";
		setTitle ( title );

		Enumeration propNames = properties.keys();
		while ( propNames.hasMoreElements() )
		{
			String name = (String) propNames.nextElement();

			// skip internal/doc properties
			if ( name.startsWith("_") )
				continue;

			Object value = properties.get(name);
			String valueStr;

			if ( value == null || AtomData.isNullProperty( value ) )
				valueStr = "null";
			else
				valueStr = AtomData.toString(value);

			// set the display traits according to whether the
			// property is local or inherited
			InspectorPanel.PropertyTraits trait = null;
			if ( atom.getField ( name ) == null )
				trait = INHERITED;
			else
				trait = LOCAL;

			// set the priority if defined
			if ( priorities.get ( name ) != null )
			{
				trait = new InspectorPanel.PropertyTraits ( trait.font, trait.colour );
				trait.priority = ((Character) priorities.get ( name )).charValue();
			}

			// add to the inspector with the appropriate mutability
			inspector.addProperty ( name,
									valueStr,
									mutable && ( immutables.get(name) == null ),
									trait );
		}

		inspector.arrange();
		scroller.add(inspector);

		// fill out the parents list
		parentList.removeAll();
		Enumeration parents = atom.getParents();
		while ( parents.hasMoreElements() )
		{
			Atom parent = (Atom) parents.nextElement();
			parentList.add ( parent.getID() );
		}

		// enable buttons appropriately
		inheritBtn.setEnabled ( mutable );
		uninheritBtn.setEnabled ( mutable );
		addPropBtn.setEnabled ( mutable );
		clearPropBtn.setEnabled ( mutable );
		nounsBtn.setEnabled ( mutable );
		editMultiBtn.setEnabled ( mutable );
		odlChoice.setEnabled ( mutable );
		whereChoice.setEnabled ( mutable && ( atom instanceof Thing ) );

		// select the correct ODL file in the choice box
		odlChoice.select ( "<None>" );
		Object odlSource = atom.getField ( "__odl__" );
		if ( odlSource != null && odlSource instanceof String )
			odlChoice.select ( (String) odlSource );

		// select the correct location in the choice box
		whereChoice.select ( "<N/A>" );
		if ( atom instanceof Thing )
		{
			Atom where = atom.getContainer();
			if ( where != null )
				whereChoice.select ( where.getID() );
		}

		invalidate();
		validate();
		repaint();
	}

	//----------------------------------------------------------------

	/**
	 *  Set the value of a property in the atom
	 *  accessed by this inspector. If the property
	 *  does not already exist in the atom, it is
	 *  created.
	 */
	public void setProperty ( String property,
							  String value )
		throws AtomException
	{
		if ( atom == null || mutable == false )
			return;

		value = conform ( value );
		inspector.addProperty( property, value );
		atom.setField( property, value );
	}

	//----------------------------------------------------------------

	/**
	 *  Set the list of atoms that may be inherited
	 *  from. This is managed by the owning tools to
	 *  avoid recalculating it unnecessarily all the
	 *  time. It should contain only static Atoms, and
	 *  should be updated when atoms are added or
	 *  removed.
	 */
	public void setAtomList ( StringVector atomNames )
	{
		atomList.removeAll();
		if ( atomNames.size() == 0 )
			return;

		for ( int i = 0; i < atomNames.size(); i++ )
			atomList.add ( atomNames.get(i) );

		repaint();
	}

	//----------------------------------------------------------------

	/**
	 *  Set the list of ODL files that an atom may
	 *  belong to. This is maintained by the owning
	 *  tools, since this class doesn't really know
	 *  anything about it. It should be updated when
	 *  ODL files are created or opened.
	 */
	public void setODLList ( StringVector odlFiles )
	{
		// I sort of want to have a "<NEW>" item
		// in this menu, but that poses a problem
		// since odlFiles are the business of the
		// owning app, not this inspector
		// -- something to think about later
		odlChoice.removeAll();
		if ( odlFiles.size() == 0 )
			return;

		for ( int i = 0; i < odlFiles.size(); i++ )
			odlChoice.add ( odlFiles.get(i) );

		// for atoms that don't have an ODL source
		odlChoice.add ( "<None>" );

		// set the appropriate value for the the current atom (if any)
		if ( atom != null )
		{
			odlChoice.select ( "<None>" );
			String odlSource = atom.getString ( "__odl__" );
			if ( odlSource != null )
			{
				odlChoice.select ( odlSource );
			}
		}

		repaint();
	}

	//----------------------------------------------------------------

	/**
	 *  Set the list of the containers that things
	 *  may be placed into.
	 */
	public void setContainerList ( StringVector containers )
	{
		whereChoice.removeAll();
		if ( containers.size() == 0 )
			return;

		for ( int i = 0; i < containers.size(); i++ )
			whereChoice.add ( containers.get(i) );

		// non-things cannot be in a container
		whereChoice.add ( "<N/A>" );

		// set the appropriate value for the the current atom (if any)
		if ( atom != null
			 && (atom instanceof Thing) )
		{
			// select the correct location in the choice box
			whereChoice.select ( "<N/A>" );
			if ( atom instanceof Thing )
			{
				Atom where = atom.getContainer();
				if ( where != null )
					whereChoice.select ( where.getID() );
			}
		}

		repaint();
	}

	//----------------------------------------------------------------

	/**
	 *  Set the priority associated with a particular property.
	 *  Priority controls the order in which the properties are
	 *  displayed in the inspector. Lower values appear higher
	 *  in the list. By default, all properties have a priority
	 *  of '~', which is the largest printable 7-bit ASCII
	 *  character.
	 */
	public static void setPriority ( String propertyName,
									 char priority )
	{
		propertyName = propertyName.trim().toLowerCase().intern();
		if ( priority == InspectorPanel.PropertyTraits.DEFAULT_PRIORITY )
			priorities.remove ( propertyName );
		else
			priorities.put ( propertyName, new Character ( priority ) );
	}

	//----------------------------------------------------------------

	/**
	 *  Determine the priority assigned to a particular
	 *  property.
	 */
	public static char getPriority ( String propertyName )
	{
		propertyName = propertyName.trim().toLowerCase().intern();
		Character ch = (Character) priorities.get ( propertyName );
		if ( ch == null )
			return InspectorPanel.PropertyTraits.DEFAULT_PRIORITY;
		else
			return ch.charValue();
	}

	//----------------------------------------------------------------

	/**
	 *  Set a particular property name to be immutable even
	 *  when the atom it is contained within is editable.
	 *  Some properties are managed by the system and should
	 *  not be edited by hand in case inconsistencies result.
	 *  These properties can be set as immutable, in which
	 *  case the inspector will not allow them to be changed.
	 */
	public static void setImmutable ( String propertyName,
									  boolean isConstant )
	{
		propertyName = propertyName.trim().toLowerCase().intern();
		if ( isConstant )
			immutables.put ( propertyName, Boolean.TRUE );
		else
			immutables.remove ( propertyName );
	}

	//----------------------------------------------------------------

	/**
	 *  Determine whether a given property can be edited
	 *  manually.
	 */
	public static boolean isImmutable ( String propertyName )
	{
		propertyName = propertyName.trim().toLowerCase().intern();
		return ( immutables.get ( propertyName ) != null );
	}

	//----------------------------------------------------------------
	//  ActionListener implementation
	//----------------------------------------------------------------

	/**
	 *  Respond to actions in controls.
	 */
	public void actionPerformed ( ActionEvent e )
	{
		if ( e.getSource() == addPropBtn )
		{
			newProperty();
		}
		else if ( e.getSource() == clearPropBtn )
		{
			clearProperty();
		}
		else if ( e.getSource() == nounsBtn )
		{
			NounEditor.edit ( (Frame) getParent(),
							  atom );
		}
		else if ( e.getSource() == editMultiBtn )
		{
			editMultiLine();
		}
		else if ( e.getSource() == editStyledBtn )
		{
			editStyled();
		}
		else if ( e.getSource() == inheritBtn )
		{
			inherit();
		}
		else if ( e.getSource() == uninheritBtn )
		{
			uninherit();
		}
		else
		{
			System.out.println ( "AtomInspector.actionPerformed(): unknown action \""
								 + e.getActionCommand()
								 + "\"" );
		}
	}

	//----------------------------------------------------------------
	//  ItemListener implementation
	//----------------------------------------------------------------

	/**
	 *  Handle item events. When an item is deselected,
	 *  its new value is flushed to the subject atom.
	 */
	public void itemStateChanged ( ItemEvent e )
	{
		// item selections in the inspector panel
		if ( e.getSource() == inspector )
		{
			if ( e.getStateChange() == ItemEvent.DESELECTED )
			{
				String propName = (String) e.getItem();
				String value = inspector.getPropertyValue ( propName );

				// if the property has been given a new value, set
				// it in the atom
				try
				{
					setField ( propName, value );
				}
				catch ( AtomException x )
				{
					Alert.showAlert ( (Frame) getParent(),
									  "Error setting property "
									  + propName
									  + ": "
									  + x );

					// reselect the property to get the user
					// to set a valid value
					inspector.setSelection ( propName );
				}
			}
			else if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				// put the property comments into the docs pane
				String propName = (String) e.getItem();
				String docComment = atom.getString ( "__" + propName );
				if ( docComment == null )
					docComment = "<color red>No comment available</color>";

				docs.setStyled ( "<b>" + propName + "</b><p>" + docComment );
			}
		}
		// item selections in the odl choice box
		else if ( e.getSource() == odlChoice )
		{
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				if ( odlChoice.getSelectedItem().equals("<None>") )
					atom.clearField("__odl__");
				else
					atom.setField("__odl__", odlChoice.getSelectedItem() );
			}
		}
		// item selections in the location choice box
		else if ( e.getSource() == whereChoice && atom instanceof Thing )
		{
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				if ( ! whereChoice.getSelectedItem().equals("<N/A>") )
				{
					// put the atom into the chosen container
					try
					{
						com.ogalala.mua.Container location
							= (com.ogalala.mua.Container) atom.getWorld().getAtom( whereChoice.getSelectedItem() );
						location.putIn ( atom );
					}
					catch ( AtomException x )
					{
						Toolkit.getDefaultToolkit().beep();
					}
				}

				// resync the choice with the actual container
				// (the putIn may have failed for various reasons)
				whereChoice.select ( ((Thing) atom).getContainer().getID() );
			}
		}
	}

	//----------------------------------------------------------------
	//	action handlers
	//----------------------------------------------------------------

	/**
	 *  Create a new property in this atom, setting its value
	 *  and providing documentation comments.
	 */
	public void newProperty()
	{
		// set up and show the new property dialog
		propDlg.name.setText("");
		propDlg.value.setText("");
		propDlg.comments.setText("");
		propDlg.show();

		if ( propDlg.cancelled )
			return;

		// create the new property and its associated comment
		atom.setField ( propDlg.name.getText(), conform(propDlg.value.getText()) );
		atom.setField ( "__" + propDlg.name.getText(), propDlg.comments.getText() );

		propDlg.name.setText("");
		propDlg.value.setText("");
		propDlg.comments.setText("");

		// reset the atom to flush the changes through
		setAtom (atom);
	}

	//----------------------------------------------------------------

	/**
	 *  Remove the currently selected property from the current
	 *  atom. If the property is not defined on the current atom,
	 *  this does nothing. If the property is overidden by the
	 *  current atom, the inherited version will still be
	 *  present -- inherited properties cannot be removed.
	 *  If the property is first defined by this atom, it will
	 *  disappear altogether.
	 */
	public void clearProperty()
	{
		// can't edit final atoms
		if ( ! mutable )
			return;

		// get the currently selected property name
		String selection = inspector.getSelectedItem();

		// can't clear non-existent or non-local properties
		if ( selection == null
			 || atom.getField( selection ) == null )
			return;

		atom.clearField( selection );
		atom.clearField( "__" + selection );

		// if the property is inherited, update it with
		// the new value and the appropriate traits
		Object newValue = atom.getRawProperty( selection );

		if ( newValue != null )
		{
			String newValStr = AtomData.toString ( newValue );
			TextField valueField = (TextField) inspector.getPropertyComponent ( selection );
			valueField.setText ( newValStr );
			if ( priorities.get( selection ) == null )
				inspector.setPropertyTraits ( selection, INHERITED );
			else
			{
				InspectorPanel.PropertyTraits trait
					= new InspectorPanel.PropertyTraits ( INHERITED.font, INHERITED.colour );
				trait.priority = ((Character) priorities.get( selection )).charValue();
				inspector.setPropertyTraits ( selection, trait );
			}
		}
		else
		{
			// reset the atom to rebuild inspector
			inspector.removeProperty ( selection );
			setAtom ( atom );
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Edit the currently selected property using a
	 *  multi-line edit field. The resulting text is
	 *  run through the <tt>conform()</tt> method
	 *  before being set in the atom.
	 */
	public void editMultiLine()
	{
		// can't edit final atoms
		if ( ! mutable )
			return;

		// get the currently selected property name
		String propName = inspector.getSelectedItem();

		// get the current value (in the inspector)
		String oldValue = inspector.getPropertyValue( propName );

		// determine whether the value is currently inherited
		boolean isLocal = ( atom.getField ( propName ) != null );

		// open an edit window to do the editing
		String newValue = TextEditor.edit ( (Frame) getParent(),
						  					"Editing " + propName,
						  					StringUtil.replace ( oldValue,
						  										 "<p>",
						  										 System.getProperty( "line.separator" )));
		if ( newValue == null )
			return;

		// set the value in the atom
		setField ( propName, conform ( newValue ) );
	}

	//----------------------------------------------------------------

	/** ### Not currently implemented. ### */
	public void editStyled() {}

	//----------------------------------------------------------------

	/**
	 *  Inherit from the currently selected item in the
	 *  atom list.
	 */
	public void inherit ()
	{
		if ( ! mutable )
			return;

		try
		{
			// get the selected parent
			String parentName = atomList.getSelectedItem();
			Atom parent = atom.getWorld().getAtom ( parentName );

			// inherit the current atom from it
			if ( ! atom.isDescendantOf ( parent ) )
			{
				atom.inherit ( parent );

				// rebuild the dialog to take account of
				// the change
				setAtom ( atom );
			}
		}
		catch ( AtomException e )
		{
			Alert.showAlert ( (Frame) getParent(),
							  "Exception when trying to add parent: "
							  + e );
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Stop inheriting from the currently selected item
	 *  in the parents list.
	 */
	public void uninherit ()
	{
		if ( ! mutable )
			return;

		try
		{
			// get the selected parent
			String parentName = parentList.getSelectedItem();
			Atom parent = atom.getWorld().getAtom ( parentName );

			// inherit the current atom from it
			if ( atom.isDescendantOf ( parent ) )
			{
				atom.uninherit ( parent );

				// rebuild the dialog to take account of
				// the change
				setAtom ( atom );
			}
		}
		catch ( AtomException e )
		{
			Alert.showAlert ( (Frame) getParent(),
							  "Exception when trying to remove parent: "
							  + e );
		}
	}

	//----------------------------------------------------------------

	/**
	 *  Convert an arbitrary string into a valid property
	 *  value. The following changes are made:
	 *  <ul>
	 *  <li>Leading and trailing whitespace is trimmed.
	 *  <li>If the string begins with a '[' character,
	 *      the brackets are balanced and any linebreak
	 *      characters are replaced with spaces.
	 *  <li>If the string is a valid integer, it is left unchanged.
	 *  <li>If the string is "true" or "false" it is left unchanged.
	 *  <li>If the string begins with a $ or a ! everything after
	 *      the first non-valid character is dropped.
	 *  <li>Otherwise, the string is taken to represent a string.
	 *      Any surrounding quotes are stripped, any internal
	 *      quotes are escaped and any linebreaks are replaced
	 *      with &lt;p&gt; tokens.
	 *  </ul>
	 */
	public String conform ( String source )
	{
		if ( source == null )
			return null;

		source = source.trim();

		// boolean?
		if ( source.equalsIgnoreCase( "true" )
			 || source.equalsIgnoreCase( "false" ) )
			return source;

		// integer?
		try
		{
			int num = Integer.parseInt ( source );
			return ( source );
		}
		catch ( NumberFormatException e )
		{
		}

		// action or atom
		if ( source.startsWith ( "$" )
			 || source.startsWith ( "!" ) )
		{
			// a single ! or $ is invalid, so replace it
			// with the empty string ""
			if ( source.length() == 1 )
				return ( "\"\"" );

			// scan through the string until we reach an
			// invalid identifier character and return
			// the chunk to that point
			for ( int i = 1; i < source.length(); i++ )
			{
				char ch = source.charAt ( i );
				if ( ( ch < 'a' && ch > 'z' )
					 && ( ch < 'A' && ch > 'Z' )
					 && ( ch < '0' && ch > '9' )
					 && ch != '_' )
				{
					// as above, a single $ or ! is invalid
					if ( i == 1 )
						return "\"\"";
					else
						return source.substring( 0, i );
				}
			}
		}

		// for lists and tables, parse the list and then dump
		// it to a string
		if ( source.startsWith( "[" ) )
		{
			TableParser parser = new TableParser ( source.substring(1) );
			Hashtable table = new Hashtable ();

			parser.parseStream ( table );
			Object result = table.get ( "" );
			if ( result instanceof Vector )
			{
				return listToString ( (Vector) result );
			}
			else if ( result instanceof Dictionary )
			{
				return tableToString ( (Dictionary) result );
			}
			else
				return ( "[]" );
		}

		// the only remaining possibility is that it's a String
		// strip surrounding quotes and escape any internal ones

		// strip starting and ending quotes
		if ( source.startsWith("\"") )
			source = source.substring(1);
		if ( source.endsWith("\"") )
			source = source.substring( 0, source.length() - 1 );

		// unescape any already-escaped quotes to avoid
		// accumulating backslashes
		source = StringUtil.replace ( source, "\\\"", "\"" );

		// escape quotes
		source = StringUtil.replace ( source, "\"", "\\\"" );

		// transform linebreaks
		source = StringUtil.replace ( source,
									  "\r\n",
									  "<p>" );
		source = StringUtil.replace ( source,
									  "\r",
									  "<p>" );
		source = StringUtil.replace ( source,
									  "\n",
									  "<p>" );
		return source;
	}

	//----------------------------------------------------------------

	/**
	 *  Get a string representation of a list, as understood
	 *  by ODL.
	 */
	public String listToString ( Vector list )
	{
		Enumeration enum = list.elements();
		if ( ! enum.hasMoreElements() )
			return "[]";

		String buildUp = "[ ";

		while ( enum.hasMoreElements() )
		{
			Object item = enum.nextElement();

			if ( item instanceof String )
				buildUp += "\"" + conform ( (String) item ) + "\"";
			else if ( item instanceof Vector )
				buildUp += listToString ( (Vector) item );
			else if ( item instanceof Dictionary )
				buildUp += tableToString ( (Dictionary) item );

			buildUp += " ";
		}

		return buildUp + "]";
	}

	//----------------------------------------------------------------

	/**
	 *  Get a string representation of a table, as
	 *  understood by ODL.
	 */
	public String tableToString ( Dictionary table )
	{
		Enumeration enum = table.keys();
		if ( ! enum.hasMoreElements() )
			return "[]";

		String buildUp = "[ ";

		while ( enum.hasMoreElements() )
		{
			String key = (String) enum.nextElement();

			Object item = table.get( key );
			String itemStr;

			if ( item instanceof String )
				buildUp += key + "= \"" + conform ( (String) item ) + "\"";
			else if ( item instanceof Vector )
				buildUp += key + "=" + listToString ( (Vector) item );
			else if ( item instanceof Dictionary )
				buildUp += key + "=" + tableToString ( (Dictionary) item );

			buildUp += " ";
		}

		return buildUp + "]";
	}

	//----------------------------------------------------------------

	/**
	 *  Utility for setting field values and making any
	 *  requisite changes to the inspector (traits etc).
	 */
	protected void setField ( String fieldName, String newValue )
	{
		// if the new value is the same as the current value,
		// do nothing
		Object oldValue = atom.getRawProperty( fieldName );
		String oldValStr;

		if ( oldValue == null )
			oldValStr = "null";
		else
			oldValStr = AtomData.toString ( oldValue );

		if ( oldValStr.equals ( newValue ) )
			return;

		// check to see whether the current value is inherited
		boolean nonLocal = ( atom.getField( fieldName ) == null );

		// set the value in the atom
		newValue = conform ( newValue );
		atom.setField ( fieldName, newValue );

		// adjust the value in the inspector, altering the traits
		// if necessary
		TextField valueField = (TextField) inspector.getPropertyComponent ( fieldName );
		valueField.setText ( newValue );
		inspector.repaint();
		if ( nonLocal )
		{
			if ( priorities.get( fieldName ) == null )
				inspector.setPropertyTraits ( fieldName, LOCAL );
			else
			{
				InspectorPanel.PropertyTraits trait
					= new InspectorPanel.PropertyTraits ( LOCAL.font, LOCAL.colour );
				trait.priority = ((Character) priorities.get( fieldName )).charValue();
				inspector.setPropertyTraits ( fieldName, trait );
			}
		}
	}

	//----------------------------------------------------------------
}