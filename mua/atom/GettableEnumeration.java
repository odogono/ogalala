// $Id: GettableEnumeration.java,v 1.12 1999/04/09 15:53:44 alex Exp $
// Traverse all accessible atoms in a container.
// James Fryer, 23 Sept 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** 
* Selectively traverse all accessible atoms in a container.
* It will only return atoms that are:
*		- located on the level below the container (no traversing down through containers)
*		- that are obvious
*/
public class GettableEnumeration extends VisibleEnumeration
{
	
	Atom descendedFrom;
	

    public GettableEnumeration(Atom start, Atom viewerContainer, Atom actor)
    {
    	this(start,viewerContainer, null, actor);
    }//*/

    
    public GettableEnumeration(Atom start, Atom viewerContainer, Atom descendedFrom, Atom actor)
    {
    	willTraverseTransparentContainers = false;
    	this.detectionCapability = actor.getInt(DETECTION_SKILL)/100;
    	this.descendedFrom = descendedFrom;
    	this.viewerContainer = viewerContainer;
        init(start); 
    }//*/

}