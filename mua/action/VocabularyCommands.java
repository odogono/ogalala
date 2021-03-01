// $Id: VocabularyCommands.java,v 1.11 1999/04/29 10:01:35 jim Exp $
// Vocabulary modification commands
// James Fryer, 18 Sept 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;

/** Add a noun
*/
public class ModAddNoun extends JavaAction
{
    private static final long serialVersionUID = 1;
    
    public boolean execute()
    {
        // Check args
        //### Need help/usage system
        if (event.getArgCount() < 2)
    		throw new AtomException("!NOUN pattern atom");
        
        // Get the args
        String pattern = event.getArg(0).toString();
        String atomID = event.getArg(1).toString();
        
        // Get the atom
        Atom atom = world.getAtom(atomID);
        if (atom == null)
            throw new AtomException("Atom not found: " + atomID);
        
        // Add the noun
        if( world.getVocabulary().addNoun(actor, atom, pattern) )
        	// Report to the user
        	actor.output("Noun added: " + atom.getID() + " (defined as): " + pattern);
        else
        	actor.output("Noun " + atom.getID() + " is already defined in some form");
        return true;
    }
}

/** Remove a noun
*/
public class ModRemoveNoun extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		//check args
		if( event.getArgCount() < 1 )
			throw new AtomException("!REMOVENOUN pattern");
			
		// Get the args
		String pattern = event.getArg(0).toString();
			
		//remove the noun
		world.getVocabulary().removeNoun(actor, pattern);
			
		return true;
	}
}

/**
* Wordtype allows us to query the type of a word
*/
public class WordType extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		// a word has been queryed.
    	//if( sentence.size() >= 2 )
    	if( event.getArgCount() >= 1 )
    	{
    		int types = world.getVocabulary().getWordTypes( (String)event.getArg() );
    		String values[] = Word.lookUpWordValues( types );
    		if( values == null || values.length == 0 )
    			actor.output("The word '" + (String)event.getArg() + "' is Unknown.");
    		
    		else
    		{
    			StringBuffer result = new StringBuffer("The word '" + (String)event.getArg() + "' is of type ");
    			for(int i=0;i<values.length;i++)
    				result.append( values[i] + "," );
    			
    			result.setCharAt( result.length()-1, '.' );
    			actor.output( result.toString() );
    		}
    	}
    	else
    		actor.output("Usage: !Wordtype <word>");
    		
		return true;
	}	
}

/**
* Won't work yet because we need to get the privilege property from the parser
*/
/*public class Privilege extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		//report the users privilege level
    	if( event.getArgCount() == 0 )
    	{
    		actor.output("Privilege level set at " + Privilege.getDescription(privilege) + ".");
			return true;
		}
		Privileged request;
		
		//try for a verb
		request = (Privileged)(world.getVocabulary()).getVerb( (String)event.getArg(0) );
		//otherwise try a rawverb.
		if( request == null)
			request = (Privileged)(world.getVocabulary()).getRawVerb( (String)event.getArg(0) );
			
		if( request != null )
    	{
    		//probably trying to change the privilege level
    		if( event.getArgCount() == 2 )
    		{
    			int newPrivilege = Privilege.getValue( (String)event.getArg(1) );
    			actor.output( (String)event.getArg(0) + " privilege previously set at " + Privilege.getDescription(request.getPrivilege()) + ".");
    			if( newPrivilege != -1 )
    			{
    				request.setPrivilege(newPrivilege);
    				actor.output( (String)event.getArg(0) + " privilege set to " + Privilege.getDescription(request.getPrivilege()) + ".");
    			}
    			return true;
    		}
			//report the verbs privilege level
    	
    		actor.output( (String)event.getArg(0) + " privilege set at " + Privilege.getDescription(request.getPrivilege()) + ".");
    		return true;
    	}
    	actor.output("Usage: !Priv - Display Current Privilege level. !Priv <verb> - Display Verb Privilege level. !Priv <verb> <level> - Set Verb Privilege level");
        return true;
	}
	
}//*/
/**
* This utility reports the numbers of each of the contained
*  wordtypes in the vocabulary.
*/
public class Status extends JavaAction
{
    private static final long serialVersionUID = 1;
    
	public boolean execute()
	{
		actor.output( world.getAtomDatabase().size() + " atoms in the database.");
		actor.output( world.getVocabulary().getWordsSize() + " words (generic,adverbs,adjectives) loaded in the Vocabulary.");
		actor.output( world.getVocabulary().getVerbsSize() + " verbs loaded in the Vocabulary.");
		actor.output( world.getVocabulary().getAdverbsSize() + " adverbs loaded in the Vocabulary.");
		actor.output( world.getVocabulary().getNounsSize() + " nouns loaded in the Vocabulary.");
		actor.output( world.getVocabulary().getAdjectivesSize() + " adjectives loaded in the Vocabulary.");
		actor.output( world.getVocabulary().getRawVerbsSize() + " raw verbs loaded in the Vocabulary.");
		
		return true;
	}
}

