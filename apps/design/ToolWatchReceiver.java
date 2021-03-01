// $Id: ToolWatchReceiver.java,v 1.1 1999/03/08 16:22:05 matt Exp $
// Interface for classes that use the ToolWatcher
// Matthew Caldwell, 8 March 1999
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.tools;

/**
 *  An interface for classes that use the ToolWatcher
 *  for monitoring a World.
 */
public interface ToolWatchReceiver
{
	/**
	 *  Receive output from the World.
	 */
	public void receive ( String message );
}