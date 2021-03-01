// $Id: Database.java,v 1.3 1998/05/02 14:48:31 jim Exp $
// Database class for Death on the Nile
// James Fryer, 27 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.database;

import java.io.*;
import java.util.*;

/** The Database class represents the game database.
    <p>
    The database has the following states:
    <ul>
    <li>Does not exist -- the database has not been built.
    <li>Paused -- the database has been built, but 'start' has not
        been called, or 'stop' has been called.
    <li>Playing -- the game is playing.
    </ul>
*/
public class Database
    {
    // Database file version numbers
    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 1;

    // A list of all nouns in the game
    protected NounList nouns = new NounList(1000);

    // Flag the noun factory to invent an ID
    public static final String UNSPECIFIED_ID = "?";

    // The properties taxonomy
    //###

    // The database file name
    protected File fileName;

    // The sequence number used to generate unique item IDs
    protected int sequenceNumber = 0;

    /** Constructors
    */
    public Database(String fileName)
        {
        this.fileName = new File(fileName);
        //### Should eventually call 'read'???
        }

    /** Create an empty database
    */
    public static void create(String fileName) throws IOException
        {
        //### Future implementations (e.g. using a DBMS) may require this function
        //###   to be implemented more fully. This version simply creates an empty
        //###   file, so that subsequent calls to 'exists' will return 'true'.
        FileOutputStream out = new FileOutputStream(fileName);
        out.close();
        }

    /** Does the database exist?
    */
    public static boolean exists(String fileName)
        {
        File f = new File(fileName);
        return f.exists();
        }

    /** Delete a database
    */
    public static void delete(String fileName)
        {
        File f = new File(fileName);
        f.delete();
        }

    /** Read the database from disk
    */
    public void read() throws IOException, ClassNotFoundException
        {
        synchronized (nouns)
            {
            // Clear the current database contents
            nouns.clear();

            // Open the input file
            ObjectInputStream stream = new ObjectInputStream(new FileInputStream(fileName));

            // Read header data
            int majorVersion =  stream.readInt();
            int minorVersion =  stream.readInt();
            if (majorVersion != VERSION_MAJOR)
                throw (new IOException("Incompatible database version"));
            sequenceNumber = stream.readInt();

            // Get the number of nouns to read
            int nNouns = stream.readInt();

            // Read the nouns and add them to the list
            for (int i = 0; i < nNouns; i++)
                addNoun((Noun)stream.readObject());
            }
        }

    /** Write the database to disk
    */
    public void write() throws IOException
        {
        synchronized (nouns)
            {
            // Back up the previous data file
            //###

            // Create the output file
            ObjectOutput stream = new ObjectOutputStream(new FileOutputStream(fileName));

            // Write header data
            stream.writeInt(VERSION_MAJOR);
            stream.writeInt(VERSION_MINOR);
            stream.writeInt(sequenceNumber);

            // Say how many nouns we will be writing to the file
            stream.writeInt(nouns.size());

            // Write all the nouns to the output file
            Enumeration enum = nouns.elements();
            while (enum.hasMoreElements())
                stream.writeObject(enum.nextElement());

            stream.flush();
            }
        }

    /** Call 'start' on all the game objects
    */
    public void start()
        {
        synchronized (nouns)
            {
            Enumeration enum = nouns.elements();
            while (enum.hasMoreElements())
                {
                Noun noun = (Noun)enum.nextElement();
                noun.start();
                }
            }
        }

    /** Call 'stop' on all the game objects
    */
    public void stop()
        {
        synchronized (nouns)
            {
            Enumeration enum = nouns.elements();
            while (enum.hasMoreElements())
                {
                Noun noun = (Noun)enum.nextElement();
                noun.stop();
                }
            }
        }

    /** Noun factory function
        Return: newly created noun

        ### I have a feeling this function should not be in the database...
        ###     Should there be a 'NounFactory' class? Or should the Noun
        ###     have its own factory object? It's here for now though.
    */
    public Noun makeNoun(String className, String id, String creatorName)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException
        {
        // Make sure the ID is unique
        if (id.equals(UNSPECIFIED_ID))
            id = getUniqueID(className);

        else {
            //### Check for uniqueness
            }

        // Make the full class name
        //### Temporary implementation
        String fullClassName = "com.ogalala.nile.database." + className;

        // Get the class for the type of noun to be created
        Class nounClass = Class.forName(fullClassName);

        // Create an instance of the class
        //### Should we catch some of the exceptions here??
        Noun result = null;
        try {
            result = (Noun)nounClass.newInstance();
            }
        catch (NoSuchMethodError e)
            {
            // Convert this error to an exception
            throw new IllegalAccessException();
            }

        // If the class isn't a Noun, give up
        //### It would be better to do this *before* we create the result, but I can't figure out how to do it
        if (!(result instanceof Noun))
            throw new ClassNotFoundException(className + " is not a Noun");

        // Fill in the vital details of the class
        result.setID(id);
        result.setCreator(creatorName);

        // Add the noun to the database
        addNoun(result);

        return result;
        }

    /** Get a Noun by ID
    */
    public Noun getNoun(String nounID)
        {
        return nouns.findByID(nounID);
        }

    /** Add a Noun to the database
    */
    public void addNoun(Noun noun)
        {
        // Set the database field of the noun and add it to the list
        noun.database = this;
        nouns.add(noun);
        }

    /** Get an enumeration of the nouns
    */
    public Enumeration elements()
        {
        return nouns.elements();
        }

    /** Get a unique item ID
    */
    public String getUniqueID(String base)
        {
        synchronized (nouns)
            {
            while (true)
                {
                String result = base + "_" + sequenceNumber;
                sequenceNumber++;
                if (nouns.findByID(result) == null)
                    return result;
                }
            }
        }

    /** Get the database file name
    */
    public String getName()
        { return fileName.toString(); }

    }
