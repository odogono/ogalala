// $Id: ChatServer.java,v 1.4 1998/05/02 14:48:32 jim Exp $
// Chat Server test class
// James Fryer, 13 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.test;

import java.net.*;
import com.ogalala.server.*;

public class ChatServer
    extends Server
	{
	// Chat application
    Chat chat = new Chat();

	public ChatServer(int port)
		{
		super(port);
		BasicLogin.setMessages("Welcome to Ogalala test chat server\r\n", "Enter user name for Chat: ");
		}

    protected void createLogin(Socket socket)
        { new BasicLogin(this, socket); }

    public void createConnection(Socket socket, String userName)
        { new ChatConnection(this, socket, userName); }
	}
