// $Id: ChannelOpenException.java,v 1.3 1998/06/24 21:12:52 jim Exp $
// Exception thrown when channel can't be opened
// James Fryer, 3 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

/** Exception thrown when channel can't be opened
*/
public class ChannelOpenException
	extends Exception
    {
	public ChannelOpenException() {}
	public ChannelOpenException(String s) { super(s); }
    }
