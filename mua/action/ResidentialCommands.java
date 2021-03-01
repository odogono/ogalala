// $Id: ResidentialCommands.java,v 1.8 1999/03/12 17:48:56 alex Exp $
// Commands which involve residential matters
// Alexander Veenendaal, 3 Janurary 1999
// Copyright (C) HotGen Studios Ltd <www.hotgen.com>


package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;


/**
* Converts a standard room into an atrium for a specified
* number of residences.
*
* Takes the following arguments:
* <atriumAtom> - An already existing room
* <residenceCount> - number of residences to generate.
* <porchAtom> - a template porch to duplicate
* <porchOrientation> - Direction of the atrium from the porch ( one of n,w,s,e )
*/
public class ModGenerateAtrium extends JavaAction
{
	private static final long serialVersionUID = 1;
	
	protected static final String PORCH_TABLE = "porch_table";
	protected static final String HOME_NUMBER = "home_numberHOME_NUMBER";
	protected static final String HOME_COUNT = "home_count";
	
	protected static final String ATRIUM = "atrium";
	protected static final String PORCHEXIT = "porchExit";
	protected static final String EXIT = "exit";
	
    public boolean execute()
    {
    	//make sure we are getting enough
    	if( event.getArgCount() < 3 )
    	{	
    		actor.output("Usage !GenerateAtrium <atrium> <homeCount> <porch> <atriumDirection>");
    		return true;
    	}
    	
    	//crack open and check the arguments
    	// the intended atrium room. must not inherit from atrium
    	Atom atrium = world.getAtom( (String)event.getArg() );
    	// the number of homes to create
    	int homeCount = new Integer((String)event.getArg(1)).intValue();
    	// a specified atom which will make up the porchs
    	Atom porchAtom = world.getAtom( (String)event.getArg(2) );
    	// direction of the atrium from the porch (looked up from the Exittable)
    	int porchExit = ExitTable.toDirection( (String)event.getArg(3) );
    	
    	//get the container of the roomAtom. Thats the place we will put the porches into
	    Atom atriumArea = atrium.getContainer();
	    
    	//check all of the arguments are ok
    	if( !validateArguments(atrium, homeCount, porchAtom, porchExit) )
	    	return true;
	    	
	    //add atrium as a parent to the roomAtom
	    atrium.inherit( world.getAtom(ATRIUM) );
	  
	  	//contains a list of all the added porches for the atriums use.
	  	Dictionary directory = new Hashtable();
	  	
	  	//generate the porches
	    for(int i=0;i<homeCount;i++)
	    {
	    	Atom clonedPorch = generatePorch(porchAtom, atrium, atriumArea, porchExit);
	    	//set the 'street' number
	    	clonedPorch.setString("name", "Outside apartment " + (i+1) );
	    	clonedPorch.setInt(HOME_NUMBER, i+1);
	    	clonedPorch.setString("description", clonedPorch.getString("description_root") + (i+1) );
	    	
	    	//add the reference of this porch to the atrium list
	    	directory.put( new Integer(clonedPorch.getInt(HOME_NUMBER)), clonedPorch.getID()  ); 
	    }
	    
	    //set the directory on the porch
	    atrium.setTable(PORCH_TABLE, directory);

	    //set the number of porches generated
	    atrium.setInt(HOME_COUNT, homeCount);
	    
	    actor.output("Atrium generated on " + atrium.getString("name") + " with " + homeCount + " porches");
        return true;
    }
	
	
	/**
	* Clones a porch and attaches an exit from it to a supplied atrium
	*/
	protected Atom generatePorch(Atom porchAtom, Atom atrium, Atom atriumArea, int porchExit)
	{
		//clone the porchAtom just once to generate our master
	    Atom porch = world.deepCloneAtom(porchAtom);
	    //move the clone to a more appropriate place
	    world.moveAtom(porch, atriumArea);
    	
    	//generate the actual exit
	    String s = world.getUniqueID(PORCHEXIT);
	    Atom exit = world.newThing(s, world.getAtom(EXIT));
	    com.ogalala.util.Debug.assert(exit != null, "(ResidentialCommands.java/69)");
	    //move it into the cloned porch
	    world.moveAtom(exit, porch);
	    world.addExit(porchExit, porch, exit, atrium);
	    
	    //set the atrium field on our new porch so that it points towards the supplied atrium reference
	    porch.setAtom(ATRIUM, atrium);
	    
	    //set a field pointing towards the exit to the atrium
	    porch.setAtom("atriumExit", exit);
	    
	    return porch;
	}
	
	
	/**
	* Checks that the given arguments are acceptable; reports if they are not
	*/
	protected boolean validateArguments(Atom roomAtom, int homeCount, Atom porchAtom, int orientation)
	{
		//the room that becomes an atrium cannot already be one
		if( roomAtom == null || roomAtom.isDescendantOf( world.getAtom(ATRIUM) ) )
    	{
    		actor.output("the proposed atrium either does not exist, or it is not valid.");
    		return false;
    	}
    	if( homeCount < 1 )
    	{
    		actor.output("Non-valid value for homeCount");
    		return false;
    	}
    	if( porchAtom == null )
    	{
    		actor.output("the proposed porch atom either does not exist, or is not valid.");
    		return false;
    	}
    	if( !porchAtom.isDescendantOf( world.getAtom("room") ) )
    	{
    		actor.output("the proposed porch atom must inherit from room");
    		return false;
    	}
    	if( !porchAtom.isDescendantOf( world.getAtom("porch") ) )
    	{
    		actor.output("the proposed porch atom must inherit from porch");
    		return false;
    	}
    	if( orientation == -1 )
    	{
    		actor.output("Non-valid value for porchOrientation");
    		return false;
    	}
    	//everythings ok !
    	return true;
	}
	
}

/**
* Removes atrium capabilites from the specified room
* Works through the following steps.
* loops through each of the atriums porches :
*		deletes any associated room places
*		deletes porches themselves
* uninherits atrium from the atrium atom.
*/
public class ModRemoveAtrium extends JavaAction
{
	private static final long serialVersionUID = 1;
	
	public boolean execute()
	{
		//make sure we are getting enough
    	if( event.getArgCount() < 1 )
    	{	
    		actor.output("Usage !RemoveAtrium <atrium>");
    		return true;
    	}
    	
		actor.output("RemoveAtrium is not yet implemented.");
		return true;
	}
}

/**
*
* Unfortunatly, AtriumGo is sufficiently different that we can't extend from Go
*/
public class AtriumGo extends JavaAction
{
	private static final long serialVersionUID = 1;
	
	protected static final String PORCH_TABLE = "porch_table";
	protected static final String NO_PORCH_MSG = "no_porch_msg";
	protected static final String GO_MSG = "go_msg";
	protected static final String LOOK = "look";
	protected static final String ON_ENTER = "on_enter";
    protected static final String ON_EXIT = "on_exit";
    protected static final String ENTER_OMSG = "enter_omsg";
    protected static final String EXIT_OMSG = "exit_omsg";
    
	public boolean execute()
	{
		//get the home number from the passed event
		Integer homeNumber = (Integer)event.getArg();
		
		//get the atrium the actor is currently standing in
		Atom atrium  = actor.getContainer();
		
		//extract the porch table ( a list of ap numbers and porch room references )
		Dictionary porchTable = atrium.getTable(PORCH_TABLE);
		
		//check whether this table is even valid
		if ( porchTable == null )
		{
			actor.output( atrium.getString(NO_PORCH_MSG) );
			return true;
		}
		
		//attempt to get the porch room ref from the table
		String porchString = (String)porchTable.get(homeNumber);
		
		//check whether this porch exists
		if ( porchString == null )
		{
			actor.output( atrium.getString(NO_PORCH_MSG) );
			return true;
		}
		
		Atom porchRoom = world.getAtom( porchString );
		
		//check whether this porch exists again
		if ( porchRoom == null )
		{
			actor.output( atrium.getString(NO_PORCH_MSG) );
			return true;
		}
		
		// Ask the atrium container if we can leave it
        if (!world.callEvent(actor, ON_EXIT, atrium))
            return true;
        
        // See if we can enter the 'to' container
        if (!world.callEvent(actor, ON_ENTER, porchRoom))
            return true;
            
		//move the actor to the returned porch
		world.moveAtom( actor, porchRoom );
		actor.output(current.getString(GO_MSG));
		
		// Notify actor/users :
        //  - If the exit has an exit message display it
        //  - Display the "to" container's enter message if there is one
        String s = atrium.getString(EXIT_OMSG);
        if (s != null)
        	atrium.output(s, actor);
		
		s = porchRoom.getString(ENTER_OMSG);
		if (s != null)
			porchRoom.output(s, actor);
        
        //all done ! Display the new room description.
		return world.callEvent(actor, LOOK, world.getRoot());
	}
}

/**

*/



/**
* Creates a habitation off an atrium
*
* !createhome <atrium> <number> <home place> <apartment entrance>
*  where atrium is the hub off which the home will be located
*  the home place which will be moved to the atriums container
*/
public class ModCreateHome extends JavaAction
{
	private static final long serialVersionUID = 1;
	
	protected static final String PORCH_TABLE = "porch_table";
	protected static final String ATRIUM = "atrium";
	protected static final String NO_PORCH_MSG = "no_porch_msg";
	protected static final String HOME = "home";
	protected static final String HOME_COUNT = "home_count";
	protected static final String PORCHEXIT = "porchExit";
	protected static final String HOME_EXIT_TYPE = "homeExitType";
	
	protected static String INVALID_EXIT_MSG = "invalid_exit_msg";
	
	//the atrium to which this home will live off
	protected Atom atrium;
	
	//the proposed number this home will 'reside' at
	protected Integer homeNumber;
	
	//the porch that the home will hang off
	protected Atom porchRoom;
	
	//the place that encloses the home
	protected Atom homePlace;
	
	//the room in the entrance that serves as the entry hall
	protected Atom homeEntrance;
	
	//the exit type between the home and the porch
	protected Atom homeExitAtom;
	
	public boolean execute()
	{	
    	//make sure we are getting enough
    	if( event.getArgCount() < 4 )
    	{	
    		actor.output("Usage !CreateResidence <atrium> <home place> <home entrance> <home exit atom> <number>");
    		return true;
    	}
		
		//---------------------------------------------------------------------------------
		//check the atrium reference is correct; does it exist and is it actually an atrium
		
		atrium = world.getAtom( (String)event.getArg() );
		
		if( atrium == null )
		{
			actor.output("atrium does not exist.");
			return true;
		}
		
		if( !atrium.isDescendantOf( world.getAtom(ATRIUM) ) )
		{
			actor.output("proposed atrium does not inherit from atrium.");
			return true;
		}
		
		//--------------------------------------------------------------------------------
		//extract the home Number from the supplied arguments
		
		//a home number has been specified
		if( event.getArgCount() == 5 )
	    	//check that the home number exists and is not already used.
			homeNumber = new Integer((String)event.getArg(4));
		else
			//just get the first number available.
			homeNumber = getFirstEmptyPorchNumber(atrium);
		
		if( homeNumber == null || homeNumber.intValue() > atrium.getInt(HOME_COUNT) )
		{
			actor.output( atrium.getString(NO_PORCH_MSG) );
			return true;
		}
		
		//--------------------------------------------------------------------------------
		//extract the porchRoom from the atrium and see if we can attach our room
		
		//get the porchtable off of the atrium
		Dictionary porchTable = atrium.getTable(PORCH_TABLE);
		
		//check whether this table is even valid
		if ( porchTable == null )
		{
			actor.output( atrium.getString(NO_PORCH_MSG) );
			return true;
		}
		
		//attempt to get the porch room ref from the table
		String porchString = (String)porchTable.get(homeNumber);
		
		//check whether this porch exists
		if ( porchString == null )
		{
			actor.output( atrium.getString(NO_PORCH_MSG) );
			return true;
		}
		
		//The porch Room is the porch that will connect the home with the outside world
		porchRoom = world.getAtom( porchString );
		
		//check whether this porch exists again
		if ( porchRoom == null )
		{
			actor.output( atrium.getString(NO_PORCH_MSG) );
			return false;
		}
			
		//check whether the porch has a home attached
		if( (porchRoom.getAtom(HOME) != null) )
		{
			actor.output("Residence already attached at this number.");
			return true;
		}
		
		//--------------------------------------------------------------------------------
		//get the exit type between the home and the porch
		homeExitAtom = world.getAtom( (String)event.getArg(3) );
		
		if( homeExitAtom == null )
		{
			actor.output( atrium.getString(INVALID_EXIT_MSG) );
			return true;
		}
		
		
		
		//---------------------------------------------------------------------------------
		//check that the home place exists
		
		homePlace = world.getAtom( (String)event.getArg(1) );
		if( homePlace == null )
		{
			actor.output("proposed home place does not exist.");
			return true;
		}
		
		//---------------------------------------------------------------------------------
		//check that the home entrance exists
		homeEntrance = world.getAtom( (String)event.getArg(2) );
		if( homeEntrance == null )
		{
			actor.output("proposed home entry room does not exist.");
			return true;
		}
		
		//---------------------------------------------------------------------------------
		//move the home place into the atriums container
		world.moveAtom( homePlace, atrium.getContainer() );
		
		
		//---------------------------------------------------------------------------------
		//modify the exitable on the porch to point to the home entrance
		connectResidenceToPorch();
		
		//set the home place on the porch atom
		porchRoom.setAtom("home", homePlace);
		
		actor.output("Residence number " + homeNumber.toString() + " created on " + atrium.getString("name") + ".");
		return true;
	}
	
	
	/**
	* Extracts an empty Porch Number randomly from a specified atrium.
	*
	* @returns				the number of an empty porch, or null if there are none.
	*/
	public Integer getEmptyPorchNumber(Atom atrium)
	{
		//get the total number of porches off of this atrium
		int homeCount = atrium.getInt(ModGenerateAtrium.HOME_COUNT);
		
		//get the porchtable off of the atrium
		Dictionary porchTable = atrium.getTable(PORCH_TABLE);
		
		//check whether this table is even valid
		if ( porchTable == null )
			return null;
		
		String porchString;
		Atom porchRoom;
		
		//try homeCount/2 times
		for(int i=0;i<homeCount/2;i++)
		{
			//get the string name of a porch from the atriums porchtable
			porchString = (String)porchTable.get( new Integer(Dice.roll(homeCount)) );
			
			//if its null, then try again with another
			if( porchString == null )
				continue;
			
			//get the porch atom 
			porchRoom = world.getAtom( porchString );
			
			if( porchRoom == null )
				continue;
			
			//check whether the porch has a home defined on it
			if( porchRoom.getAtom(HOME) == null)
				return new Integer(i);
		}
		//we have failed :(
		return null;
	}
	
	/**
	* Returns the first empty porch number
	*/
	public Integer getFirstEmptyPorchNumber(Atom atrium)
	{
		//get the total number of porches off of this atrium
		int homeCount = atrium.getInt(ModGenerateAtrium.HOME_COUNT);
		
		//get the porchtable off of the atrium
		Dictionary porchTable = atrium.getTable(PORCH_TABLE);
		
		//check whether this table is even valid
		if ( porchTable == null )
			return null;
		
		String porchString;
		Atom porchRoom;
		
		for(int i=1;i<homeCount+1;i++)
		{
			//get the string name of a porch from the atriums porchtable
			porchString = (String)porchTable.get(new Integer(i));
			
			//if its null, then try again with another
			if( porchString == null )
				continue;
			
			//get the porch atom 
			porchRoom = world.getAtom( porchString );
			
			if( porchRoom == null )
				continue;
			
			//check whether the porch has a home defined on it
			if( porchRoom.getAtom(HOME) == null)
				return new Integer(i);
		}
		//we have failed :(
		return null;
	}
	
	
	
	
	/**
	* Connects the newly created Residence to the porch
	*/
	protected void connectResidenceToPorch()
	{
		//get the exit to the atrium
		Atom porchToAtriumExit = porchRoom.getAtom("atriumExit");
		
		//get the direction that is leading, and convert that to the opposite for our entry direction
		int direction = ExitTable.getOppositeDirection( ExitTable.toDirection(porchToAtriumExit.getString("direction")) );
		
		//generate the exit for the porch to the homes entrace
	    String s = world.getUniqueID(PORCHEXIT);
	    Atom pExit = world.newThing(s, homeExitAtom);
	    //Atom pExit = world.newThing(s, porchRoom.getAtom(HOME_EXIT_TYPE) );
	    com.ogalala.util.Debug.assert(pExit != null, "(ResidentialCommands.java/69)");
	    
	    //set the property on the porch 
	    porchRoom.setAtom("homeExit", pExit);
	    
	    //move the exit into the porch
	    world.moveAtom(pExit, porchRoom);
	    
	    //generate the exit from the home to the porch
	    String t = world.getUniqueID(PORCHEXIT);
	    Atom rExit = world.newThing(t, homeExitAtom);
	    //Atom rExit = world.newThing(t, porchRoom.getAtom(HOME_EXIT_TYPE) );
	    com.ogalala.util.Debug.assert(rExit != null, "(ResidentialCommands.java/69)");
	    
	    //move the exit into the home
	    world.moveAtom(rExit, porchRoom);
	    
	    // generate an exit in the specified direction, from the porchRoom, with the exit, to the homeEntrance
	    world.addExit(direction, porchRoom, pExit, ExitTable.getOppositeDirection(direction), homeEntrance, rExit);
	    
		return;
	}
}




/**
* Removes the habitation from existence.
*/
public class ModRemoveHome extends JavaAction
{
	private static final long serialVersionUID = 1;
	
	public boolean execute()
	{
		actor.output("RemoveHome is not yet implemented.");
		return true;
	}
}
