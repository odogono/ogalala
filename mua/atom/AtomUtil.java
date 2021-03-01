// $Id: AtomUtil.java,v 1.26 1999/04/09 10:05:36 alex Exp $
// Misc atom utilities
// James Fryer, 27 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/** Miscellaneous atom utilities: Strings, ...
*/
public class AtomUtil
    {
    /** Format a string for output.
        @see OutputFormatter
    */
    public static String formatOutput(Event event, String msg)
        {
        return new OutputFormatter(event).format(msg);
        }
    
    /**
    *	Format a string for output, using a defined set of strings to substitute in
    */
	public static String formatOutput(Event event, String msg, String value[])
        {
        return new OutputFormatter(event, value).format(msg);
        }
    
    /** Format a string for output, with specified default atom
        @see OutputFormatter
    */
    public static String formatOutput(Event event, Atom defaultAtom, String msg)
        {
        return new OutputFormatter(event, defaultAtom).format(msg);
        }
    
    /**
    *	Format a string for output, using a defined set of strings to substitute in
    */
    public static String formatOutput(Event event, Atom defaultAtom, String msg, String value[])
    	{
    	return new OutputFormatter(event, defaultAtom, value).format(msg);
    	}
    
    /** Prevent instantiation
    */
    private AtomUtil()
        {
        }
        
    /**
     * Does the 'container' contain an atom which is descended from 'type'
     * 
     * @param container 		The container in which to look
     * @param type				the atom to test for
     * @return					true if the atom descendant exists, false if not
     */
    public static boolean containsDescendant(Atom container, Atom type)
    	{
    	Enumeration containerContents = container.getContents();
    	
    	while( containerContents.hasMoreElements() )
    		{
    		Atom candidate = (Atom)containerContents.nextElement();
    		if( candidate.isDescendantOf( type ) )
    			return true;
    		}
    	
    	//we found nothing of value....
    	return false;
    	}
    
    /**
    * Return the first atom in the container that descends from 'type'
    *
    * @param container			The container in which to look
    * @param type				The atom to test for
    * @returns					The first atom found that descends from type, or null if none is found
    */
    public static Atom findDescendant(Atom container, Atom type)
    	{
    	Enumeration containerContents = container.getDeepContents();
    		
    	while( containerContents.hasMoreElements() )
    		{
    		Atom candidate = (Atom)containerContents.nextElement();
    		if( candidate.isDescendantOf( type ) )
    			return candidate;
    		}
    	return null;		
    	}
    
    
    
    }
