// $Id: ToolWatcher.java,v 1.2 1999/03/08 16:18:27 matt Exp $
// A Watcher that forwards output to a WorldRunner app
// Matthew Caldwell, 11 November 1998
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.tools;

import com.ogalala.mua.*;

import com.ogalala.util.*;

/**
 *  A simple watcher that forwards all messages to a
 *  WorldRunner application.
 */
public class ToolWatcher
	extends Watcher
{
	//----------------------------------------------------------------
	//  instance variables
	//----------------------------------------------------------------

	/** The application on whose behalf this object is watching. */
	private ToolWatchReceiver theApp = null;
	
	//----------------------------------------------------------------
	//  construction
	//----------------------------------------------------------------

	/** Constructor. */
	public ToolWatcher ( Atom atom, boolean isLead, ToolWatchReceiver app )
	{
		super ( atom, isLead );
		theApp = app;
	}

	//----------------------------------------------------------------
	//  usage
	//----------------------------------------------------------------

	/**
	 *  Forward any received messages to the owning application.
	 */
	protected void doOutput ( String msg, Event evt )
	{
		if ( msg != null )
			theApp.receive ( msg );
	}

	//----------------------------------------------------------------
}