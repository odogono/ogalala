// $Id: Tester.java,v 1.7 1998/11/05 12:02:14 rich Exp $
// Tester for user database
// Tester program to harass the database
// Richard Morgan, 18 May 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package	com.ogalala.server.test;
import com.ogalala.server.database.*;
import java.io.IOException;

import COM.odi.util.*;
import COM.odi.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Tester
{
	UserDatabase db;

	public Tester(String[] argv)
	{
        try
        {
            if (UserDatabase.exists(argv[0]))
            {
                if (Session.ofThread(Thread.currentThread()) != null)
                {
                    System.out.println("db b4 open Why are we joined to a session?");
                }

                db = new UserDatabase(argv[0]);

                if (Session.ofThread(Thread.currentThread()) != null)
                {
                    System.out.println("db opened:Why are we joined to a session?");
                }

                getAdmin();

				BufferedReader in = new BufferedReader( new InputStreamReader(System.in));

				String s;

				while (true)
				{
					System.out.println("read or write to/from the db[r/w]?");

					s = in.readLine();
					if (s.equalsIgnoreCase("r") || s.equalsIgnoreCase("w") || s.equalsIgnoreCase("q"))
						break;
        	    }

				if (s.equalsIgnoreCase("w")) putStuffInDB();
				if (s.equalsIgnoreCase("r")) getStuffFromDB();
				if (s.equalsIgnoreCase("q")) System.exit(0);

            }
            else
            {
				db = new UserDatabase();

                db.create(argv[0], argv[1]);

                if (Session.ofThread(Thread.currentThread()) != null)
                {
                    System.out.println("create over:Why are we joined to a session?");
                }
            }
        }
        catch (IOException e)
        {
            Debug.printStackTrace( e );
        }
	}

	public void	putStuffInDB()
	{
        try
        {

			BufferedReader in = new BufferedReader( new InputStreamReader(System.in));

			String s;

			while (true)
			{
				System.out.println("auto or manual fill [a/m]?");

				s = in.readLine();
				if (s.equalsIgnoreCase("a") || s.equalsIgnoreCase("m") || s.equalsIgnoreCase("q"))
					break;
    	    }

			if (s.equalsIgnoreCase("a")) autoFill();
			if (s.equalsIgnoreCase("r")) manualFill();
			if (s.equalsIgnoreCase("q")) System.exit(0);
		}
		catch (IOException e)
		{
			Debug.printStackTrace( e );
		}
	}

	public void autoFill()
	{
		com.ogalala.server.stressTest.Names names = new com.ogalala.server.stressTest.Names();

		String name = null;
		String password = null;

		do
		{
			name = names.nextName();
			password = name.toUpperCase();

			try
			{
				db.addUserRecord( name, password );
				System.out.println( "Added User: " + name + "\nwith password: " + password);
			}
			catch (IOException e)
			{
				Debug.printStackTrace( e );
			}
			catch (BadRecordException e)
			{
				Debug.printStackTrace( e );
			}

		}
		while (name != null);
	}

	public void manualFill()
	{
		try {

			BufferedReader in = new BufferedReader( new InputStreamReader(System.in));

        	String userId = null;
        	String password = null;
			final String exit = "exit";

			while (true)
			{
				System.out.print("Enter userid ");
				userId = in.readLine();

				if (exit.equalsIgnoreCase(userId))
					break;

				System.out.print("Enter Password (enter for default 'xxx')");
				password = in.readLine();

				try
				{
					db.addUserRecord(userId,password);
				}
				catch (IOException e)
				{
					Debug.printStackTrace( e );
				}
				catch (BadRecordException e)
				{
					Debug.printStackTrace( e );
				}
			}
		}
		catch (IOException e)
		{
			Debug.printStackTrace( e );
		}
	}

    private void sessionCheck()
    {
        if (Session.ofThread(Thread.currentThread()) != null)
        {
            System.out.println("Why are we joined to a session?");
        }
    }

	public void	getStuffFromDB()
	{
        try
        {
			BufferedReader in = new BufferedReader( new InputStreamReader(System.in));

        	String userId = null;
        	String password = null;
			final String exit = "q";

			while (true)
			{
				System.out.print("Enter userid ");
				userId = in.readLine();

				if (exit.equalsIgnoreCase(userId))
					break;
				try
				{
					System.out.println(db.getUserRecord(userId));
				}
				catch (UserNotFoundException e)
				{
					System.out.println("user not in db");
					Debug.printStackTrace( e );
				}
			}

            sessionCheck();
        }
        catch (IOException e)
        {
            Debug.printStackTrace( e );
        }
    }

    void getAdmin ()
    {
        UserRecord admin = null;

        try
        {
            admin = db.getUserRecord("administrator");
        }
        catch (UserNotFoundException e)
        {
            Debug.printStackTrace( e );
        }
        catch (IOException e)
        {
            Debug.printStackTrace( e );
        }

        System.out.println( admin.toString() );
    }


	//----------------------------------------------------------------------------------

	public static void main(String[] argv)
	{
		if (argv.length	< 1)
		{
			System.out.println("error, params required, press any key\n");

            // pause until key press so that the user gets a chance to read this message
        	try
        	{
        		System.in.read();
        	}
        	catch (java.io.IOException e) {};

			return;
		}
		else
		{
		    new	Tester(argv);
		}
	}


}

