// $Id: AtomDocumenter.java,v 1.6 1999/03/11 10:41:08 alex Exp $
// Class to write atom documentation to a browser tree node.
// Matthew Caldwell, 30 November 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import com.ogalala.util.*;
import com.ogalala.mua.*;
import com.ogalala.widgets.*;
import java.util.*;
import java.io.*;
import java.awt.*;

/**
 *  A class that formats atom documentation as a string
 *  for display by the building tools.
 *  <p>
 *  There should really be some unification of this and
 *  the AtomFormatter classes, but at the moment this isn't
 *  possible due to rather short-sighted design of those
 *  classes. I'll look into this at a later date...
 */
public class AtomDocumenter
{
	//-------------------------------------------------------------
	//  class variables
	//-------------------------------------------------------------

	/** Style in which to display normal atoms. */
	public static final Style ATOM_STYLE
		= Style.getDefaultStyle().changeColour ( Color.blue );

	/** Style in which to display internal atoms. */
	public static final Style INTERNAL_STYLE
		= Style.getDefaultStyle();

	/** Style in which to display things. */
	public static final Style THING_STYLE
		= Style.getDefaultStyle().changeColour ( Color.red );

	/** Style in which to display exits. */
	public static final Style EXIT_STYLE
		= Style.getDefaultStyle().changeColour ( Color.green.darker() );

	//-------------------------------------------------------------
	//  utility functions
	//-------------------------------------------------------------

	/**
	 *  Generate a tree node containing the data pertinent to
	 *  the given atom.
	 */
	public static OpenClosedTree getAtomTreeNode ( Atom atom )
	{
		String name = getAtomName( atom );
		Style style;
		String data = getAtomInfo ( atom );

		if ( atom.getField("__") == null )
			style = INTERNAL_STYLE;
		else if ( atom.getClassName().equals("Atom") )
			style = ATOM_STYLE;
		else if ( name.indexOf(".") != -1 )
			style = EXIT_STYLE;
		else
			style = THING_STYLE;

		return new OpenClosedTree (new OutlineNodeData (name, data, style));
	}

	//-------------------------------------------------------------

	/**
	 *  Generate a string containing the pertinent details of
	 *  an atom, in internal markup format.
	 */
	public static String getAtomInfo ( Atom atom )
	{
		// name
		String name = "<b><size 14>"
					  + atom.getID()
					  + "</size></b> ";

		// comments
		String comments = (String) atom.getField("__" + atom.getID());
		if ( comments == null )
			comments = "";
		else
			comments = "<p>" + comments;

		// source files
		String origin = (String) atom.getField("__");
		if ( origin == null )
			origin = "<colour magenta><i>(Internal)</i></colour><p>";
		else
			origin = "<colour magenta><i>" + origin + "</i></colour><p>";

		String sourceODL = (String) atom.getField("__odl__");
		if ( sourceODL == null )
			sourceODL = "<colour magenta><i>(Internal)</i></colour><p>";
		else
			sourceODL = "<colour magenta><i>" + sourceODL + "</i></colour><p>";

		// parents
		String parents = "<b>Parents:</b>";
		Enumeration enum = atom.getParents();
		while ( enum.hasMoreElements() )
		{
			Atom parent = (Atom) enum.nextElement();
			parents += " " + parent.getID();
			if ( enum.hasMoreElements() )
				parents += ",";
		}
		parents += "<p>";

		// children
		String children = "";
		if ( atom.hasChildren() )
		{
			children = "<b>Children:</b>";
			enum = atom.getChildren();
			while ( enum.hasMoreElements() )
			{
				Atom child = (Atom) enum.nextElement();
				children += " " + child.getID();
				if ( enum.hasMoreElements() )
					children += ",";
			}
			children += "<p>";
		}

		// nouns
		String nouns = "";
		Vector nounList = atom.getWorld().getVocabulary().getAtomNouns( atom );

		if ( nounList != null
			 && nounList.size() > 0 )
		{
			nouns = "<b>Nouns:</b>";
			enum = nounList.elements();
			while ( enum.hasMoreElements() )
			{
				nouns += " " + enum.nextElement().toString();
				if ( enum.hasMoreElements() )
					nouns += ",";
			}
			nouns += "<p>";
		}

		return name + origin + sourceODL + parents
				+ children + nouns + comments;
	}

	//-------------------------------------------------------------

	/**
	 *  Get the readable name of an atom. In most cases
	 *  this is just its ID. In the case of exit atoms,
	 *  rather than a name like <tt>exit601</tt>, the name
	 *  is given as <i>room.direction</i>, which should be
	 *  somewhat more legible. Other such naming conventions
	 *  may be added later.
	 */
	public static String getAtomName ( Atom atom )
	{
		if ( atom == null )
			return null;

		// check for exits
		Atom exitAtom = atom.getWorld().getAtom("exit");
		if ( exitAtom != null
			 && atom instanceof Thing
			 && atom.isDescendantOf ( exitAtom ) )
		{
			Atom room = atom.getContainer();
			if ( room == null )
				return atom.getID();

			return room.getID() + "." + atom.getField("direction");
		}

		return atom.getID();
	}

	//-------------------------------------------------------------
}