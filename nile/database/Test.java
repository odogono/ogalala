// $Id: Test.java,v 1.3 1998/05/02 14:48:31 jim Exp $
// Test driver for 1932.com database
// James Fryer, 27 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.database;

import java.util.*;

class Test
    {
    static final String FILE_NAME = "junk";

    public static void main(String args[])
        {
        // Create a database to output
        createDb();
        Database dbOut = new Database(FILE_NAME);
        createNouns(dbOut);
        writeDb(dbOut);
        printDb(dbOut);
        dbOut = null;

        // Now try and read it back in
        Database dbIn = new Database(FILE_NAME);
        readDb(dbIn);
        printDb(dbIn);
        }

    private static void createNouns(Database db)
        {
        for (int i = 0; i < 10; i++)
            {
            String iAsString = Integer.toString(i);
            String name = new String(iAsString + " XXX");
            String description = new String(iAsString + " abc " + iAsString + " abc " + iAsString + " abc ");

            Noun noun = null;
            try { //###
                noun = db.makeNoun("Noun", "?", "test");
                }
            catch (Exception e)
                {
                fatal("createNouns: " + e);
                }
            noun.setName(name);
            noun.setDescription(description);
            db.addNoun(noun);
            }
        }

    private static void createDb()
        {
        try {
            Database.create(FILE_NAME);
            }
        catch (Exception e)
            {
            fatal("createDb: " + e);
            }
        }

    private static void writeDb(Database db)
        {
        try {
            db.write();
            }
        catch (Exception e)
            {
            fatal("writeDb: " + e);
            }
        }

    private static void readDb(Database db)
        {
        try {
            db.read();
            }
        catch (Exception e)
            {
            fatal("readDb: " + e);
            }
        }

    private static void printDb(Database db)
        {
        Enumeration enum = db.elements();
        while (enum.hasMoreElements())
            {
            Noun noun = (Noun)enum.nextElement();
            System.out.println("Noun [" + noun.getID() + "] " + noun.getName() + " / " + noun.getDescription());
            }
        }

    private static void fatal(String msg)
        {
        System.err.println("ERROR: " + msg);
        System.exit(1);
        }
    }