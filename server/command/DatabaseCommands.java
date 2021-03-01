// $Id: DatabaseCommands.java,v 1.10 1998/11/05 12:02:13 rich Exp $
// Commands classes for altering data in the database
// James Fryer & Richard Morgan, 17 June 98

/* 	           Copyright Big Toe Productions Limited 1997/98
 *
 * 	This document and the information it contains is intended for internal
 *	use by the employees of Big Toe Productions Limited and Ogalala Limited
 *  and for no other person or organisation. It is confidential, legally
 *  privileged and protected in law. Unauthorised use, copying or disclosure
 *  of any of it may be unlawful. If you have received this document in error,
 *  please contact us immediately by telephone on (from the UK) 0171 613 5544
 *  or fax on 0171 613 3444. We will accept a reverse charge (collect) call.
 *  Overseas callers should dial +44 171 613 5544 (tel) or +44 171 613 3444 (fax)
 */

package com.ogalala.server;

import java.util.Enumeration;
import com.ogalala.server.database.*;
import com.ogalala.util.*;

//-----------------------------------------------------------------------------

/** Examine details of a user in the database
*/
class ExamineCommand
    extends ServerCommand
    {
    public ExamineCommand(ServerCommandInterpreter cli, int privilegeLevel, int logLevel)
        {
        super(cli, "EXAMINE userID\tSee user's database entry", privilegeLevel, logLevel);
        cli.addCommand("EXAMINE", this);
        }

    public void execute(Enumeration args, User user)
        {
        // which user are we refering to ?
        String userId = args.nextElement().toString();

		UserDatabase userDatabase = user.getServer().getUserDatabase();

 		UserRecord userRecord = null;

		try
		{
			userRecord = userDatabase.getUserRecord( userId );
		}
		catch ( UserNotFoundException e )
		{
			user.outputError( "The user '" + userId + "' does not exist in the user database." );
			return;
		}
		catch ( java.io.IOException e )
		{
			// some body probelly closed the db
			user.outputError( "Internal database problem" );
			Debug.printStackTrace( e );
			return;
		}

		Enumeration enum = userRecord.keys();

		while (enum.hasMoreElements())
			{
			StringBuffer buff = new StringBuffer();

			String key = enum.nextElement().toString();
			buff.append( "EXAMINE:" );
			buff.append( key );
			buff.append( "=" );
			buff.append( userRecord.get( key ) );
			user.outputMessage( buff.toString() );
			}
		user.outputAck();
        }
    }

//-----------------------------------------------------------------------

/** @SET -- Change user database details
*/
class SetCommand
    extends ServerCommand
    {
    public SetCommand( ServerCommandInterpreter cli, int privilegeLevel, int logLevel )
        {
        super( cli, "SET userID name=value\tChange user's database entry", privilegeLevel, logLevel );
        cli.addCommand( "SET", this );
        }

    public void execute( Enumeration args, User user )
    	{
    	String targetUser;
    	String expression;

        // get user out of database

		if ( args.hasMoreElements() )
			{
			targetUser = args.nextElement().toString();
			}
		else
			{
			user.outputError( "Parameters required" );
			return;
			}

        UserRecord userRecord = null;

		try
			{
			userRecord = user.getServer().getUserDatabase().getUserRecord( targetUser );
			}
		catch ( UserNotFoundException e )
			{
			user.outputError( "The user '" + targetUser + "' does not exist in the user database." );
			return;
			}
		catch ( java.io.IOException e )
			{
			// some body probelly closed the db
			user.outputError( "Internal database problem" );
			Debug.printStackTrace( e );
			return;
			}

        // got the user, evaluate key and value

		SimpleExpressionParser parser = null;

		try
			{
			parser = new SimpleExpressionParser( args );
			}
		catch ( InvalidExpressionException e )
			{
			user.outputError( e.toString() );
			return;
			}

		userRecord.put( parser.getLhs(), parser.getRhs() );

		try
			{
			userRecord.commit();
			user.outputAck();
			return;
			}
		catch ( java.io.IOException e )
			{
			// some body probelly closed the db
			user.outputError( "Internal database problem" );
			Debug.printStackTrace( e );
			}
		}
    }

//-----------------------------------------------------------------------

/** @FULLSET -- Change user database details without restriction
*/
class FullSetCommand
    extends ServerCommand
    {
    public FullSetCommand( ServerCommandInterpreter cli, int privilegeLevel, int logLevel )
        {
        super( cli, "FULLSET userID name=value\tChange user's database entry", privilegeLevel, logLevel );
        cli.addCommand( "FULLSET", this );
        }

    public void execute( Enumeration args, User user )
    	{
    	String targetUser;
    	String expression;

        // get user out of database

		if ( args.hasMoreElements() )
			{
			targetUser = args.nextElement().toString();
			}
		else
			{
			user.outputError( "Parameters required" );
			return;
			}

        UserRecord userRecord = null;

		try
			{
			userRecord = user.getServer().getUserDatabase().getUserRecord( targetUser );
			}
		catch ( UserNotFoundException e )
			{
			user.outputError( "The user '" + targetUser + "' does not exist in the user database." );
			return;
			}
		catch ( java.io.IOException e )
			{
			// some body probelly closed the db
			user.outputError( "Internal database problem" );
			Debug.printStackTrace( e );
			return;
			}

        // got the user, evaluate key and value

		SimpleExpressionParser parser = null;

		try
			{
			parser = new SimpleExpressionParser( args );
			userRecord.fullput( parser.getLhs(), parser.getRhs() );
			userRecord.commit();
			user.outputAck();
			return;
			}
		catch ( InvalidExpressionException e )
			{
			user.outputError( e.toString() );
			return;
			}
		catch ( InvalidValueException e )
			{
			user.outputError( e.toString() );
			return;
			}
		catch ( java.io.IOException e )
			{
			// some body probelly closed the db
			user.outputError( "Internal database problem" );
			Debug.printStackTrace( e );
			}
		}
    }


//---------------------------------------------------------------------------------------

/** @PASSWORD -- Change password
*/
class PasswordCommand
    extends ServerCommand
    {
    public PasswordCommand( ServerCommandInterpreter cli, int privilegeLevel, int logLevel )
        {
        super( cli, "PASSWORD\tChange your password", privilegeLevel, logLevel );
        cli.addCommand( "PASSWORD", this );
        }

    public void execute( Enumeration args, User user )
        {
        String newPassword = null;

		if ( args.hasMoreElements() )
			{
			newPassword = args.nextElement().toString().trim();
			}
		else
			{
			user.outputError( "PASSWORD: No password." );
			return;
			}

		// get the user record

		// fetch the user record

        try
            {
    		user.getUserRecord().setPassword( newPassword );
            }
        catch (InvalidValueException e)
            {
            user.outputError(e.toString());
            return;
            }

		try
			{
			user.getUserRecord().commit();
			user.outputAck();
			return;
			}
		catch ( java.io.IOException e )
			{
			// some body probelly closed the db
			user.outputError( "PASSWORD: Internal database problem" );
			Debug.printStackTrace( e );
			}
		}
    }

class AddUserCommand
    extends ServerCommand
    {
    private static final String ADDUSER = "ADDUSER";

    public AddUserCommand( ServerCommandInterpreter cli, int privilegeLevel, int logLevel )
        {
        super( cli, ADDUSER + " <userId> <password>\tAdd a new user to the database", privilegeLevel, logLevel );
        cli.addCommand( ADDUSER, this );
        }

    public void execute( Enumeration args, User user )
        {
		String userId;
		String password;

		if ( !args.hasMoreElements() )
			{
			user.outputError( ADDUSER + ": Parameters required" );
			return;
			}

		userId = args.nextElement().toString();

		// check that there is not an account in that name

		UserDatabase userDatabase = user.getServer().getUserDatabase();

		try
			{

			if ( userDatabase.userExists( userId ) )
				{
				user.outputError( ADDUSER + ": user already exists" );
				return;
				}

			if (!args.hasMoreElements())
				{
				user.outputError( ADDUSER + ": Password required" );
				return;
				}

			password = args.nextElement().toString();
			userDatabase.addUserRecord( userId , password );
			user.outputMessage( ADDUSER + ": user " + userId + " added successfully." );
			user.outputAck();
			}
		catch ( java.io.IOException e )
			{
			// some body probelly closed the db
			user.outputError( "Internal database problem" );
			Debug.printStackTrace( e );
			return;
			}
		catch ( BadRecordException e )
			{
			user.outputError( ADDUSER + e.toString() );
			}
        }
    }

