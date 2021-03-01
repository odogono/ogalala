// $Id: Parser.java,v 1.4 1998/09/30 12:05:39 jim Exp $
// Intened for Parser Testing only
// Alexander Veenendaal, 30	September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>
package com.ogalala.test.mua;

import java.io.*;

import com.ogalala.mua.*;

public class ParserTest
{
	
    public ParserTest()
    {
        //Parser.DEBUG = true;
		Parser parser = new Parser(null);
        // Send commands to the cli
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in), 1);
        String s;
		try{
	        while ((s = in.readLine()) != null )
	        {
	            parser.parseSentence(s);
	            System.out.println("-------------------------------");
	        }
		}catch(IOException e){ }
    }

    public static void main(String args[])
    {
        System.out.println("ParserTest");
        new ParserTest();
    }
    
    
}