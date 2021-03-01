// $Id: OutputFormatter.java,v 1.3 1999/04/09 10:07:45 alex Exp $
// Format output strings
// James Fryer, 2 Dec 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

import com.ogalala.util.*;

/** Format strings for output.
    <p>
    By default characters are passed through to the result. Format
    strings enclosed in braces {} are interpreted as references to
    atoms.
    <p>
    Format strings have the following syntax:
    "{" [ "-" options SP ] atomID "." fieldID "}"
    <p>
    Permitted options:
    <ul>
    <li><b>-a</b> Add indefinite article
    <li><b>-t</b> Add definite article
    <li><b>-p</b> Add possessive apostrophe
    <li><b>-u</b> Convert first char to uppercase
    <li><b>-l</b> Convert first char to lowercase
    </ul>
    <p>
    Some special atom IDs are allowed:
    <ul>
    <li> current
    <li> actor/me
    <li> container/here
    <li> arg
    </ul>
*/
public final class OutputFormatter
    {
    /** The default atom, which is searched for properties when no atom is specified
    */
    private Atom defaultAtom;

    /** The event context
    */
    private Event event;

	/** A set of userdefined values to substitute into a passed string
	*/
	private String value[];
	
    /** Ctor, uses 'event.current' as default atom
    */
    public OutputFormatter(Event event)
        {
        Atom atom = event.getCurrent();
        if (atom == null)
            atom = event.getActor();
        init(event, atom);
        }

	/**
	*	Constructor with a set of values that will be substituted into 
	*	the passed string.
	*/
	public OutputFormatter(Event event, String value[])
		{
		this(event);
		this.value = value;
		}
		
    /** Ctor
    */
    public OutputFormatter(Event event, Atom defaultAtom)
        {
        init(event, defaultAtom);
        }
    
    /**
	*	Constructor with a set of values that will be substituted into 
	*	the passed string.
	*/
    public OutputFormatter(Event event, Atom defaultAtom, String value[])
    	{
    	init(event, defaultAtom);
    	this.value = value;
    	}
    
    /** Internal initialisation
    */
    private void init(Event event, Atom defaultAtom)
        {
        this.event = event;
        this.defaultAtom = defaultAtom;
        }

    public String format(String fmt)
        {
        // States
        final int PASSTHRU = 0;
        final int IN_BRACES = 1;
        final int IN_OPTIONS = 2;
        final int IN_VALUE = 3;

        StringBuffer buf = new StringBuffer();

        // Position in source string
        int srcPos = 0;

        // Start state machine
        StringBuffer options = null;
        StringBuffer value = null;
        int state = PASSTHRU;
        while (true)
            {
            // See if it's time to stop
            if (srcPos >= fmt.length())
                break;

            // Get the next char
            char c = fmt.charAt(srcPos++);

            // Process it
            switch (state)
                {
            // Copy chars to the result
            case PASSTHRU:
                if (c == '{')
                    {
                    options = new StringBuffer();
                    value = new StringBuffer();
                    state = IN_BRACES;
                    }
                else
                    buf.append(c);
                break;

            // Look for options or value
            case IN_BRACES:
                // If it's a brace, back to passthru
                if (c == '}')
                    state = PASSTHRU;

                // If it's a dash, start collecting options
                else if (c == '-')
                    state = IN_OPTIONS;

                // Any other non-space char is the start of the format value
                else if (!Character.isWhitespace(c))
                    {
                    value.append(c);
                    state = IN_VALUE;
                    }
                break;

            // Collect up options
            case IN_OPTIONS:
                if (Character.isWhitespace(c))
                    state = IN_BRACES;
                else
                    options.append(c);
                break;

            // Collect up the value
            case IN_VALUE:
                // If we hit the closing brace, process the value string
                if (c == '}')
                    {
                    String s = expandFormatString(event, value.toString());
                    s = applyOptions(s, options.toString());
                    buf.append(s);
                    state = PASSTHRU;
                    }

                // Else, add the char to the value
                else
                    value.append(c);
                break;
                }
            }

        return buf.toString();
        }

	
	
    /** 
    *	Expand a format string
    */
    private String expandFormatString(Event event, String formatString)
        {
        
        // if this object has been given values to substitute into a string,
        // see whether the formatString could be a number
        if( value != null && formatString.length() <= 2 )
        	{
        	int numericValue = getIntValue(formatString);
        	//if it is in fact a number, and there appears to be a string value corresponding 
        	// to it, return this value.
        	if( numericValue != -1 && numericValue < value.length )
        		//recursively format it.
        		return value[numericValue];
        	}
        
        // Parse the value string
        LValue lvalue = new LValue(defaultAtom, event);
        lvalue.parse(formatString);
        
        // Get the result
        Atom atom = lvalue.getAtom();
        Object value = atom.getProperty(lvalue.getFieldID());
        
        // Recursively expand the result
        Atom oldDefault = defaultAtom;
        defaultAtom = atom;
        String result = format(AtomData.toString(value));
        defaultAtom = oldDefault;
        return result;
        }

    /** Apply format options
    */
    private String applyOptions(String s, String options)
        {
        // Option flag values
        final int INDEFINITE = 1;   // -a
        final int DEFINITE = 2;     // -t
        final int POSSESSIVE = 4;   // -p
        final int TOUPPER = 8;      // -u
        final int TOLOWER = 16;     // -l

        // Convert the option chars into flags
        int optFlags = 0;
        for (int i = 0; i < options.length(); i++)
            {
            char c = Character.toLowerCase(options.charAt(i));
            switch (c)
                {
            case 'a':
                optFlags |= INDEFINITE;
                break;
            case 't':
                optFlags |= DEFINITE;
                break;
            case 'p':
                optFlags |= POSSESSIVE;
                break;
            case 'u':
                optFlags |= TOUPPER;
                break;
            case 'l':
                optFlags |= TOLOWER;
                break;
            default:
                throw new AtomException("Unrecognised format character: \'" + c + "\'");
                }
            }

        // Apply the options to the string
        if ((optFlags & INDEFINITE) != 0)
            s = StringUtil.addIndefinite(s);

        if ((optFlags & DEFINITE) != 0)
            s = StringUtil.addDefinite(s);

        if ((optFlags & POSSESSIVE) != 0)
            s = StringUtil.addPossessive(s);

        if ((optFlags & TOUPPER) != 0)
            s = StringUtil.firstToUpper(s);

        if ((optFlags & TOLOWER) != 0)
            s = StringUtil.firstToLower(s);

        return s;
        }
        
        /**
		*	Converts the string value to an int, returning null if it isn't an int
		*/
		private int getIntValue(String string)
			{
				try
        			{
        			return new Integer(string).intValue();
        			} 
        			catch( NumberFormatException e )
        			{
        			return -1;
        			}
			}
    }
