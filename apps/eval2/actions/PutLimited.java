// $Id: PutLimited.java,v 1.4 1999/03/11 10:41:08 alex Exp $
//
// Richard Morgan, 31 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;
import com.ogalala.mua.*;
import com.ogalala.util.Debug;

/** this version of put checks to see if an object can fit into it.
	Currently this is done by looking at the hierracy to see if the
	thing is a descendant of the template object which is stored
	in the field "contains_atoms" defined in the limited container atom.
*/
public class PutLimited
    extends PutIn
{
	public boolean checkIfFits( Thing thing, World world, Mobile actor, Container box, Container container, Event event )
	{
		Atom atom = box.getAtom( LimitedContainer.CONTAINS_ATOMS );

		Debug.assert( atom != null, "Limited Container does not have the contains_atoms field!");

		boolean fits = thing.isDescendantOf( atom );

		if ( fits )
		{
			return true;
		}
		else
		{
	   		event.setResult(box.getString( ContainerDef.TOO_BIG_MSG ));
	   		return false;
		}
	}
}

