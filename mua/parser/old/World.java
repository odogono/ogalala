package com.ogalala.mua.parser;


public class World
{
	
	public Event newEvent(Atom actor, String id, Atom current, Object args[])
    {
    	System.out.print("posting event " + id + " ");
    	for(int i=0;i<args.length;i++)
    		System.out.print((String)args[i] + " ");
    	System.out.println("");
        return new Event(this, actor, id, current, args);
    }
	public Event newEvent(Atom actor, String id, Atom current)
    {
        return new Event(this, actor, id, current, null);
    }
    public void postEvent(Event event)
    {
    }
}