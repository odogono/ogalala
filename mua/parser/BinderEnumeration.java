// $Id: BinderEnumeration.java,v 1.40 1999/04/29 10:01:35 jim Exp $
// The BinderEnumeration returns items from the database from the interpretation of NounPhrases.
// Alexander Veenendaal, 28 September 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.util.Enumeration;
import java.util.Vector;
import com.ogalala.util.Debug;
import com.ogalala.util.ResettableEnumeration;
import com.ogalala.util.JoinEnumeration;
import com.ogalala.util.SubtractEnumeration;
import com.ogalala.util.UnionEnumeration;

/**
* Because of the nature of ParserEvents, a BinderEnumeration will return only one *kind*
*  of event but one or more times. However, the current atoms be different
*	
*/
public class BinderEnumeration implements Enumeration
{
	/** 
	* How like something does a adjective property have to be
	*  before it is considered to be that something.
	*/
	public static int isnessThreshold = 50;
	
	/** How many times have we enumerated ?
	*/
	private int enumerated = 0;
	
	/** how many items are in this enumeration ?
	*/
	private int count = 0;
	
	/** contains stuff
	*/
	private ParserEvent parserEvent;
	
	/**
	*/
	private Event next = null;
	
	/** Contains a list of all available atoms for the current argument
	*/
	private Vector currentAtoms;
	private Vector arg1Atoms;
	private Vector arg2Atoms;
	
	//---------------------------- Constructor ------------------------------//
	public BinderEnumeration(ParserEvent parserEvent)
	{
		this.parserEvent = parserEvent;
		
		//------- bind Current		
		if( parserEvent.getUnboundCurrent() instanceof NounPhrase )
		{
			currentAtoms = beginBindNounPhrase( (NounPhrase)parserEvent.getUnboundCurrent() );
			if(currentAtoms != null)
				count = currentAtoms.size();
		}
		
		//------- bind First Argument
		if( parserEvent.getArg(0) instanceof NounPhrase )
		{
			arg1Atoms = beginBindNounPhrase( (NounPhrase)parserEvent.getArg(0) );
			
			if( arg1Atoms != null && arg1Atoms.size() > count )
				count = arg1Atoms.size();
		}
		
		//------- bind Second Argument
		if( arg2Atoms != null && parserEvent.getArg(1) instanceof NounPhrase )
			arg2Atoms = beginBindNounPhrase( (NounPhrase)parserEvent.getArg(1) );
		
		//if no event count has been set by any of the NounPhrases, set a single event.
		if( count == 0 )
			count = 1;
			
		gotoNext();
    }
	
	public BinderEnumeration()
	{
	}
	
	//----------------------- Interface Obligations -------------------------//
	public boolean hasMoreElements()
	{
		return next != null;
	}
	
	public Object nextElement()
	{
		Event result = next;
		gotoNext();
		return result;
	}
	
	//-------------------------- Private Methods ----------------------------//	
	
	private void gotoNext()
	{
		next = null;
		//Usually, the number of events to be generated from this Enumeration is 
		//specified
		if(enumerated < count)
		{
			//current should not be null by the time it gets to emmision
			Atom current = null;
			Object args[] = new Object[2];
			
			// Assume first that the current was a NounPhrase
			if( currentAtoms != null && currentAtoms.size() != 0 )
			{
					if( enumerated < currentAtoms.size() )
						current = (Atom)currentAtoms.elementAt( enumerated );
					else if( currentAtoms.size() > 0 )
						current = (Atom)currentAtoms.elementAt( 0 );
			}
			
			//it may be that the current is an Atom already
			else if( parserEvent.getUnboundCurrent() instanceof Atom )
				current = (Atom)parserEvent.getUnboundCurrent();
			
			//Cater for exits, which are always in the form of an integer
			else if( parserEvent.getUnboundCurrent() instanceof Integer )
			{
				Object exit = parserEvent.getActor().getContainer().getExit( ((Integer)parserEvent.getUnboundCurrent()).intValue() );
				
				if( exit instanceof Atom )
					current = (Atom)exit;
					
				else if( exit instanceof String )
					parserEvent.getActor().output( (String)exit );
				
				else if( exit == null )
				    current = parserEvent.getActor().getContainer();
			}
			
			if( arg1Atoms != null )
			{
				if( enumerated < arg1Atoms.size() )
					args[0] = arg1Atoms.elementAt( enumerated );
			}
			
			//current, atom already ? root
			else if( parserEvent.getArg(0) instanceof Atom )
				args[0] = (Atom)parserEvent.getArg(0);
			//Otherwise just pass back
			else
				args[0] = parserEvent.getArg(0);
			
						
			if( arg2Atoms != null )
				if( enumerated < arg2Atoms.size() )
					args[1] = arg2Atoms.elementAt( enumerated );
			
			//current, atom already ? root
			else if( parserEvent.getArg(1) instanceof Atom )
				args[1] = (Atom)parserEvent.getArg(1);
			
			//Otherwise just pass back
			else
				args[1] = parserEvent.getArg(1);
			

			if(current != null)
			{
				//set the pronoun, as long as we haven't already set a pronoun in this event
				if( ! parserEvent.getPronounFound() )
					parserEvent.getParser().setPronoun(current);
                
				next = parserEvent.getWorld().newEvent(parserEvent.getActor(), parserEvent.getID(), current, args);
			}
			enumerated++;
		}
	}
	
	
		
	/**
	* Kicks of the binding process on a NounPhrase
	*
	* @param target
	* @returns
	*/
	protected Vector beginBindNounPhrase(NounPhrase target)
	{
		
		Vector result = new Vector();
		
		//it may be that a pronoun flagged NounPhrase has found its way through.
		// we need to asssign it here.
		if( target.getPronounStatus() )
		{
			Atom pronoun = parserEvent.getParser().getPronoun();
			if( pronoun != null )
			{
				//return the pronoun from the parser
				result.addElement( pronoun );
				parserEvent.setPronounFound(true);
				return result;
			}
			else
				return null;
		}
			
		//The seedEnumeration contains *all* objects within the selected container
		Enumeration seedEnumeration = createSeedEnumeration(target);
		
		
		//compare this seed enumeration with what is actually being searched for (in the NounPhrase).
		// the returned set will contain all atoms that have something in common with the NounPhrases
		// search paramateres
		Enumeration set = target.bindNounPhrase( parserEvent, seedEnumeration );
		
		
		if( set == null )
			return null;
		
		//If we have an ACQUIRE_SEARCH....
		if( ! target.checkForArgumentType(VerbTemplate.LOOK_SEARCH) && target.checkForArgumentType(VerbTemplate.CURRENT)  )
		{
			Atom item = null;
			
			// We can only aquire things that are portable...
			Atom portable = parserEvent.getWorld().getAtom("portable");
			
			//add all things that are portable to our result vector
			while(set.hasMoreElements())
			{
				item = (Atom)set.nextElement();
				if( item.isDescendantOf( portable ) )
					result.addElement(item);
			}
			
			//if none of the items were portable, then return an error message
			if( result.size() == 0 )
			{
				parserEvent.getActor().output( generateAtomPropertyError(parserEvent, item, parserEvent.getVerbString()) );
				//we have reported an error, so set a flag on the event
				parserEvent.setHandledError();
				return null;
			}
		}
		else
			while( set.hasMoreElements() )
				result.addElement( set.nextElement() );
		
		return result;
	}
	
	/**
	* Generates a error message using the parser events verb.
	*/
	protected static String generateAtomPropertyError(ParserEvent parserEvent, Atom atom, String property)
	{
		Event coverEvent = parserEvent.getWorld().newEvent(parserEvent.getActor(), property, atom, null);
		Object outputResult = atom.getRawProperty( property );
			 
		if ( outputResult != null && outputResult instanceof String )
			return coverEvent.formatOutput( (String)outputResult );
		
		return "You can not get anything to " + parserEvent.getVerbString() + " on.";
		//return "There is nothing to " + parserEvent.getVerbString();
	}
	
	/**
    * Returns the contents of container, as long as they are not offLimit, as an Enumeration.
    *
    * @param contents					the contents of the container
    * @param container					the container itself
    * @param offLimit					any atoms contained within offLimit will be ignored.
    */
    protected static Enumeration getAccessibleAtoms(Enumeration contents, Atom container, Atom offLimit)
    {
    	if( container != null )
    	{
    		//create the result set
    		Vector result = new Vector();
    		
    		//if the contents of the container are not related to atom, add them to the result
    		while( contents.hasMoreElements() )
    		{
    			Atom candidate = (Atom)contents.nextElement();
    			if( offLimit == null )
    				result.addElement( candidate );
    			else if( !offLimit.contains(candidate) )
    				result.addElement( candidate );
    		}
    		return new ResettableEnumeration(result);
    	}
    	return null;
    }
	
	/**
	* Extracts the number count from the root
	*/
	protected static int extractCountFromNounPhrase(NounPhrase root)
	{
		return root.getCount();
	}
	
	
	
	
	
	/**
	* Creates the starting enumeration of the bind. This generally contains all objects that
	* fall within one of the crude look/search boundries :
	*
	*	
	*/
	protected Enumeration createSeedEnumeration(NounPhrase target)
	{
		Enumeration seedEnumeration = null;
		
		if( target.checkForArgumentType(VerbTemplate.SEARCH_CONTAINER) )
		{
			if( target.checkForArgumentType(VerbTemplate.SEARCH_INVENTORY) )
			{
				//bind from the Inventory and the Room
				if( target.checkForArgumentType( VerbTemplate.LOOK_SEARCH ) )
				{
					seedEnumeration = new JoinEnumeration( getAccessibleAtoms( new ContentsEnumeration(parserEvent.getActor() ), 
																			parserEvent.getActor(), 
																			null ),
														getAccessibleAtoms( new VisibleEnumeration(parserEvent.getActor().getContainer(), parserEvent.getActor().getContainer(), parserEvent.getActor() ), 
																			parserEvent.getActor().getContainer(), 
																			null ) 
													);
				}
				else //AQUIRE_SEARCH
				{
					seedEnumeration = new JoinEnumeration( getAccessibleAtoms( new ContentsEnumeration(parserEvent.getActor()), 
																			parserEvent.getActor(), 
																			null ),
														getAccessibleAtoms( new GettableEnumeration( parserEvent.getActor().getContainer(), parserEvent.getActor().getContainer(), parserEvent.getActor() ), 
																			parserEvent.getActor().getContainer(), 
																			null )
													);
				}
			}
			// we are search the container only
			else
			{
				//LOOK_SEARCH
				if( target.checkForArgumentType( VerbTemplate.LOOK_SEARCH ) )
				{ 
					seedEnumeration = getAccessibleAtoms( new VisibleEnumeration(parserEvent.getActor().getContainer(), parserEvent.getActor().getContainer(), parserEvent.getActor() ),
														parserEvent.getActor().getContainer(), 
														parserEvent.getActor());
				}
				else //ACQUIRE_SEARCH
				{ 
					seedEnumeration = getAccessibleAtoms( new GettableEnumeration( parserEvent.getActor().getContainer(), parserEvent.getActor().getContainer(), parserEvent.getActor() ),
														parserEvent.getActor().getContainer(), 
														parserEvent.getActor());
				}
					
					
			}
		}
		//bind items only from the inventory
		else if( target.checkForArgumentType(VerbTemplate.SEARCH_INVENTORY) )
		{
			
			if( target.checkForArgumentType( VerbTemplate.LOOK_SEARCH ) )
			{ 
				seedEnumeration = getAccessibleAtoms( new ContentsEnumeration(parserEvent.getActor() ),
													parserEvent.getActor(), 
													null );
			}
			else //ACQUIRE_SEARCH
			{ 
				seedEnumeration = getAccessibleAtoms( new ContentsEnumeration(parserEvent.getActor() ),
													parserEvent.getActor(), 
													null );
			}
		}
		
		if( !(seedEnumeration instanceof ResettableEnumeration) )
			seedEnumeration = new ResettableEnumeration(seedEnumeration);
		
		return seedEnumeration;
	}
}
