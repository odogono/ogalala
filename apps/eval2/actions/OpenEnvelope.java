// $Id: OpenEnvelope.java,v 1.2 1998/08/17 18:14:08 rich Exp $
// 
// Richard Morgan, 31 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;
import com.ogalala.mua.*;
import com.ogalala.util.Debug;
import java.util.*;

/** open envelope requires a knife ...
*/
public class OpenEnvelope
    extends OpenWithObject
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
		super.execute( world, actor, current, container, event );

		boolean closed = current.getBool(Openable.IS_CLOSED);
		
		if ( !closed ) 
			{
			current.setString( Openable.CLOSE, "You cannot reseal the {name} once it has been opened");
			}
        }
    }

