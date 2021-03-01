// $Id: WorldFactory.java,v 1.13 1999/04/29 13:49:32 jim Exp $
// Manager for Worlds
// James Fryer, 16 March 99
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import java.io.*;

/** The World Factory is responsible for the loading and saving of the 
    world.
*/
public class WorldFactory
    {
    /** File name suffix for world database
    */
    public static final String SUFFIX = ".db";

    /** File name suffix for dynamic state files
    */
    public static final String EXPORT_SUFFIX = ".state";
    
    /** If a world 'name' exists, load it. If not, create a new world
        of that name.
    */
    public static World newWorld(String name, String paths)
        throws IOException, WorldException
        {
        if (exists(name))
            return loadWorld(name, paths);
        else
            return createWorld(name, paths);
        }

    /** Create a new world 'name'
    */
    public static World createWorld(String name, String paths)
        throws WorldException
        {
        String fileName = getFileName(name);
        World result = new World(removeSuffix(fileName, SUFFIX));
        if (paths != null)
            result.addPath(paths);
        result.init();   //### This should really be in the world constructor?
        return result;
        }

    /** Load an existing world 'name'
    */
    public static World loadWorld(String name, String paths)
        throws IOException
        {
        World result = null;
        try {
            String fileName = getFileName(name);
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
            result = (World)in.readObject();
            in.close();
			
			// Set the file name and paths of the newly loaded world.            
            result.setFileName(removeSuffix(name, SUFFIX));
            if (paths != null)
                result.addPath(paths);
            }
        catch (ClassNotFoundException e)
            {
            throw new IOException("Invalid World file: " + name);
            }

        return result;
        }

    /** Save a world
    */
    public static void saveWorld(World world)
        throws IOException
        {
        String fileName = getFileName(world.getFileName());
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(world);
        out.close();
        }

    /** Import dynamic state into an existing world.
        <p>
        Note that if the dynamic state file does not exist, no error occurs.
    */
    public static void importState(World world, String name)
        throws IOException
        {
        String fileName = getFileName(name, EXPORT_SUFFIX);
        if (new File(fileName).exists())
            {
            InputStream in = new FileInputStream(fileName);
            world.importState(in);
            in.close();
            }
        }
        
    /** Export dynamic state
    */
    public static void exportState(World world, String name)
        throws IOException
        {
        String fileName = getFileName(name, EXPORT_SUFFIX);
        OutputStream out = new FileOutputStream(fileName);
        world.exportState(out);
        out.close();
        }
        
    /** Does a world 'name' exist?
    */
    public static boolean exists(String name)
        {
        return new File(getFileName(name)).exists();
        }

    /** Ensure the file name ends in the default suffix
    */
    private static String getFileName(String result)
        {
        if (!result.endsWith(SUFFIX))
            result = result + SUFFIX;
        return result;
        }

    /** Ensure the file name ends in the supplied suffix
    */
    private static String getFileName(String result, String suffix)
        {
        // Remove the default suffix if present, then add the new one.
        result = removeSuffix(result, SUFFIX);
        if (!result.endsWith(suffix))
            result = result + suffix;
        return result;
        }
        
    private static String removeSuffix(String result, String suffix)
        {
        if (result.endsWith(suffix))
            result = result.substring(0, result.length() - suffix.length());
        return result;
        }
        
    /** Prevent instantiation
    */
    private WorldFactory()
        {
        }
    }
