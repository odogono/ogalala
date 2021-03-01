// $Id: DummyWorld.java,v 1.2 1998/11/05 17:35:40 matt Exp $
// Public wrapper for the dummy world class.
// Matthew Caldwell, 8 October 1998
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.io.IOException;
import java.io.FileNotFoundException;

/**
 *  DummyWorld is a public wrapper for the fake World class used
 *  by the tools. The justification for this extra wrapper is a
 *  bit dubious, but here goes: I want to keep the dummy classes
 *  in an obvious place that doesn't risk confusion with the
 *  real World.java, Event.java etc files. Hence, MuaDummies.java.
 *  However, IIRC javac requires that a file contain only a single
 *  public class, and that it have the same name as the file.
 *  DocCompiler needs public access to World, which would require
 *  a file called World.java, precisely what I want to avoid.
 *  Hence, DummyWorld.java.
 *  <p>
 *  My hope is that I'll be able to swap out MuaDummies.java and
 *  replace it with the proper classes if such an occasion arises
 *  and everything will continue to work in exactly the same way.
 *  But don't hold me to that.
 */
public class DummyWorld
	extends World
{
	public DummyWorld ( String fileName )
		throws IOException
	{
		if ( exists ( fileName ) )
			open ( fileName );
		else
			create ( fileName );
	}

	public AtomDatabase getPublicAtomDatabase ()
	{
		return getAtomDatabase();
	}
}