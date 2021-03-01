// $Id: ValidatorTest.java,v 1.2 1998/06/09 16:05:42 matt Exp $
// App to test the workings of the PasswordValidator class
// Matthew Caldwell, 11 May 1998
// Copyright (c) Ogalala Ltd <info@ogalala.com>

import com.ogalala.crypt.*;

public class ValidatorTest
{
	// a test of the PasswordValidator class, using two instances,
	// one acting as the server and the other acting as the client
	// the passwords they use are the supplied args, or they
	// default to "ogalala"
	
	public static void main ( String[] args )
	{
		String pass1 = "ogalala", pass2 = "ogalala";
		
		if ( args.length > 0 )
			pass1 = args[0];
		
		if ( args.length > 1 )
			pass2 = args[1];
		
		PasswordValidator client = new PasswordValidator();
		PasswordValidator server = new PasswordValidator();
		
		try
		{
			client.setSeed( server.getSeed() );
		
			String clienthash = client.getHash( pass1 );
			
			System.out.println ( "Client sends the hash: " + clienthash );
			
			boolean ok = server.authenticate ( pass2, clienthash );
			
			if ( ok )
				System.out.println ( "Server accepts: the password was correct" );
			else
				System.out.println ( "Server rejects: the password was wrong" );
		
		}
		catch ( AuthenticationException e )
		{
			System.out.println ( e.toString() );
		}
		
		// dumb thing to stop the console window from vanishing when
		// run in Win95
		try { System.in.read(); }
		catch ( Exception e ) {}
	}
}
