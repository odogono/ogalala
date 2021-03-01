// $Id: WorldTest.java,v 1.51 1999/07/08 14:40:48 jim Exp $
// Test driver for Atom database
// James Fryer, 9 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.test.mua;

import java.io.*;
import java.util.*;
import com.ogalala.util.*;
import com.ogalala.mua.*;

public class WorldTest
    {
    /** Set this to true if you want to see the admin messages
    */
    private static final boolean ADMIN_LOG = true;
    
    private static void sendDebugCommands()
        {
//###            world.parseCommand("x", player);
		//world.parseCommand("!nibblers_addplayer alex", player);
		//world.parseCommand("!setplayer alex", player);
		//world.parseCommand("", player);
        }

    private static final String VERSION = "0.4";
    private static String fileName;
    private static World world;
    private static Parser parser;
    private static Atom player;
    private WorldTestFe fe = new WorldTestFe(); 
    
    // store the command history
    private static Vector commands = new Vector();
    
    // index into command history
    private static int commandIndex = 0;

    public WorldTest(String args[])
        throws IOException, WorldException
        {
        // Print banner message
        System.out.println("Ogalala World Database test front end. Version " + VERSION);
        System.out.println("Copyright (C) 1999 Ogalala Ltd. <www.ogalala.com>");
        System.out.println("Running java " + System.getProperty("java.version") + 
                " on " + System.getProperty("java.vendor"));
        
        // Get the arguments
        if (args.length < 2)
            {
            System.err.println("Usage: WORLDTEST filename paths");
            System.exit(1);
            }

        // Create or open the database, get the player mobile and parser
        startWorld(args[0], args[1]);
        
        // display the gui front end
        fe.show();

        // Send commands to the cli
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in), 1);
        String s;

        while ((s = in.readLine()) != null )
            parseCommand(s);
        
        stopWorld();
        waitForKeypress();
        }
    
    public static void main(String args[])
        throws IOException, WorldException
        {
        // Set up debug stuff
        Debug.logToFile(true);
        Debug.logToConsole(true);
        Debug.setAsserts(true);

        new WorldTest( args );
        }

    private static void waitForKeypress()
        {
        System.out.println("Press Return");
        try {
            System.in.read();
            }
        catch ( Exception e )
            {
            Debug.printStackTrace( e );
            }
        }
        
    public static void warning(String msg)
        {
        System.err.println("Warning: " + msg); 
        }

    public static void fatal(String msg)
        {
        System.err.println("Fatal error: " + msg);
        System.exit(1);
        }

    public static void startWorld(String fileNameArg, String paths)
        throws IOException, WorldException
        {
        fileName = fileNameArg;
        boolean exists = WorldFactory.exists(fileName);
        world = WorldFactory.newWorld(fileName, paths);
        world.setAdminLog(ADMIN_LOG);
        if (!exists)
            world.runStartupScripts();
        System.out.println((exists ? "Loaded" : " Created") + " world " + fileName);

        parser = world.newParser();
        player = world.getAtom(world.ADMIN_ID);
        
        world.setEventLog(true);
        world.setAdminLog(true);
        world.start();
        sendDebugCommands();
        }

    public static void stopWorld()
        throws IOException, WorldException
        {
        world.stop();
        WorldFactory.saveWorld(world);
        }

    private static void parseCommand(String command)
        {
		commands.addElement(command);
        commandIndex = commands.size();
        if (parseLocalCommand(command) == false)
            world.parseCommand(command, player, parser);
        }
    
    /** Parse a local test command -- these all start with '/'
    */
    private static boolean parseLocalCommand(String str)
        {
        // Only accept commands starting with '/'
        if (!str.startsWith("/"))
            return false;
        
        String command = StringUtil.getFirstWord(str).toLowerCase();
        String args = StringUtil.getTrailingWords(str);
        
        // Set WorldTest player
        if (command.equals("/setplayer"))
            {
    		Atom newPlayer = world.getAtom(args);
    		if( newPlayer == null)
	        	{
			    player.output("Invalid player.");
    			return true;
	        	}
            
            // Report to new and old players
    		player.output("Possesed " + newPlayer.getName());
    		newPlayer.output("Possesed " + player.getName());
    		
    		setPlayer( newPlayer );
	    	newPlayer.addWatcher( new ConsoleWatcher( player , true ) );

            return true;
            }
            
        // Export dynamic state
        else if (command.equals("/export"))
            {
            try {
                world.stop();
                WorldFactory.exportState(world, fileName);
                world.start();
                System.out.println("Exported dynamic state.");
                }
            catch (Exception e)
                {
                e.printStackTrace();
                warning("Error exporting world state: " + e);
                }
            return true;
            }
            
        // Import dynamic state
        else if (command.equals("/import"))
            {
            try {
                world.stop();
                WorldFactory.importState(world, fileName);
                System.out.println("Imported dynamic state.");
                world.start();
                }
            catch (Exception e)
                {
                e.printStackTrace();
                warning("Error importing world state: " + e);
                }
            return true;
            }
            
        else
            return false;
        }
     
    /**
    *	Sets the player under the consoles control to something
    *	other than the default of the admin.
    *	### Experimental
    *
    */
    public static void setPlayer(Atom player)
    {
    	WorldTest.player = player;    
    }
        
    // Inner classes for the Gui Frontend.
    class WorldTestFe extends java.awt.Frame
        {
        java.awt.TextField textIn;
        GuiAction guiAction = new GuiAction();
        GuiKeyListener keyListener = new GuiKeyListener();

        public WorldTestFe()
            {
            super( "Console in" );
            
        	SymWindow aSymWindow = new SymWindow();
	    	this.addWindowListener( aSymWindow );

            //setLayout(new BorderLayout(0,0));
            
            textIn = new java.awt.TextField();
            textIn.addActionListener( guiAction );
            textIn.addKeyListener( keyListener );
            add( textIn );
            setSize(400,50);

            }

        /** pass the command to the world parser
        */
        public void executeCommand( String command )
            {
            textIn.selectAll();
			parseCommand(command);
            }
        
        
        private void prevCommand()
            {
            if ( commandIndex <= 0 || commands == null )
                return;
                
            commandIndex--;
            
            textIn.setText( commands.elementAt(commandIndex).toString() );
            }
            
        private void lastCommand()
            {
            if ( ( commandIndex >= commands.size() - 1 ) || commands == null )
                return;

            commandIndex++;

            textIn.setText( commands.elementAt(commandIndex).toString() );
            }
        
        class GuiAction implements java.awt.event.ActionListener
        	{
    		public void actionPerformed( java.awt.event.ActionEvent event )
    	    	{
                String commandline = textIn.getText();
                executeCommand( commandline );
    		    }
        	}

         // recieve window eventd
         class SymWindow extends java.awt.event.WindowAdapter
        	{
    		public void windowClosing(java.awt.event.WindowEvent event)
    	    	{
    			Object object = event.getSource();
    			if ( object == WorldTestFe.this )
    				WindowClosing(event);
        		}
        	} 
        	
        // catch up / down 
        class GuiKeyListener extends java.awt.event.KeyAdapter
        	{
            public void keyPressed( java.awt.event.KeyEvent e )
                {
                if ( e.getKeyCode() == java.awt.event.KeyEvent.VK_UP )
                    {
                    prevCommand();
                    }
                else if ( e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN )
                    {
                    lastCommand();
                    }
                else super.keyPressed( e );
                }
        	}
        
        void WindowClosing(java.awt.event.WindowEvent event)
        	{
    		hide();		 // hide the Frame
			try {
		        stopWorld();
			    }
		   	catch ( IOException e ) 
			   	{
		   		System.err.println("IO error while closing DB");
                Debug.printStackTrace( e );
		   		}
		   	catch ( WorldException e ) 
			   	{
		   		System.err.println("World error while closing DB");
                Debug.printStackTrace( e );
		   		}
		   
    		System.exit(1);
    	    } 
        } 
    }

