// $Id: ParserPipelineException.java,v 1.5 1999/04/29 10:01:36 jim Exp $
// Exception for parser errors
// Alexander Veenendaal, 13 September 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua;

public class ParserPipelineException extends ParserException
{
	/** ParserPipelineExceptions may be assigned a level of severity
	*/
	protected static final byte USUAL = 0;
	protected static final byte UNUSUAL = 1;
	protected static final byte FATAL = 2; 
	
	private byte state = USUAL;
	
	public ParserPipelineException(String s, byte state)
	{
		super(s);
		this.state = state;
	}
    public ParserPipelineException(String s) 
    { 
    	this(s,USUAL); 
    }
    public byte getState()
    {
    	return state;
    }
}

