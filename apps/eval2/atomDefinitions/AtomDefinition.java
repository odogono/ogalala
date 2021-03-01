// $Id: AtomDefinition.java,v 1.19 1998/08/19 16:42:13 rich Exp $
// 
// Richard Morgan, 31 July 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;
import com.ogalala.mua.*;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;


public class Breakable 
{
	/*
	!set breakable.fix !Fix
	!set breakable.is_broken = false
	!set breakable.fixedName = "a unbroken thing."
	!set breakable.fixedDescription = "It looks you you could break it."
	!set breakable.fixedLongDescription = "It looks you you could break it."
	!set breakable.brokenName = "a broken thing."
	!set breakable.brokenDescription = "it so broken you cannot tell what it was."
	!set breakable.brokenLongDescription = "it so broken up so much you don't know what it was."
	!set breakable.break_msg = "the {name} breaks"
	!set breakable.break_omsg = "the {name} breaks"
	!set breakable.fix_msg = "the {name} is fixed"
	!set breakable.fix_omsg = "the {name} is fixed"
	!set breakable.is_already_broken_msg = "the {name} is already broken."
	!set breakable.is_already_fixed_msg = "the {name} does not need fixing."
	
	*/


	public static final String FIXED_NAME 				= "fixedName".intern();
	public static final String IS_BROKEN 				= "is_broken".intern();
	public static final String FIXED_DESCRIPTION 		= "fixed_description".intern();
	public static final String FIXED_LONG_DESCRIPTION 	= "fixed_description".intern();
	public static final String BROKEN_NAME 				= "broken_name".intern();
	public static final String BROKEN_DESCRIPTION 		= "broken_description".intern();
	public static final String BROKEN_LONG_DESCRIPTION 	= "broken_description".intern();
	public static final String BREAK_MSG				= "break_msg".intern();
	public static final String BREAK_OMSG 				= "break_omsg".intern();
	public static final String FIX_MSG		 			= "fix_msg".intern();
	public static final String FIX_OMSG		 		= "fix_omsg".intern();
	public static final String IS_ALREADY_BROKEN_MSG= "is_already_broken_msg".intern();
	public static final String IS_ALREADY_FIXED_MSG	= "is_already_fixed_msg".intern();
	// actions
//###	public static final Break breakfunction = null;
	
/*	static 
	{
		addAtomDef( atomName, new Breakable() );
	}
	
	// initilizer
	Breakable()
	{
        fields = new Vector( 10 );
        fields.addElement( IS_BROKEN );
		fields.addElement( FIXED_NAME );
		fields.addElement( FIXED_DESCRIPTION );
		fields.addElement( BROKEN_NAME );
		fields.addElement( BROKEN_DESCRIPTION );
		fields.addElement( BREAK_MSG );
		fields.addElement( BREAK_OMSG ); 
		fields.addElement( FIX_MSG );		 	
		fields.addElement( FIX_OMSG	);
		fields.addElement( IS_ALREADY_BROKEN_MSG );		 	
		fields.addElement( IS_ALREADY_FIXED_MSG	);
	}
*/	
}

public class Described 
{
	/*
	!atom described
	!set described.name "thing"
	!set described.description "thing"
	!set described.long_description ""
	*/
	public static final String NAME = "name".intern();
	public static final String DESCRIPTION = "description".intern();
	public static final String LONG_DESCRIPTION = "long_description".intern();

}

public class Mirror 
{
	/*
	!set mirror.view_msg "You look into the mirror and see:"
	!set mirror.view_omsg "{name} looks into the mirror"
	*/
	public static final String VIEW_MSG = "view_msg".intern();
	public static final String VIEW_OMSG = "view_omsg".intern();
	
}

public class OpenableWithObject 
{
	/*
	!openable_with_object key null
	!openable_with_object open_fail_msg "You need something to help you open this. "
	*/
	public static final String atomName = "openable_with_object".intern();

	public static final String KEY_OBJECT = "key_atom".intern();
	public static final String OPEN_FAIL_MSG = "open_fail_msg".intern();
}

public class ContainerDef 
{
	/*
	!set container.capacity 1
	!set container.enter_omsg "{-u actor.name} has arrived."
	!set container.exit_omsg "{-u actor.name} has left."
	!set container.look !LookContainer
	!set container.put_in_msg "You put {-t arg.name} in {-t name}.
	!set container.put_in_omsg "{-u actor.name} puts {-t arg.name} in {-t name}.
	!set container.get_out_msg "You get {-t arg.name} from {-t name}.
	!set container.get_out_omsg "{-u actor.name} gets {-t arg.name} from {-t name}.
	!set container.put_in !PutIn
	!set container.get_out !GetOut
	!set container.too_big_msg "{-ut arg.name} is too big to fit in {-t name}."
	!set container.contents_suffix_msg "(in {-t name})"
	*/
	public static final String CAPACITY = "capacity".intern();
	public static final String ENTER_OMSG = "enter_omsg".intern();
	public static final String EXIT_OMSG = "exit_omsg".intern();
	public static final String LOOK = "look".intern();
	public static final String PUT_IN_MSG = "put_in_msg".intern();
	public static final String PUT_IN_OMSG = "put_in_omsg".intern();
	public static final String GET_OUT_MSG = "get_out_msg".intern();
	public static final String GET_OUT_OMSG = "get_out_omsg".intern();
	public static final String PUT_IN = "put_in".intern();
	public static final String GET_OUT = "get_out".intern();
	public static final String TOO_BIG_MSG = "too_big_msg".intern();
	public static final String CONTENTS_SUFFIX_MSG = "contents_suffix_msg".intern();
	public static final String IS_TRANSPARENT = "is_transparent".intern();
}
	
public class LimitedContainer 
{
	/*
	!atom limited_container container
	!set envelope.put_in !PutLimited
	!set envelope.contains_atoms $root
	!set container.too_big_msg "You can only put {contains_atoms.name} or {his} children into an {-t name}"
	*/
	
	public static final String CONTAINS_ATOMS = "contains_atoms";

}

public class ThingDef
{
	/*
	!set thing.look !LookThing
	!set thing.size 0
	!set thing.hit_points 0
	!set thing.is_quiet false
	!set thing.is_unique false
	*/

	public static final String LOOK = "look".intern();
	public static final String SIZE = "size".intern();
	public static final String HIT_POINTS = "hit_points".intern();
	public static final String IS_QUIET = "is_quiet".intern();

}

public class Seat
{
    /*
    !set seat.stand_fail_msg "You are not sitting on anything."
    */
    public static final String atomName = "seat".intern();
    
    public static final String STAND_FAIL_MSG = "stand_fail_msg".intern();
}

public class Weapon
{
/*
	!set weapon.name "weapon"
	!set weapon.description "A generic weapon"
	!set weapon.attack "You cannot attack with {name}"
	!set weapon.attack_success_msg "you attack and hit {-u target.name} with {-t name}"
	!set weapon.attack_success_smsg "you are attacked and hurt by {-u target.name} with {-t name}"
	!set weapon.attack_success_omsg "{-u actor.name} attacks and hits {-u target.name} with {-t name}"
	!set weapon.attack_fail_msg "you attack and miss {-u target.name} with {-t name}"
	!set weapon.attack_fail_smsg "you are attacked and but not hurt by {-u target.name} with {-t name}"
	!set weapon.attack_fail_omsg "{-u actor.name} attacks and misses {-u target.name} with {-t name}"
	!set weapon.attack_damage 0
*/

	public static final String ATTACK_SUCCESS_MSG = "attack_success_msg".intern();
	public static final String ATTACK_SUCCESS_OMSG = "attack_success_omsg".intern();
	public static final String ATTACK_SUCCESS_SMSG = "attack_success_smsg".intern();
	public static final String ATTACK_FAIL_MSG = "attack_fail_msg".intern();
	public static final String ATTACK_FAIL_OMSG = "attack_fail_omsg".intern();
	public static final String ATTACK_FAIL_SMSG = "attack_fail_smsg".intern();
	public static final String ATTACK_FINGER_PRINT = "attack_finger_print".intern();

	public static final String ATTACK_DAMAGE = "attack_damage";

}

public class Openable 
{
	public static final String atomName = "read-able".intern();

    public static final String OPEN = "open".intern();
    public static final String CLOSE = "close".intern();
    public static final String MSG = "open_msg".intern();
    public static final String OMSG = "open_omsg".intern();
    public static final String ALREADY_OPEN_MSG = "already_open_msg".intern();
    public static final String IS_CLOSED = "is_closed".intern();
}

public class Readable 
{
	public static final String atomName = "readable".intern();

	public static final String READ = "read".intern();
	public static final String MSG = "read_msg".intern();
	public static final String OMSG = "read_omsg".intern();
	public static final String TEXT = "read_text".intern();
	public static final String IS_BLANK_MSG = "reading_omsg".intern();

}

public class Smelly 
{
	public static final String atomName = "smelly".intern();

	public static final String SMELL = "smell".intern();
	public static final String MSG = "smell_msg".intern();
	public static final String OMSG = "smell_omsg".intern();
	public static final String SUCCESS_MSG = "smell_success_msg".intern();
	public static final String FAIL_MSG = "smell_fail_msg".intern();

}

public class Textured  
{
	public static final String FEEL = "feel".intern();
	public static final String MSG = "textured_msg".intern();
	public static final String OMSG = "textured_omsg".intern();
	public static final String RESULT_MSG = "textured_result_msg".intern();
}

public class Tastable 
{
	public static final String TASTE = "taste".intern();
	public static final String MSG = "taste_msg".intern();
	public static final String OMSG = "taste_omsg".intern();
	public static final String SUCCESS_MSG = "taste_success_msg".intern();
	public static final String FAIL_MSG = "taste_fail_msg".intern();
}

public class MobileDef
{
	/*
	!set mobile.strength 10
	!set mobile.look !LookMobile
	!set mobile.size 1
	!set mobile.is_mortal true
	!set mobile.death_cry_msg "Your eyes glaze over as life slowing drains out of you. You have died."
	!set mobile.death_cry_omsg 
	*/
	public static final String STRENGTH = "strength".intern();
	public static final String IS_MORTAL = "is_mortal".intern();
	public static final String DEATH_CRY_MSG = "death_cry_msg".intern();
	public static final String DEATH_CRY_OMSG = "death_cry_omsg".intern();
	public static final String DEATH_ROOM = "death_room".intern();

	public static final String MURDER_MSG = "murder_msg".intern();
}

public class Corpse
{
	public static final String atomName = "corpse".intern();
	public static final String OLD_NAME = "old_name".intern();
	public static final String CAUSE_OF_DEATH = "cause_of_death".intern();
}

