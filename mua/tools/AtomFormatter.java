// $Id: AtomFormatter.java,v 1.4 1999/03/17 16:39:39 matt Exp $
// Abstract class to handle formatted output of atoms
// Matthew Caldwell, 16 November 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import com.ogalala.util.*;
import com.ogalala.mua.*;
import java.util.*;
import java.io.*;

/**
 *  Abstract base class for documentation formatters that
 *  output documentation on an atom database.
 */
public abstract class AtomFormatter
{
	//-------------------------------------------------------------
	//  instance variables
	//-------------------------------------------------------------

	protected AtomDatabase data = null;
	
	protected World world = null;
	
	protected String outputPrefix = "".intern();
	
	protected String localPrefix = outputPrefix;
	
	protected PrintWriter err = null;
	
	//-------------------------------------------------------------
	//  construction
	//-------------------------------------------------------------

	/** Default constructor. */
	public AtomFormatter () {}

	//-------------------------------------------------------------
	//  usage
	//-------------------------------------------------------------
	
	/**
	 *  Initialize the AtomFormatter with the details it needs.
	 */
	public void init ( World world,
					   String outputPrefix,
					   PrintWriter err )
	{
		this.world = world;
		
		if ( outputPrefix != this.outputPrefix )
		{
			this.outputPrefix = outputPrefix;
			
			if ( outputPrefix.indexOf( File.separatorChar ) != -1 )
			{
				if ( outputPrefix.lastIndexOf ( File.separatorChar ) + 1 < outputPrefix.length() )
					localPrefix = outputPrefix.substring( outputPrefix.lastIndexOf( File.separatorChar ) );
				else
					localPrefix = "";
			}
			else
			{
				localPrefix = outputPrefix;
			}
		}
		
		if ( err == null )
			this.err = new PrintWriter ( System.err, true );
		else
			this.err = err;
		
		this.data = world.getAtomDatabase ();
	}
	
	//-------------------------------------------------------------

	/**
	 *  Generate documentation on a single named atom.
	 *  This is the only method that a subclass <i>must</i>
	 *  implement.
	 */
	public abstract void documentAtom ( Atom atom );

	//-------------------------------------------------------------
	
	/**
	 *  Generate documentation on the nouns defined in
	 *  the hashtable. The hashtable contains only a mapping
	 *  of nouns to the names of the atoms that define them.
	 *  Any additional information must be extracted from the
	 *  atom database.
	 *  <p>
	 *  By default, this does nothing.
	 */
	public void documentNouns ( Hashtable nouns ) {}

	//-------------------------------------------------------------
		
	/**
	 *  Generate an index of all atoms.
	 *  <p>
	 *  By default this does nothing.
	 
	 @param includeThings  Whether to include Things in the
	                       index or limit it to the abstract
	                       Atoms.
	 
	 */
	public void indexAtoms ( boolean includeThings ) {}
	
	//-------------------------------------------------------------

	/**
	 *  Do any necessary cleaning up before terminating.
	 */
	public void cleanup () {}

	//-------------------------------------------------------------
}