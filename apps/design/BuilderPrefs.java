// $Id: BuilderPrefs.java,v 1.1 1999/03/12 15:28:21 matt Exp $
// Preferences manager for the building tools
// Matthew Caldwell, 12 March 1999
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import com.ogalala.widgets.*;
import com.ogalala.util.SymbolParser;

/**
 *  An object to hold preferences data for the AtomBuilder
 *  application, and handle reading them from and writing
 *  them to a file.
 */
public class BuilderPrefs
{
	//---------------------------------------------------------------
	//  class variables
	//---------------------------------------------------------------
	
	private static final String DEFAULT_USER = "<default user>";
	
	//---------------------------------------------------------------
	//  instance variables
	//---------------------------------------------------------------

	/** The name of the user. */
	public String user = DEFAULT_USER;
	
	/** The bounds of the main window. */
	public Rectangle mainBounds = null;
	
	/** The bounds of the atom inspector window. */
	public Rectangle atomBounds = null;
	
	/** The bounds of the exit inspector window. */
	public Rectangle exitBounds = null;
	
	/** The bounds of the open file dialog. */
	public Rectangle openFileBounds = null;
	
	/** The bounds of the create file window. */
	public Rectangle createFileBounds = null;
	
	/** The bounds of the folder selection window. */
	public Rectangle folderBounds = null;
	
	/** The bounds of the new atom dialog. */
	public Rectangle newAtomBounds = null;
	
	/** The bounds of the new exit dialog. */
	public Rectangle newExitBounds = null;
	
	/** Whether the atom inspector window is visible. */
	public boolean atomVisible = false;
	
	/** Whether the chat messages window is visible. */
	public boolean exitVisible = false;
		
	//---------------------------------------------------------------
	//  construction
	//---------------------------------------------------------------
	
	/** Default constructor. */
	public BuilderPrefs () {}
	
	//---------------------------------------------------------------
	//  i/o
	//---------------------------------------------------------------

	/**
	 *  Write the preferences to a specified file.
	 */
	public void write ( String fileName )
		throws IOException
	{
		FileWriter file = new FileWriter ( fileName );
		
		file.write ( "user=\"" + user + "\"\r\n" );
		
		file.write ( "mainBounds=" + rectToString(mainBounds) + "\r\n" );
		file.write ( "atomBounds=" + rectToString(atomBounds) + "\r\n" );
		file.write ( "exitBounds=" + rectToString(exitBounds) + "\r\n" );
		file.write ( "openFileBounds=" + rectToString(openFileBounds) + "\r\n" );
		file.write ( "createFileBounds=" + rectToString(createFileBounds) + "\r\n" );
		file.write ( "folderBounds=" + rectToString(folderBounds) + "\r\n" );
		file.write ( "newAtomBounds=" + rectToString(newAtomBounds) + "\r\n" );
		file.write ( "newExitBounds=" + rectToString(newExitBounds) + "\r\n" );
		
		file.write ( "atomVisible=" + atomVisible + "\r\n" );
		file.write ( "exitVisible=" + exitVisible + "\r\n" );
		
		file.flush();
		file.close();
	}

	//---------------------------------------------------------------

	/**
	 *  Read the preferences from a specified file.
	 */
	public void read ( String fileName )
		throws IOException
	{
		InputStream file = new FileInputStream ( fileName );
		SymbolParser parser = new SymbolParser ( file );
		Hashtable results = new Hashtable ();
		
		parser.parseStream ( results );
		
		user = (String) results.get ( "user" );
		if ( user == null )
			user = DEFAULT_USER;
			
		atomVisible = "true".equals( results.get("atomVisible") );
		exitVisible = "true".equals( results.get("exitVisible") );
		
		mainBounds = rectify ( results.get("mainBounds") );
		atomBounds = rectify ( results.get("atomBounds") );
		exitBounds = rectify ( results.get("exitBounds") );
		openFileBounds = rectify ( results.get("openFileBounds") );
		createFileBounds = rectify ( results.get("createFileBounds") );
		folderBounds = rectify ( results.get("folderBounds") );
		newAtomBounds = rectify ( results.get("newAtomBounds") );
		newExitBounds = rectify ( results.get("newExitBounds") );
		
		file.close();
	}
	
	//---------------------------------------------------------------
	//  utilities
	//---------------------------------------------------------------

	/**
	 *  Convert an object read in from a preferences
	 *  file into a Rectangle. If the object is a Vector
	 *  of four strings, each of which is a valid string
	 *  representation of an integer (as produced by
	 *  <tt>rectToString()</tt>) the result is a proper
	 *  rectangle. Otherwise, <tt>null</tt> is returned.
	 */
	public Rectangle rectify ( Object obj )
	{
		if ( obj == null )
			return null;
		else
		{
			try
			{
				Vector list = (Vector) obj;
				return new Rectangle ( Integer.parseInt( (String) list.elementAt(0) ),
									   Integer.parseInt( (String) list.elementAt(1) ),
									   Integer.parseInt( (String) list.elementAt(2) ),
									   Integer.parseInt( (String) list.elementAt(3) ) );
			}
			catch ( RuntimeException e )
			{
				return null;
			}
		}
	}
	
	//---------------------------------------------------------------

	/**
	 *  Convert a Rectangle into a string of the form
	 *  "[x y width height]", suitable for writing to a
	 *  prefs file and converting back into a Rectangle with
	 *  <tt>rectify()</tt>.
	 */
	public String rectToString ( Rectangle rect )
	{
		if ( rect == null )
			return "";
		else
			return "[" + rect.x + " " + rect.y + " "
			       + rect.width + " " + rect.height + "]";
	}
	
	//---------------------------------------------------------------	
}