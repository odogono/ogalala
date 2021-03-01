// $Id: MechanicalCommands.java,v 1.17 1999/03/11 10:41:08 alex Exp $
// Commands which involve read world Mechanical actions
// Alexander Veenendaal, 1 December 1998
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;



/**
* You can only burn things WITH things.
*/
public class Burn extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		if( event.getArg() == null )
		{
			actor.output("What do you want to " + event.getID() + " the " + current.getString("name") + " with ?");
		}
		actor.output("Burn is not yet implemented.");
		return true;
	}
}



/**
*
*/
public class Ignite extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		actor.output("Ignite is not yet implemented.");
    	return true;
    }
}

/**
* The action associated with Illuminatable
* When the Illuminate action is called, a check is made to see whether the light
*  is currently on or off.
* If the light is off, it is turned on, and the enclosing containers ambient_light
*  property is increased by the light_emitted value.
* If the light is on, it is turned off, and the enclosing containers ambient_light
*  property is decreased by the light_emitted value.
*/
public class Illuminate extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
    	//Get the state
    	boolean illuminate_state = current.getBool("state");
    	
    	//Get the lights emitted value
    	int light_emitted = current.getInt("light_emitted");
    	
    	//Get the containers current light level
    	int ambient_light = current.getContainer().getInt("ambient_light");
    	
    	//if the light is off
		if(!illuminate_state)
		{
			//switch the light on
			current.setBool("state", true);
			
			//add the light_emitted to the containers ambient_light property
			current.getContainer().setInt("ambient_light", ambient_light + light_emitted);
			
			//tell everyone in the room about it
			container.output( current.getString("illuminate_on_msg") );
		}
		
		//if the light is off
		else
		{
			//switch the light off
			current.setBool("state", false);
			
			//remove the light_emitted from the containers ambient_light property
			current.getContainer().setInt("ambient_light", ambient_light - light_emitted);
			
			//tell everyone in the room about it
			container.output( current.getString("illuminate_off_msg") );
		}
		
        return true;
    }
}

public class Load extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		Atom supply = (Atom)event.getArg();
		
		Atom currentAmmoType = current.getAtom("ammunition_type");
		
		int currentAmmoCount = current.getInt("ammunition_count");
		
		//we need something to load with !
		if( supply == null )
		{
			actor.output( current.getString("load_arg_missing_msg") );
			return true;
		}
		
		//check that we are loading the correct ammo type
		if( !supply.isDescendantOf( current.getAtom("ammunition_taken") ) )
		{
			actor.output( current.getString("wrong_ammo_type_msg") );
			return true;
		}
		
		//check that if there is ammo already loaded, that the new ammo is of the same type
		if( currentAmmoType != null )
		{
			if( ! current.isDescendantOf( currentAmmoType ) )
			{
				//tell the actor that you can't load the weapon with a differing ammo type
				actor.output( current.getString("wrong_existing_ammo_msg") );
				return true;
			}	
		}
		else
		{
			//set the type of ammo
			current.setAtom("ammunition_type", supply);
		}
		//load the ammo into the weapon
		int supplyAmmoCount = supply.getInt("ammunition_count");
		//otherwise, the ammo type is the same, so add the ammo to the weapon
		current.setInt("ammunition_count", currentAmmoCount + supplyAmmoCount);
		
		//tell everyone what happened
		actor.output( current.getString("load_msg") );
		container.output( current.getString("load_omsg"), actor);
		
		//destroy the ammo
		world.moveAtom(supply, world.getAtom("limbo") );
		
		return true;
	}
}

/**
* 
*	
*/
public class Photograph extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		boolean photographRoom = false;
		
		if( event.getArg() == null )
			photographRoom = true;
		
		//get the thing to photograph
		Atom targetToPhoto;
		
		//its the room
		if( photographRoom )
			targetToPhoto = actor.getContainer();
		//its some specified object
		else
			targetToPhoto = (Atom)event.getArg();
		
		//you cannot take a photograph of the camera you are using !
		if( targetToPhoto.getID() == current.getID() )
		{
			actor.output( current.getString("photograph_self_msg") );
			return true;
		}
		
		//create the photo
		Atom photograph = world.newThing(  null, current.getAtom("film") );
		
		//set its description
		photograph.setString("description", "A photograph of " + targetToPhoto.getString("name") );
		
		//set the location
		world.moveAtom(photograph, actor);
		
		//display the relevent messages
		actor.output( current.getString("photograph_msg") );
		container.output( current.getString("photograph_omsg"),actor );
		
		return true;
	}
}

public class Shoot extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		//what we are shooting at
		Atom targetToShoot = (Atom)event.getArg();
		
		//The actor must be at least a being, otherwise their skill won't be
		// evaluated.
		if( !actor.isDescendantOf(world.getAtom("being")) )
		{
			actor.output( current.getString("not_a_being_msg") );
			return true;
		}
		
		//we need something to shoot at !
		if( targetToShoot == null )
		{
			actor.output( current.getString("shoot_arg_missing_msg") );
			return true;
		}
		
		//Make sure we are not attempting to shoot a Character
		if( targetToShoot.isDescendantOf( world.getAtom("character") ) )
		{
			actor.output( current.getString("attack_character_msg") );
			return true;
		}
		
		int currentAmmoCount = current.getInt("ammunition_count");
		
		//Check that this shooter isn't out of ammo
		if( current.getAtom("ammunition_type") != null && currentAmmoCount <= 0 )
		{
			actor.output( current.getString("out_of_ammo_msg") );
			return true;
		}
		
		//get the damage level of the bullet loaded in the weapon
		int bullet_damage = (current.getAtom("ammunition_type")).getInt("damage");
		
		if( targetToShoot.isDescendantOf( world.getAtom("being") ) )
		{
			//display the attacking messages
			actor.output( current.getString("attack_being_msg") );
			targetToShoot.output( current.getString("attack_being_smsg") );
			container.output( current.getString("attack_being_omsg"),actor, targetToShoot );
		}
		else
		{
			//display the attacking messages
			actor.output( current.getString("attack_thing_msg") );
			targetToShoot.output( current.getString("attack_thing_smsg") );
			container.output( current.getString("attack_thing_omsg"),actor, targetToShoot );
		}
		//the skill rating of the actor at this operation
		int shooting_skill = actor.getInt("shooting_weapon_skill");
		
		//get the current hitpoints of the target
		int targetHitPoints = targetToShoot.getInt("hit_points");
		
		//get the size of the target
		int targetSize = targetToShoot.getInt("size");
		
		// figure out whether we hit or missed
		int targetPostDamage = GameEquations.shootTarget( bullet_damage, shooting_skill, targetHitPoints, targetSize );
		
		//set the new hit level
		targetToShoot.setInt("hit_points", targetPostDamage);
		
		//decrement the ammo count
		currentAmmoCount -= 1;
		
		//check whether we are out of ammo
		if(currentAmmoCount <= 0)
		{
			//set the bullet type to null
			current.setAtom("ammunition_type", null);
		}
		
		//refresh the amount of ammo in the gun
		current.setInt("ammunition_count", currentAmmoCount);
		
		//check whether the targets hit points where completely zapped by the shot
		if( targetPostDamage <= 0 )
		{
			//if what was shot was sentient, show some messages
			if( targetToShoot.isDescendantOf( world.getAtom("being") ) )
			{
				//show the relevent messages
				actor.output( current.getString("being_die_msg") );
				targetToShoot.output( current.getString("being_die_smsg") );
				container.output( current.getString("being_die_omsg"),actor, targetToShoot );
				
				//at this point, you would change the being to a dead being....
				return true;
			}
			else
			{
				//show the relevent messages
				actor.output( current.getString("thing_die_msg") );
				container.output( current.getString("thing_die_omsg"),actor, targetToShoot );
				
				//effectively destroy this thing. should eventually be replaced by rubble or something
				world.moveAtom( targetToShoot, world.getAtom("limbo") );
				
				return true;
			}
		}
		return true;
	}
}


public class Switch extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public static final String ON_CHANGESTATE = "on_changestate";
	
    public boolean execute()
    {
    	//get the list of listeners
    	Vector listeners = current.getVector("listeners");
    	
    	if(listeners != null && listeners.size() > 0)
    	{
	    	//tell the actor
	    	actor.output( current.getString("switch_msg") );
	    	
	    	//tell everyone else
	    	container.output( current.getString("switch_omsg"), actor);
    	}
    	else
    	{
    		actor.output( current.getString("switch_fail_msg") );
    		return true;
    	}
    	
    	//step through each of the listeners, activating their changestate handler
    	for(int i=0;i<listeners.size();i++)
    		world.callEvent(actor, ON_CHANGESTATE, (Atom)listeners.elementAt(i) );
    	    	
        return true;
    }
}
