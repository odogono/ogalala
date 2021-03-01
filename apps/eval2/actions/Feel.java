// $Id: Feel.java,v 1.5 1998/08/17 18:14:08 rich Exp $
// 
// Richard Morgan, 28 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;
import com.ogalala.mua.*;
import com.ogalala.util.Debug;

public class Feel
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
		// tell the world
   		container.output( new Output( "misc","msg",event.formatOutput( current.getString( Textured.OMSG ) ) ), event, actor );
       
       	// tell the actor
		event.setResult( current.getString( Textured.MSG ) + " " + current.getString( Textured.RESULT_MSG ) );
        }
    }

