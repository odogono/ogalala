// $Id: GameEquations.java,v 1.3 1999/01/22 17:03:07 alex Exp $
// Game Equations
// Alexander Veenendaal, 2nd December 1998
// Copyright (C) Ogalala Ltd <www.ogalala.com>


package com.ogalala.mua.action;



public class GameEquations
{
	/**
	* @returns				the new hit points of the thing shot
	*/
	public static final int shootTarget( int bullet_damage, int shooting_skill, int target_hitpoints, int target_size )
	{
		//return (bullet_damage * shooting_skill) - (target_hitpoints * target_size);
		return 0;
	}
    
    /**
    *
    */
    public static final int attackBeing(  int attackerStrength, int weaponModifier, int defenderStrength, int defenderHitPoints)
    {
        
        return 0; // returns the defenders hit points
    }
    
    
    public static final void poisonConsume()
    {
        
    }
    
    public static final void evaluateStats()
    {
        
    }
    
}