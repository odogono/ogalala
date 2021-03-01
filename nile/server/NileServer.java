// $Id: NileServer.java,v 1.5 1998/06/08 15:50:42 rich Exp $
// Server for 1932.com Death on the Nile
// James Fryer, 25 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.server;

import java.net.*;
import com.ogalala.server.*;
import com.ogalala.nile.world.*;

public class NileServer
    extends Server
	{
	// Game world application
    World world;

	public NileServer(int port, String worldName)
		{
		super(port);
		BasicLogin.setMessages("Welcome to 1932.com Death on the Nile\n", "Login: ");
		world = new World(worldName);
		world.start();
		}

    protected void createLogin(Socket socket)
        { new BasicLogin(this, socket); }

    public void createConnection(Socket socket, String userName)
        { new com.ogalala.nile.world.User(this, socket, userName); }

    public World getWorld()
        { return world; }
	}
