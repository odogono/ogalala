// $Id: VisibleEnumeration.java,v 1.12 1999/04/12 17:24:10 alex Exp $
// Traverse all visible atoms in a container.
// James Fryer, 23 Sept 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Traverse all visible atoms in a container.
    <p>
    Containers are searched if they are open, transparent and revealing.
    <p>
    Atoms are returned if they are in the same container as the viewer,
    or if they have the 'is_obvious' property set. 
*/
public class VisibleEnumeration
    extends ContainerEnumeration
    {

    /** Property to check if a container is closed
    */
    static final String IS_CLOSED = "is_closed";

    /** Property to check if a container is transparent
    */
    static final String IS_TRANSPARENT = "is_transparent";
    
    /**
    *	Property to check for the visibility of an object, ie. whether
    *	it can be seen by a particular actor
    */
    static final String VISIBILITY = "visibility";
    
    /**
    *	Property to check for the concealment of an item
    */
    static final String CONCEALMENT = "concealment";
    
    /**
    *
    */
    static final String DETECTION_SKILL = "detection_skill";
    
    /** 
    *	The container that the viewer is in. The default for this is
    *   the top-level atom.
    */
    protected Atom viewerContainer;
    
    /**
    *	Constant used for calculating the effective visibility of an object
    *	in the same container as the viewer
    */
    protected static final int K1 = 0;
    
    
    /**
    *	Constant used for calculating the effective visibility of an object
    *	in a different container to the character
    */
    protected static final int K2 = 0;

	/**
	*	The detection skill of the actor who called this VisibleEnumeration
	*/ 
    protected int detectionCapability = 0;
    /** 
    *	Should we enter transparent containers if they are closed? The visible
    *   search will, the gettable search won't.
    */
    protected boolean willTraverseTransparentContainers = true;
    
    
    //========================================================================//
    //--------------------------- Constructors -------------------------------//
    //========================================================================//
    /** 
    *	The constructor takes the start atom for the traversal, plus the actor
    *	to extract its viewing capacity
    */
	public VisibleEnumeration(Atom start, Atom actor )
        {
        super(start);
        this.detectionCapability = actor.getInt(DETECTION_SKILL)/100;
        }
        
    /** 
    *	The constructor as well as adding the vantage point, also adds the
    *	detection skill of the actor.
    */
    public VisibleEnumeration(Atom start, Atom viewerContainer, Atom actor )
    	{
    	this.detectionCapability = actor.getInt(DETECTION_SKILL)/100;
    	this.viewerContainer = viewerContainer;
    	init(start);
    	}
    	
    /** 
    *	Dummy ctor for 'GettableEnumeration'
    */
    protected VisibleEnumeration()
        {
        }
        
	/**
	*	Should 'atom' be returned as an element of the enumeration?
	*	<p>
	*	True if the atom is in the same container as the viewer, AND the atom is visible enough for the
	*	players detection skill to read
	*/
	protected boolean acceptForReturn(Atom atom)
		{
			//is the atom in the viewerContainer ?
			if( viewerContainer == null || viewerContainer == atom.getContainer() )
				return isImmediatelyVisible(atom);
				
			
			//otherwise, the atom is somewhere else
			else
				return isContainedVisible(atom, atom.getContainer());
				
			//return false;
		}//*/
		
	
	/**
	*
	*/
	protected boolean isImmediatelyVisible(Atom atom)
		{
		int vis = atom.getInt(VISIBILITY);
		
		int result = 100 - (atom.getInt(VISIBILITY) - K1);
		
		if( result <= detectionCapability)
			return true;
		return false;
		}
	
	/**
	*
	*/
	protected boolean isContainedVisible(Atom atom, Atom container)
		{
		if( 100 - (atom.getInt(VISIBILITY) - container.getInt(CONCEALMENT) - K2) <= detectionCapability )
			return true;
			
		return false;
		}
    
    
    /**
    *	Should 'atom' be traversed as a container ?
    *
    *	the container must be the viewers container, or open
    */
    protected boolean acceptForTraversal(Atom atom)
    	{
    	return	viewerContainer == null || 
    			viewerContainer == atom || 
    			!atom.getBool(IS_CLOSED) || 
    			(willTraverseTransparentContainers && atom.getBool(IS_TRANSPARENT));
    	}
    	
    }
    
    
    