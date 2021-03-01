// $Id: SenseCommands.java,v 1.4 1999/03/10 16:58:06 jim Exp $
// This file covers the senses feel, taste and smell
// Richard Morgan
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;

public abstract class SenseAction 
    extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        // check for sillyness
        if (current == actor)
        {
            actor.output(getSillyMsg());
            return true;
        }
        
        // Output to actor and room
        actor.output(getMsg() + " " + getDesc());
        container.output(getOmsg(), actor);

        return true;
    }
    
    /** Get the message to display to the actor if they try to sense themselves...
    */
    protected String getSillyMsg()
    { 
        //### Temporary implementation until I think of something better.
        return "You can't do that!"; 
    }
    
    /** Get the message to display to the actor, e.g. "You smell the cat."
    */
    protected abstract String getMsg();
    
    /** Get the message to send to the room, e.g. "Rich smells the cat."
    */
    protected abstract String getOmsg();

    /** Get the message describing the result, e.g. "It smells like a cat."
    */
    protected abstract String getDesc();
}

public class Taste 
    extends SenseAction
{
    private static final long serialVersionUID = 1;
    
    protected String getMsg()
    {
        return current.getString("taste_msg");
    }
    
    protected String getOmsg()
    {
        return current.getString("taste_omsg");
    }

    protected String getDesc()
    {
        return current.getString("taste_desc");
    }
}
    
public class Smell
    extends SenseAction
{
    private static final long serialVersionUID = 1;
    
    protected String getMsg()
    {
        return current.getString("smell_msg");
    }
    
    protected String getOmsg()
    {
        return current.getString("smell_omsg");
    }

    protected String getDesc()
    {
        return current.getString("smell_desc");
    }
}
    
    
public class Touch
    extends SenseAction
{
    private static final long serialVersionUID = 1;
    
    protected String getMsg()
    {
        return current.getString("touch_msg");
    }
    
    protected String getOmsg()
    {
        return current.getString("touch_omsg");
    }

    protected String getDesc()
    {
        return current.getString("touch_desc");
    }
}
