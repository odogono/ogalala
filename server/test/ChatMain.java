// $Id: ChatMain.java,v 1.3 1998/05/02 14:48:32 jim Exp $
// Main class for chat server
// James Fryer, 13 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.test;

import com.ogalala.server.*;

class ChatMain
    {
	// Default port
	public static final int PORT  = 1932;

    public static void main(String[] args)
    	{
    	// Create the server
		Server server = new ChatServer(PORT);
		server.start();
    	}
    }
