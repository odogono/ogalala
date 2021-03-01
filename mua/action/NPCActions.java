// $Id: NPCActions.java,v 1.20 1999/05/05 15:16:11 matt Exp $
// Generic NPC actions
// Matthew Caldwell, 12 April 1999
// Copyright (C) HotGen Studios Ltd <www.hotgen.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;
import com.ogalala.nile.*;

/**
 *  NPCBase is the base class for NPC actions that use
 *  "scripts" (ie, tables of commands, not to be confused
 *  with server scripts). It can be used by itself to
 *  execute scripts provided to the Event in its argument
 *  list.
 *  <p>
 *  The standard script control structures from the MuaScript
 *  class are used here, with this class handling the
 *  additional operator "ALL" and doing the actual execution
 *  of script commands.
 */
public abstract class NPCBase
	extends JavaAction
{
	//--------------------------------------------------------------
	//  class variables
	//--------------------------------------------------------------
	
	private static final long serialVersionUID = 1;
	
	// property names used by the NPC classes
	public static final String DO_WALK = "on_do_walk";
	public static final String WALK_DISABLED = "walk_disabled";
	public static final String WALK_SCRIPTS = "walk_scripts";
	public static final String WALK_INTERVAL = "walk_interval";
	public static final String LAST_PLACE = "last_place";
	
	public static final String DO_CLOCK = "on_do_clock";
	public static final String CLOCK_DISABLED = "clock_disabled";
	public static final String CLOCK_SCRIPTS = "clock_scripts";
	
	// note that the RESPONSE_SCRIPTS table contains redirections to
	// other script tables, not the scripts themselves -- by default
	// all talk scripts are kept in the property "talk_scripts" (matching
	// the convention for walk_scripts and clock_scripts), but this
	// can be redefined via RESPONSE_SCRIPTS
	public static final String RESPONSE_SCRIPTS = "response_scripts";
	public static final String TALK_DISABLED = "talk_disabled";
	public static final String DO_TALK = "on_do_talk";
	public static final String WILL_OVERHEAR = "will_overhear";
	
	public static final String MISC = "misc";
	public static final String LOOSE_TALK = "loose_talk";
	public static final String DIRECT_TALK = "direct_talk";
	public static final String ROOM_UPDATE = "room_update";
	
	public static final String SELF_EMOTE_PREFIX = "You roleplay:";
	
	// special keywords
	public static final String END = "end";
	public static final String NOP = "nop";
	public static final String ALL = "all";
	
	public static final String UNKNOWN = "unknown";
	
	//--------------------------------------------------------------
	//  instance variables
	//--------------------------------------------------------------
	
	/**
	 *  Flag set if an "END" command was reached. "END"
	 *  stops all further processing, both internally and
	 *  for descendant actions that use timer events.
	 */
	protected boolean wasTerminated = false;

	//--------------------------------------------------------------
	//  utility methods
	//--------------------------------------------------------------
	
	/**
	 *  Execute a "script" that has been found in an NPC
	 *  stimulus-response table. In most cases, the control
	 *  structures here are those used in <tt>MuaScript.select()</tt>,
	 *  with the addition of the operator "ALL", which executes
	 *  all of the commands in a script. The actual command
	 *  execution is performed by the <tt>doScriptCommand()</tt>
	 *  method.
	 */
	protected final void doScript ( Object script )
	{
		Debug.println ( "NPCBase.doScript(): executing " + script );
		
		try
		{
			if ( script == null )
				return;
			
			// vectors need interpretation
			if ( script instanceof Vector )
			{
				Vector list = (Vector) script;
				
				if ( list.size() == 0 )
					return;
				
				// intercept the special operator "ALL"
				Object opcode = list.elementAt(0);
				
				if ( opcode instanceof String
					 && ((String) opcode).equalsIgnoreCase(ALL) )
				{
					// execute each contained script in turn
					Enumeration enum = list.elements();
					
					// skip the operator
					enum.nextElement();
					
					while ( enum.hasMoreElements() )
						doScript ( enum.nextElement() );
				}
				// all other operators are handled by
				// the MuaScript class
				else
					doScript ( MuaScript.select ( script, event ) );
			}
			// anything else must be a command
			else
				doScriptCommand ( script );
		}
		catch ( RuntimeException e )
		{
			Debug.println ( "NPCBase.doScript(): " + e );
		}

	}

	//--------------------------------------------------------------

	/**
	 *  Execute a script command from an NPC script table.
	 *  The command may be either a String, in which case
	 *  substitution is performed and the result sent to
	 *  the parser as a command, or an Action, in which case
	 *  the Action is executed within the context of the
	 *  current Event. Commands of any other class are
	 *  simply ignored.
	 */
	protected void doScriptCommand ( Object command )
	{
		Debug.println ( "NPCBase.doScriptCommand(): executing " + command );
		
		if ( command == null )
			return;
		
		if ( command instanceof String )
		{
			OutputFormatter formatter = new OutputFormatter ( event, current );
			String commandStr = formatter.format ((String) command);
			
			// check for special keywords END and NOP,
			// otherwise send to the parser for execution
			// -- note that the CURRENT object (ie, the NPC)
			// becomes the ACTOR
			if ( commandStr.equalsIgnoreCase(END) )
				wasTerminated = true;
			else if ( ! commandStr.equalsIgnoreCase(NOP) )
				world.parseCommand ( commandStr, current );
		}
		else if ( command instanceof Action )
		{
			// at present, the return value of the action
			// (if any) is ignored -- this may change!
			((Action) command).execute ( event );
		}
	}

	//--------------------------------------------------------------
	
	/**
	 *  Execute any scripts passed in the Event's argument
	 *  list.
	 */
	public boolean execute ()
	{
		for ( int i = 0; i < event.getArgCount(); i++ )
		{
			if ( wasTerminated )
				break;
			else
				doScript ( event.getArg( i ) );
		}
		
		return true;
	}
	
	//--------------------------------------------------------------
}

//------------------------------------------------------------------

/**
 *  Action called when Talker NPCs are started. Creates a Watcher
 *  and adds it to the NPC. Note that this does not reset the
 *  <b>talk_disabled</b> property, since that needs to be
 *  preserved across database saves and reloads. It is assumed
 *  that if <b>talk_disabled</b> has been set there must have been
 *  a reason for doing so, and it must be reset explicitly.
 */
public class StartTalker
	extends NPCBase
{
	public boolean execute ()
	{
		// add an NPC watcher to the NPC
		try
		{
			Debug.println ( "StartTalker.execute(): starting " + current.getID() );
			
			current.addWatcher ( new NPCWatcher ( current,
												  false,
												  DO_TALK ) );
			return pass();
		}
		catch ( RuntimeException e )
		{
			Debug.println ( "StartTalker.execute(): " + e );
			return true;
		}
	}
}

//------------------------------------------------------------------

/**
 *  Action called by the NPC watcher in response to received
 *  messages. The message text is passed in the event's first
 *  argument. This text is matched against the fragments
 *  in the appropriate script table, and if a match is found
 *  the resulting script is executed.
 */
public class DoTalk
	extends NPCBase
{
	//--------------------------------------------------------------
	//  instance variables
	//--------------------------------------------------------------

	/** The full packet text. */
	private String msg = null;
	
	/** Jump table for the different packet types. */
	private Dictionary lookup = null;
	
	/** The parsed packet. */
	private Dictionary details = null;
	
	/** The packet type. */
	private String type = null;
	
	/** The message type. */
	private String messageType = null;
	
	/** The script table for this packet. */
	private Dictionary scriptTable = null;
	
	/** The script to be executed. */
	private Object script = null;
	
	//--------------------------------------------------------------
	//  class variables
	//--------------------------------------------------------------
	
	// names of properties on the NPC that should be set with
	// details of the message
	
	private static final String MESSAGE = "message";
	private static final String MESSAGE_SOURCE = "message_source";
	private static final String MESSAGE_SOURCE_ID = "message_source_id";
	private static final String MESSAGE_TARGET = "message_target";
	private static final String MESSAGE_TARGET_ID = "message_target_id";
	private static final String IS_DEPARTURE = "is_departure";
	private static final String DIRECTION = "direction";
	
	//--------------------------------------------------------------
	//  usage
	//--------------------------------------------------------------

	/** Look for an appropriate script and execute it. */
	public boolean execute ()
	{
		try
		{
			initialize();
			parseMessage();
			
			if ( isSourceNPC() )
				return true;
			
			findScriptTable();
			findScript ();
			
			if ( script != null )
			{
				setProperties();
				doScript ( script );
			}

			return true;
		}
		catch ( RuntimeException e )
		{
			return true;
		}
	}
	
	//--------------------------------------------------------------

	/** Set up the basic instance variables from the event. */
	private void initialize ()
	{
		// get the message packet text
		msg = (String) (event.getArgs()[0]);
		
		// get the indirection table
		lookup = current.getTable ( RESPONSE_SCRIPTS );
		
		Debug.println ( "DoTalk.initialize(): msg = " + msg );
	}
	
	//--------------------------------------------------------------

	/** Parse the message packet. */
	private void parseMessage ()
	{
		// parse the message to work out what kind it is
		TableParser parser = new TableParser ( msg );
		details = new Hashtable ();
		parser.parseLine ( details );
	}
	
	//--------------------------------------------------------------
	
	/**
	 *  Check whether the message came from an NPC. If so,
	 *  the function returns true, causing <tt>execute()</tt>
	 *  to abort. This is to ensure that NPCs don't get too chatty.
	 */
	private boolean isSourceNPC ()
	{
		// packets do not currently include the from_id field
		// but they will when Alex gets around to it
		String sourceID = (String) details.get ( "from_id" );
		if ( sourceID == null )
			return false;
		
		Atom source = world.getAtom ( sourceID );
		if ( source == null )
			return false;
		
		if ( source.isDescendantOf ( world.getAtom ( "npc" ) ) )
			return true;
		
		return false;
	}
	
	//--------------------------------------------------------------

	/** Select the appropriate script table. */
	private void findScriptTable ()
	{
		type = (String) details.get("type");
		
		// get the appropriate script table type
		if ( "room".equals( type ) )
		{
			if ( details.get("update") != null )
				messageType = ROOM_UPDATE;
			else
				messageType = MISC;
		}
		else if ( "say".equals( type )
				  || "whisper".equals( type )
				  || "shout".equals( type ) )
		{
			// #### this name recognition is pretty lacking
			// should attempt something more sophisticated later
			String ownName = current.getString ( "name" );
			String targetName = (String) details.get("to");
			String sourceName = (String) details.get("from");
			
			// ignore messages from oneself
			if ( sourceName != null
				 && ( sourceName.equalsIgnoreCase("you")
				 	  || sourceName.equalsIgnoreCase(ownName) ) )
				return;
			
			if ( targetName == null )
				messageType = LOOSE_TALK;
			else if ( targetName.equalsIgnoreCase(ownName) )
				messageType = DIRECT_TALK;
			else if ( current.getBool ( WILL_OVERHEAR ) )
				messageType = LOOSE_TALK;
		}
		else if ( "emote".equals( type ) )
		{
			String emotion = (String) details.get("msg");
			if ( emotion.startsWith(SELF_EMOTE_PREFIX) )
				return;
			else
				messageType = MISC;
		}
		else
			messageType = MISC;
		
		Debug.println ( "DoTalk.findScriptTable(): getting table of type " + messageType );
		
		String tableName = (String) lookup.get ( messageType );
		
		Debug.println ( "DoTalk.findScriptTable(): table name is " + tableName );
		
		scriptTable = current.getTable ( tableName );
		
		Debug.println ( "DoTalk.findScriptTable(): got table " + scriptTable );
	}
	
	//--------------------------------------------------------------

	/** Locate a matching script in the script table. */
	private void findScript ()
	{
		// use a lower case version of the message for matching purposes
		String lowMsg = msg.toLowerCase();
		
		// search the keys of the script table until we
		// find one that exists in the message string
		Enumeration tags = scriptTable.keys();
		while ( tags.hasMoreElements() )
		{
			String tag = ((String) tags.nextElement()).toLowerCase();
			
			// check for a match in the current message
			if ( lowMsg.indexOf ( tag ) != -1 )
			{
				script = scriptTable.get ( tag );
				return;
			}
		}
		
		// try the default tag
		script = scriptTable.get( UNKNOWN );
	}
	
	//--------------------------------------------------------------

	/** Pass the message details to the NPC as properties. */
	private void setProperties ()
	{
		current.setProperty ( MESSAGE, details.get("msg") );
		current.setProperty ( MESSAGE_SOURCE, details.get("from") );
		current.setProperty ( MESSAGE_SOURCE_ID, details.get("from_id") );
		current.setProperty ( MESSAGE_TARGET, details.get("to") );
		current.setProperty ( MESSAGE_TARGET_ID, details.get("to_id") );
		current.setProperty ( DIRECTION, details.get("direction") );
		
		Object updateField = details.get ( "update" );
		if ( updateField == null )
			current.setProperty ( IS_DEPARTURE, null );
		else if ( updateField instanceof String )
			if ( ((String)updateField).equalsIgnoreCase("remove") )
				current.setProperty ( IS_DEPARTURE, Boolean.TRUE );
			else
				current.setProperty ( IS_DEPARTURE, Boolean.FALSE );
		else
			current.setProperty ( IS_DEPARTURE, null );
	}

	//--------------------------------------------------------------
}

//------------------------------------------------------------------

/**
 *  Watcher that receives messages for an NPC and invokes
 *  its talk handler.
 */
public class NPCWatcher
	extends Watcher
{
	//--------------------------------------------------------------
	//  instance variables
	//--------------------------------------------------------------

	/**
	 *  The name of the handler to be invoked in response to
	 *  incoming messages.
	 */
	protected String handlerName = NPCBase.DO_TALK;
	
	//--------------------------------------------------------------
	//  construction
	//--------------------------------------------------------------
	
	/**
	 *  Constructor specifying the name of the handler to be
	 *  invoked in response to incoming messages.
	 */
	public NPCWatcher ( Atom atom,
						boolean isLead,
						String handlerName )
	{
		super ( atom, isLead );
		
		Debug.println ( "NPCWatcher.NPCWatcher(): "
						+ atom.getID()
						+ "->"
						+ handlerName );
							 
		if ( handlerName != null )
			this.handlerName = handlerName;
	}

	//--------------------------------------------------------------

	/**
	 *  Constructor using the default talk handler.
	 */
	public NPCWatcher ( Atom atom,
						boolean isLead )
	{
		super ( atom, isLead );
		
		Debug.println ( "NPCWatcher.NPCWatcher(): "
						+ atom.getID()
						+ "->"
						+ handlerName );	 
	}
	
	//--------------------------------------------------------------
	//  usage
	//--------------------------------------------------------------
	
	/**
	 *  Pass incoming message to the watched atom's talk handler.
	 *  The message is passed in the first element of the created
	 *  event's argument list.
	 */
	protected void doOutput ( String msg,
					  		  Event event )
	{
		Debug.println ( "NPCWatcher.doOutput(): received for "
						+ atom.getID()
						+ ": "
						+ msg ); 
		String[] args = { msg };
		atom.getWorld().postEvent ( atom.getWorld().newEvent( atom,
															  handlerName,
															  atom,
															  args ) );
	}
	
	//--------------------------------------------------------------	
}

//------------------------------------------------------------------

/**
 *  Action called when Walker NPCs are started. Creates the initial
 *  timer event to start the NPC walking, and resets the
 *  <b>walk_disabled</b> property. This needs to be called only
 *  once. It does not need to be called when the walker is reloaded
 *  from the database, because timer events are persistent.
 */
public class StartWalker
	extends NPCBase
{
	public boolean execute ()
	{
		Debug.println ( "StartWalker.execute(): starting " + current.getID() );
		
		// reset walk_disabled
		current.setProperty ( WALK_DISABLED, "FALSE" );
		
		// create a timer event for the current atom with 
		// the delay specified in the walk_interval property
		// (note that the current atom is copied to the actor
		// for the timer event)
		world.timerEvent( current,
						  DO_WALK,
						  current,
						  current.getInt ( WALK_INTERVAL ) );
		return pass();
	}
}

//------------------------------------------------------------------

/**
 *  Action called in order to stop Walker NPCs. Sets the
 *  <b>walk_disabled</b> property.
 */
public class StopWalker
	extends NPCBase
{
	public boolean execute ()
	{
		// set the walk_disabled property
		current.setProperty ( WALK_DISABLED, "TRUE" );
		return true;
	}
}

//------------------------------------------------------------------

/**
 *  Action called by the timer events that drive Walker NPCs.
 *  Searches for an appropriate script for the current location
 *  and executes it.
 */
public class DoWalk
	extends NPCBase
{
	/**
	 *  Respond to a DO_WALK timer event. The WALK_SCRIPTS table
	 *  is searched for a script matching the current container,
	 *  and failing that for scripts matching further containers
	 *  all the way up to Limbo. If all those fail too, a final
	 *  check is made against the special container "unknown".
	 *  If a script is found, it is executed by the inherited
	 *  <tt>doScript()</tt> method. If the script results in the
	 *  NPC moving to a new room, the room it started from is
	 *  recorded in its <b>last_place</b> property (in case it
	 *  needs to backtrack).
	 */
	public boolean execute ()
	{
		try
		{
			Debug.println ( "DoWalk.execute(): executing for " + current.getID() );
			
			// bail if walking is disabled
			if ( current.getBool ( WALK_DISABLED ) )
			{
				Debug.println ( "DoWalk.execute(): walking is disabled" );
				return true;
			}
			
			// search for a script on the current room, place
			// and everywhere
			Object script = null;
			Dictionary scriptTable = current.getTable ( WALK_SCRIPTS );
			
			// if the script table is absent, do nothing (and don't
			// post any more timer events)
			if ( scriptTable == null )
			{
				Debug.println ( "DoWalk.execute(): script table not found" );
				return true;
			}
			
			// search up the container hierarchy for a matching script
			Atom what = current;
			Atom where = container;
			
			while ( script == null
					&& where != null
					&& where != world.getLimbo() )
			{
				where = what.getContainer();
				script = scriptTable.get ( where.getID() );
				what = where;
			}
			
			// if we've got all the way to Limbo and still haven't found
			// a script, try the catch-all UNKNOWN tag
			if ( script == null )
				script = scriptTable.get ( UNKNOWN );
			
			// if we've found a script, execute it
			if ( script != null )
			{
				// remember the current container
				Atom oldContainer = container;
				
				// #### there needs to be a check here on random
				// #### choices to discourage oscillation, and
				// #### also support for the BACKTRACK command
				// #### -- probably should override doScriptCommand()
				doScript ( script );
				
				// check the current container and if we've moved
				// store where we've just been in LAST_PLACE
				if ( oldContainer != current.getContainer() )
					current.setProperty ( LAST_PLACE,
										  oldContainer.getID() );
			}
			
			// set the next timer event, unless an "END" command
			// was reached
			if ( ! wasTerminated )
			{
				Debug.println ( "DoWalk.execute(): setting new timer event in "
									 + current.getInt ( WALK_INTERVAL )
									 + " secs" );
				world.timerEvent( actor,
								  DO_WALK,
								  current,
								  current.getInt ( WALK_INTERVAL ) );
			}
			else
			{
				Debug.println ( "DoWalk.execute(): END encountered, no new timer event set" );
			}
	
			return true;
		}
		catch ( RuntimeException e )
		{
			Debug.println ( "DoWalk.execute(): " + e );
			return true;
		}
	}
}

//------------------------------------------------------------------

/**
 *  Action called when Clocker NPCs are created. Creates the
 *  initial table of timer events that drive the NPC. Subsequent
 *  events may be created by these events to keep the whole
 *  process going. This is called by on_create rather than
 *  on_start because the timer events are persistent.
 */
public class CreateClocker
	extends NPCBase
{
	public boolean execute ()
	{
		try
		{
			Debug.println ( "CreateClocker.execute(): called for " + current.getID() );
			
			// get the clock_scripts table
			Dictionary scriptTable = current.getTable ( CLOCK_SCRIPTS );
			
			if ( scriptTable == null )
			{
				Debug.println ( "CreateClocker.execute(): no script table found" );
				return pass();
			}
			
			// set a timer event for each specification
			Enumeration timeSpecs = scriptTable.keys();
			while ( timeSpecs.hasMoreElements() )
			{
				// all keys should be strings, since that's
				// what the TableParser produces, but just in case...
				Object timeSpec = timeSpecs.nextElement();
				if ( timeSpec instanceof String )
				{
					Debug.println ( "CreateClocker.execute(): setting clock event for spec " + timeSpec );
					DoClock.setClock ( current, (String) timeSpec );
				}
			}
			
			return pass();
		}
		catch (RuntimeException e)
		{
			Debug.println ( "CreateClocker.execute(): " + e );
			return true;
		}
	}
}

//------------------------------------------------------------------

/**
 *  Action called in response to clock timer events to
 *  execute scripts at a given time. The actual time
 *  specification string is passed on in the timer
 *  event's argument list to enable the right script
 *  to be identified and a further timer event to be
 *  set if necessary.
 */
public class DoClock
	extends NPCBase
{
	/**
	 *  Respond to a DO_CLOCK timer event. The CLOCK_SCRIPTS table
	 *  is searched for a script matching the supplied argument,
	 *  and if found, it is executed. A new timer event is then
	 *  created from the same time specification. (Where this is
	 *  not the desired behaviour, the time script should finish
	 *  with an "END" command.)
	 */
	public boolean execute ()
	{
		try
		{
			Debug.println ( "DoClock.execute(): entering for " + current.getID() );
			
			// bail if clock handling is disabled
			if ( current.getBool ( CLOCK_DISABLED ) )
			{
				Debug.println ( "DoClock.execute(): clock handling disabled" );
				return true;
			}
			
			// get the time spec from the event's argument list
			Object time = event.getArg(0);
			if ( time == null || !( time instanceof String ) )
			{
				Debug.println ( "DoClock.execute(): no time spec found" );
				return true;
			}
			
			String timeSpec = (String) time;
			
			// get the script table
			Dictionary scriptTable = current.getTable ( CLOCK_SCRIPTS );
			
			// if the script table is absent, do nothing (and don't
			// post any more timer events)
			if ( scriptTable == null )
			{
				Debug.println ( "DoClock.execute(): no script table found" );
				return true;
			}
			
			// locate the appropriate script, if any
			Object script = scriptTable.get( timeSpec );
			if ( script == null )
			{
				Debug.println ( "DoClock.execute(): no script found matching " + timeSpec );
				return true;
			}
			
			// run the script
			Debug.println ( "DoClock.execute(): executing script for timeSpec "
							+ timeSpec
							+ ": "
							+ script );
			doScript ( script );
	
			// set the next timer event, unless an "END" command
			// was reached
			if ( ! wasTerminated )
			{
				Debug.println ( "DoClock.execute(): installing new clock event for " + timeSpec );
				setClock ( current, timeSpec );
			}
			else
			{
				Debug.println ( "DoClock.execute(): END encountered, no new event installed" );
			}
			
			return true;
		}
		catch ( RuntimeException e )
		{
			Debug.println ( "DoClock.execute(): " + e );
			return true;
		}
	}

	//--------------------------------------------------------------
	
	/**
	 *  Utility function to set a timer event for a given
	 *  target atom at a time specified using the GameDate
	 *  "cron job" syntax. The timer event will be set to
	 *  call the DO_CLOCK handler (<i>clocker.on_do_clock</i>),
	 *  which is typically an instance of this action class.
	 */
	public static void setClock ( Atom target,
								  String timeSpec )
	{
		try
		{
			Debug.println ( "DoClock.setClock(): setting event for "
							+ target.getID()
							+ " @ "
							+ timeSpec );
			
			String[] args = { timeSpec };
			Event evt = target.getWorld().newEvent ( target,
													 DO_CLOCK,
													 target,
													 args );
													 
			GameDate now = new GameDate ( target.getWorld().getTime() );
			GameDate then = new GameDate ( timeSpec, now );
					
			target.getWorld().timerEventAt( evt, then.getSeconds() );
		}
		catch ( GameDateFormatException e )
		{
			// ignore any exceptions
			Debug.println ( "DoClock.setClock(): " + e );
		}
	}
	
	//--------------------------------------------------------------	
}

//------------------------------------------------------------------


