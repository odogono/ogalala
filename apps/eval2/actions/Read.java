// $Id: Read.java,v 1.7 1998/08/17 18:14:09 rich Exp $
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
public class Read
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
		container.output( new Output( "misc","msg",event.formatOutput( current.getString( Readable.OMSG ) ) ), event, actor );		

		event.setResult( current.getString( Readable.MSG ) + current.getString( Readable.TEXT ) );
        }
    }

