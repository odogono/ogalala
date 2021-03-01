// $Id: KnifeAttack.java,v 1.7 1998/08/20 12:52:40 rich Exp $
// 
// Richard Morgan 17 Aug 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import com.ogalala.mua.*;
import com.ogalala.util.Debug;
import java.util.Enumeration;

public class Attack
    extends JavaAction
{
    public void execute(World world, Mobile attacker, Atom weaponAtom, Container room, Event event)
    {
    	// get the target
    	Atom atomTarget = (Atom)event.getArg(0);
		Mobile target = null;
		try 
		{
			target = ( Mobile ) atomTarget;
		}
		catch ( ClassCastException e )
		{
			event.setResult("You cannot attack that!");
		}

		// get the weapon ### should this catch casts exceptions too?
		Thing weapon = ( Thing ) weaponAtom;
		

		Debug.assert( target != null, "null pointer to target. Attack.execute");
    
		if ( tryAttack( target, world, attacker, weapon, room, event ) ) 
		{
			doDamageAndReport( target, world, attacker, weapon, room, event ); 	
		}
		else
		{
			// report miss to observes
			room.output( new Output( "misc","msg",event.formatOutput( weapon.getString( Weapon.ATTACK_FAIL_OMSG ) ) ), event, attacker, target );		

			// report to target
			target.output( new Output( "misc","msg",event.formatOutput( weapon.getString( Weapon.ATTACK_FAIL_SMSG ) ) ), event );					

			// report to attacker
			event.setResult( weapon.getString( Weapon.ATTACK_FAIL_MSG ) );
		}
    }

	/** try to attack, returns true if we hit false if we miss
	*/
	boolean tryAttack( Mobile target, World world, Mobile actor, Thing weapon, Container container, Event event )
	{
		// 50 / 50
		return ( AtomUtil.rollDice(6) > 3 );
	}

	/** do the damage to the target. For now this moves the target to the death room
	*/
	void doDamageAndReport( Mobile target, World world, Mobile actor, Thing weapon, Container container, Event event )
	{
		// event.setResult("bugger all has happened, but you hit the target!!!!");
		doDeath( target, world, actor, weapon, container, event );
	}    

	void doDeath( Mobile victim, World world, Mobile murderer, Thing weapon, Container room, Event event )
	{
		// is the victim mortal??
		if ( victim.getBool( MobileDef.IS_MORTAL ) )
		{
			// report death to victim
			Output vistimMsg = new Output( "misc","msg",event.formatOutput( victim.getString( MobileDef.DEATH_CRY_MSG ) ) );
			victim.output( vistimMsg, event );
			
			// report death to observers
			Output omsg = new Output( "misc","msg",event.formatOutput( victim.getString( MobileDef.DEATH_CRY_OMSG ) ) );
			room.output( omsg, event, victim );
	
			// move victim to death room
			Container deathRoom = ( Container ) victim.getAtom( MobileDef.DEATH_ROOM );
			deathRoom.add( (Thing) victim );
			
			// make the corpse
            String corpseID = world.getDatabase().getUniqueID( Corpse.atomName );
            Atom corpseParent = world.getDatabase().getAtom( Corpse.atomName );
            Thing corpse = ( Thing )world.getDatabase().newAtom( corpseID, "Thing", corpseParent);

			// set the name of the corpse
			corpse.setString( Corpse.OLD_NAME, victim.getString( Described.NAME ) );
			// set the style of death to the corpse
			corpse.setString( Corpse.CAUSE_OF_DEATH, weapon.getString( Weapon.ATTACK_FINGER_PRINT ) );

			// replace with a corpse 
            room.add( corpse );
            
			// move contents of victim to floor
			Enumeration enum = victim.getContents();			
			while ( enum.hasMoreElements() )
			{
				room.add( ( Thing ) enum.nextElement() );
			}	

			// report death to murderer
			event.setResult( murderer.getString( MobileDef.MURDER_MSG ) );

		}	
		else 
		{
			event.setResult("You can't just go around killing imortals you know ...");
		}
	}
}


