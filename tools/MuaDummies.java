// $Id: MuaDummies.java,v 1.9 1999/03/11 10:41:10 alex Exp $
// Ersatz MUA classes to abstract the atom database from the game.
// Matthew Caldwell, 8 October 1998
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import java.io.*;
import com.ogalala.util.*;

//-----------------------------------------------------------------------
//  the World class is a wrapper for the the atom database
//-----------------------------------------------------------------------

/**
 *  This dummy World class masquerades as the real game world
 *  when using the atom database inside the tools. Maybe.
 *  It's a simple wrapper around AtomDatabase, with functions
 *  for opening and closing a database.
 */
class World
{
	protected AtomDatabase database;

	/**
	 *  Create a dummy world from the given file.
	 *  If the file already exists, load it; otherwise,
	 *  create the database anew.
	 */
	public World ( String fileName )
		throws IOException
	{
		if ( AtomDatabase.exists ( fileName ) )
			open ( fileName );
		else
			create ( fileName );
	}

	/**
	 *  Open an existing database.
	 */
	public void open (String fileName)
		throws IOException
	{
		database = new AtomDatabase ( this );
		database.open ( fileName );
	}

	/**
	 *  Create a new database.
	 */
	public void create ( String fileName )
		throws IOException
	{
		database = new AtomDatabase ( this );
		database.create ( fileName );
	}

	/**
	 *  Close the dummy world.
	 */
	public void close ()
		throws IOException
	{
		database.close();
	}

	public Atom getRoot () { return database.getRoot(); }
	public AtomDatabase getAtomDatabase () { return database; }
	public Atom getAtom ( String id ) { return database.getAtom(id); }
	public Container getLimbo () { return database.getLimbo(); }
	public Event getCurrentEvent () { return null; }
	public Event newEvent ( Atom actor, String id, Atom current ) { return null; }
    public void pushEvent ( Event e ) { }
    public void popEvent() { }

    /**
     *  Create a new Thing, Container, etc. depending on the type of the parent
     *  <p>
     *  This implies that the parent must be of the highest static type the
     *  Thing needs to be. I.e. if you want a Java Container, 'parent' must be at
     *  least a container atom.
     */
    public final Atom newThing(String id, Atom parent)
    {
        // Determine the static type to create
        String className = AtomDatabase.ATOM_ID;
        if (parent.isDescendantOf(getAtom(AtomDatabase.MOBILE_ID)))
            className = AtomDatabase.MOBILE_ID;
        else if (parent.isDescendantOf(getAtom(AtomDatabase.ROOM_ID)))
            className = AtomDatabase.ROOM_ID;
        else if (parent.isDescendantOf(getAtom(AtomDatabase.CONTAINER_ID)))
            className = AtomDatabase.CONTAINER_ID;
        else
            className = AtomDatabase.THING_ID;

        return database.newAtom(id, parent, className);
    }

}


//-----------------------------------------------------------------------
//  the remaining classes are REAL dummies, provided solely to
//  satisfy the dependencies of the various atom database classes
//-----------------------------------------------------------------------

class Action implements Serializable
{
	protected String name;
	public Action ( String name ) { this.name = name; }
	public String toString() { return name; }
	public void execute ( Event e ) {}
}

//-----------------------------------------------------------------------

class JavaAction
{
	public static Action loadAction ( String name ) { return new Action ( name ); }
}

//-----------------------------------------------------------------------

class OutPkt
{
	public OutPkt ( String x, String y, String z ) {}
	public static String quoteString ( String x ) { return x; }
}

//-----------------------------------------------------------------------

class Watcher
{
	public void output ( String x, Event y ) {}
	public boolean isLead () { return true; }
	public void setNext ( Watcher x ) {}
	public Watcher getNext () { return null; }
	public void onAdd () {}
	public void onRemove () {}
	public Atom getWatchedAtom () { return null; }
}

//-----------------------------------------------------------------------

class Event
{
	public Object getResult () { return null; }
	public String formatOutput ( String x ) { return x; }
	public Atom getCurrent () { return null; }
	public Atom getActor () { return null; }
}

//-----------------------------------------------------------------------

class LValue
{
	public LValue ( Atom x, Event y ) {}
	public void parse ( String x ) {}
	public Atom getAtom () { return null; }
	public String getFieldID () { return null; }
}

//-----------------------------------------------------------------------
