// $Id: ParserTokenizer.java,v 1.4 1999/04/29 10:01:36 jim Exp $
// A special variation of the StreamTokenizer for the Parser
// Alexander Veenendaal, 18 September 98
// Copyright (C) Ogalala Ltd. <www.ogalala.com>

package com.ogalala.mua;

import java.io.*;
import java.util.Enumeration;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;

/** 
*   The CommandLineTokenizer is a wrapper around StreamTokenizer. It
*   breaks a command line into strings, allowing quoted strings.
*/
public class ParserTokenizer implements Enumeration
{
    
    private StreamTokenizer tokenizer;
    
    // Hash is used for comments
    private static final char COMMENT_CHAR = '#';
    
    public ParserTokenizer(String s)
    {
        tokenizer = new StreamTokenizer(new StringReader(s));
        initTokenizer();
    }
    
    private void initTokenizer()
    {
        // Automatically turn everything to lowercase
//###        tokenizer.lowerCaseMode(true);
        
        // Reset the tokenizer syntax 
        tokenizer.resetSyntax();

        // Treat EOL as whitespace
        tokenizer.eolIsSignificant(false);
        
        // Whitespace chars
        tokenizer.whitespaceChars(0, ' ');

        // All non-space characters are treated as word chars
        tokenizer.wordChars(33, 126);
        tokenizer.wordChars(128, 255);
        
        //we want to take notice of numbers
        tokenizer.parseNumbers();

        // Comments
        tokenizer.commentChar(COMMENT_CHAR);

        // Single and double quotes
        tokenizer.quoteChar('"');
        tokenizer.quoteChar('\'');
        
        // fullstop is a word char
        tokenizer.wordChars('.', '.');
        //### Alex wants it to be a token to allow multiple commands in the same sentence
//###        tokenizer.ordinaryChar('.');
        
    }
    
    /** Get the next string. 
        <p>
        Null can be returned by this function if an IO error occurs or 'hasMoreTokens' is false.
    */
    public String nextToken()
    {
        // Get the next token
        int tokenType = StreamTokenizer.TT_EOF;
        try 
        {
            tokenType = tokenizer.nextToken();
        }// IO errors are treated as EOF.
        catch (IOException e) { }
        
        // Depending on the type of token we have, construct the result.
        String result = null;

        // TT_WORD and quoted strings, get the string value
        boolean isWord = (tokenType == StreamTokenizer.TT_WORD || tokenType == '\"' || tokenType == '\'');
        if (isWord)
            result = tokenizer.sval;
            
        //if we have a number, turn it back to a string, but add a special character to the end
        // so that we can tell what it is.
        else if(tokenType == StreamTokenizer.TT_NUMBER)
            result = "~" + new Integer((int)tokenizer.nval).toString() + "~";
            
        // Convert anything else to a one-character string
        else 
        {
            StringBuffer buf = new StringBuffer();
            buf.append((char)tokenType);
            result = buf.toString();
        }
        
        //for strings, we add a quote to the beginning and end, so that the parser can tell its a string.
        if(tokenType == '\"' || tokenType == '\'')
            result = "\"" + result + "\"";
            
        return result;
     }
     
     public boolean hasMoreTokens()
     {
        // Get the next token
        int tokenType = StreamTokenizer.TT_EOF;
        try 
        {
            tokenType = tokenizer.nextToken();
        }
        // If an IO error occurs, treat as EOF
        catch (IOException e) { }

        // Are there more tokens left?
        boolean result = (tokenType != StreamTokenizer.TT_EOF);

        // If not at EOF, push the token back for 'nextToken'
        if (result)
            tokenizer.pushBack();

        return result;
     }

    public Object nextElement()
        { return nextToken(); }

    public boolean hasMoreElements()
        { return hasMoreTokens(); }
}

/*
// Test driver
class Test
    {
    static java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in), 1);
    static PrintStream out = System.out;

    public static void main(String args[])
        {
        out.println("Ready");
        out.flush();
        while (true)
            {
            String s = getString();
            if (s == null)
                return;

            ParserTokenizer parser = new ParserTokenizer(s);
            while (parser.hasMoreTokens())
                {
                out.println("<" + parser.nextToken() + ">");
                out.flush();
                }
            }
        }

    // Read a string from 'in'
    //  Return a string, or null if EOF or error
    static String getString()
        {
        String result = null;
        try {
            result = in.readLine();
            }
        catch (java.io.IOException e)
            { }
        return result;
        }
    }
//*/
