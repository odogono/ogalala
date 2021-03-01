// $Id: LValue.java,v 1.7 1998/09/24 09:23:26 jim Exp $
// Parse LValues for atom commands
// James Fryer, 22 July 98 (pi day)
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

/** This object parses strings of the form:
    <pre>
    fieldID
    atomID.fieldID
    </pre>
    A default atom is assumed if the atom ID is absent
*/
public final class LValue
    {
    /** The event context (can be null)
    */
    private Event event;

    /** The default atom
    */
    private Atom defaultAtom;

    /** The current atom
    */
    private Atom atom;
    
    /** The current atom ID
    */
    private String atomID;

    /** The current field ID
    */
    private String fieldID;

    /** Constructor
    */
    public LValue(Atom defaultAtom)
        {
        this (defaultAtom, null);
        }
    
    /** Constructor
    */
    public LValue(Atom defaultAtom, Event event)
        {
        if (defaultAtom == null)
            throw new NullPointerException("LValue: Null default atom");
        this.defaultAtom = defaultAtom;
        this.event = event;
        }
    
    /** Parse a string
    */
    public void parse(String s)
        throws AtomException
        {
        if (s == null)
            throw new NullPointerException("LValue: Null parse string");
        s = s.trim();
        if (s.length() == 0)
            throw new IllegalArgumentException("LValue: Empty parse string");
            
        // Assume no atom is specified
        atom = defaultAtom;
        atomID = atom.getID();
        fieldID = null;
        
        // Look for the separating dot
        int dotPos = s.indexOf('.');
        
        // If no dot is found, the entire string is the field name
        if (dotPos < 0)
            fieldID = s;
            
        // Else, split the string at the dot and resolve the atom
        else {
            atomID = s.substring(0, dotPos);
            fieldID = s.substring(dotPos + 1);
            atom = getAtom(atomID);
            if (atom == null)
                throw new AtomException("LValue: Atom not found: " + atomID);
            }
        }
        
    /** Get an atom given its ID or event context definition
    */
    private Atom getAtom(String atomID)
        {
        // First check for the "special" IDs defined in the event context
        if (event != null)
            {
            if ("current".equalsIgnoreCase(atomID))
                return event.getCurrent();
            else if ("actor".equalsIgnoreCase(atomID) || "me".equalsIgnoreCase(atomID))
                return event.getActor();
            else if ("container".equalsIgnoreCase(atomID) || "here".equalsIgnoreCase(atomID))
                return event.getContainer();
            else if ("arg".equalsIgnoreCase(atomID))
                return AtomData.toAtom(event.getArg(0));
            }
            
        // If we get here then 'atomID' is the plain old ID of an atom from the database
        World world = defaultAtom.getWorld();
        return world.getAtom(atomID);
        }
        
    /** Get the default atom
    */
    public Atom getDefaultAtom()
        {
        return defaultAtom;
        }
        
    /** Get the current atom
    */
    public Atom getAtom()
        {
        return atom;
        }
        
    /** Get the current atom's ID
    */
    public String getAtomID()
        {
        return atomID;
        }
        
    /** Get the current field ID
    */
    public String getFieldID()
        {
        return fieldID;
        }
    }

