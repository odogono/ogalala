// $Id: UserDatabase.java,v 1.30 1999/11/12 12:12:20 jim Exp $
// UserDatabase,
// @author Richard Morgan, 18 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.database;

import COM.odi.util.*;
import COM.odi.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import com.ogalala.util.*;

/**	This class is an API into a database that stores the login details of all the
	 users that connect to the server.<p>

	 This classes current implementaion is using objectStore. All threads that use
	 the object Stores API need to be 'tracked' by a session. When anything is fetched
	 or added to the database the session needs to start a transaction, this can be
	 either UPDATE or READONLY. Once you are happy with the changes in a transaction
	 you can commit() the changes made by the current thread, otherwise you can abort()
	 the changes. ObjectStore works with persitant objects, you can either implement
	 the interfaces and methods your self or run a post processor on the class. To
	 advoid any body else having to learn how to use the pre-processor from objectStore
	 and also to save me having to understand the nasty API objectstore requires to
	 make the objects persitant - I opted to use the persitant classes provided with
	 the object store release: OSHashtable and OSVector and hence advoided to dirty
	 any of our code with ObjectStore fluff.<p>

	 This class defines one session that all threads are joined to as they enter a
	 function and removed before they leave the function. This is fine for our needs
	 as the API has three primary functions (in order of use) retrieving a user record
	 for the login process, updating a user record (like changing a password) and
	 adding a new user. For the login process the thread is going to be accessing the
	 API no more than three (see the login process, as this may change)<p>

	 The version of this class is:<p>
	 <tt>
	 $Id: UserDatabase.java,v 1.30 1999/11/12 12:12:20 jim Exp $
	 </tt>
*/
public class UserDatabase
{
	/**	abstractation for representation of fields
	*/
	private GlobalKeys globalFields = null;

	/** Debugging this class on/off
	*/
	private final boolean verbose = false;

	/** Database name
	*/
	private String name;

	/*  When a db is created a defualt/ inital user is added to the database
		here follows the setup for that user.
	*/
	public static String ADMIN_USER_ID = "administrator";
	private static String ADMIN_USER_PASSWORD = "administrator";
	private static int ADMIN_USER_PRIVILEGE = Privilege.SYSOP;


	/** Create initance and open db in one sweep.
		@param fileName The name of the database to open
	*/
	public UserDatabase(String dbName) throws IOException
	{
		open(dbName);
		name = dbName;
	}

	/** Create instance of this class for opening later
	*/
	public UserDatabase()
	{
		// object created but does not have an open db
		db = null;
		name = null;
	}

	/** Open a existing database
		@param fileName The name of the database to open
		@exception IOException database does not exist
	*/
	public synchronized void open(String dbName) throws IOException
	{
		if ( db != null && db.isOpen() )
		{
			close();
			session.terminate();
		}

		db = null;

		session = Session.create(null,null);

		if ( !dbName.endsWith(".odb") )
		{
			dbName = dbName   + ".odb";
		}

		try
		{
			joinSession();

			// Open the database and allow us to read/write to it
			db = Database.open( dbName, ObjectStore.OPEN_UPDATE );

			// begin transaction that is read only
			Transaction tr = Transaction.begin( ObjectStore.READONLY );

			// get user	defined	fields out of the database
			OSVector vector   = null;

			try
			{
				vector = ( OSVector ) db.getRoot( GLOBAL_FIELDS );
			}
			catch ( DatabaseRootNotFoundException e )
			{
				// Error the the database does not have a important object in it
				tr.abort();
				db.close();
				db = null;
				leaveSession();
				throw new IOException( "not valid database, missing root " + GLOBAL_FIELDS );
			}

			globalFields = new GlobalKeys( vector, this );

			// close the transaction, for readonly transaction abort() / commit() do the same thing
			tr.abort();

			leaveSession();
		}
		catch ( DatabaseNotFoundException e )
		{
			leaveSession();
			throw new IOException( "No data base exists by the name " + dbName );
		}
	}

	/** Close the database, beware once it is closed the single instance of the db is closed
	*/
	public void close()  throws IOException
	{
		if ( db == null )	throw new IOException( "Database not initialized" );

		name = null;

		db.close();
	}

	/** Does the userId exist in the database?
		@return true the userId is being used, false the userId is free to use
	*/
	public boolean userExists( String userId ) throws IOException
	{
		if ( db == null )	throw new IOException( "Database not initialized" );

		joinSession();

		if ( !db.isOpen() ) throw new IOException( "Database not open" );

		// we are not altering the db
		Transaction tr = Transaction.begin( ObjectStore.READONLY );

		// get the user list out
		OSHashtable users =  null;

		try
		{
			users =  ( OSHashtable ) db.getRoot( ALL_USERS );
		}
		catch ( DatabaseRootNotFoundException e )
		{
			tr.abort();
			db.close();
			db = null;
			leaveSession();
			throw new IOException( "Not valid database, missing root" );
		}

		UserRecord user   = null;

		// pull out the persitant hash containing the users details
		OSHashtable userhash = ( OSHashtable ) users.get( userId );

		boolean out = ( userhash != null);

		tr.abort();

		leaveSession();

		return out;
	}

	/**	Retrieve a user	record from	their unique userId	(login name).
		@param UserId the Id number/string of the user to find.
		@return	The	user record	that matches the id	requested or null. If the function returns
		null the record does not exist in the db
	*/
	public synchronized UserRecord getUserRecord(String userId) throws IOException, NullPointerException, UserNotFoundException
	{
		if ( db == null )	  throw new IOException("Database	not	initialized");

		joinSession();

		if ( !db.isOpen() ) throw new   IOException("Database not open");

		// we are not altering the db
		Transaction tr = Transaction.begin(ObjectStore.READONLY);

		// get the user list out
		OSHashtable users =  null;

		try
		{
			users =  (OSHashtable) db.getRoot(ALL_USERS);
		}
		catch ( DatabaseRootNotFoundException e )
		{
			tr.abort();
			db.close();
			db = null;
			leaveSession();
			throw new IOException("not valid database, missing root");
		}

		UserRecord user   = null;

		// pull out the persitant hash containing the users details
		OSHashtable phash = (OSHashtable) users.get(userId);

		if ( phash == null )
		{
			// the user does not exist

			if ( verbose )	System.out.println("getUserRecord: left a session");
			tr.abort();
			leaveSession();
			throw new UserNotFoundException(userId + " does not exist in the database");
		}

		user = new UserRecord(userId,phash,this);

		tr.abort();

		leaveSession();

		return user;
	}

	/**	Add	a new user to the currently	open database. Throws an exception if the new record
		cannot be added	to the database, for instance if the userId	(login name) is	areadly	been
		used.
		@param userId The unique userId	that the user will log into	the	server with.
		@param password	The	hash containing	the	userId+password
		@param privileges The privileges given to the user,	please do not use integers here, use:<p>
			<tt>
			STATUS_GUEST<br>
			STATUS_USER<br>
			STATUS_MODERATOR<br>
			STATUS_PROGRAMMER<br>
			</tt>
		@return	The	newly created user record that has been	added to the data base
		@expection IOException There is a problem with the database
		@expection BadRecordException The user already exists or their is a problem
		with the integrity of the database.
	*/
	public synchronized UserRecord addUserRecord(String userId, String password)  throws IOException,  BadRecordException
	{
		if ( db == null ) throw new IOException("Database not initialized");

		joinSession();

		if ( !db.isOpen() )
		{
			leaveSession();
			throw new IOException("Database not open");
		}

		Transaction  tr = Transaction.begin(ObjectStore.READONLY);

		// first check the user is not already in the database
		OSHashtable users =  null;

		try
		{
			users =  (OSHashtable) db.getRoot(ALL_USERS);
		}
		catch ( DatabaseRootNotFoundException e )
		{
			tr.abort();
			db.close();
			db = null;
			leaveSession();
			throw new IOException("not valid database, missing root");
		}

		if ( users.get(userId) == null )
		{
			UserRecord userRec = new UserRecord(userId,password,this);

			// abort readonly transaction
			tr.abort();

			commit(userRec);

			// ensure that commit terminated the thread
			if ( session.ofThread(Thread.currentThread()) != null )
			{
				leaveSession();
			}

			return userRec;
		}
		else
		{
			tr.abort();
			leaveSession();
			throw new BadRecordException("User already exists " + userId);
		}

	}

	/** close the db before it is garbage collected.
	*/
	public void finalize()
	{
		if ( db.isOpen() )
			db.close();
	}

	/**	A static function to create	new	database with records that implement the minimum of
		information	to run the server and the world. Extra userdefined fields like hair
		colour and shoe	size must be specified in the fields parameter.	For	instance to
		create a database called "Bertie Basset" with the above	extra behavour would be
		formated like the following:<p>

		<tt>
		UserDataBase.create("Bertie Basset","hair colour,shoe size")
		</tt><p>

		These fields must not contain either empty strings or nulls.<p>

		For	just the basic database	and	no extra fields	pass a null	to the constructor like	so:<p>
		<tt>
		UserDataBase.create("Bertie",null)
		</tt><p>

		Note that this function adds one user to the database 'administrator' with no password.

		@param name	The	name for the new database, must	not	be a null or an	empty string.
		@param fields The string stores	the	labels of the extra	fields in comma	separted
		form, for now all fields are textfields	but	this might be extended later.
	*/
	public void create(String name, String userDefinedFieldDescription)
	{
		if ( !name.endsWith( ".odb" ) )
		{
			name = name + ".odb";
		}

		// set up fields
		GlobalKeys fields = new GlobalKeys(userDefinedFieldDescription, null);
		session = Session.create(null,null);
		joinSession();
		db = Database.create( name, 0777 );
		Transaction tx = Transaction.begin( ObjectStore.UPDATE );
		db.createRoot( GLOBAL_FIELDS,fields.getPersistant() );

		// setup users

		OSHashtable users = new OSHashtable();
		db.createRoot( ALL_USERS, users );
		tx.commit();

		// add administator to the db

		if ( verbose )	System.out.println( "database '" + name + "' created and opened" );
		leaveSession();
		addInitalUser();
	}

	private void addInitalUser ()
	{
		try
		{
			UserRecord userRec = addUserRecord( ADMIN_USER_ID, ADMIN_USER_PASSWORD );
			userRec.setPrivilege( ADMIN_USER_PRIVILEGE );
			userRec.commit();
		}

		// an exception should ever be thrown, print it in case ...
		catch ( Exception e )
		{
			Debug.printStackTrace( e );
		}

		System.out.println("Inital user added to the database with userId '" + ADMIN_USER_ID
								 + "' and password '" + ADMIN_USER_PASSWORD + "' and the privilege setting '"
								 + Privilege.getDescription( ADMIN_USER_PRIVILEGE ) + "'");
	}

	/** Is the database open?
		@return true the db is open<br>false the db is closed
	*/
	public boolean isOpen() throws IOException
	{
		if ( db == null )	  throw new IOException("Database	not	initialized");

		boolean result = db.isOpen();

		// isOpen() adds us to a session, lets remove yourselves
		leaveSession();

		return result;
	}

	/** Does the database with <name> exist and accessible to this class.
		@return true the database can be loaded<br>false the database cannot be found
	*/
	public static boolean exists(String name)
	{
		if ( !name.endsWith(".odb") )
		{
			name = name + ".odb";
		}

		java.io.File file =  new   java.io.File(name);
		return file.exists();
	}


	//--------------------------------------------------------------
	// object store	fluff

	/**	Objectstore	database handle
	*/
	private static Database   db = null;
	private static Session session = null;

	private static String GLOBAL_FIELDS   = "global Fields";
	private static String ALL_USERS = "ALL Users";

	// ### Made public by AV 31/8/99 for use with netFootball
	public synchronized void commit(UserRecord rec) throws IOException
	{
		// is this thread associated with a session?

		if ( session.getCurrent() == null )
		{
			session.join();
		}

		// begin transaction we are going alter the database
		Transaction tr = Transaction.begin(ObjectStore.UPDATE);

		// pull all the users
		OSHashtable users =  null;

		try
		{
			users = (OSHashtable) db.getRoot(ALL_USERS);
		}
		catch ( DatabaseRootNotFoundException e )
		{
			tr.abort();
			db.close();
			db = null;
			throw new IOException("not valid database, missing root");
		}

		// put in the record
		users.put(rec.getUserId(),rec.getPersitantHashtable());

		// commit the transaction
		tr.commit();

		if ( verbose )	System.out.println("Sucessfully added/update record " + rec.getUserId());

		leaveSession();
	}

	public Enumeration getUserEnumeration () throws IOException, NullPointerException
	{
		if ( db == null )	  throw new IOException("Database	not	initialized");

		if ( verbose )	System.out.println("Joined a session");
		session.join();

		if ( !db.isOpen() ) throw new   IOException("Database not open");

		// we are not altering the db
		Transaction tr = Transaction.begin(ObjectStore.READONLY);

		// get the user list out
		OSHashtable users =  null;

		try
		{
			users =  (OSHashtable) db.getRoot(ALL_USERS);
		}
		catch ( DatabaseRootNotFoundException e )
		{
			tr.abort();
			db.close();
			db = null;
			throw new IOException("not valid database, missing root");
		}

		Vector userCache = new Vector(users.size());

		// note that if we return the following enumeration we need to
		// keep the Session open or the object will become stale and unuseable

		Enumeration enum = users.keys();

		while ( enum.hasMoreElements() )
		{
			userCache.addElement(enum.nextElement());
		}

		tr.abort();

		leaveSession();

		return userCache.elements();
	}


	private final void joinSession()
	{
		try
		{
			if ( session.ofThread(Thread.currentThread()) == null )
			{
				session.join();
				if ( verbose )	System.out.println("Joined a session");
			}
			else
			{
				if ( verbose )	System.out.println("Already joined to a session");
			}
		}
		catch ( Exception e )
		{
			Debug.printStackTrace( e );
		}
	}

	private final void leaveSession()
	{
		try
		{
			session.leave();
			if ( verbose )	System.out.println("left a session");
		}
		catch ( Exception e )
		{
			Debug.printStackTrace( e );
		}
	}


	//----------------------------------------------------------------------------
	// LoginInterface Stuff

	public String getPasswordForUser( String userId ) throws UserNotFoundException, IOException
	{
		UserRecord user = getUserRecord( userId );

		return user.getPassword();
	}

	public int getAccountStatusForUser( String userId ) throws UserNotFoundException, IOException
	{
		UserRecord user = getUserRecord( userId );

		return user.getAccountStatus();
	}
}

