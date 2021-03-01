// $Id: Noun.java,v 1.3 1998/05/02 14:48:31 jim Exp $
// Noun class for Death on the Nile
// James Fryer, 27 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.database;

/** The Noun class represents the properties common to all physical objects
    in the game.
*/
public class Noun
    implements java.io.Serializable
    {
// Implementation features

    // The noun's ID, unique in the database
    private String id;

    // The creator of the noun. This is a user name, not a character name.
    private String creatorUserName;

    // Back-pointer to the database
    transient Database database;

    // The container this noun is in
    //### AbstractContainer container;


// Game features

    // The name of the noun
    protected String name;

    // The verbose description of the noun
    protected String description;


// Constructors

    /** Default ctor, for use when reading from file or by factory
    */
    protected Noun()
        { }

    /** Ctor, for use when creating nouns in program
    */
    public Noun(String id, String creator)
        {
        this.id = id;
        this.creatorUserName = creator;
        }

// Initialisation protocol

    /** Called after the noun is first created
    */
    public void init()
        {
        }

    /** Called before the noun is destroyed
    */
    public void destroy()
        {
        }

    /** Called when the game is started
    */
    public void start()
        {
        }

    /** Called when the game is paused
    */
    public void stop()
        {
        }

// Access functions

    /** Get the noun ID
    */
    public String getID()
        { return id; }

    /** Set the noun ID (used by factory only)
    */
    void setID(String id)
        { this.id = id; }

    /** Get the name of the user who created the noun
    */
    public String getCreator()
        { return creatorUserName; }

    /** Set the name of the user who created the noun
    */
    public void setCreator(String creatorUserName)
        { this.creatorUserName = creatorUserName; }

    /** Get the noun name
    */
    public String getName()
        { return name; }

    /** Set the noun name
    */
    public void setName(String name)
        { this.name = name; }

    /** Test if the 'name' arg matches this noun
    */
    public boolean equalsName(String name)
        {
        //### Need a more flexible algorithm...
        return this.name.equalsIgnoreCase(name);
        }

    /** Get the noun description
    */
    public String getDescription()
        { return description; }

    /** Set the noun description
    */
    public void setDescription(String description)
        { this.description = description; }

    public String toString()
        { return new String(getID() + "(" + getName() + ")"); }

    }
