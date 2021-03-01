// $Id: Bartle.java,v 1.3 1999/03/10 16:58:06 jim Exp $
// The Bartle
// Alexander Veenendaal, 4th September 98
// Copyright (C) BTP Ltd <www@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;

/** Start Bartleing
*/
public class BartleStart extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        if (current instanceof Thing)
        {
            Thing thing = (Thing)current;
            thing.addWatcher(new BartleWatcher(world, thing, true));
            world.timerEvent((Mobile)current, thing, 3);
        }
        return true;
    }

    class BartleWatcher extends Watcher
    {
        private World world;
		private Thing thing;
		
        public BartleWatcher(World world, Thing thing, boolean isLead)
        {
            super(thing,isLead);
            this.world = world;
            this.thing = thing;
        }

        protected void doOutput(String msg, Event event)
        {
            if(event == null)
                return;
            msg = msg.toLowerCase();
            
            if(msg.indexOf("swamp.") != -1 && msg.indexOf("sinks") != -1)
            {
            	world.parseCommand("emote nods approvingly", (Mobile)thing );
            }
        }
    }
}
