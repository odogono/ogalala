// $Id: Stand.java,v 1.5 1999/03/11 10:41:08 alex Exp $
//
// Richard Morgan, 14 Aug 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;
import com.ogalala.mua.*;
import com.ogalala.util.Debug;

/**
*/
public class Stand
    extends JavaAction
{
    public void execute( World world, Mobile actor, Atom current, Container container, Event event )
    {
        // get a refrence to the seat atom
        AtomDatabase data = world.getDatabase();

        Atom seatAtom = data.getAtom( Seat.atomName );

        // get the actors container
        Container seat = actor.getContainer();

        // is the actors container a seat?
        if ( seat.isDescendantOf( seatAtom ) )
        {
            // yes the actor is in a seat container move the actor to the 'super container'
            Container room = seat.getContainer();
            Debug.assert( room != null, "Tried to stand up from a seat but there is no super container" );

			// construct the message for others
            String omsg = seat.getString( ContainerDef.GET_OUT_OMSG );
			Output outOther = new Output( "misc","msg",event.formatOutput( omsg ) );

			// construct the message for actor
            String msg = seat.getString( ContainerDef.GET_OUT_MSG );
			Output outActor = new Output( "misc","msg",event.formatOutput( msg ) );

			// move the actor
            room.add( actor );

            // tell the room we stood up
            room.output( outOther, event, actor );

            // tell the actor
            event.setResult( outActor );
        }
        else
        {
            String msg = seatAtom.getString( Seat.STAND_FAIL_MSG );
            event.setResult( msg );
        }
    }
}

