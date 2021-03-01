// $Id: Timer.java,v 1.13 1999/04/29 13:49:32 jim Exp $
// Timer for game world
// James Fryer, 28 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.util.*;
import java.io.*;
import com.ogalala.util.*;

/** The Timer has two functions: 
    <ul>
    <li> Keep track of World Time, defined as the number of seconds the game 
        has been in 'active' state. This is the total time between calls to 
        'world.start' and 'world.stop'.
    <li> Support timed events, which are to be posted to the event processor 
        at some future time.
    </ul>
    All measurements are in real seconds. If game time is required to flow
    at some other rate (e.g. 2 game seconds == 1 real second) this must be 
    maintained in a game-specific module.
    <p>
    No time formatting or output functions are provided. This is assumed to be 
    the responsibility of a game-specific module.
*/
final class Timer
    implements Runnable, java.io.Serializable
    {
    public static final int serialVersionUID = 1;
    
    /** Time to sleep between looking for mature events (milliseconds)
    */
    private static final int SLEEP_TIME = 200;
    
    /** Back-pointer to world
    */
    private World world;

    /** Timed event queue
    */
    private EventTimerQueue queue = new EventTimerQueue();

    /** The number of seconds since the world was created
    */
    private long worldTime = 0L;

    /** The system time when the world was started with 'world.start'
    */
    private transient long startTimeMillis = 0L;

    /** The thread
    */
    private transient Thread thread;
    
    /** Constructor
    */
    public Timer(World world)
        {
        this.world = world;
        }

    /** Default constructor
    */
    protected Timer()
        {
        }

    /** Start the timer
    */
    public void start()
        {
        thread = new Thread(this, "MUA world timer");
        startTimeMillis = new Date().getTime();
  	    thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
        }

    /** Stop the timer
    */
    public void stop()
        {
        thread.stop();
        worldTime = getTime();
        thread = null;
        }

    /** Get the number of seconds the game has been running
    */
    public long getTime()
        {
        return worldTime + getElapsedTime();
        }

    /** Get the time in seconds since the timer was started
    */
    private final long getElapsedTime()
        {
        return (new Date().getTime() - startTimeMillis) / 1000;
        }

    /** Cause an event to be sent to the queue when its time matures
    */
    public void putEvent(Event event)
        {
        queue.put(event);
        }
        
    /** The thread waits until an event matures on the timer queue, then 
        posts it to the event processor queue.
    */
    public void run()
        {
        while (true)
            {
            // If the queue is not empty, post any matured events
            if (!queue.isEmpty())
                {
                // The time to wait until the next event
                long nextEventDelay = 0;
                do  {
                    // See if an event has matured
                    nextEventDelay = queue.peek().getTime() - getTime();
                    if (nextEventDelay <= 0)
                        {
                        // Remove the event from the timer queue and post it to the processor queue
                        Event event = queue.get();
                        world.postEvent(event);
                        }
                    }
                while (nextEventDelay <= 0);
                }
                
            // Go to sleep for a short time
            try {
                thread.sleep(SLEEP_TIME);
                }
            catch (InterruptedException e)
                {
                }
            }
        }

    /** How many elements in the timer queue?
    */
    public int queueSize()
        {
        return queue.count();
        }
        
    /** Export the timer's dynamic state. This contains the time and all
        events whose current atom is not frozen.
    */
    protected final void exportState(PrintWriter out)
        throws IOException
        {
        // Write the time
        out.println(Long.toString(worldTime));
        
        // Write the events
        for (int i = 0; i < queue.count(); i++)
            {
            Event event = queue.elementAt(i);
            Atom curr = event.getCurrent();
            if (curr != null && !curr.isFrozen())
                out.println(event.toExportFormat());
            }
        out.println(AtomDatabase.EXPORT_DELIMITER);
        }

    /** Import a dynamic state file
    */
    protected final void importState(BufferedReader in)
        throws IOException
        {
        // Get the time
        String s = in.readLine();
        worldTime = Long.parseLong(s);
        
        // Add the events to the queue
        while (true)
            {
            s = in.readLine();
            if (s == null || s.equals(AtomDatabase.EXPORT_DELIMITER))
                break;
            try {
                Event event = importEvent(s);
                putEvent(event);
                }
            catch (RuntimeException e)
                {
                Debug.println("MUA Timer: Can't import timer event");
                Debug.printStackTrace(e);
                }
            }
        }

    /** Import an event
    */
    private Event importEvent(String s)
        {
        StringTokenizer tzr = new StringTokenizer(s, AtomDatabase.EXPORT_DELIMITER);
        // Id
        String id = tzr.nextToken();
        
        // Actor
        String s2 = tzr.nextToken();
        Atom actor = world.getAtom(s2);
        
        // Current
        s2 = tzr.nextToken();
        Atom current = world.getAtom(s2);
        
        // Args -- get them as a vector then convert to array
        // (Note we remove the first character of the string -- the opening bracket --
        //  before sending it to the table parser.)
        s2 = tzr.nextToken();
        s2 = s2.substring(1);
        TableParser parser = new TableParser(s2);
        Vector argv = (Vector)parser.getParsed(TableParser.NO_CONVERSION);
        Object args[] = null;
        if (argv.size() > 0)
            {
            args = new Object[argv.size()];
            for (int i = 0; i < argv.size(); i++)
                args[i] = AtomData.parse(argv.elementAt(i).toString(), world);
            }
            
        // Time
        s2 = tzr.nextToken();
        long time = Long.parseLong(s2);
        
        // Create the event and set the time
        Event result = world.newEvent(actor, id, current, args);
        result.setTime(time);
        return result;
        }
    }
