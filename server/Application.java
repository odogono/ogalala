// $Id: Application.java,v 1.10 1999/09/01 15:05:32 jim Exp $
// Server application interface
// James Fryer, 28 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.util.Enumeration;

/** An abstract application that runs under the server.
    <p>
    An application has an ID which can map to a file name. This is used to access
    an application by users who want to run it.
    <p>
    The application has an interface similar to a file: 'create', 'open', 
    'close', 'delete', 'exists'. Creating or opening an application results
    in a running application which can then be added to the server's list.
    <p>
    The 'newChannel' function is used to connect an application to a user session.
    A channel also has an ID: this can represent a user's persistent state in 
    an app (e.g. MUD characters) or simply be the user's ID (e.g. chat systems).      
    <p>
    For apps which have no persistent state, the 'open' and 'create' functions 
    will be identical, and the 'delete' function will do nothing. For apps
    which have state these functions become important.
    <p>
    'Create' and 'open' take an enumeration of string arguments. The meaning 
    of these is defined by the application. Null is an acceptable value if there
    are no arguments.
*/
public interface Application
    {
    /** Create a new application. Implementations should avoid overwriting 
        existing apps with the same ID.
    */
    public void create(String appID, Enumeration args)
        throws ApplicationOpenException;

    /** Open an existing application
    */
    public void open(String appID, Enumeration args)        
        throws ApplicationOpenException;

    /** Close this application. Preserve state, if appropriate.
    */
    public void close();

    /** Remove an application's persistent state. 
        (USE WITH CARE!)
    */
    public void delete(String appID);

    /** Does an application called 'appID' exist?
    */
    public boolean exists(String appID);

    /** Get the ID of this application
    */
    public String getID();

    /** Get some information about this application
    */
    public String getInfo();
    
    /** Create a channel for this application
    */
    public Channel newChannel();
    }

