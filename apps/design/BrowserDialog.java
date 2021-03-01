// $Id: BrowserDialog.java,v 1.4 1999/02/24 16:56:42 matt Exp $
// Browser window with an outline view and styled info pane.
// Matthew Caldwell, 26 November 1998
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.ogalala.util.*;
import com.ogalala.widgets.*;
import com.ogalala.widgets.text.*;

/**
 *  A browser window which combines an Outline view of
 *  some tree of information with a simple styled text
 *  viewer to display the information associated with
 *  the selected node.
 */
public class BrowserDialog
	extends Dialog
	implements Viewer
{
	//----------------------------------------------------------------
	//  instance variables
	//----------------------------------------------------------------

	/** The outline view pane. */
	protected Outline outline = null;
	
	/** The styled text viewer pane. */
	protected ScrollingStyledTextPanel info = new ScrollingStyledTextPanel ();
	
	/** The scrolling panel in which the outline appears. */
	protected ScrollPane scroller = new ScrollPane();

	//----------------------------------------------------------------
	//  construction
	//----------------------------------------------------------------
	
	/**
	 *  Create a BrowserDialog with the specified contents.
	 */
	public BrowserDialog ( Frame parent,
						   String title,
						   OpenClosedTree contents )
	{
		super ( parent, title, false );
		setSize ( 400, 300 );
		
		// create peer so insets exist
		addNotify();
		Insets insets = getInsets();
		setBackground ( Color.white );
		
		// create contents and lay them out
		outline = new Outline ( contents, this );

		setLayout ( new BindLayout() );
		
		add ( info,
			  new BindConstraints ( 200, 300,
			  						BindConstraints.NONE,
			  						insets.top,
			  						insets.right,
			  						insets.bottom ) );
		
		scroller.add ( outline );
		
		add ( scroller,
			  new BindConstraints ( 200, 300,
			  						insets.left,
			  						insets.top,
			  						200,
			  						insets.bottom ) );

		outline.invalidate();
		outline.validate();
		invalidate();
		validate();
	}
	
	//----------------------------------------------------------------
	//  handling item events
	//----------------------------------------------------------------

	/** Add a listener for item events in the contained outline. */
	public void addItemListener ( ItemListener l )
	{
		outline.addItemListener ( l );
	}
	
	//----------------------------------------------------------------

	/** Remove a listener for item events in the contained outline. */
	public void removeItemListener ( ItemListener l )
	{
		outline.removeItemListener ( l );
	}
	
	
	//----------------------------------------------------------------
	//  accessors
	//----------------------------------------------------------------

	/**
	 *  Get a reference to the outline view this dialog contains.
	 */
	public Outline getOutline ()
	{
		return outline;
	}
	
	//----------------------------------------------------------------
	
	/**
	 *  Get the name of the currently selected item.
	 */
	public String getSelectedItem ()
	{
		return outline.getSelectedItem();
	}

	//----------------------------------------------------------------
	//  Viewer implementation
	//----------------------------------------------------------------
	
	/**
	 *  Display the information associated with the selected node.
	 */
	public void view ( Object obj )
	{
		info.setStyled ( obj.toString() );
	}

	//----------------------------------------------------------------

}