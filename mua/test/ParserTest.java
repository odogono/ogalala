// $Id: Parser.java,v 1.4 1998/09/30 12:05:39 jim Exp $
// Intened for Parser Testing only
// Alexander Veenendaal, 30	September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>
package com.ogalala.test.mua;

import java.io.*;
import java.util.*;
import com.ogalala.util.*;
import com.ogalala.mua.*;
import com.ogalala.mua.World;

public class ParserTest
{
    
    public ParserTest()
    {
    	Parser parser = new Parser(new World());
        //parser.DEBUG = true;
		parser.parseSentence("look");
        // Send commands to the cli
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in), 1);
        String s;
		try {
	        while ((s = in.readLine()) != null )
	        {
	        	try{
	            parser.parseSentence(s);
	            System.out.println("-------------------------------");
	            }catch(ParserException p){ System.out.println(p); }//*/
	        }
		} catch (IOException e) { }
    }

    public static void main(String args[])
    {
        System.out.println("ParserTest");
        new ParserTest();
    }
}