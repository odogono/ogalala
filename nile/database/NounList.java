// $Id: NounList.java,v 1.3 1998/05/02 14:48:31 jim Exp $
// List of Noun objects
// James Fryer, 2 April 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.database;

import java.util.*;

/** The NounList is a utility class used when a collection of Nouns is
    required. Functions are supplied to find nouns by ID and by name -- this
    may result in more than one noun being found, as names are not unique.
    <p>
    Note that the NounList is not itself a Noun.
*/
public class NounList
    {
    // The noun collection
    //### This should be a tree structure.
    private Hashtable nouns;

    public NounList(int capacity)
        {
        nouns = new Hashtable(capacity);
        }

    public NounList()
        {
        this(20);
        }

    public void add(Noun noun)
        {
        // ASSERT(noun != null);

        synchronized (this)
            {
            nouns.put(noun.getID(), noun);
            }
        }

    /** Find the noun that has an ID matching 'nounID'
        Return the noun, or null if not found.
    */
    public Noun findByID(String nounID)
        {
        synchronized (this)
            {
            return (Noun)nouns.get(nounID);
            }
        }

    /** Find one or more nouns with a name matching 'nounName'
        Return a (possibly empty) new NounList containing the found nouns.
    */
    public NounList findByName(String nounName)
        {
        return findByName(nounName, null);
        }

    /** Find one or more nouns with a name matching 'nounName'
        Return a (possibly empty) new NounList containing the found nouns.
    */
    public NounList findByName(String nounName, NounList result)
        {
        // Create a new list to return, if necessary
        if (result == null)
            result = new NounList();

        // Loop through the list adding matching nouns to the result list
        //### There has to be a better algorithm for this!!
        //###   However, improvements must also handle things such as adverbs...
        //### Possibly the sequential search is not too bad if there are generally
        //###   few items in the list. Time (and profiling) will tell.
        synchronized (this)
            {
            Enumeration enum = nouns.elements();
            while (enum.hasMoreElements())
                {
                Noun noun = (Noun)enum.nextElement();
                if (noun.equalsName(nounName))
                    result.add(noun);
                }
            }

        return result;
        }

    /** If there is only one noun in the list, this is a handy way to get it.
        Return the only noun in the list, or null if there is more than one noun

        //### I am not very keen on this system, perhaps it would be better to
        //###   have a function 'findUniqueByName' instead???
    */
    public Noun getNoun()
        {
        Noun result = null;

        synchronized (this)
            {
            if (nouns.size() == 1)
                {
                Enumeration enum = nouns.elements();
                result = (Noun)enum.nextElement();
                }
            }

        return result;
        }

    /** Get an enumeration for the list
    */
    public Enumeration elements()
        {
        return nouns.elements();
        }

    /** How many nouns are in the list?
    */
    public int size()
        {
        return nouns.size();
        }

    /** Remove all nouns
    */
    public void clear()
        {
        synchronized (this)
            {
            nouns.clear();
            }
        }
    }
