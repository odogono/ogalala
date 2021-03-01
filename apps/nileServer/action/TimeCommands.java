// $Id: TimeCommands.java,v 1.3 1999/04/19 17:05:51 alex Exp $
// Game Date and Time Functions and commands
// Alexander Veenendaal, 1 April 1999
// Copyright (C) HotGen Studios <http://www.hotgen.com/>


package com.ogalala.mua.action;

import java.io.*;
import java.util.*;
import com.ogalala.mua.*;
import com.ogalala.util.*;

import com.ogalala.nile.GameDate;

/**
*   Reports the GameTime in the format <hour>:<minutes> <am/pm>
*/
public class ReportTime extends JavaAction
{
    private static final long serialVersionUID = 1;
	
    public boolean execute()
    {
    	//construct the date object with the seconds from the server
    	GameDate gameDate = new GameDate( world.getTime() );

    	//output the game time
    	actor.output( gameDate.toTimeString() );
        return true;
    }

}

/**
*   Reports the GameDate in the format <day> the <date> of <month>, <year>
*/
public class ReportDate extends JavaAction
{
    private static final long serialVersionUID = 1;

    public boolean execute()
    {
    	//construct the date object with the seconds from the server
    	GameDate gameDate = new GameDate( world.getTime() );
    	
    	//output the game time
		actor.output( gameDate.toDateString() );
		
		return true;
    }
    
}