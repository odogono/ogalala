// $Id: ParserEvent.java,v 1.14 1999/04/29 10:01:36 jim Exp $
// ParserEvent: An unbound event emmitted by the parser
// Alexander Veenendaal, 16 September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.util.Enumeration;

public class ParserEvent extends Event
{
    public static final int serialVersionUID = 1;
    
	/** The exact verb that was used by the actor
	*/
	private String verbString;
	
    /** The current object. This may be a NounPhrase, an Atom or simply null (root atom)
    */
    private Object unboundCurrent;
    
    /** A reference to the parser that spawned this ParserEvent.
    */
    private Parser parser;
    
    /** Has a pronoun been found within this sentence yet ?
    */
    private boolean pronounFound = false;
    
    /** Has this parserEvent output any appropriate error messages yet ?
    */
    private boolean handledError = false;
    
    
    
    //-------------------------------- Constructors --------------------------//
    
    public ParserEvent(Parser parser, World world, Atom actor, String propertyID, String verbString, Object current, Object argument1, Object argument2)
    {
    	super(world, actor, propertyID, null, null);
    	this.parser = parser;
    	this.verbString = verbString;
    	this.unboundCurrent = current;
    	args = new Object[2];
    	args[0] = argument1;
    	args[1] = argument2;
    }
    
    
    public ParserEvent(Parser parser, World world, Atom actor, String propertyID, Object current, Object argument1, Object argument2)
    {
    	this(parser, world, actor, propertyID, "", current, argument1, argument2);
    }
    
    public ParserEvent(Parser parser, World world, Atom actor, String propertyID, Object current, Object args[])
    {
    	this(parser,world,actor, propertyID, "", current, args[0], args[1]);
    }
    //------------------------------- Public Methods -------------------------//
    
    public void setPronounFound(boolean pronounFound)
    {
    	this.pronounFound = pronounFound;
    }
    
     public boolean getPronounFound()
    {
    	return pronounFound;
    }
  	/**
  	* Returns a reference to the parser that spawned this ParserEvent
  	*
  	* @returns				The originating parser object
  	*/
    public Parser getParser()
    {
    	return parser;
    }
    
    /**
    * Sets the reference to a parser object
    *
    * @param
    */
    public void setParser(Parser parser)
    {
    	this.parser = parser;
    }
    public Object getUnboundCurrent()
    {
    	return unboundCurrent;
    }
    
    public void setUnboundCurrent(Object current)
    {
    	this.unboundCurrent = current;
    }
    
    public void setVerbString(String verbString)
    {
    	this.verbString = verbString;
    }
    
    public String getVerbString()
    {
    	return verbString;
    }
    
    /** If this function returns true, the event is 'bound' and can be executed
        immediately. If this is false, the 'getBindings' function must be called.
    */
    public boolean isBound()
    {
        return false;
    }
	
    /** If the event needs to be bound to game objects, this function will 
        return an enumeration of the bound events. An unbound event cannot 
        be executed directly; instead the events returned by this function 
        should be executed.
    */
    public Enumeration getBindings()
    {
        return new BinderEnumeration(this);
    }
    
    /**
    * Looks through the current and args for a NounPhrase which has been marked
    * as a pronoun. If it has, substitute it for the passed NounPhrase
    */
    public void substituteNounPhrasePronoun(NounPhrase pronoun)
    {
    	if(unboundCurrent instanceof NounPhrase)
    	{
    		if( ((NounPhrase)unboundCurrent).getPronounStatus() )
    			unboundCurrent = pronoun;
    	}
    	else if(args[0] instanceof NounPhrase)
    	{
    		if( ((NounPhrase)args[0]).getPronounStatus() )
    			args[0] = pronoun;
    	}
    	else if(args[1] instanceof NounPhrase)
    	{
    		if( ((NounPhrase)args[0]).getPronounStatus() )
    			args[1] = pronoun;
    	}
    }
    
    
    /**
    * Returns true if the parserevent has sent out an error message to 
    * the calling actor
    *
    * @return			true if an error has been displayed, false otherwise.
    */
    public boolean hasHandledError()
    {
    	return handledError;
    }
    
    /**
    * Sets the handled error flag to true
    */
    public void setHandledError()
    {
    	handledError = true;
    }
}
