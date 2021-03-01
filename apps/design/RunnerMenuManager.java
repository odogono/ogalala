// $Id: RunnerMenuManager.java,v 1.4 1998/11/20 16:54:58 matt Exp $
// An object to manage menus for the world runner.
// Matthew Caldwell, 11 November 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;

import com.ogalala.widgets.*;
import com.ogalala.client.*;

/**
 *  An object to take care of the menu management for the
 *  MUA WorldRunner application. This creates and connects
 *  the menus, modifies them in response to received events,
 *  and forwards menu action events to any registered
 *  listeners. It's main purpose is to centralize all the
 *  menu specifics so that they can be easily modified.
 */
public class RunnerMenuManager
	extends MenuManager
{	
	//---------------------------------------------------------------

	public RunnerMenuManager ( Frame parent )
	{
		super(parent);
		
		// build the menus -- note that although it isn't
		// necessary to explicitly include the action command
		// when it's the same as the menu name, I've done so
		// here so that the menu text can be changed without
		// breaking the messages received by the client -- in
		// the unlikely event that the program ever needs to
		// be localized, only the names need to be changed
		addMenuItem ( "File", "Reload World", KeyEvent.VK_R, false, "Reload" );
		addSeparator ( "File" );
		addMenuItem ( "File", "New ODL", KeyEvent.VK_N, false, "New" );
		addMenuItem ( "File", "Edit ODL...", KeyEvent.VK_O, false, "Edit" );
		addMenuItem ( "File", "Import ODL...", "Import" );
		addSeparator ( "File" );
		addMenuItem ( "File", "Update", "Update" );
		addMenuItem ( "File", "Check In...", "CheckIn" );
		addSeparator ( "File" );
		addMenuItem ( "File", "Quit", KeyEvent.VK_Q, false, "Quit" );
		
		addMenuItem ( "Edit", "Undo", KeyEvent.VK_Z, false, "Undo" );
		addSeparator ( "Edit" );
		addMenuItem ( "Edit", "Cut", KeyEvent.VK_X, false, "Cut" );
		addMenuItem ( "Edit", "Copy", KeyEvent.VK_C, false, "Copy" );
		addMenuItem ( "Edit", "Paste", KeyEvent.VK_V, false, "Paste" );
		addMenuItem ( "Edit", "Clear", "Clear" );
		addSeparator ( "Edit" );
		addMenuItem ( "Edit", "Preferences...", "Prefs" );
		
		addMenuItem ( "View", "Short Descriptions", -1, false, "Terse", true );
		addMenuItem ( "View", "Echo Outgoing Messages", -1, false, "NoEchoOut", true );
		setItemChecked ( "Echo Outgoing Messages", true );
		addMenuItem ( "View", "Show Debug Messages", -1, false, "NoDebug", true );
		setItemChecked ( "Show Debug Messages", true );
		addMenuItem ( "View", "Show Atom IDs", -1, false, "NoAtomIDs", true );
		setItemChecked ( "Show Atom IDs", true );
		
		addMenuItem ( "Help", "About 1932.com...", "About" );
		addMenuItem ( "Help", "Help", "Help" );
		
		// some items are initially disabled because they depend
		// on there being an active world
		setItemEnabled ( "Import ODL", false );
		
		// grey out unimplemented items and menus
		setMenuEnabled ( "Help", false );
		setItemEnabled ( "Undo", false );
		setItemEnabled ( "Cut", false );
		setItemEnabled ( "Copy", false );
		setItemEnabled ( "Paste", false );
		setItemEnabled ( "Clear", false );
		
		setItemEnabled ( "Preferences...", false );
		setItemEnabled ( "Help", false );
		setItemEnabled ( "About 1932.com...", false );
	}

	//---------------------------------------------------------------
}
