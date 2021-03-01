// $Id: AdminCommands.java,v 1.7 1999/03/17 14:37:03 jim Exp $
// MUA administration commands
// James Fryer, 18 Sept 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.util.*;

import com.ogalala.mua.*;
import com.ogalala.util.Debug;

/** Run a script
    <p>
    Although this command is included here, it is in fact implemented 
    inside the parser.
*/
public class ModRun
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check args
        if (event.getArgCount() < 1)
            throw new AtomException("!RUN script-file-name");
        
        // Run the script
        try {
            world.execScript(event.getArg(0).toString(), actor);
            }
        catch (java.io.IOException e)
            {
			// write the stack trace to the log
            Debug.printStackTrace( e );
            
            // Rethrow IO errors as runtime exceptions -- the caller will display to the user
            //### COuld catch different types of IO error (e.g. file not found) to 
            //###   make the user error message more informative.
            throw new RuntimeException(e.getMessage());
            }
            
        // Report back to the user
		actor.output("Script complete.");
		
		return true;
        }
    }

/** Save database
*/
public class ModSave
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Save the world data
        try {
            WorldFactory.saveWorld(world);
            }
        catch (java.io.IOException e)
            {
            // Rethrow IO errors as runtime exceptions -- the caller will display to the user
            throw new RuntimeException(e.getMessage());
            }
        
        // Report back to the user
        actor.output("Database saved.");
        
        return true;
        }
    }

/** Get elapsed time
*/
public class ModTime
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Get the number of seconds the game has been up
        long time = world.getTime();
/*        
        //### Debugging stuff...
        if (event.getArgCount() > 0)
            time = Integer.parseInt(event.getArg(0).toString());
*/

        // Format the time string
        //### This lot should probably go in a utility class somewhere.
        final long MINUTE = 60;
        final long HOUR = MINUTE * 60;
        final long NHOURS = 24;
        final long DAY = HOUR * NHOURS;
        long days = time / DAY;
        long hours = (time % DAY) / HOUR;
        long minutes = (time % HOUR) / MINUTE;
        long seconds = time % MINUTE;
        StringBuffer timeMsg = new StringBuffer("Elapsed game time: ");
        if (days > 0)
            {
            timeMsg.append(days);
            timeMsg.append(":");
            }
        if (hours < 10)
            timeMsg.append("0");
        timeMsg.append(hours);
        timeMsg.append(":");
        if (minutes < 10)
            timeMsg.append("0");
        timeMsg.append(minutes);
        timeMsg.append(":");
        if (seconds < 10)
            timeMsg.append("0");
        timeMsg.append(seconds);
        
        // Report back to the user
        actor.output(timeMsg.toString());
        
        return true;
        }
    }
