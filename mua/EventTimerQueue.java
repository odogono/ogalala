// $Id: EventTimerQueue.java,v 1.4 1999/04/07 15:06:36 jim Exp $
// A priority queue for events
// James Fryer, 28 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import com.ogalala.util.*;

/** A priority queue for events.
    <p>
    This class is written as a wrapper round the Priority Queue because
    of the need for syncronization.
*/
final class EventTimerQueue
    implements java.io.Serializable
    {
    public static final int serialVersionUID = 1;
    
    private static final int SIZE = 1000;
    
    private PQ queue = new PQ();
    
    /** Constructor.
    */
    public EventTimerQueue()
        {
        }
        
    synchronized void put(Event event)
        {
        queue.put(event);
        }
        
    synchronized Event get()
        {
        return (Event)queue.get();
        }
        
    synchronized Event peek()
        {
        return (Event)queue.peek();
        }
        
    synchronized Event elementAt(int i)
        {
        return (Event)queue.elementAt(i);
        }
        
    synchronized int count()
        {
        return queue.count();
        }

    synchronized boolean isEmpty()
        {
        return queue.isEmpty();
        }
    
    /** Inner class implements the priority queue
    */
    class PQ
        extends PriorityQueue
        {
        PQ()
            {
            super(SIZE, true);
            }

        /** Compare events
        */
        protected int compare(Object o1, Object o2)
            {
            long time1 = ((Event)o1).getTime();
            long time2 = ((Event)o2).getTime();
            if (time1 > time2)
                return 1;
            else if (time1 < time2)
                return -1;
            else
                return 0;
            }
        }
    }
