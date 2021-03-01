// $Id: OutputQueue.java,v 1.10 1998/11/05 12:02:11 rich Exp $
// Output queue for the server framework
// James Fryer, 16 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server;

import com.ogalala.util.*;

/** The output queue waits for output strings to be added and then dispatches
    them to the connection's 'processOutput' function.

    Synchronization: The 'put' and 'get' functions are synched. This class uses
        an "Optimistic single-threaded" strategy to wait for items to be added to
        the queue. (See Java Language Reference, O'Reilly, section 8.2.2.)
*/
class OutputQueue
    implements Runnable
    {
    // The queue of output strings
    Queue queue = new Queue();

    // Back-pointer to connection
    Connection connection;

    OutputQueue(Connection connection)
        {
        this.connection = connection;
        }

    /** Add a string to the queue. (Called only by 'connection.output')
    */
    synchronized void put(String s)
        {
        // Add the object to the queue
        queue.put(s);

        // Inform waiting processes that something has been added
        notify();
        }

    /* Get a string from the queue. (Called only by 'run')
    */
    synchronized private String get()
        {
        // Wait until something arrives
        while (queue.isEmpty())
            {
            try {
                wait();
                }

            //### catch exception and report it to the error stream
            catch (Exception e)
                {
                System.err.println("OutputQueue::get - " + e);
                Debug.printStackTrace( e );
                }
            }

        // Return the queue item
        return (String)queue.get();
        }

    public void run()
        {
        while (true)
            {
            // Get the string and dispatch it
            String s = get();
            connection.processOutput(s);
            }
        }
    }
