// $Id: ApplicationFactory.java,v 1.5 1998/08/06 18:34:53 jim Exp $
// Server class
// James Fryer, 2 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import java.util.*;

/** Create an Application by loading its class then creating an instance
    of its class.
    <p>
    The app thus created is not open or attached to any app data. 'Create' 
    or 'open' must be called on the object returned from this function.
*/
class ApplicationFactory
	{
	/** This is the package where apps are kept.
	    ### This should probably be settable, perhaps in a config file, 
	    ###   perhaps a list of packages should be searched.
	*/
	private static final String packageName = "com.ogalala.server.apps";
	
	/** Create an application object
	    @return a new, inactive application object or null if the app was not found or an error occurred.
	*/
    static Application newApplication(String appClassName)
        {
        // Make the full class name
        String fullClassName = packageName + "." + appClassName;

        try {
            // Get the class for the type of noun to be created
            Class appClass = Class.forName(fullClassName);
            
            // Create an instance of the class
            Application result = (Application)appClass.newInstance();
            
            return result;
            }
            
        // All errors, just return null.
        catch (Exception e)
            {
            return null;
            }
        }
        
	/** Prevent instantiation of this class
	*/
	private ApplicationFactory()
	    {
	    }
	}
