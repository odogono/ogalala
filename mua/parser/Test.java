// Test driver
import java.io.*;

public class Test
    {
    static java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in), 1);
    static PrintStream out = System.out;

    public static void main(String args[])
        {
	System.out.println( (int)'.' );
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
