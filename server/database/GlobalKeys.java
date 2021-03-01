// $Id: GlobalKeys.java,v 1.10 1998/07/06 12:11:11 rich Exp $
// Global Keys an abstract representation of the user defined fields
// @author Richard Morgan, 18 May 98 
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.database;

import COM.odi.util.*;
import COM.odi.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;

/** This class is part of the database. It holds the user defined fields that are specified 
	 on the creation of the database. It also hold the labels for the perminatant fields that 
	 each database must hold.<p>
	 
	 The user defined fields are held in a Vector, this allows handy enumerations and allows
	 flexibility should we allow the user to add more fields later on. The addFields function
	 call partly implements this functionality, but it has been hiden away for now. We need
	 to have a 'removeKey' function too. These function highlight the problem of synchronsization
	 between what is held in the database and what is held here, for instance is it complusory for 
	 the user defined fields to have a value in each UserRecord? When we delete a user field do we
	 go through each UserRecord and remove that field. I feel that this can be extended later 
	 once we know the practical requirements of the db.<p>

	 <tt>
	 $Id: GlobalKeys.java,v 1.10 1998/07/06 12:11:11 rich Exp $
	</tt>
*/
public class GlobalKeys
{
	protected static String ACCOUNT_STATUS = "accountStatus";
	protected static String PASSWORD = "password";
	protected static String PRIVILEGE = "privilege";

	Vector fields = null;

	UserDatabase userDatabase;

	/** Constructor
		 @param keyNames a comma separted string containing the names for the user defined
		 fields.
		 @param UserDatabase the database this class belongs to.
		 @throws NullPointerException if the UserDatabase is null 
		 @throws NameSpaceClashException if the user defined fields contain a repeated word
	*/
	protected GlobalKeys(String keyNames, UserDatabase db) throws NullPointerException , NameSpaceClashException
	{
		userDatabase = db;

		fields = new Vector();

		// if the key names is null we do not have to fill the fields vector
		if ( keyNames != null )
		{
			StringTokenizer tokens = new StringTokenizer(keyNames,",");

			while ( tokens.hasMoreTokens() )
			{
				addKey(tokens.nextToken());
			}
		}
	}
	/** Add a new field to the database.
		 @param name the name of the new field for the db
		 @throws NameSpaceClashException if the new field name is already in the database
	*/
	protected void addKey(String name) throws NullPointerException, NameSpaceClashException
	{
		if ( isReservedWord(name) )
		{
			throw new NameSpaceClashException("used a reserved word " + name);
		}

		Enumeration enum = fields.elements();

		while ( enum.hasMoreElements() )
		{
			String key = (String) enum.nextElement();
			if ( key.equals(name) )
			{
				throw new NameSpaceClashException("used a repeated word");
			}
		}
	}

	/** Certain keys are permanatly reserved by the database, this allows you to check if
		 if a word is reserved and hence cannot be used as a user defined field.
		 @return true, the word is a reserved word.<br>false the word is free to use
	*/
	public static boolean isReservedWord( String keyHash )
	{
		if ( GlobalKeys.ACCOUNT_STATUS.equals( keyHash ) )
		{
			return true;
		}
		else if ( GlobalKeys.PASSWORD.equals( keyHash ) )
		{
			return true;
		}
		else if ( GlobalKeys.PRIVILEGE.equals( keyHash ) )
		{
			return true;
		}
		else return false;
	}

	/** Has a key been specified as a field in the database?
		 @param the key to check
		 @return true the key has been specified in the database<br>false the key is not used
		 @throws NullPointerException if keyHash == null
	*/
	public boolean isUserDefinedField(String keyHash) throws NullPointerException
	{
		Enumeration enum = fields.elements();

		while ( enum.hasMoreElements() )
		{
			String userField = enum.nextElement().toString();
			if ( keyHash.equals(userField) )
				return true;
		}

		return false;
	}

	/** Get all the user defined keys
		 @return Enumeration of Strings
	*/
	public Enumeration keys()
	{
		return fields.elements();
	}

	//---------------------------------------------------------------------
	// Object Store fluff

	/** Restore a Global fields object from the database
	*/
	protected GlobalKeys(OSVector pfields, UserDatabase db)
	{
		userDatabase = db;

		Enumeration enum = pfields.elements();

		fields = new Vector(pfields.size());

		while ( enum.hasMoreElements() )
		{
			fields.addElement(enum.nextElement());
		}
	}

	/** Get a persistant object representing the data in this class
		 @returns OSVector, a persistant vector class from objectstore
	*/
	protected OSVector getPersistant()
	{
		OSVector pfield = new OSVector(fields.size());

		Enumeration enum = fields.elements();

		while ( enum.hasMoreElements() )
		{
			pfield.addElement(enum.nextElement());
		}

		return new OSVector();
	}
}

