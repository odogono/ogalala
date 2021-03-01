// $Id: UserRecord.java,v 1.21 1998/11/03 16:57:46 jim Exp $
// UserRecord, an api repesenting a record in the database
// @author Richard Morgan, 18 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.database;

import COM.odi.util.*;
import COM.odi.*;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

//import com.ogalala.server.User;
import com.ogalala.util.*;

/**	This class forms the API to a user record with in the database. The class contains
	 a hash table that store all the information for the user. There are
	 provided API's to alter perminant fields: password, privilege and account Status.
	 Otherwise this class mimics hashtable there but with a few limitations added. You cannot
	 alter the perminant fields these HAVE to be altered via the provided API. It also blocks
	 you from adding keys that are reserved and you cannot retrieve the password via get. <p>

	 The userid is perminate thoughout the life of the object, you cannot change this value it
	 is set on construction of the class.

	$Id: UserRecord.java,v 1.21 1998/11/03 16:57:46 jim Exp $
*/
public class UserRecord
{
	// the data is held here
	private Hashtable hash;

	// the load factor for the hashtable, should be a prime number
	private static final int SIZE = 23;

	/* The range of values for the 'account status'
	*/
	public final static int ACTIVE = 0;
	public final static int PENDING = 1;
	public final static int BANNED = 2;
	public final static int EXPIRED = 3;

	private String accoutStatusStrings[] = {"Active","Pending","Banned","Expired"};

	/** The login name this is the primary field the database is searched and ordered on.
		This field can contain no white space and there will probelly be a limit to
		how many charactors the string can contain.
	*/
	private final String userId;

	private UserDatabase db = null;

	private GlobalKeys gKeys = null;

	/** Create a new user record
		 @param userId the 'key' for this record
		 @param password the password for this user
		 @param userDatabase backpointer to the database
	*/
	protected UserRecord(String userIdIN, String password, UserDatabase userDatabase) throws BadRecordException
	{
		hash = new Hashtable(SIZE);
		this.userId = userIdIN;
		this.db = userDatabase;
		try
		{
			setPassword(password);
			setAccountStatus(PENDING);
			setPrivilege(Privilege.GUEST);
		}
		catch ( InvalidValueException e )
		{
			e.printStackTrace();
			throw new BadRecordException(e.toString());
		}
	}

	/** Get the user id (login name) for this account.
		 @return The userId / login name
	*/
	public String getUserId()
	{
		return userId;
	}


	public int getAccountStatus() throws NullPointerException
	{
		Integer accountStatus = (Integer)hash.get(GlobalKeys.ACCOUNT_STATUS);
		if ( accountStatus != null )
		{
			return accountStatus.intValue();
		}
		else
		{
			throw new NullPointerException("no account status");
		}
	}

	public void setAccountStatus(int accountStatus) throws InvalidValueException
	{
		if ( accountStatus != ACTIVE && accountStatus != PENDING && accountStatus != EXPIRED && accountStatus != BANNED )
		{
			throw new InvalidValueException("invalid value for account status");
		}

		// check that the value is valid
		hash.put(GlobalKeys.ACCOUNT_STATUS,new Integer(accountStatus));
	}

	/** Get the encripted password.
		@return The password + userId hash
	*/
	public String getPassword() throws NullPointerException
	{
		String password = (String)hash.get(GlobalKeys.PASSWORD);

		if ( password == null )
		{
			throw new NullPointerException("no password");
		}

		return password;
	}

	/** Sets a new encripted userId+password hash.<p>
		 note: this function trims the password.
	*/
	public void setPassword(String password) throws InvalidValueException
	{
		if ( password == null || password.trim().length() == 0 )
			throw new InvalidValueException( "Invalid Password" );
		hash.put( GlobalKeys.PASSWORD, password.trim() );
	}

	/** Get the privileges for this user account.
		@return a integer value representing one of the following.<p>
		@see com.ogalala.server.database.Privlege.GUEST<br>
		@see com.ogalala.server.database.Privlege.USER<br>
		@see com.ogalala.server.database.Privlege.MODERATOR<br>
		@see com.ogalala.server.database.Privlege.PROGRAMMER<br>
	*/
	public int getPrivilege() throws NullPointerException
	{
		String str = hash.get( GlobalKeys.PRIVILEGE ).toString();
		if ( str != null )
		{
			return Privilege.getValue( str );
		}
		else
		{
			throw new NullPointerException ( "Internal error: No Privileges set" );
		}
	}

	public int getPrivilegeString() throws NullPointerException
	{
		Integer as = ( Integer ) hash.get( GlobalKeys.PRIVILEGE );
		if ( as != null )
		{
			return as.intValue();
		}
		else
		{
			throw new NullPointerException ( "Internal error: No Privileges set" );
		}
	}


	/** Set the privileges available for this user record.
	  @param value Do not use integer values for this parameter it may throw an expection,
	  instead please use values from @see com.ogalala.User.PROGRAMMER
	  @return true if the new privilege setting is valid, false if the setting was invalid
  */
	public void setPrivilege( int value ) throws InvalidValueException
	{
		if ( Privilege.contains( value ) )
		{
			hash.put( GlobalKeys.PRIVILEGE, Privilege.getDescription( value ) );
		}
		else
		{
			throw new InvalidValueException( "The privilege value: " + value + " is out of range" );
		}
	}

	/** Set the privileges available for this user record.
	  @param value Do not use integer values for this parameter it will throw an expection,
	  instead please use values from @see com.ogalala.User.PROGRAMMER
	  @return true if the new privilege setting is valid, false if the setting was invalid
  */
	public void setPrivilege( String desc ) throws InvalidValueException
	{
		if ( Privilege.contains( desc ) )
		{
			hash.put( GlobalKeys.PRIVILEGE, desc );
		}
		else
		{
			throw new InvalidValueException( "The privilege value: " + desc + " is not reconised. The set of possible values are " + Privilege.getValueRange() );
		}
	}

	//---------------------------------------------------------------
	// object store fluff

	/** Create this object with information from that persitent hashtable
		 @param phash ObjectStore persitent hash table containing the information for the fields in this class
		 @param userId The userid for this user
		 @param db A refrence to the UserDatabase for the commit() operation
	*/
	protected UserRecord( String userId, OSHashtable phash, UserDatabase dbIN )
	{
		hash = new Hashtable( phash.size() );
		this.db = dbIN;
		this.userId = userId;
		Enumeration enum = phash.keys();

		while ( enum.hasMoreElements() )
		{
			Object key = enum.nextElement();
			hash.put( key, phash.get(key) );
		}
	}

	/** Write the state of this current record to the database, ie update this record. If the
		 function returns it has worked if not it will throw an exception
		 @throws IOException should the commit fail for any reason.
	*/
	public void commit() throws IOException
	{
		db.commit(this);
	}

	/** Create copy of the internal hashtable which is persitent and hence can be stored in ObjectStore
		 @return OSHashtable A ObjectStore persitent hashtable
	*/
	protected OSHashtable getPersitantHashtable()
	{
		OSHashtable phash = new OSHashtable( hash.size() );
		Enumeration enum = hash.keys();

		while ( enum.hasMoreElements() )
		{
			Object key = enum.nextElement();
			phash.put(key,hash.get(key));
		}

		return phash;
	}

	//--------------------------------------------------------------------
	// Overiding base class behavoir

	/** Checks that the key is not reserved, if it is not it will call the data class
		@see java.util.Hashtable#remove;
	*/
	public synchronized Object remove(String key) throws NameSpaceClashException
	{
		if ( !GlobalKeys.isReservedWord(key.toString()) )
		{
			return hash.remove(key);
		}
		else throw new	NameSpaceClashException("Key " + key.toString() + " is Reserved");
	}

	/** Adds or update information with the key. This checks that the key is not reserved,
		if it is not it will call the data class method
		@see java.util.Hashtable#put;
	*/
	public Object put( String key, Object object ) throws NameSpaceClashException
	{
		if ( !GlobalKeys.isReservedWord( key.toString() ) )
		{
			return hash.put( key, object );
		}
		else throw new	NameSpaceClashException( "Key " + key.toString() + " is Reserved");
	}

	/** Adds or update information with the key. This <b>does not check that the key
		is reserved and hence is more 'dangerous' than the other functions.
		@see java.util.Hashtable#put;
	*/
	public Object fullput( String key, String value ) throws InvalidValueException
	{
		// ### should redirect the setting of values to the specific functions like
		// ### setpassword, setPrivilege and setStatus, this will stop invalid
		// ### values being entered into the user record.

		if ( GlobalKeys.PASSWORD.equalsIgnoreCase( key ) )
		{
			setPassword( value );
			return null;
		}

		if ( GlobalKeys.PRIVILEGE.equalsIgnoreCase( key ) )
		{
			setPrivilege( value );
			return null;
		}

		return hash.put( key, value );
	}


	/** @see java.util.Hashtable#get
	*/
	public Object get(String key)
	{
		String akey = key.toString();

		if ( akey.equals(GlobalKeys.ACCOUNT_STATUS) )
		{
			return accoutStatusStrings[getAccountStatus()];
		}

/*		else if ( akey.equals(GlobalKeys.PASSWORD) )
		{
			return "********";
		}
*/
		return hash.get(key);
	}

	/** @see java.util.Hashtable#keys
	*/
	public Enumeration keys ()
	{
		return hash.keys();
	}
}
