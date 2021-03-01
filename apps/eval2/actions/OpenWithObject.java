// $Id: OpenWithObject.java,v 1.3 1999/03/11 10:41:08 alex Exp $
// Open command
// Richard Morgan, 24 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;
import com.ogalala.util.Debug;
import java.util.*;

/** Open action for the Openable atom
*/
public class OpenWithObject
    extends Open
    {

    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
		if ( !checkForKeyObject( world, actor, current, container, event ) )
			{
			event.setResult( new Output( "misc", "msg", current.getString( OpenableWithObject.OPEN_FAIL_MSG ) ) );
			return;
			}

		super.execute( world, actor, current, container, event );
        }

	public boolean checkForKeyObject( World world, Mobile actor, Atom current, Container container, Event event )
		{
		Enumeration enum = actor.getDeepContents();

		Atom keyObj = current.getAtom( OpenableWithObject.KEY_OBJECT );

		while ( enum.hasMoreElements() )
			{
			Atom atom = ( Atom ) enum.nextElement();
			if ( atom.isDescendntOf( keyObj ) )
				return true;
			}

		// didn't find the key object
		return false;
		}
    }

