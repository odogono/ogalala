// $Id: ClayShooterAction.java,v 1.2 1999/03/10 16:58:06 jim Exp $
// A Clay pigeon shooting device
// Alexander Veenendaal, 7th December 1998
// Copyright (C) BTP Ltd <www.ogalala.com>


package com.ogalala.mua.action;

import com.ogalala.mua.*;

public class ClayShooterStart extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        //create a watcher
        Thing thing = (Thing)current;
        thing.addWatcher(new ClayShooterWatcher(world, thing, true));
        
        return true;
    }
    
    class ClayShooterWatcher extends Watcher
    {
        private World world;
        
        public ClayShooterWatcher(World world, Thing thing, boolean isLead)
        {
            super(thing, isLead);
            this.world = world;
        }
        
        protected void doOutput(String msg, Event event)
        {
            if(event == null)
                return;
            msg = msg.toLowerCase();
            
            if( msg.indexOf("pull") != -1 )
            {
            	//create a new clay pigeon and shoot it
            	atom.getContainer().output( atom.getString("pigeon_shot_msg") );
            	
            	//get the target atom. it may be null.
				//Atom targetParent = (Atom)atom.getAtom("pigeon");
            }
        }
    }
        
        
}
