// $Id: CombatCommands.java,v 1.7 1999/03/31 15:54:18 alex Exp $
// Combat Commands. y'know, for killing and stuff.
// Alexander Veenendaal, 19th Feburary 1999
// Copyright (C) HotGen Ltd <www.hotgen.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;


/**
* A single, low damage blow
*
*/
public abstract class Attack extends JavaAction
{
	
	private static final long serialVersionUID = 1;
	
	/** Combat utility weapon
	*/
	protected Atom combatAtom;
	
	protected static final String PHYSICAL_ABILITY = "physical_ability";
	
	protected static final int STRIKE	= 10;
	protected static final int BEAT 	= 50;
	protected static final int MAIM 	= 75;
	protected static final int MURDER	= 100;
	
	//the type of attack we are engaging in
	protected int attackType = STRIKE;
	
	//variables for each participant in the combat
	protected int attackerPhysicalAbility;
	protected int victimPhysicalAbility;
	protected int attackerAttackBonus;
	protected int victimDefenceBonus;
	
    public boolean execute()
    {
    	//initialise all variables and conditions
    	init();
		
		//check whether the attacker is ready for combat
		if( isCombatReady() )
			return true;
		
    	//display the opening messages
    	
    	//attacks the victim. reducing their physical ability if neccesary
    	attackVictim();
        
        return true;
    }
    
    
	/**
	* Initialises the various variables
	*/
	private void init()
	{
		//retrieve the atom which holds the details to do with the game.
		combatAtom = world.getAtom("dotn_combat");
		
		attackerPhysicalAbility = actor.getInt(PHYSICAL_ABILITY);
		victimPhysicalAbility = current.getInt(PHYSICAL_ABILITY);
		
		//initialise the attack bonus by seeing whether a weapon was specified
		// (... with <weapon>). If no weapon was specified, it is assumed that
		// the attacker is using their hands.
		if( event.getArg() != null )
			attackerAttackBonus = ((Atom)event.getArg()).getInt("attack_bonus");
		else
			attackerAttackBonus = 0;
		
		//figure out the defence bonus of the victim
		victimDefenceBonus = sumVictimDefenceBonus();
	}
	
	
	/**
	* checks whether the actor is resting after a bout of attack
	*/
	protected boolean isCombatReady()
	{
		if( actor.getBool("combat_resting") )
			return false;
		return true;
	}
	
	/**
	* complete the attack.
	* figure out the victims damage rating.
	*/
	protected void attackVictim()
	{
		/*
		int attack = computeTotalDamage( attackerPhysicalAbility, attackerAttackBonus, victimPhysicalAbility, victimDefenceBonus );
		
		if( attack < 10 )
		
		else if( attack < 20 )
		
		else if( attack < 30 )
		
		else if( attack < 40 )
		
		else
		//*/
	}
	
	/**
	* Works out the total defense bonus by summing
	* each of the victims items defence bonus'
	*/
	protected int sumVictimDefenceBonus()
	{
		Atom item;
		ContentsEnumeration contents = new ContentsEnumeration( current );
		int totalDefence = 0;
		
		while( contents.hasMoreElements() )
		{
			item = (Atom)contents.nextElement();
			
			totalDefence += item.getInt("defence_bonus");
		}
		
		return totalDefence;
	}
    /**
    *
    * @param attackerPA					the attackers Physical Ability
    * @param attackerAB					the attackers Attack Bonus
    * @param victimPA					the victims Physical Ability
    * @param victimDB					the victims Defence Bonus
    */
    protected final static int computeTotalDamage(int attackerPA, int attackerAB, int victimPA, int victimDB)
    {
    	//get the actors physical ability and add it to the attack bonus
    	// of any weapon, for the Total Attack.
    	int totalAttack = attackerPA + attackerAB;

    	//add the victims Physical Ability and Defence Bonus of any weapon
    	// or armour, for the Total Defence
    	int totalDefence = victimPA + victimDB;

    	//subtract Total Defense from Total Attack to determine the Attack
    	// adjustment. Then add a random number (1-100) to determine the
    	// attack rating.
    	return (totalAttack - totalDefence) + Dice.roll(100);
    }


}


/**
* Attempt to knock the victim down to half their remaining PA
*
*/
public class Beat extends JavaAction
{
	private static final long serialVersionUID = 1;
	
    public boolean execute()
    {
        actor.output("Attack is not yet implemented.");
        return true;
    }
}

/**
* Attempt to knock victim down to a quarter of their remaining PA
*
*/
public class Maim extends JavaAction
{
	private static final long serialVersionUID = 1;
	
    public boolean execute()
    {
        actor.output("Attack is not yet implemented.");
        return true;
    }
}

/**
* Attempt to rob victim of all remaining PA, thus killing them
*
*/
public class Murder extends JavaAction
{
	private static final long serialVersionUID = 1;
	
    public boolean execute()
    {
        actor.output("Attack is not yet implemented.");
        return true;
    }
}

public class PickPocket extends JavaAction
{
	
	private static final long serialVersionUID = 1;
	
	/** The perpetrators Criminal Status
	*/
	int perpCS;
	
	/** The victims Social (Detective) Status
	*/
	int victimSS;
	
	/** The target of the light fingeredness.
	*/
	Atom victim;
	
	/** This atom contains a lot of useful stuff to do with pickpocketing
	*/
	Atom pickPocket;// = world.getAtom("pickpocket");

	/** The maximum size a pickpocketed item can be.
	*/
	int itemSizeThreshold;
	
	public boolean execute()
	{
		init();
		
		if( attemptPickPocket() )
		{
			//steal an item at random and report its results
			stealRandomItem();
			return true;
		}
		else if( didVictimNotice() )
		{
			//inform the victim that an attempt has been made on their pockets !
			victim.output( pickPocket.getString("victim_noticed_smsg") );
				
			//inform the perp that their attempt failed.
			actor.output( pickPocket.getString("victim_noticed_msg") );
		
			return true;
		}
		
		//the attempt completely failed. report back.
		actor.output( pickPocket.getString("attempt_failed_msg") )
		;
		
		return true;
	}
	
	private final void init()
	{
		//initialise the perps criminal skill
		perpCS = actor.getInt("criminal_skill");
		
		//initialise our victim
		victim = (Atom)current;
		
		//intialise the victims social skill
		victimSS = victim.getInt("social_skill");
	
		//get a reference to the atom which contains various bits of information
		pickPocket = world.getAtom("pickpocket");
	
		//get the maximum size a pickpocketable item can be
		itemSizeThreshold = pickPocket.getInt("item_size_threshold");
		
	}
    
    
	/**
	* Performs a calculation for the success of the pickpocket manoeuvre
	*/
	private final boolean attemptPickPocket()
	{
		if( pickPocketCalculation(perpCS, victimSS) )
			return true;
		else
			return false;
	}
	
	/**
	* goes through the victims inventory, and randomly picks out an item
	* to steal.
	*/
	private final void stealRandomItem()
	{
		Vector victimContents = (Vector)AtomData.enumToVector(victim.getContents());
		
		//the victim may not have anything to steal
		if( victimContents.size() == 0 )
		{
			actor.output( pickPocket.getString("attempt_failed_msg") );
			return;
		}
		
		int random = Dice.roll(victimContents.size());
		
		if( random >= victimContents.size() )
			random -= 1;
			
		//select a random item from the inventory
		Atom stolenItem = (Atom)victimContents.elementAt( random );
		
		//if the item selected was too big, the return in failure.
		if( stolenItem.getInt("size") > itemSizeThreshold )
		{
			actor.output( pickPocket.getString("attempt_failed_msg") );
			return;
		}
		
		//move it into the perps inventory
		world.moveAtom( stolenItem, actor );
		
		//report the actors new find
		actor.output("You manage to pickpocket a " + stolenItem.getString("name") + " from " + victim.getString("name") );
		
		return;
	}
	
	/** 
	* performs a die roll to see whether the victim notices
	*
	* @return		true if the victim did notice, false otherwise
	*/
	private final boolean didVictimNotice()
	{
		if( pickPocketCalculation(perpCS, victimSS) )
			return false;
		else
			return true;
	}
	
	/**
	* The main logic for the pickpocketing action
	*/
	private final static boolean pickPocketCalculation(int perpCS, int victimSS)
	{
		int modifier = ((perpCS / 10) - (victimSS / 10));
		int success = 50 + modifier;
		
		int randomRoll = Dice.roll(100);
		
		if( randomRoll < success )
			return true;
		else
			return false;
	}
}


