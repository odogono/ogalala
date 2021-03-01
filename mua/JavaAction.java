// $Id: JavaAction.java,v 1.12 1999/03/10 16:58:06 jim Exp $
// An Action implemented as a Java class
// James Fryer, 30 June 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;
import com.ogalala.util.Debug;

/** A JavaAction is an Action implemented in Java code. It is 
    loaded at run time.
*/
public abstract class JavaAction
    extends Action
    {
    /** All actions must have this variable defined, because they may be
        serialised. The convention is to set it to 1 in all actions.
    */
    private static final long serialVersionUID = 1;

    private static final String actionPackage = "com.ogalala.mua.action.";
    
    /** Load an action class file
        <p>
        <pre>
        ### Currently only the package "com.ogalala.mua.action" is searched
        ### for actions. I think a future version should provide functions
        ### to add and search other paths.
        </pre>
        @return a new Action class, or null if the action was not found
    */
    public static Action loadAction(String actionClassName)
        {
        // Assume failure
        JavaAction result = null;

        String fullClassName = actionPackage + actionClassName;
        
        try {
            // Get the action's class
            Class actionClass = Class.forName(fullClassName);

            // Create an instance of the class
            //### It would be nice to prevent more than one instance of each action type
            result = (JavaAction)actionClass.newInstance();
            }
            
        // All errors result in a null return
        catch (Exception e)
            {
            }
        
        return result;
        }
    }
