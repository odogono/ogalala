// $Id: EventQueue.java,v 1.12 1999/04/29 13:49:32 jim Exp $
// Queue for mua events
// James Fryer, 13 July 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.mua;

import com.ogalala.util.Queue;

/** A queue and processing structure for events.
    <p>
    Synchronization: The 'put' and 'get' functions are synched. This class uses
    an "Optimistic single-threaded execution" strategy to wait for items to 
    be added to the queue. (See Java Language Reference, O'Reilly, section 8.2.2.)
*/
abstract class EventQueue
    implements Runnable, java.io.Serializable
    {
    public static final int serialVersionUID = 1;

    // The queue of events
    Queue queue = new Queue();
    
    private transient Thread thread = null;
    
    public static final int PRIORITY = Thread.NORM_PRIORITY + 1;
    
    public EventQueue()
        {
        }
    
    /** Start the thread
    */
    public void start()
        {
        if (thread == null)
            {
            thread = new Thread(this, "MUA EventQueue");
            thread.setPriority(PRIORITY);
            }
        thread.start();
        }
        
    /** Stop the thread
    */
    public void stop()
        {
        thread.stop();
        thread = null;
        }
        
    /** Pull events off the queue as they become available and 
        dispatch to 'processEvent'.
    */
    public void run()
        {
        // Used for performance monitoring
        long retrievedAt, processedAt;
        while (true)
            {
            // Get the event and process it
            Event event = get();
            retrievedAt = System.currentTimeMillis();
            processEvent(event);
            processedAt = System.currentTimeMillis();
            if (World.DEBUG)
                event.getWorld().logEvent(event, retrievedAt, processedAt);
            }
        }
    
    protected abstract void processEvent(Event event);
        
    /** Add an event
    */
    public synchronized void put(Event event)
        {
        event.timeQueued = System.currentTimeMillis();
        
        // Add the object to the queue
        queue.put(event);

        // Inform waiting processes that something has been added
        notify();
        }

    /* Get an event from the queue.
    */
    protected synchronized Event get()
        {
        // Wait until something arrives
        while (queue.isEmpty())
            {
            try {
                wait();
                }
            catch (InterruptedException e)
                {
                }
            }

        // Return the queue item
        return (Event)queue.get();
        }

    /** Is the queue empty?
    */
    public final boolean isEmpty()
        {
        return queue.isEmpty();
        }

    /** How many items on the queue?
    */
    public final int size()
        {
        return queue.size();
        }

    /** Is the thread running?
    */
    public final boolean isActive()
        {
        if (thread == null)
            return false;
        else
            return thread.isAlive();
        }
    }
