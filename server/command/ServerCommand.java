// $Id: ServerCommand.java,v 1.10 1998/11/05 12:02:14 rich Exp $
// Abstract server command
// James Fryer, 22 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.util.*;
import com.ogalala.util.*;

abstract public class ServerCommand
    extends Command
    {
    /** The privilege level required to use this command
    */
    int privilegeLevel;

    public ServerCommand(ServerCommandInterpreter cli, String shortHelp, int privilegeLevel, int logLevel)
        {
        super(cli, shortHelp);
        this.privilegeLevel = privilegeLevel;
        this.logLevel = logLevel;
        }

    /** This implementation of the 'execute' function is a wrapper around
        the more specialised function that takes a User object as its context.
    */
    public void execute(Enumeration args, Object context)
        {
        // Convert 'context' to a User
        // ASSERT(context instanceof User)
        User user = (User)context;

        // Check privilege level
        if (user.getPrivilege() < privilegeLevel)
            {
            user.outputError("You are not permitted to run this command");
            return;
            }

        // Pass on to the abstract 'execute'
        if (user != null)
            {
            try {
                execute(args, user);
                }
            catch (Exception e)
                {
                user.outputError("Java exception: " + e.toString());
                Debug.printStackTrace( e );
                }
            }
        }

    /** This function does the work
    */
    abstract public void execute(Enumeration args, User user);

    /** Call this function from unimplemented commands
    */
    static void notImplemented(User user)
        {
        String s = user.getServer().formatError("Command not implemented");
        user.output(s);
        }
    }
