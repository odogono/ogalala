// $Id: ChatConnection.java,v 1.5 1999/04/15 14:46:17 matt Exp $
// Connection for the chat server
// James Fryer, 13 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.test;

import java.net.*;
import com.ogalala.server.*;

public class ChatConnection
    extends Connection
	{
    private Chat chat;

    public ChatConnection(Server server, Socket socket, String userName)
    	{
    	super(server, userName, socket);
    	}

    public void init()
        {
        super.init();
        chat = ((ChatServer)server).chat;
        }

    protected void processInput(String s)
        {
        chat.execute(this, s);
        }

    public void start()
        {
        super.start();
        output("\r\n\nWelcome to the chat room, " + getUserId() + ", /? for help\n");
        chat.announceArrival(this);
        }

    public void stop()
        {
        chat.announceDeparture(this);
        super.stop();
        }
	}
