// $Id: BuilderMenuManager.java,v 1.11 1999/03/11 14:37:15 matt Exp $
// An object to manage menus for atom builder.
// Matthew Caldwell, 26 November 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;

import com.ogalala.widgets.*;
import com.ogalala.client.*;

/**
 *  An object to take care of the menu management for the
 *  MUA AtomBuilder application. This creates and connects
 *  the menus, modifies them in response to received events,
 *  and forwards menu action events to any registered
 *  listeners. It's main purpose is to centralize all the
 *  menu specifics so that they can be easily modified.
 */
public class BuilderMenuManager
	extends MenuManager
{	
	//---------------------------------------------------------------

	public BuilderMenuManager ( Frame parent )
	{
		super(parent);
		
		// build the menus -- note that although it isn't
		// necessary to explicitly include the action command
		// when it's the same as the menu name, I've done so
		// here so that the menu text can be changed without
		// breaking the messages received by the client -- in
		// the unlikely event that the program ever needs to
		// be localized, only the names need to be changed
		addMenuItem ( "File", "New ODL", "NewODL" );
		addMenuItem ( "File", "Save ODL", KeyEvent.VK_S, false, "Save" );
		addMenuItem ( "File", "Save All", "SaveAll" );
		addSeparator ( "File" );
		addMenuItem ( "File", "Import ODLs", KeyEvent.VK_I, false, "Import" );
		addMenuItem ( "File", "Run Script", "Run" );
		addSeparator ( "File" );
		addMenuItem ( "File", "Quit", KeyEvent.VK_Q, false, "Quit" );

		addMenuItem ( "World", "New Atom", KeyEvent.VK_N, false, "NewAtom" );
		addMenuItem ( "World", "Delete Atom", KeyEvent.VK_DELETE, false, "DelAtom" );
		addSeparator ( "World" );
		addMenuItem ( "World", "Find Atom", KeyEvent.VK_F, false, "FindAtom" );
		addSeparator ( "World" );
		addMenuItem ( "World", "New Exit", "NewExit" );
		addMenuItem ( "World", "Remove Exit", "DelExit" );
		addSeparator ( "World" );
		addMenuItem ( "World", "Update Browser", KeyEvent.VK_U, false, "Refresh" );
		addSeparator ( "World" );
		addMenuItem ( "World", "Test World", KeyEvent.VK_T, false, "TestWorld" );
		
		addMenuItem ( "Version", "Revert to core database", "Revert" );
		addSeparator ( "Version" );
		addMenuItem ( "Version", "Update", "Update" );
		addMenuItem ( "Version", "Check In...", "CheckIn" );
		
		addMenuItem ( "Window", "Inspector", KeyEvent.VK_1, false, "ShowInspector" );
		addMenuItem ( "Window", "Exits", KeyEvent.VK_2, false, "ShowExits" );
	}

	//---------------------------------------------------------------
}
