// $Id: SpeechCommands.java,v 1.5 1999/04/20 11:13:55 alex Exp $
// Commands for direct communication between players
// James Fryer, 2 Dec 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;

/** SAY covers both say and say_to
*/
public class Say
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    protected static final String NAME = "name";
    protected static final String SAY = "say";
    protected static final String FROM = "from";
    protected static final String MSG = "msg";
    protected static final String YOU = "you";

    Atom target = null;
    String msg;

    public boolean execute()
        {
        // Get the uppermost open container
        container = actor.getEnclosingContainer();
        
        // does the say have a target ??
        if ( !current.isRoot() )
            {
            target = current;
            }
        
        // get the message
        msg = event.getArg(0).toString();
        
        // output for the room
        OutPkt roomMsg = new OutPkt( SAY, FROM, actor.getString( NAME ) );
        roomMsg.addField( "msg", msg );

        // output for the actor
        OutPkt actMsg = new OutPkt( SAY, FROM, YOU );
        actMsg.addField( "msg", msg );

        // if there is a target mobile add the to field
        if ( target != null )
            {
            actMsg.addField( "to", target.getString( NAME ) );
            roomMsg.addField( "to", target.getString( NAME ) );
            }

        // send the packets
        container.output( roomMsg, actor );
        actor.output( actMsg );
        return true;
        }
    }

/** WHISPER
*/
public class Whisper
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Whisper has three different packets to send:
        //  The sender, S: "You whisper blah to T"
        //  The target, T: "S whispers blah to you"
        //  Other people: "S whispers something to T"

        // Get the args
        Mobile target = null;
		try {
        	target = (Mobile) event.getArg(0);
        	}
        catch ( ClassCastException e )
        	{
        	actor.output("You can't whisper to that!");
        	return true;
        	}

        String msg = event.getArg(1).toString();

        // Build the packets
        OutPkt senderOut = new OutPkt("whisper", "from", "you");
        senderOut.addField("to", target.getString(Say.NAME)); //### ???
        senderOut.addField("msg", msg);

        OutPkt targetOut = new OutPkt("whisper", "from", actor.getString(Say.NAME));
        targetOut.addField("msg", msg);

        OutPkt otherOut = new OutPkt("whisper", "from", actor.getString(Say.NAME));
        otherOut.addField("to", target.getString(Say.NAME));

		// to others
 		container.output( otherOut, target, actor );

 		// to target
 		target.output( targetOut );

		// to actor
		actor.output( senderOut );

        return true;
        }
    }

/** EMOTE
*/
public class Emote
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Get the uppermost open container
        container = actor.getEnclosingContainer();

        // Build the message
        String msg = "<b>" + actor.getString(Say.NAME) + "</b> " + event.getArg(0).toString();

        // Output to the room (except the actor)
        OutPkt output = new OutPkt("emote", "msg", msg);
        container.output(output, actor);
		
        // Confirm to the actor
        output = new OutPkt("emote", "msg", "You roleplay: " + msg);
		actor.output(output);

        return true;
        }
    }


public class Think extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		// Get the uppermost open container
        container = actor.getEnclosingContainer();

        // Build the message
        String msg = "<b>" + actor.getString(Say.NAME) + "</b> .oO( " + event.getArg(0).toString() + " )";

        // Output to the room (except the actor)
        OutPkt output = new OutPkt("emote", "msg", msg);
        container.output(output, actor);
		
        // Confirm to the actor
        output = new OutPkt("emote", "msg", "You think: " + msg);
		actor.output(output);

        return true;
     }
}
//*/

/** !SAY
*/
public class ModSay
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check/parse args
        if (event.getArgCount() < 1)
            throw new AtomException("!SAY <i>string</i>");
        String msg = event.getArg(0).toString();
            
        // Get the room container
        Atom room = actor.getEnclosingContainer();
        
        // Broadcast the message
        room.output(msg);

        return true;
        }
    }
    
/** !WHISPER
*/
public class ModWhisper
    extends JavaAction
    {
    private static final long serialVersionUID = 1;
    
    public boolean execute()
        {
        // Check/parse args
        if (event.getArgCount() < 2)
            throw new AtomException("!WHISPER <i>atomID</i> <i>string</i>");
        String atomID = event.getArg(0).toString();
        String msg = event.getArg(1).toString();
        
        // Get the target atom
        Atom target = world.getAtom(atomID);
        if (target == null)
            throw new AtomException("Atom not found: " + atomID);
            
        // Send the message
        target.output(msg);

        return true;
        }
    }
    
