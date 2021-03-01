// $Id: Taste.java,v 1.7 1998/08/17 18:14:11 rich Exp $
// 
// Richard Morgan, 31 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;
import com.ogalala.mua.*;
import com.ogalala.util.Debug;

/** Time comand
	<p>
	template: "time"
	<p>
	### Note to rich -- please preserve this function as 'ModTime' when you 
	    write the time command.
*/
public class Taste
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
		// tell the world
   		container.output( new Output( "misc","msg",event.formatOutput( current.getString( Tastable.OMSG ) ) ), event, actor );
       
       	// tell the actor
		event.setResult( current.getString( Tastable.MSG ) + " " + current.getString( Tastable.SUCCESS_MSG ) );
        }
    }

