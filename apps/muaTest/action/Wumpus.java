// $Id: Wumpus.java,v 1.6 1999/03/10 16:58:06 jim Exp $
// The Wumpus
// Alexander Veenendaal, 4th September 98
// Copyright (C) BTP Ltd <www@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.util.*;
import com.ogalala.mua.*;

/** Start Wumpussing
*/
public class WumpusStart extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        if (current instanceof Thing)
        {
            Thing thing = (Thing)current;
            thing.addWatcher(new WumpusWatcher(world, thing, true));
            world.timerEvent((Mobile)current, thing, 3);
        }
        return true;
    }

    class WumpusWatcher extends Watcher
    {
        private World world;

        public WumpusWatcher(World world, Thing thing, boolean isLead)
        {
            super(thing,isLead);
            this.world = world;
        }

        protected void doOutput(String msg, Event event)
        {
            if(event == null)
                return;
            msg = msg.toLowerCase();
        }
    }
}

public class WumpusTimer extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		int r = Dice.roll(100);

		// Half the time, growl or be evil depending on "evil" flag
		
		if( r <= 20 )
		{
			container.output( current.getString("growl_msg"), (Mobile)current);
			world.timerEvent(actor, current, Dice.roll(2, 20));
		}
		else if( r <= 35 )
		{
			container.output( "The Wumpus silently plots your demise", (Mobile)current);
			world.timerEvent(actor, current, Dice.roll(2, 20));
		}
		else if( r <= 50 )
		{
			
			container.output( current.getString("evil_msg"), (Mobile)current);
			world.timerEvent(actor, current, Dice.roll(2, 20));
		}
		//try to exit the room
		else
		{
			int where = Dice.roll(100);
			
			if( where <= 25 && container.getExit(ExitTable.DIR_WEST) != null )
				world.parseCommand("go west", (Mobile)current);
			if( where <= 50 && container.getExit(ExitTable.DIR_EAST) != null )
				world.parseCommand("go east", (Mobile)current);
			if( where <= 75 && container.getExit(ExitTable.DIR_NORTH) != null )
				world.parseCommand("go north", (Mobile)current);
			if( where <= 100 && container.getExit(ExitTable.DIR_SOUTH) != null )
				world.parseCommand("go south", (Mobile)current);
			
            world.timerEvent(actor, current, 5);
        }
        
		
		return true;
	}
}