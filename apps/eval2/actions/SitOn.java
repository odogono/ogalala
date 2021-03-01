// $Id: SitOn.java,v 1.4 1998/11/05 12:02:02 rich Exp $
// 
// Richard Morgan, 31 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;
import com.ogalala.mua.*;
import com.ogalala.util.Debug;
import java.util.*;

/** open envelope requires a knife ...
*/
public class SitOn
    extends PutIn
{
	public void execute( World world, Mobile actor, Atom current, Container room, Event event )
    {
		// the current object is a 
		Container seat = ( Container ) current;

		// is the seat full?
		if ( !checkIfFits( actor, world, actor, seat, room, event ) )
		{
            // tell the user his arse is too big
            String msg = current.getString( ContainerDef.TOO_BIG_MSG );
            event.setResult( new Output( "misc", "msg", msg ) );
			return;
		}
		else
		{
			// notify the room that it has happened (the reason for this is if it is done after the move the actor 
			// will hear it when it is broadcast to the sofa
            
            String omsg = current.getString( ContainerDef.PUT_IN_OMSG );
            room.output( new Output( "misc", "msg", event.formatOutput( omsg ) ), event, actor );

			// do the move
			seat.add( actor );
            String msg = current.getString( ContainerDef.PUT_IN_MSG );
			event.setResult( new Output( "misc", "msg", msg ) );
		}
	}

	/** This is a test to see if the new target container will except a new object. This 
		allows subclasses overide the defualt check. This function and it children are responsible
		for filling in the result on the event.
		@return the thing that is to be moved
	*/
	public boolean checkIfFits( Thing thing, World world, Mobile actor, Container seat, Container container, Event event )
	{
		int sizeOfActor = actor.getInt( ThingDef.SIZE );

		int spaceAvailable = seat.getInt( ContainerDef.CAPACITY );
		
		// space used by others things on the chair
		Enumeration enum = seat.getContents();
		
		while ( enum.hasMoreElements() ) 
			{
			try	
				{
				Thing item = ( Thing ) enum.nextElement();
				spaceAvailable -= item.getInt( ThingDef.SIZE );
				}
			catch ( ClassCastException e )
				{
				Debug.println( "Seat contains a non-thing cast exception thrown" );
				Debug.printStackTrace( e );
				}
			}
		
		return sizeOfActor <= spaceAvailable;
	}
}