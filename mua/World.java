// $Id: World.java,v 1.114 1999/04/29 13:49:32 jim Exp $
// The World in which a game takes place
// James Fryer, 17 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import java.io.*;
import com.ogalala.util.*;

/** The World contains all the various data objects that are needed to 
    run a game: atom database, event handler, parser, vocabulary, etc.
    <p>
    There are several groups of functions:
    <ul>
    <li> World management: 'start' and 'stop'.
    <li> Atom creation and management (<code>newAtom</code>, <code>moveAtom</code>, etc.)
    <li> Event creation and dispatch (<code>newEvent</code>, <code>postEvent</code>, etc.)
    <li> Utilities (<code>addPath</code>, etc.)
    </ul>
    To create a world, use 'WorldFactory'.
    <p>
    Atoms and Things are created with 'newAtom' and 'newThing'. They are 
    deleted with 'deleteAtom'. 
    <p>
    Events are handled in one of the following ways:
    <ul>
    <li> Called: the event is processed immediately
    <li> Posted: the event is placed on the event queue and processed when it 
        reaches the head of the queue
    <li> Timed: the event is placed on a timer queue and posted when it 
        reaches the head of the queue.
    </ul>
*/
public class World
    implements Serializable
    {
    public static final int serialVersionUID = 1;

    /** Debug flag
    */
    public static final boolean DEBUG = true;
    
    /** Readfile names
    */
    public static final String CREATE_SCRIPT_NAME = "main";
    
    /** The ID of the administrator mobile, used to boot the system.
    */
    public static final String ADMIN_ID = "admin".intern();
    
    /** IDs of properties called and used
    */
    public static final String ON_CREATE = "on_create";
    public static final String ON_DESTROY = "on_destroy";
    public static final String ON_START = "on_start";
    public static final String ON_STOP = "on_stop";
    public static final String ON_RESET = "on_reset";
    public static final String ON_TIMER = "on_timer";
    public static final String DEPENDENTS = "dependents";
    public static final String DESTINATION = "destination";
    public static final String OTHER_SIDE = "other_side";
    
    /** The base file name (also the application ID)
    */
    private transient String fileName;
    
    /** The paths
    */
    private transient PathList pathList;
    
    /** The atom database
        <p>### Would be better to inherit from this
    */
    private AtomDatabase database;

    /** The event processor
    */
    private EventProcessor eventProcessor;
    
    /** The game timer
    */
    private Timer timer;

    /** The default parser, used if no other parser is supplied
    */
    private Parser defaultParser;
    
    /** The vocabulary 
        ### This really belongs to the parser
    */
    private Vocabulary vocabulary;
    
    /** The event stack
    */
    private Stack eventStack;
    
    /** Is this world in an active state?
    */
    private transient boolean isActive = false;
    
    /** Default watcher, displays world activity to console
    */
    private transient ConsoleWatcher adminWatcher = null;

    /** Should we log events?
    */
    public transient boolean loggingEvents = false;
    
    private transient PrintWriter eventLog = null;
    
    /** Construct a new World file
    */
    protected World(String fileName)
        throws WorldException
        {
        this.fileName = fileName;
        pathList = new PathList();
        database = new AtomDatabase(this);
        eventProcessor = new EventProcessor(this);
        vocabulary = new Vocabulary();
        defaultParser = newParser();
        timer = new Timer(this);
        eventStack = new Stack();
        }

    /** Default ctor for serialization
    */
    protected World()
        {
        }
        
    /** Get the world's filename
    */
    public String getFileName()
        {
        return fileName;
        }

    /** Set the filename
    	<p>
    	(Used by WorldFactory)
    */
    protected void setFileName(String fileName)
        {
        this.fileName = fileName;
        }

    /** Create the required atoms
    */
    protected void createCoreAtoms()
        {
        //### This function should be merged with 'database.createCoreAtoms'
  
        // Create the administrator mobile 
        newThing(ADMIN_ID, getAtom(AtomDatabase.MOBILE_ID));
      
        Debug.assert(checkCoreAtoms(), "(World/197)");
        }

    /** Are the required atoms present?
    */
    protected boolean checkCoreAtoms()
        {
        //### This function should be merged with 'database.checkCoreAtoms'

        return database.checkCoreAtoms() && 
                getAtom(ADMIN_ID) != null;
        }

    /** Initialise the game
        <p>
        ### This function will probably be removed eventually as it would be 
        ###     better to inherit from 'AtomDatabase'. At present it is called
        ###     only by 'WorldFactory'.
    */
    protected void init()
        throws WorldException
        {
        // Initialise the database        
        database.init();
        createCoreAtoms();
        
        // Add vital commands to the vocabulary
        initVocabulary();
        }
        
    /** Start running the game
    */
    public void start()
        throws WorldException
        {
        if (!checkCoreAtoms())
            throw new AtomException("Not a valid Game World: " + fileName);
        eventProcessor.start();
        timer.start();
        startAtoms();
        isActive = true;
        }
        
    /** Stop running the game
    */
    public void stop()
        throws WorldException
        {
        stopAtoms();
        timer.stop();
        eventProcessor.stop();
        isActive = false;
        }
        
    /** Is this world in an active state?
    */
    public boolean isActive()
        {
        return isActive;
        }
    
    /** Run the startup scripts
    */
    public void runStartupScripts()
        throws WorldException
        {
        execAdminScript(CREATE_SCRIPT_NAME);
        }
        
    /** Activate or deactivate admin logging
    */
    public final void setAdminLog(boolean f)
        {
        Atom admin = getAtom(ADMIN_ID);
        if (f)
            {
            if (adminWatcher == null)
                {
                adminWatcher = new ConsoleWatcher(admin, false);
                admin.addWatcher(adminWatcher);
                }
            }
        else {
            if (adminWatcher != null)
                {
                admin.removeWatcher(adminWatcher);
                adminWatcher = null;
                }
            }
        }

    /** Activate or deactivate event logging
    */
    public final void setEventLog(boolean f)
        {
        loggingEvents = f;
        }
        
    /** Stop the Watchers
    */
    protected void stopWatchers()
        {
        Atom admin = getAtom(ADMIN_ID);
        }
        
    /** Send startup messages
    */
    protected void startAtoms()
        {
        Atom actor = getAtom(ADMIN_ID);
        Enumeration atoms = getAtoms();
        while (atoms.hasMoreElements())
            {
            Atom atom = (Atom)atoms.nextElement();
            if (atom instanceof Thing)
                callEvent(actor, ON_START, atom);
            }
        }
        
    /** Send stop messages
    */
    protected void stopAtoms()
        {
        Atom actor = getAtom(ADMIN_ID);
        Enumeration atoms = getAtoms();
        while (atoms.hasMoreElements())
            {
            Atom atom = (Atom)atoms.nextElement();
            if (atom instanceof Thing)
                callEvent(actor, ON_STOP, atom);
            }
        }
        
    /** Get the vocabulary
    */
    public final Vocabulary getVocabulary()
        {
        return vocabulary;
        }
        
    /** Initialise the vocabulary
        <p>
        This function initalises verbs which must exist before any input can be understood.
        <p>
        ### This initialisation really should be in the Vocabulary class?
    */
    protected void initVocabulary()
        {
        defaultParser.addRawVerb(getAdmin(), Privilege.DEFAULT, "!SET", "_set", AtomData.parse("!ModSet", this));
        defaultParser.addRawVerb(getAdmin(), Privilege.DEFAULT, "!CLEAR", "_clear", AtomData.parse("!ModClear", this));
        defaultParser.addRawVerb(getAdmin(), Privilege.DEFAULT, "!NOUN", "_add_noun", AtomData.parse("!ModAddNoun", this));
        }


    /** Run a script as administrator. Any error in the script will cause the world 
        to stop processing events.
    */
    protected void execAdminScript(String fileName)
        throws WorldException
        {
        // Get the administrator mobile
        Atom actor = getAtom(ADMIN_ID);
        
        // Get the qualified file name and check it exists. No exception is thrown
        //  if the file doesn't exist.
        File f = findFile(fileName);
        if (!f.exists())
            {
            actor.output("Script file not found: " + f);
//###            System.err.println("Script file not found: " + f);
            return;
            }
        
        // Execute the script
        try {
            execScript(fileName, actor);
            }
        catch (RuntimeException e)
            {
            // Rethrow runtime exceptions
            throw e;
            }
        catch (Throwable e)
            {
            // Convert all others to World exception
            throw new WorldException(e.getMessage());
            }
        }

    /** Run a script. 
    */
    public void execScript(String fileName, Atom actor)
        throws IOException
        {
        // Open the file and execute commands
        File f = findFile(fileName);
        actor.output("Executing Script: " + f.getAbsolutePath());
        BufferedReader in = new BufferedReader(new FileReader(f));
        String s;
        while ((s = in.readLine()) != null)
            {
            // Note we do not use 'parseCommand' from this class because we want 
            //  to stop processing the script if there is an error 
            defaultParser.parseSentence(s, actor);
            }
        in.close();
        }

    /** Create a parser
    */
    public Parser newParser()
        {
        return new Parser(this);
        }
        
    /** Parse a command using the default parser
    */
    public final void parseCommand(String command, Atom actor)
        {
        parseCommand(command, actor, defaultParser);
        }
        
    /** Parse a command
    */
    public void parseCommand(String command, Atom actor, Parser parser)
        {
        try {
            parser.parseSentence(command, actor);
            }
            
        // No errors are allowed to get past this point!
        catch (Throwable e)
            {
            outputError(e, actor);
            }
        }
        
    /** Send an error message to the user
    */
    private static void outputError(Throwable e, Atom actor)
        {
        try {
            Debug.printStackTrace(e);
            String msg = e.getMessage();
            if (msg == null)
                msg = "Unknown error";
            if (actor != null)
                {
                OutPkt output = new OutPkt("error");
                output.addField("msg", msg);
                String stackTrace = StringUtil.crsToHTML(Debug.getStackTrace(e));
                output.addField( "debug", stackTrace );
                actor.output(output);
                }
            }   
        
        // If we get here, an exception has been thrown in the error output code.
        //  Give up attempting to report to the user, write it to the console instead.
        catch (Throwable e2)
            {
            Debug.println("Error sending output to user: " + e2);
            Debug.printStackTrace( e2 );
            }
        }
        
    /** Get the number of seconds the game has been running
    */
    public long getTime()
        {
        return timer.getTime();
        }
    
    /** Create an Event
    */
    public Event newEvent(Atom actor, String id, Atom current, Object args[])
        {
        return new Event(this, actor, id, current, args);
        }

    /** Create an Event with no args
    */
    public Event newEvent(Atom actor, String id, Atom current)
        {
        return new Event(this, actor, id, current, null);
        }

    /** Put an event on the queue
    */
    public void postEvent(Event event)
        {
        // If the world is active, we add the event to the queue. Otherwise we execute it straight away.
        if (isActive())
            eventProcessor.put(event);
        else
            callEvent(event);
        }
        
    /** Request an event to be sent in 'delay' seconds
    */
    public void timerEvent(Event event, int delay)
        {
        timerEventAt(event, getTime() + delay);
        }
        
    /** Request a timer event to be sent to 'current' in 'delay' seconds
    */
    public void timerEvent(Atom actor, Atom current, int delay)
        {
        timerEvent(newEvent(actor, ON_TIMER, current, null), delay);
        }
        
    /** Request a named timer event to be sent to 'current' in 'delay' seconds
    */
    public void timerEvent(Atom actor, String id, Atom current, int delay)
        {
        timerEvent(newEvent(actor, id, current, null), delay);
        }
        
    /** Request an event to be sent at the specified world time (in seconds)
    */
    public void timerEventAt(Event event, long time)
        {
        event.setTime(time);
        timer.putEvent(event);
        }
        
    /** Call an event 
    */
    public boolean callEvent(Atom actor, String id, Atom current, Object args[])
        {
        return callEvent(newEvent(actor, id, current, args));
        }
        
    /** Call an event with no args
    */
    public boolean callEvent(Atom actor, String id, Atom current)
        {
        return callEvent(newEvent(actor, id, current, null));
        }
        
    /** Call an event with no current
    */
    public boolean callEvent(Atom actor, String id, Object args[])
        {
        return callEvent(newEvent(actor, id, null, args));
        }
        
    /** Call an event with no current or args
    */
    public boolean callEvent(Atom actor, String id)
        {
        return callEvent(newEvent(actor, id, null, null));
        }
        
    /** Call an event. Return true if the event is handled.
    */
    public /*synchronized*/ boolean callEvent(Event event)
        {
        boolean result = false;
        
        // Clear the event result
        event.clearResult();
        
        // Get the target object, or root. We get the target, not current,
        //  because the event property is defined on target if this event
        //  has been offered to some other object or passed to the parent.
        Atom current = event.getTarget();
        if (current == null)        //### Should be redundant with new parser
            current = getRoot();
        
        // Get the property for this event
        Object value = current.getRawProperty(event.getID());

        // If it is null, we can go no further
        if (value == null || AtomData.isNullProperty(value))
            return result;
        
        try {
            // Push the event onto the event stack, making it the current event.
            pushEvent(event);
                
            // If it's an action, call it and get its result
            if (value instanceof Action)
                {
                Action action = (Action)value;
                result = action.execute(event);
                }
                
            // Else If it's a bool or int, return it as the result
            else if (value instanceof Boolean)
                result = ((Boolean)value).booleanValue();
            else if (value instanceof Integer)
                result = ((Integer)value).intValue() != 0;
                
            // Else, call the default action, which will output the property value to the actor.
            else {
                //### This is temporary !! Should call the default action.
    //###            event.setResult(AtomData.toString(value));
                event.getActor().output(AtomData.toString(value));
                result = true;  //###
                }
            }
            
        // Catch and rethrow any exceptions, using a finally block to clean up the event stack.
        catch (RuntimeException e)
            {
            throw e;
            }
        finally
            {
            // Remove the event from the stack
            popEvent();
            }
        
        return result;
        }
        
    /** Write event processing performance to a log file. Used by the event queue.
        All times in milliseconds.
        @param event The event being processed
        @param retrievedAt The time the event was retrieved
        @param processedAt The time the event processing finished
    */
    void logEvent(Event event, long retrievedAt, long processedAt)
        {
        if (loggingEvents)
            {
            // If the file is not open, open it and write the header 
            if (eventLog == null)
                {
                String fileName = getFileName() + ".event_log";
                try {
                    eventLog = new PrintWriter(new FileOutputStream(fileName), true);
                    }   
                catch (IOException e)
                    {
                    // If we can't create the file there's nothing we can do about it...
                    return;
                    }
                eventLog.println("QueueTime\tLag\tProcessTime\tQueueSize\tTimerQueueSize\tAtomCount\tEvent");
                }

            // Write the log data
            StringBuffer buf = new StringBuffer();
            buf.append(Long.toString(event.timeQueued));
            buf.append("\t");
            buf.append(Long.toString(retrievedAt - event.timeQueued));
            buf.append("\t");
            buf.append(Long.toString(processedAt - retrievedAt));
            buf.append("\t");
            buf.append(eventProcessor.size());
            buf.append("\t");
            buf.append(timer.queueSize());
            buf.append("\t");
            buf.append(size());
            buf.append("\t");
            buf.append(event.toString());
            eventLog.println(buf.toString());
            eventLog.flush();
            }
        }

    /** Get the current event (possibly null).
    */
    public Event getCurrentEvent()
        {
        if (eventStack.empty())
            return null;
        else
            return (Event)eventStack.peek();
        }
        
    /** Push an event onto the stack
    */
    public void pushEvent(Event event)
        {
        eventStack.push(event);
        }
        
    /** Pop an event from the stack
    */
    public void popEvent()
        {
        eventStack.pop();
        }
        
    /** Construct a fully-qualified file name. Use this for files you intend
        to create.
    */
    public final File getFile(String name)
        {
        return pathList.getFile(name);
        }

    /** Construct a fully-qualified file name. Use this for files you expect to 
        exist already. If the file doesn't exist, the effect is the same as 'getFile'.
    */
    public final File findFile(String name)
        {
        return pathList.findFile(name);
        }

    /** Add directories to the search path
    */
    public final void addPath(String path)
        {
        if (pathList == null)
            pathList = new PathList();
        pathList.addPath(path);
        }


// Atom manipulation functions
    
    /** Create a new Atom
        <p>
        An ID of null will generate a unique ID
    */
    public final Atom newAtom(String id, Atom parent)
        {
        return database.newAtom(id, parent);
        }

    /** Create a new Atom with multiple parents
        <p>
        An ID of null will generate a unique ID
    */
    public final Atom newAtom(String id, Vector parents)
        {
        return database.newAtom(id, parents);
        }

    /** Create a new Thing, Container, etc. depending on the type of the parent
        <p>
        The parent must be of the highest static type the 
        Thing needs to be. I.e. if you want a Java Container, 'parent' must be at 
        least a container atom.
        <p>
        An ID of null will generate a unique ID
    */
    public final Thing newThing(String id, Atom parent)
        {
        Thing result = database.newThing(id, parent);
        initThing(result);
        return result;
        }
    
    /** Create a new Thing with multiple parents
    */
    public final Thing newThing(String id, Vector parents)
        {
        Thing result = database.newThing(id, parents);
        initThing(result);
        return result;
        }
    
    /** Initialise a Thing by calling its ON_CREATE and ON_START handlers
    */
    private void initThing(Thing thing)
        {
        Atom actor = getAdmin();
        callEvent(actor, ON_CREATE, thing);
        if ( isActive() )
        	callEvent(actor, ON_START, thing);
        }
        
    /** 
    *   Create a copy of an existing Atom
    *   <p>
    *   The new atom has the same fields and inheritance as the existing one,
    *   but is created in Limbo and if it is a container then it will be created
    *   empty.
    *
    * @param atom					the atom to clone
    * @returns						the cloned atom
    */
    public final Atom cloneAtom(Atom atom)
        {
        Atom result = database.cloneAtom(atom);
        if (atom instanceof Thing)
            initThing((Thing)result);
        return result;
        }
    
    /**
    * Create a copy of an existing Atom
    * <p>
    * The new atom has the same fields and inheritance as the existing one,
    * but is created in Limbo. This differs from the method cloneAtom(), in
    * that the contents of the Atom (if it is a container), will also be 
    * cloned.
    *
    * @param atom					the atom to clone
    * @returns						the cloned atom
    */
    public final Atom deepCloneAtom(Atom atom)
    {
    	Atom parent = cloneAtom(atom);
    	Atom child;
    	
    	ContentsEnumeration contents = new ContentsEnumeration(atom);
    	
    	//loop through the contents of the atom
    	while(contents.hasMoreElements())
    	{
    		child = (Atom)contents.nextElement();
    		
    		//If what we have is a thing, clone the bugger and then
    		// move it into the parent.
    		if( child instanceof Thing )
    			moveAtom(deepCloneAtom(child), parent);
    	}
    	//
    	return parent;
    }
    
    /** Delete a named atom from the database
    */
    public final void deleteAtom(String id)
        {
        Atom atom = getAtom(id);
        if (atom == null)
            throw new AtomException("Can't find atom: " + id);
        deleteAtom(atom);
        }

    /** Delete an atom from the database
    */
    public final void deleteAtom(Atom atom)
        {
        Atom actor = getAdmin();
        if (atom instanceof Thing)
            {
            if ( isActive() )
            	callEvent(actor, ON_STOP, atom);
            callEvent(actor, ON_DESTROY, atom);
            }
        database.deleteAtom(atom);
        }

    /** Move an atom to a new location
    */
    public final void moveAtom(Atom atom, Atom newContainer)
        {
        newContainer.putIn(atom);
        }

    /** Replace an atom with another. The original atom is moved to Limbo.
    */
    public final void replaceAtom(Atom atomToBeReplaced, Atom newAtom)
        {
        Atom container = atomToBeReplaced.getContainer();
        moveAtom(atomToBeReplaced, getLimbo());
        moveAtom(newAtom, container);
        }

    /** Reset an atom and its dependents
    */
    public final void resetAtom(Atom atom)
        {
        Atom actor = getAdmin();
        callEvent(actor, ON_RESET, atom);
        
        // Propagate the reset to the dependents if any
        Object o = atom.getRawProperty(DEPENDENTS);
        if (o != null && o instanceof Vector)
            {
            Enumeration dependents = ((Vector)o).elements();
            while (dependents.hasMoreElements())
                resetAtom((Atom)dependents.nextElement());
            }
        }

    /** Add an exit between two containers
        <p>
        This function sets up the exit data structures.
    */
    public final void addExit(int direction1, Atom container1, Atom exit1, int direction2, Atom container2, Atom exit2)
        {
        // Link this side to the other side
        exit1.setProperty(DESTINATION, container2);
        exit1.setProperty(OTHER_SIDE, exit2);
        exit2.setProperty(DESTINATION, container1);
        exit2.setProperty(OTHER_SIDE, exit1);
        
        // Add the exits to the container
        container1.addExit(direction1, exit1);
        container2.addExit(direction2, exit2);
        }
        
    /** Add a one-way exit between two containers
    */
    public final void addExit(int direction1, Atom container1, Atom exit1, Atom container2)
        {
        // Link this side to the other side
        exit1.setProperty(DESTINATION, container2);
        
        // Add the exit to the container
        container1.addExit(direction1, exit1);
        }
        
    /** Remove an exit
        <p>
        This function removes and deletes the exit atom and its opposite, keeping both
        sides consistent.
    */
    public final void removeExit(int direction, Atom container1)
        {
        // Get the exit on this side. Ignore if it doesn't exist
        Atom exit1 = container1.getExit(direction);
        if (exit1 == null)
            return;
            
        // Get the destination and other side exit
        Atom container2 = exit1.getAtom(DESTINATION);
        Atom exit2 = exit1.getAtom(OTHER_SIDE);

        // Remove and delete the exits. 
        container1.removeExit(exit1);
        deleteAtom(exit1);
        
        // Note that the other side can be null if this is a one-way exit.
        if (exit2 != null)
            {
            container2.removeExit(exit2);
            deleteAtom(exit2);
            }
        }
        
    /** Enumerate all the atoms in the world
    */
    public final Enumeration getAtoms()
        {
        return database.getAtoms();
        }

    /** Get an existing Atom
    */
    public final Atom getAtom(String id)
        {
        return database.getAtom(id);
        }

    /** Get the atom at the root of the inheritance hierarchy
    */
    public final Atom getRoot()
        {
        return database.getRoot();
        }

    /** Get the "limbo" container
    */
    public final Atom getLimbo()
        {
        return database.getLimbo();
        }

    /** Get the administrator mobile
    */
    public final Atom getAdmin()
        {
        return getAtom(ADMIN_ID);
        }
        
    /** Create an ID unique in the database, prefixed with the 'base' argument
    */
    public final String getUniqueID(String base)
        {
        return database.getUniqueID(base);
        }

    /** Get the atom database
    */
    public final AtomDatabase getAtomDatabase()
        {
        return database;
        }

    /** How many atoms in the database
    */
    public final int size()
        {
        return database.size();
        }
        
    /** Freeze all atoms in the database
    */
    public final void freeze()
        {
        database.freeze();
        }

    /** Export the world's dynamic state
    */
    public final void exportState(OutputStream _out)
        throws IOException
        {
        PrintWriter out = new PrintWriter(_out, true);
        database.exportState(out);
        timer.exportState(out);
        }

    /** Import a dynamic state file
    */
    public final void importState(InputStream _in)
        throws IOException
        {
        BufferedReader in = new BufferedReader(new InputStreamReader(_in));
        database.importState(in);
        timer.importState(in);
        }
    }
