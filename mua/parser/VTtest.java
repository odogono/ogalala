package com.ogalala.mua.parser;

public class VTtest
{

    public static void main(String args[])
    {
        new VTtest();
    }

    public VTtest()
    {
        Verb verb = new Verb();
        verb.addTemplate(new VerbTemplate("current to thing at numeric","sell"));
        verb.addTemplate(new VerbTemplate("current to thing for numeric","examine"));
        verb.addTemplate(new VerbTemplate("current to thing","look_at"));
        verb.addTemplate(new VerbTemplate("current","look"));
        
        System.out.println("to and for = " + verb.matchTemplate("to","for"));
        System.out.println("to = " + verb.matchTemplate("to"));
        System.out.println("none = " + verb.matchTemplate());
        System.out.println("to and at = " + verb.matchTemplate("to","at"));
        /*VerbTemplate v = new VerbTemplate("current to thing for numeric","look");
        if( (v.getArgType(0) & VerbTemplate.CURRENT) != 0)
            System.out.println("we have a current");
            
        if( (v.getArgType(1) & VerbTemplate.THING) != 0)
            System.out.println("we have a thing");    
            
        if( (v.getArgType(2) & VerbTemplate.NUMERIC) != 0)
            System.out.println("we have a numeric");//*/
            
        System.out.println("ok");

    }




}
