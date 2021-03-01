// $Id: Look.java,v 1.30 1999/03/24 13:25:39 alex Exp $
// Core user commands
// Richard Morgan, 22 Oct 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;

/*

notes on the different behavour of the different container above

room
- list the actors container
- if open or closed list contents
- list things
- list mobiles
- list exits
- "the room is empty"

box / seats
- list current
- if open list its contents, if closed don't
- list things
- list mobiles
- "the name is empty"

inv
- list actor himself
- don't forward info about the container (mobile)
- if open or closed list contents
- list things
- list mobiles
- "you are carrying nothing"

*/

/** abstact look function
*/
public abstract class Look extends JavaAction
{
    static final String LIST_DESC = "list_desc";
    
    // the output pkt to be returned the actor
    protected OutPkt outpkt;
    
    // the thing / container we are looking at
    protected Atom target;

    public boolean execute()
    {
        initCurrent();
        createPkt();
        buildPkt();
        actor.output( outpkt );
        return true;
    }

    /** Override to initialise the 'current' pointer, if required
    */
    protected void initCurrent()
    {
    }

    /** overide to create the specific output pkt
    */
    protected abstract void createPkt();

    /** overide to fill the ouput pkt
    */
    protected abstract void buildPkt();

    /** Get an atom's name -- actually its 'list_desc' field.
        <p>
        If 'enclosingContainer' is not null, and is different from the atom's
        own container, the description will have the container's contents suffix 
        message (such as "in the box") appended to it.
    */
    protected final String getName(Atom atom, Atom enclosingContainer)
    {
        // The 'list_desc' field describes the object
        String result = atom.getString(LIST_DESC);
        result = AtomUtil.formatOutput( event, atom, result );

        // If the thing is not directly contained in the room, add the container's prefix string
        Atom container = atom.getContainer();
        if ( enclosingContainer != null && container != enclosingContainer )
            result = addContainerSuffix(result, container);

        return result;
    }
        
    /** Qualify an atom's name with its location
    */
    protected final String addContainerSuffix(String result, Atom container)
    {
        // Get the container's suffix message and expand it in the context of the container
        String containerMsg = container.getString( "contents_suffix_msg" );
        Event event = world.newEvent( actor, "dummy", container, null);
        containerMsg = AtomUtil.formatOutput( event, containerMsg );
        result = result + " " + containerMsg;
        return result;
    }
        
    /** Get an atom's 'list_desc' field without qualifying it with the container
    */
    protected final String getName(Atom atom)
        {
        return getName(atom, null);
        }
    }

/** LOOK at a thing, this is the most basic output pkt,
    all the other look commands require the id, name
    and description
*/
public class LookThing extends Look
{
    private static final long serialVersionUID = 1;
    
    /** static references to perminant strings, these need
        to be synchronized with the client.
    */
    static final String THING = "thing";
    static final String ID = "id";
    static final String NAME = "name";
    static final String DESCRIPTION = "description";

    protected void buildPkt()
    {
        // fill in the output pkt
        lookBasic();
    }

    /** create the output pkt
    */
    protected void createPkt()
    {
        outpkt = new OutPkt( THING );
    }

    /** Add the basic fields to the output pkt. Currently all
        look commands require this.
    */
    protected void lookBasic()
    {
        // add object id (this is for debug info)
        outpkt.addField( ID, current.getID() );

        // add the name of the object
        addNameField();
        
        // add the description of the object
        outpkt.addField( DESCRIPTION, current.getDescription() );
    }
    
    /** Add the name field to the output packet.
        <p>
        This is broken out so that LOOK in a room can redefine it to qualify the location.
    */
    protected void addNameField()
    {
        outpkt.addField( NAME, getName(current) );
    }
}

/** LOOK at a mobile. For now this is indentical to the
    LookThing class as both look contain the same infomation.
    Of course this may change
*/
public class LookMobile extends LookThing
{
    private static final long serialVersionUID = 1;
    
    /** static references to perminant strings, these need
        to be synchronized with the client.
    */
    static final String MOBILE = "mobile";

    /** create the output pkt
    */
    protected void createPkt()
    {
        outpkt = new OutPkt( MOBILE );
    }

    protected void buildPkt()
    {
        // call the basic packet construtor to add fields to the output packet
        super.buildPkt();
        // add the extra look mobile information (currently there is none)
    }
}

/** base class for looking at containers
*/
public abstract class LookContainerBase extends LookThing
{
    /** static references to perminant strings, these need
        to be synchronized with the client.
    */
    static final String MOBILES = "mobiles";
    static final String THINGS = "things";
    static final String EXITS = "exits";

    static final String IS_EMPTY_MSG = "is_empty_msg";
    static final String IS_CLOSED_MSG = "is_closed_msg";
    static final String IS_OPEN_MSG = "is_open_msg";
    static final String IS_QUIET = "is_quiet";
    static final String EXIT_ATOM_NAME = "exit";
    
    /** refrence to the exit atom so that we can identify 
        exit objects.
    */
    Atom exitAtom;
    
    /** stores the list of exits in the container
    */
    Vector exits = new Vector(10);

    /** stores the list of things in the container
    */
    Vector things = new Vector(10);

    /** stores the list of mobiles in the container
    */
    Vector mobiles = new Vector(10);

    /** This is the generic look function, this function
        retrieves the container we want to look at, checks to
        see if the container allows us to view its contents.
        It clears the internal lists of things, mobile and exits
        and then refills these vectors via the get contents function.
    */
    protected void buildPkt()
    {
        // empty the vectors
        things.removeAllElements();
        mobiles.removeAllElements();
        exits.removeAllElements();
        
        // can we list the contents for this type of container?
        if ( canListContents() )
        {
            // divide the contents into the correct lists
            getContents();

            // if the container is empty, and we're not talking about the room then...
            if ( isEmpty() && ! current.equals(actor.getContainer()) )
            {
                // add the container is empty msg
                outpkt.addField("msg", current.getString( IS_EMPTY_MSG ));
            }
        }
        else {
            // add the container is_closed msg
            outpkt.addField("msg", current.getString( IS_CLOSED_MSG ));
        }

        // build rest of the output packet
        buildContainerPkt();
    }

    /** override this function for the permisioning accessing the container
    */
    protected abstract boolean canListContents( );
    
    /** Get the appropriate container enumeration. Default is all visible things
        starting at the current container.
    */
    protected Enumeration newContainerEnumeration()
    {
        return new VisibleEnumeration( current, current, actor );
    }

    /** is the container empty?? (ignoring exits)
    */
    protected boolean isEmpty()
    {
        return mobiles.isEmpty() && things.isEmpty();
    }

    /** build specailed container pkt
    */
    //### Ideally this would not output exits except in a room, or mobiles in an inventory, etc. 
    protected void buildContainerPkt()
    {
        // deal with things
        addAtomEnumeration( THINGS, things.elements() );

        // deal with mobiles
        addAtomEnumeration( MOBILES, mobiles.elements() );

        // deal with exits
        addAtomEnumeration( EXITS, exits.elements() );
    }

    /** Given a container divide the contents into the correct lists, exits, things and mobiles
    */
    protected void getContents( )
        {
        // check we have a ref to the exitAtom
        if ( exitAtom == null )
        {
            exitAtom = world.getAtom( EXIT_ATOM_NAME );
        }
            
        Enumeration enum = newContainerEnumeration();
        while (enum.hasMoreElements())
        {
            // Get the atom
            Atom atom = (Atom)enum.nextElement();

            // Only process atoms which don't have the "quiet" property
            if ( !atom.getBool(IS_QUIET) )
            {
                // If it's a Mobile add it to the mobiles list
                if (atom instanceof Mobile)
                {
                	if( !((Mobile)atom).equals(actor) )
                    	mobiles.addElement( atom );
				}
                // If it's a Thing, add it to the thing or exit list
                else 
                {
                    // if it is an exit add it to the exits list
                    if ( atom.isDescendantOf( exitAtom ) )
                        exits.addElement( atom );
                        
                    // Else it's a thing
                    else
                        things.addElement( atom );
                }
            }
        }
    }

    /** add a item list to the pkt from an enumeration
    */
    protected void addAtomEnumeration( String listName, Enumeration toList )
    {
        // add list to the outPkt
        outpkt.openList( listName );

        // get enumeration of the mobiles
        while (toList.hasMoreElements())
        {
            // get a atom
            Atom atom = (Atom)toList.nextElement();

            // format and output description
            String name = getName(atom, current);
            outpkt.addListItem(name);
        }

        // close the list
        outpkt.closeList();
    }
}


/** LOOK at a container for instance a box, drawer, seat etc ...
<ul>
<li>list the contents of the current atom
<li>if open list its contents, if closed don't
<li>list things
<li>list mobiles
<li>"the name is empty"
</ul>
*/
public class LookContainer extends LookContainerBase
{
    private static final long serialVersionUID = 1;
    
    static final String CONTAINER = "container";

    /** This the key to the field that decides if the 
        container is open or closed
    */
    static String IS_CLOSED = "is_closed";

    /** create the output pkt
    */
    protected void createPkt()
    {
        outpkt = new OutPkt( CONTAINER );
    }

    /** if the container is open return true
        if not return false.
    */
    protected boolean canListContents()
    {
        return !current.getBool( IS_CLOSED );
    }

    /** specialed buildPtk
    */
    protected void buildPkt()
    {
        // build the basic packet describing the container itself
        lookBasic();

        // build the lists
        super.buildPkt();
    }
}

/** This is a specialation of the Look Container, rooms have exit lists and
    do not display viewer in the list of mobiles<p>
<ul>room
<li>list the actors container
<li>if open or closed list contents
<li>list things
<li>list mobiles
<li>list exits
<li>"the room is empty"
</ul>
*/
public class LookRoom extends LookContainerBase
{
    private static final long serialVersionUID = 1;
    
    /** static references to perminant strings, these need
        to be synchronized with the client.
    */
    static final String ROOM = "room";
    static final String PLACE = "place";
    static final String URL = "url";
    static final String XPOS = "xpos";
    static final String YPOS = "ypos";

    /** create the output pkt
    */
    protected void createPkt()
    {
        outpkt = new OutPkt( ROOM );
    }
    
    /** Add "place" field
    */
    protected void lookBasic()
    {
        super.lookBasic();
        
        // The 'place' is the enclosing container's container.
        Atom place = current.getContainer();
        outpkt.addField( PLACE, getName(place) );
        
        // Add URL and position fields. If these are not defined in the
        //  room, look in the place.
        String url = current.getString(URL);
        if (url == null)
            url = place.getString(URL);
        if (url != null)
            outpkt.addField(URL, url);
        
        int xpos = current.getInt(XPOS);
        int ypos = current.getInt(YPOS);
        if (xpos >= 0 && ypos >= 0)
        {
            outpkt.addField(XPOS, Integer.toString(xpos));
            outpkt.addField(YPOS, Integer.toString(ypos));
        }
    }

    protected boolean canListContents()
    {
        return true;
    }

    protected void initCurrent()
    {
        // LOOK is called with the root as current, change this to the enclosing container of the actor
        current = actor.getEnclosingContainer();
    }

    protected void buildPkt()
    {
        lookBasic();
        super.buildPkt();
    }
    
    /** If the actor is in a sub-container off the enclosing container (current),  
        qualify the name field with the local container's suffix message.
    */
    protected void addNameField()
    {
        String s = getName(current);
        //###Atom localContainer = actor.getContainer();
        if (container != current)
            s = addContainerSuffix(s, container);
        outpkt.addField( NAME,  s);
    }

    /** Enumerate all visible things, starting at the room, see non-obvious
        things in the local container only.
    */
    protected Enumeration newContainerEnumeration()
    {
        return new VisibleEnumeration( current, container, actor );
    }
}

/** INVENTORY
<ul>
<li>list the contents of the actor<br>
<li>don't forward info about the container (mobile)<br>
<li>if open or closed list contents<br>
<li>list things<br>
<li>list mobiles<br>
<li>"you are carrying nothing"
</ul>
*/
public class LookInventory extends LookContainerBase
{
    private static final long serialVersionUID = 1;
    
    /** static references to perminant strings, these need
        to be synchronized with the client.
    */
    static final String INV = "inv";

    /** create the output pkt
    */
    protected void createPkt()
    {
        // create the invetory packet
        outpkt = new OutPkt( INV );
    }

    /** you can always look at your inventory
    */
    protected boolean canListContents()
    {
        return true;
    }

    protected void initCurrent()
    {
        // the container to list is the actor
        current = actor;
    }
    
    /** Get the appropriate container enumeration. Default is all visible things
        starting at the current container.
    */
    protected Enumeration newContainerEnumeration()
    {
        //return new VisibleEnumeration( current, actor );
        return new ContentsEnumeration( actor );
    }
}
    
/** LOOK through an exit
*/
public class LookExit extends LookThing
{
    private static final long serialVersionUID = 1;
    
    /** static references to perminant strings, these need
        to be synchronized with the client.
    */
    static final String EXIT = "exit";
    static final String DIRECTION = "direction";
    static final String DESTINATION = "destination";
    static final String IS_CLOSED_MSG = "is_closed_msg";
    static final String MSG = "msg";
    
    protected void buildPkt()
    {
        super.buildPkt();
        lookExit();
    }

    /** create the output pkt
    */
    protected void createPkt()
    {
        outpkt = new OutPkt( EXIT );
    }

    /** Add exit-specific fields
        <p>
        Exit fields are 'direction' and 'destination'.
    */
    protected void lookExit()
    {
        outpkt.addField(DIRECTION, current.getString(DIRECTION));
        
        // If the exit is closed, say so.
        if (current.isClosed())
            outpkt.addField(MSG, current.getString(IS_CLOSED_MSG));
        
        // Else describe what's there
        else 
        {
            Atom destination = current.getAtom(DESTINATION);
            outpkt.addField(DESTINATION, getName(destination));
        }
    }
}
