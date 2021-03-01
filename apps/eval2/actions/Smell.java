// $Id: Smell.java,v 1.6 1998/08/17 18:14:11 rich Exp $
// TIME command
// James Fryer, 28 July 98
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
public class Smell
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
		Debug.assert( current != null, "!smell has null current (Smell/23)" );
     
		// tell the world
   		container.output( new Output( "misc","msg",event.formatOutput( current.getString( Smelly.OMSG ) ) ), event, actor );
       
       	// tell the actor
		event.setResult( current.getString( Smelly.MSG ) + " " + current.getString( Smelly.SUCCESS_MSG ) );
        }
    }

