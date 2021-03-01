// $Id: ConnectionList.java,v 1.10 1999/04/29 15:57:30 jim Exp $
// A list of Connections
// Richard Morgan/James Fryer, 11 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.util.*;

/** This class keeps a list of all the connections to the server.<p>

    Add/remove functions in this class are called only by the Connection class,
    which manages its own adding and removal from the list.<p>

    Access is provided for applications which use this list rather than maintaining
    their own list of active users.<p>
*/

public class ConnectionList
	{
    protected Dictionary connections = new Hashtable();

    /** Add a new connection to the list. Called only by Connection creator
    */
    void add(Connection connection)
    	{
    	String userName = connection.getUserId();
        if (userName == null)
        	{
            // ### trap assertion error - a valid unique username is required for the lookup in
            // the hash table
            System.err.println("null name from login cannot add to hash table");
            return;
	        }
    	synchronized (this)
    	    {
            // Add the connection to the internal vector
            connections.put(userName,connection);
            }
	    }

    /** Remove a connection from the list. Called only by 'Connection.disconnect'
    */
    void remove(Connection connection)
    	{
    	synchronized ( this )
    	    {
            // remove from the list
            connections.remove( connection.getUserId() );
            }
	    }

    /** Find a named connection in the list. Return the connection, or null if not found.
    */
    public Connection find( String userName )
        {
        synchronized (this)
            {
            return (Connection)connections.get( userName );
            }
        }

	public void removeAll()
		{
		Enumeration enum = connections.keys();

		Vector v = new Vector ();
		
		while ( enum.hasMoreElements() ) 
			{
			v.addElement( enum.nextElement().toString() );			
			}
		
		enum = v.elements();
		
		while ( enum.hasMoreElements() )
			{
			Connection con = (Connection)connections.get( enum.nextElement().toString() );
            // shutdown the connection
			con.stop();
			}
		}

	/** Get an enumeration of the elements in the list
	    Note: the enumeration elements will need to be cast to Connections.
	*/
	public final synchronized Enumeration elements()
	    { 
	    return connections.elements(); 
	    }

	/** How many connections?
	*/
	public final synchronized int size()
	    { 
	    return connections.size(); 
	    }
	}




