// $Id: BrowserFrame.java,v 1.4 1999/03/16 16:12:40 matt Exp $
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
public class BrowserFrame
	extends Frame
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
	 *  Create a BrowserFrame with the specified contents.
	 */
	public BrowserFrame ( String title,
						  OpenClosedTree contents )
	{
		super ( title );
		setSize ( 400, 300 );
		
		Panel panel = new Panel ();
		panel.setBackground ( Color.white );
		
		// create contents and lay them out
		outline = new Outline ( contents, this );

		panel.setLayout ( new BindLayout() );
		
		panel.add ( info,
				    new BindConstraints ( 200, 300,
				  						  BindConstraints.NONE,
				  						  0,
				  						  0,
				  						  0 ) );
		
		scroller.add ( outline );
		
		panel.add ( scroller,
				    new BindConstraints ( 200, 300,
				  						  0,
				  						  0,
				  						  200,
				  						  0 ) );
		
		setLayout ( new BorderLayout() );
		add ( "Center", panel );
				  						  
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
	 *  Set the contents of the outline view.
	 */
	public void setContents ( OpenClosedTree contents )
	{
		// create contents and lay them out
		boolean wasVisible = isVisible();
		
		setVisible ( false );
		outline = new Outline ( contents, this );

		scroller.add ( outline );

		setVisible ( true );
		outline.invalidate();
		outline.validate();
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

	/**
	 *  Attempt to select the given name in the tree,
	 *  and if successful scroll so that the item is
	 *  visible onscreen.
	 */
	public void select ( String name )
	{
		OpenClosedTree result = outline.select ( name );
		if ( result == null )
			return;
		
		OutlineItem selected = outline.getSelection();
		int y = selected.getLocation().y;
		
		scroller.setScrollPosition ( 0, y-10 );
		repaint();
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