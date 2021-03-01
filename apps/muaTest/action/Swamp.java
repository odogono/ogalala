// $Id: Swamp.java,v 1.4 1999/03/10 16:58:06 jim Exp $
// Swamp Functionality commands
// Alexander Veenendaal, 4 November 98
// Copyright (C) Ogalala Ltd <www@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;

public class SwampDrop extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
    	//move the atom to the limbo container and notify users of their win
        world.moveAtom( current, world.getAtom("limbo") );
        
        container.output(current.getString("drop_omsg"), actor);
        
        container.output("The " + current.getString("name") + " sinks slowly and fartily into the swamp.");
        
        actor.output("Suddenly, you feel at least five points richer.");
        
        return true;
        
    }
    
}
