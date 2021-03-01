// $Id: OpenableLockable.java,v 1.13 1999/03/04 16:43:02 jim Exp $
// Openable and lockable
// Richard Morgan, date unknown
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;


class Openable implements Serializable
    {
    // state toggle
	protected static String IS_CLOSED = "is_closed";

    // messages for opening
	protected static String OPEN_OMSG = "open_omsg";
	protected static String OPEN_MSG = "open_msg";
	protected static String ALREADY_OPEN_MSG = "already_open_msg";

    // messages for closing
    protected static String CLOSE_OMSG = "close_omsg";
    protected static String CLOSE_MSG = "close_msg";
    protected static String ALREADY_CLOSED_MSG = "already_closed_msg";

    // hooks for other actions
    protected static String ON_OPEN = "on_open";
    protected static String ON_CLOSE = "on_close";

    public String already_open_msg( Atom atom )
        {return atom.getString( ALREADY_OPEN_MSG );}

    public String already_closed_msg( Atom atom )
        {return atom.getString( ALREADY_CLOSED_MSG );}

    public String open_omsg( Atom atom )
        {return atom.getString( OPEN_OMSG );}

    public String open_msg( Atom atom )
        {return atom.getString( OPEN_MSG );}

    public String close_msg( Atom atom )
        {return atom.getString( CLOSE_MSG );}

    public String close_omsg( Atom atom )
        {return atom.getString( CLOSE_OMSG );}

    /** Is the state of the atom open?
    */
    public boolean isOpen ( Atom atom )
        {return !atom.getBool( IS_CLOSED );}

    public void open( Atom atom )
        {atom.setBool( IS_CLOSED, false );}

    public void close( Atom atom )
        {atom.setBool( IS_CLOSED, true );}
    }

class Lockable implements Serializable
    {
	protected static String KEY = "key";        
	protected static String IS_LOCKED = "is_locked";
    protected static String LOCK_MSG = "lock_msg";
    protected static String LOCK_OMSG = "lock_omsg";
    protected static String UNLOCK_MSG = "unlock_msg";
    protected static String UNLOCK_OMSG = "unlock_omsg";
    protected static String OPEN_FAIL_MSG = "open_fail_msg";
    protected static String CLOSE_FAIL_MSG = "close_fail_msg";
    protected static String LOCKABLE_WHEN_OPEN = "lockable_when_open";
    protected static String ALREADY_LOCKED_MSG = "already_locked_msg";
    protected static String ALREADY_UNLOCKED_MSG = "already_unlocked_msg";
    protected static String KEY_CODE = "key_code";
    protected static String WRONG_KEY_MSG = "wrong_key_msg";

    public String open_fail_msg( Atom atom )
        {return atom.getString( OPEN_FAIL_MSG );}

    public String close_fail_msg( Atom atom )
        {return atom.getString( CLOSE_FAIL_MSG );}

    public String unlock_msg( Atom atom )
        {return atom.getString( UNLOCK_MSG );}

    public String unlock_omsg( Atom atom )
        {return atom.getString( UNLOCK_OMSG );}
    
    public String lock_msg( Atom atom )
        {return atom.getString( LOCK_MSG );}

    public String lock_omsg( Atom atom )
        {return atom.getString( LOCK_OMSG );}

    public String already_locked_msg( Atom atom )
        {return atom.getString( ALREADY_LOCKED_MSG );}

    public String already_unlocked_msg( Atom atom )
        {return atom.getString( ALREADY_UNLOCKED_MSG );}

    public String keyDoesNotFit( Atom lock, Atom key )
        {return lock.getString(WRONG_KEY_MSG);}

    public boolean isLocked ( Atom atom )
        {return atom.getBool( IS_LOCKED );}

    public void lock ( Atom atom )
        {atom.setBool( IS_LOCKED, true );}

    public void unlock ( Atom atom )
        {atom.setBool( IS_LOCKED, false );}
    
    /** In order to open a lock, the key must:
        <ul>
        <li> Be a descendant of the lock's 'key' property AND
        <li> Have a 'key_code' of 0 OR
        <li> Have a 'key_code' equal to the lock's 'key_code'
        </ul>
    */
    public boolean tryKey( Atom lock, Atom key )
        {
        Atom fittingKey = lock.getAtom( KEY );
        if ( !key.isDescendantOf( fittingKey ) )
            return false;
        int lockCode = lock.getInt(KEY_CODE);
        int keyCode = key.getInt(KEY_CODE);
        return keyCode == 0 || keyCode == lockCode;
        }
    }

public abstract class SimpleAction
    extends JavaAction
    {
	/**
	*/
	public boolean execute()
	    {
	    // do any initaliastion
	    init();    
	        
	    if ( !beforeAction() ) 
	        {
	        // action handled
	        return true;    
	        }

		// try the action
		if ( tryAction() )
   			{
			// the action was successfull
			successAction();
    		return true;
    		}
		else
			{
			// the action failed
    		failedAction();
    		return true;
			}
        }

	/** if the action works return true if it fails return false.
	    however it should fail in a test the action itself should
	    just happen, however I'm gonna leave this to see if it 
	    becomes useful.
	*/
	protected abstract boolean tryAction();

	/** This means the action has failed output messages
	    should go here. This may not get used ....
	*/
	protected void failedAction() {}	    

	/** This means the action has worked, output messages
	    should go here.
	*/
	protected void successAction() {}

	/** hook to fit in tests before an action. 
	    If this function returns false we break out of the action
	    with no message.
	*/
	protected boolean beforeAction()
	    {return true;};
	    
	/** init function to init stuff before the action
	*/
	protected void init() {}
    }

/** This is an outline of generic action for actions
	like open and close.
*/
public abstract class CheckedAction
    extends SimpleAction
	{

	/** we break the before action into two parts
	*/
	public boolean beforeAction()
	    {
	    // hook to break out before the test
	    if ( !beforeTest() ) return false; 
	        
	    if ( test() ) 
	        {
            // passed the test hook to do somefink
            successfulTest();
	        return afterTest();
	        }
        else
            {
            // failed the test hook to do somefink
            return failedTest();
    		}
	    }

	/** hook for fitting in more tests b4 this one. 
	    return true to countinue
	*/
	protected boolean beforeTest()
	    { return true; }
	
	/** do the test before execution of the action,
	    true continues , false fails
	*/
	protected boolean test()
	    { return true; }

	/** the test has failed, return false if you
	    want to break out of the action.
	*/
	protected boolean failedTest()
	    {
        // break out of action
	    return false;
	    }

	/** the test has passed, hook here to do somefink like output
	*/
	protected void successfulTest() {}
	
	/** hook to fix in another test after this one.
	*/
	protected boolean afterTest() { return true; }
	}

/**
*/
public class Open
	extends CheckedAction
	{
    protected Openable openable = new Openable();

	/** is the object already open? return true if it is closed
	*/
	protected boolean test()
		{
		return !openable.isOpen( current );
		}

	/** do the open action, switch the state of current to open
	*/
	protected boolean tryAction()
	    {
        openable.open( current );
	    return true;
	    }

	/** the do da is now open, inform the parties
	*/
	protected void successAction()
		{
		actor.output( openable.open_msg( current ) );
		container.output( openable.open_omsg( current ) , actor );
		}

	/** tell the actor they have failed to open the dodah as it is already open
	*/
	protected boolean failedTest()
		{
		actor.output( openable.already_open_msg( current ) );
		return false;
		}
	}

/**
*/
public class Close
	extends CheckedAction
	{
    protected Openable openable = new Openable();

	/** is the object already closed? return true if the object is open
	*/
	protected boolean test()
		{
		return openable.isOpen( current );
		}

	/** do the close 
	*/
	protected boolean tryAction()
	    {
        openable.close( current );
	    return true;
	    }

	/** the doAction is succesfull
	*/
	protected void successAction()
		{
		actor.output( openable.close_msg( current ) );
		container.output( openable.close_omsg( current ), actor );
		}

	/**
	*/
	protected boolean failedTest()
		{
		actor.output( openable.already_closed_msg( current ) );
		return false;
		}

	/** the do action cannot fail in this class so no code here ...
	*/
	protected void failedAction() {}
	}

/** this class handles the common functionality between lock and unlock. 
    This whether the object is closed and whether the key fits the lock.
*/
public abstract class Locks
    extends CheckedAction
    {
    protected Lockable lockable = new Lockable();
    protected Openable openable = new Openable();
    
    /** the key for the lock
    */
	protected Atom key;

	/** fetch the key we are using
	*/
	protected void init()
	    {
        // Get the object we are moving
        key = (Atom) event.getArg(0);
        }

	/** does the key fit the lock?
	*/
	protected boolean isFittingKey()
		{
		return lockable.tryKey( current, key );
		}

    /** is the do da closed?
    */ 
    protected boolean beforeTest()
        {
        if ( openable.isOpen( current ) )
            {
            return notClosed();
            }
        else
            {
            return isClosed();    
            }
        }
    
    /** the object needs to be closed for locking or unlocking.
    */
    protected boolean notClosed()
        {
        // ## better string handing req.
        actor.output("You need to close the {name} before you can lock or unlock it.");
        // break out of action
        return false;
        }

    /** the object is closed, does the key fit into the lock?
    */
    protected boolean isClosed()
        {
        if ( isFittingKey() )
            {
            return keyFits();    
            }
        else 
            {
            // key does not fit we should consider breaking out of the action
            return keyDoesNotFit();    
            }
        }
    
    /** the key didn't fits report and quit action
    */
    protected boolean keyDoesNotFit()
        {
		actor.output( lockable.keyDoesNotFit( current, key ) );
        // break out of action
        return false;
        }
        
    /** the key fits the lock, maybe we should have a messages
    */
    protected boolean keyFits()
        {
        return true;
        }
    }

/** something can only be lock while it is shut.
    the order of checking goes. isShut, does the key fit? isLocked
*/
public class Lock
	extends Locks
	{
	/** is the lock already locked ? return true if it is unlocked
	*/
	protected boolean test()
		{
		return !lockable.isLocked( current );
		}

	/** do the unlock action
	*/
	protected boolean tryAction()
	    {
        lockable.lock( current );
        return true;
	    }

	/** the doAction is succesfull
	*/
	protected void successAction()
		{
		actor.output( lockable.lock_msg( current ) );
		container.output( lockable.lock_omsg( current ) , actor );
		}

	/** the do da is already lock stupid!
	*/
	protected boolean failedTest()
		{
		// tell the actor they have failed
		actor.output( lockable.already_locked_msg( current ) );
        // break out!!!
		return false;
        }
	}

/**
*/
public class Unlock
	extends Locks
	{

	/** the do da is already lock stupid!
	*/
	protected boolean failedTest()
		{
		// tell the actor they have failed
		actor.output( lockable.already_locked_msg( current ) );
        // break out!!!
        return false;
        }

	/** do the unlock action
	*/
	protected boolean tryAction()
	    {
        lockable.unlock( current );
        return true;
	    }

    /** is the lock already unlocked? return true if its locked
    */
    protected boolean test()
        {
        return lockable.isLocked( current );
        }

	/** the doAction is succesfull
	*/
	protected void successAction()
		{
		actor.output( lockable.unlock_msg( current ) );
		container.output( lockable.unlock_omsg( current ) , actor );
		}
	}


/** open command thats checks to see if the object is locked b4 opening
    we do this by overiding after test. The order of the checks is 
    isOpen? isLocked?
*/
public class OpenLock
    extends Open
    {
    protected Lockable lockable = new Lockable();

	/** is the object locked shut?
	*/
    protected boolean afterTest()
        {
        if ( lockable.isLocked( current ) )
            {
            return locked();    
            }
        else
            {
            return unlocked();    
            }
        }

	/** tell the actor they have failed to open the do da as it is locked
	*/
    protected boolean locked()
        {
        actor.output( lockable.open_fail_msg( current ) );
        return false;    
        }

	/** 
	*/
    protected boolean unlocked()
        {
        return true;    
        }

	/** do the open action, switch the state of current to open
	*/
	protected boolean tryAction()
	    {
        openable.open( current );
	    return true;
	    }
    }

public class OpenWithKey
    extends JavaAction
    {
    protected Lockable lockable = new Lockable();
        
    public boolean execute()
        {
        if ( lockable.isLocked( current ) )
            world.callEvent( actor, "unlock", current, event.getArgs() );

        // magic call to Open
        return world.callEvent( actor, "open", current );
        }
    }
