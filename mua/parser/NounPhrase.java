// $Id: NounPhrase.java,v 1.28 1999/04/29 10:01:35 jim Exp $
// Encapsulates a noun phrase intended for the binder
// Alexander Veenendaal, 9 November 98
// Copyright (C) Ogalala Ltd <www.ogalala.com>

package com.ogalala.mua;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import com.ogalala.util.ResettableEnumeration;
import com.ogalala.util.UnionEnumeration;
import com.ogalala.util.SubtractEnumeration;


/**
*
*
*/


public class NounPhrase implements Cloneable
{
    
    //-------------------------- Properties -----------------------------//
    
	/**
	* The identifier is taken directly from the 
	* sentence, and is usually used in reporting
	* errors.
	*/
	protected String identifier = "";
	
	/**
	*
	*/
	protected Atom noun;
	
	/**
	*
	*/
    protected boolean pronounStatus = false;

	/**
	* The nounPhrases argument is taken from the sentences
	* verbTemplate and contains information for the binder.
	* The information includes:
	* 		where to start the search (inventory,room,world)
	*		what items to bind to (items that can be taken, items that can be seen)
	*/
    protected int argument;

	/**
	* The argument modifier properties are another filter device
	* intended for the binder. In this case, they ensure that only
	* items which contain the property are returned.
	*/
	protected Hashtable argModProperties;
	
	/**
	* The modifier string is the NL form of the
	* modifier, and is usually used for error reporting
	* purposes.
	*/
	protected String modifierString = "";
	
	/**
	* the modifier; indicates where to bind the child
	* of this NounPhrase
	*/
    protected byte modifier = MOD_NOTHING;

	/**
	* The number of atoms to bind from this NP
	*/
    protected int count;

	/** Modifier constants
	*/
	public static byte MOD_NOTHING = 0;
    public static byte MOD_IN = 1;
    public static byte MOD_UNDER = 2;
    public static byte MOD_ON = 3;
    public static byte MOD_BEHIND = 4;
    public static byte MOD_EXCEPT = 10;

    /** 
    * adjectives descibing this noun
    */
    protected Vector adjectives = new Vector(6);

    /** 
    * nouns describing this noun
    */
    protected Vector nouns = new Vector(6);


    /** whatevers 'in' 'under' etc. this noun
    */
    //private NounPhrase parent;
    protected NounPhrase child;


	//---------------------------- Constructors ---------------------------------------//
    
    /**
    *
    * @param world					the game world
    * @param nounPhrase				a vector of Words
    * @param argument				taken from the VerbTemplate and used to direct the 
    *								binder
    * @param argModProperties		taken from the VerbTemplate and used to direct the 
    *								binder
    */
    public NounPhrase(World world, Vector nounPhrase, int argument, Hashtable argModProperties)
    {
    	this.argument = argument;
    	this.argModProperties = argModProperties;
    	compile(world, nounPhrase);
    	
    }
    /**
    *
    * @param world					the game world
    * @param nounPhrase				a vector of Words
    * @param argument				culled from the VerbTemplate and used to direct the 
    *								binder
    */
    public NounPhrase(World world, Vector nounPhrase, int argument)
    {
    	this(world, nounPhrase, argument, null);
    }

	/**
	*
	* @param noun					the starting noun
	* @param count					the number of atoms to bind
	* @param argument				culled from the VerbTemplate and used to direct the
	*
	*/
    public NounPhrase(Atom noun, int count, int argument)
    {
        if(noun != null)
        	nouns.addElement(noun);
        
        this.count = count;
        this.argument = argument;
    }

    public NounPhrase(Atom noun, int count)
    {
        this(noun,count,0);
    }

    public NounPhrase()
    {
        this(null,1);
    }

	//---------------------------------- Methods ---------------------------------------//
	
	/**
	* Essentially returns a list of items from the world (beginEnumeration) based on the 
	* criteria information in this NounPhrase.
	*/
	public Enumeration bindNounPhrase( ParserEvent parserEvent, Enumeration beginEnumeration )
	{
		//if( ! (beginEnumeration instanceof ResettableEnumeration) )
		//	return null;
			
		ResettableEnumeration candidates = (ResettableEnumeration)beginEnumeration;
		
		//bind the nouns with the beginning Enumeration
		for( int i=0;i<nouns.size();i++ )
		{
			((ResettableEnumeration)beginEnumeration).resetCount();
			// get all atoms in the Enumeration that are descended from this noun, and intersect
			//  them with what we have so far
			candidates = intersectAtomEnumeration( 
								getDescendantAtomsOfAtom( (Atom)nouns.elementAt(i), beginEnumeration ),
								candidates 
								);
		}
		
		//bind the adjectives with what we have so far
		for( int i=0;i<adjectives.size();i++ )
		{
			((ResettableEnumeration)beginEnumeration).resetCount();
			// get all atoms in the Enumeration that satisfy the adjectives isness threshold, and intersect them
			//  with what we have so far (in candidates).
			candidates = intersectAtomEnumeration( 
								getDescendantAtomsOfAdjective( (String)adjectives.elementAt(i), beginEnumeration ),
								candidates
								);
		}
		
		int existingItems = candidates.size();
		
		//if we have any property modifiers for this nounphrase, apply them to the list we have.
		if( existingItems > 1 && argModProperties != null )
		{
			((ResettableEnumeration)beginEnumeration).resetCount();
		
			candidates = intersectAtomEnumeration(
								getArgModAtoms( argModProperties, beginEnumeration ),
								candidates
								);
		}//*/
		
		//now we have an enumeration which satisfys all the noun and adjective requirements.
		//the next step is to apply the count parameter of this NounPhrase, so that we have <count> atoms.
		Vector result = new Vector();
		
		
		for( int i=0;i<count;i++ )
		{
			if( candidates.hasMoreElements() )
				result.addElement(candidates.nextElement());
			else break;
		}
		
		//if nothing has bound, then there is something definetly wrong !
		if( result.size() == 0 )
		{
			/**
			*/
		    if( identifier == null || identifier.equals("") )
		    {
		    	parserEvent.getActor().output("There is nothing to " + parserEvent.getVerbString() );
		    	//we have reported an error, so set a flag on the event
				parserEvent.setHandledError();
		    	return null;
		    }
		    /** Displayed if a modifier property was specified.
		    */
		    if( argModProperties != null )
			{
				if( existingItems != 0 )
				{
					parserEvent.getActor().output("There are no more " + identifier.trim() + "s to " + parserEvent.getVerbString() );
					//we have reported an error, so set a flag on the event
					parserEvent.setHandledError();
					return null;
				}
				else
				{
					parserEvent.getActor().output("There are no " + identifier.trim() + "s to " + parserEvent.getVerbString() );
					//we have reported an error, so set a flag on the event
					parserEvent.setHandledError();
					return null;
				}//*/
				//return null;
			}
			
			if( checkForArgumentType(VerbTemplate.SEARCH_CONTAINER) && checkForArgumentType(VerbTemplate.SEARCH_INVENTORY) )
			{
				parserEvent.getActor().output("You do not have a " + identifier.trim() + " and none can be seen in the " + parserEvent.getActor().getContainer().getName() );
		    	//we have reported an error, so set a flag on the event
				parserEvent.setHandledError();
		    	return null;
			}
			else if( checkForArgumentType(VerbTemplate.SEARCH_CONTAINER) && !checkForArgumentType(VerbTemplate.SEARCH_INVENTORY) )
			{
				parserEvent.getActor().output("You cannot see a " + identifier.trim() + " in the " + parserEvent.getActor().getContainer().getName() );
		    	//we have reported an error, so set a flag on the event
				parserEvent.setHandledError();
		    	return null;
			}
			else if( !checkForArgumentType(VerbTemplate.SEARCH_CONTAINER) && checkForArgumentType(VerbTemplate.SEARCH_INVENTORY) )
			{
				parserEvent.getActor().output("You do not have a " + identifier.trim() + " in your possession.");
		    	//we have reported an error, so set a flag on the event
				parserEvent.setHandledError();
		    	return null;
			}
			
		}
		
		if( child != null )
		{
			//we need to get the contents of the appropriate atoms into one
			// enumeration, and pass this onto the child in order to bind itself
			if( modifier == MOD_IN)
			{
				Enumeration childEnum = null;
				Atom atom;
				for(int i=0;i<result.size();i++)
				{
					atom = (Atom)result.elementAt(i);
					
					//check whether the atom is closed, if it is we can't return its children
					if( atom.getBool("is_closed") )
					{
						//that should end this current attempt
						parserEvent.getActor().output(new OutputFormatter(parserEvent, atom).format(atom.getString("is_closed_msg")) );
		    			
		    			//we have reported an error, so set a flag on the event
						parserEvent.setHandledError();
						
		    			return null;
					}
					childEnum = new UnionEnumeration( new GettableEnumeration( atom, atom, parserEvent.getActor() ), childEnum );
				}
				//recurse down
				return child.bindNounPhrase( parserEvent, (ResettableEnumeration)childEnum );
			}
			//we need to subtract the contents of this NP enumeration from the results of the
			// childs enumeration
			else if( modifier == MOD_EXCEPT )
			{
				((ResettableEnumeration)beginEnumeration).resetCount();
				SubtractEnumeration sub = new SubtractEnumeration( child.bindNounPhrase(parserEvent,(ResettableEnumeration)beginEnumeration),
												result.elements()
												);
				return sub;
			}
		}
		
		//if the child of this NP is null, just return the atoms we have found
		return result.elements();
	}

	/**
	* Intersects two enumerations of atoms against each other.
	*
	* @returns				An intersection of the two enumerations as a ResettableEnumeration
	*/
	protected static ResettableEnumeration intersectAtomEnumeration(Enumeration alphaEnum, Enumeration betaEnum )
	{
	    if(alphaEnum == null || betaEnum == null)
	        return null;
	    
	    Vector result = new Vector();
	    
	    //loop through the first enumeration
	    while( alphaEnum.hasMoreElements() )
	    {
            //Object enum1Element = enumeration1.nextElement();
            Atom alphaElement = (Atom)alphaEnum.nextElement();
            
            ((ResettableEnumeration)betaEnum).resetCount();
	        
	        //look through the second enum, looking for matches with the first.
	        while( betaEnum.hasMoreElements() )
	        {
	            //Object enum2Element = enumeration2.nextElement();
                Atom betaElement = (Atom)betaEnum.nextElement();
                
                if( betaElement.isDescendantOf( alphaElement ) )
                
	            //if( enum1Element.equals( enum2Element ) );
	                result.addElement( betaElement );
	        }
	    }                               
	    return new ResettableEnumeration(result);
	}
	
	protected static Enumeration getArgModAtoms(Hashtable hashtable, Enumeration candidates)
	{
		//set up a store for all atoms that qualify
		Vector result = new Vector();
		
		String key;
		Boolean value;
		
		boolean addTarget;
		
		//loop through the supplied enumeration
		while( candidates.hasMoreElements() )
		{
			Atom target = (Atom)candidates.nextElement();
			
			addTarget = true;
			
			Enumeration properties = hashtable.keys();
			while( properties.hasMoreElements() )
			{
				//get the first hashtable item
				key = (String)properties.nextElement();
				value = (Boolean)hashtable.get((Object)key);
				
				//does the item exist in the target atom properties ?
				Object pv = target.getProperty(key);
				
				if( !pv.equals(value) )
					addTarget = false;
			}
			
			//if all the hashtable properties exist in the target atom, add it as a result.
			if( addTarget )
				result.addElement( target );
		
		}
		return new ResettableEnumeration(result);
	}
	/**
	* Returns an enumeration of all atoms within the candidates enumeration that are
	* descended from intersect
	* 
	* @param intersect				all returned atoms will be descended from this
	* @param candidates				an enumeration of atoms
	* @returns						A group of atoms all descended in some way from intersect.
	*/
	protected static Enumeration getDescendantAtomsOfAtom(Atom intersect, Enumeration candidates)
	{
		//set up a store for all atoms that qualify
		Vector result = new Vector();
		
		//loop through the supplied enumeration
		while( candidates.hasMoreElements() )
		{
			Atom target = (Atom)candidates.nextElement();
			
			//is the atom from the enumeration a descendant of the supplied Atom
			if( target.isDescendantOf( intersect ) )
				result.addElement(target);
		}
		
		return new ResettableEnumeration(result);
	}
	
	/**
	* Returns an enumeration of all atoms within the candidates enumeration that pass the isness
	*  adjective threshold test.
	*
	* @param intersect
	* @param candidates
	*/
	protected static Enumeration getDescendantAtomsOfAdjective(String intersect, Enumeration candidates)
	{
	    Vector result = new Vector();
	    
	    while( candidates.hasMoreElements() )
	    {
	        Atom dream = (Atom)candidates.nextElement();
	        Object property = dream.getRawProperty(intersect);
	        
	        //does the property exceed the isness threshold ?  
            if(  property != null && (AtomData.toInt(property) >= BinderEnumeration.isnessThreshold)  )
	            result.addElement( dream );
	    }
	    
	    return new ResettableEnumeration(result);
	}
	
	/**
	* Takes a Vector of Words and converts them into a tree of NounPhrases.
	* The process takes note of the following word types:
	*
	*		Noun			-	Forms the core of the particular NounPhrase Object. The string 
	*							form gets assigned to the NounPhrases identifier property, the 
	*							atom to the noun field and also to the Nouns vector.
	*		Preposition		-	The discovery of a preposition causes the birth of the child 
	*							nounphrase attached to the current NP. The current NPs modifier
	*							field is also set to one of the modifier types.
	*		Adjective		-	Added to the adjective vector of the current NP.
	*		Numeric			- 	Sets the count property.
	*		Pronoun			-	The prescence of a pronoun sets a flag and then exits the Word
	*							traversal.
	*		Negation		-	Functionally similar to the preposition handling, this type sets
	*							a different flag.
	*
	* The main purpose of the Word vector compilation is to present it in a form that will make
	* binding as painless as possible.
	*
	* @param world				for the purpose of looking up atoms
	* @param target				a Vector of Word Objects
	* @param argument			a int bitmap containing information directed at the binder
	*/
	private void compile(World world, Vector target)
	{
		//set up the root NP with defaults
		NounPhrase current = this;
		//if it may be that a noun is not specified, so we insert
		// this portable reference to mean everything.
		setNoun( world.getAtom("portable") );
		setModifier( MOD_NOTHING );
		setCount(1);
		
		// we step backwards through the target vector of words
		for(int i=target.size()-1;i>-1;i--)
		{
			Word word = (Word)target.elementAt(i);
			
			if( word.getType() == Word.WT_NOUN )
			{
				current.addIdentifier( word.getValue() );
				current.setNoun( world.getVocabulary().getNoun( word.getValue() ) );
				current.addNoun( world.getVocabulary().getNoun( word.getValue() ) );
			}
			
			// Prepositions are the points at which we add a child
			else if( word.getType() == Word.WT_PREPOSITION )
			{
				current.setModifier(MOD_IN);
				current.setModifierString( word.getValue() );
				
				NounPhrase addition = new NounPhrase();
				addition.setNoun( world.getAtom("portable") );
				addition.setModifier( MOD_NOTHING );
				addition.setCount(1);
				
				current.child = addition;
				current = current.child;
				//current.parent = this;
			}
			
			//adjectives get lookup up into their property names before they are added.
			else if( word.getType() == Word.WT_ADJECTIVE )
				current.addAdjective( world.getVocabulary().getAdjective( word.getValue() ) );
			
			else if( word.getType() == Word.WT_NUMERIC )
				current.setCount( word.getNumericValue() );
			
			else if( word.getType() == Word.WT_PRONOUN )
			{
				setPronounStatus( true );
				return;
			}
			
			// Just like prepositions
			else if( word.getType() == Word.WT_NEGATION )
			{
				current.setModifier(MOD_EXCEPT);
				current.setModifierString(word.getValue());
				
				NounPhrase addition = new NounPhrase();
				addition.setNoun( world.getAtom("portable") );
				addition.setModifier( MOD_NOTHING );
				addition.setCount(1);
				
				current.child = addition;
				current = current.child;
				//current.parent = this;
			}
			
		}
	}

	/**
	* Wraps up a directional NounPhrase into an Integer
	*
	* @param target				a vector of Words that make up the NounPhrase
	* @returns					an Integer representing the direction
	*/
	public static Integer compileToDirection(Vector target)
	{
		//basically return the first direction found
		for(int i=0;i<target.size();i++)
		{
			Word word = (Word)target.elementAt(i);
			if( word.getType() == Word.WT_DIRECTION )
			{
				//add the direction to the NP. it is here that the direction gets turned
				// into an internal type - in this case an integer.
				return new Integer( ExitTable.toDirection(word.getValue()) );
			}
		}
		return null;
	}
	
	/**
	* compiles a vector of words into
	*/
	public static Integer compileToNumeric(Vector target)
	{
		for(int i=0;i<target.size();i++)
		{
			Word word = (Word)target.elementAt(i);
			if( word.getType() == Word.WT_NUMERIC )
			{
				//add the direction to the NP. it is here that the direction gets turned
				// into an internal type - in this case an integer.
				return new Integer( word.getNumericValue() );
			}
		}
		return null;
	}
	
	/**
	* compiles a specified NounPhrase Vector of Words into a string
	*
	* @param target				a vector of Words that make up the NounPhrase
	* @returns					a complete String of the Words
	*/
	public static String compileToString(Vector target)
	{
		//Where we store the final string
		StringBuffer result = new StringBuffer();
		Word temp;
		
		//loop through all the Words, adding their string values onto the result
		for( int i=0;i<target.size();i++)
			result.append( ((Word)target.elementAt(i)).getValue() + " ");
		
		return result.toString().trim();
	}
	

    //------------------------------ Accessor/Mutator ----------------------------------//
	
	/** 
	* As argument is a bit property int, do relevent check
	
    * @param type			the type to check for
    */
    public boolean checkForArgumentType(int type)
    {
    	if( (argument & type) != 0 ) 
            return true;
        else 
            return false;
    }
    
	/**
	* Returns this NounPhrases identifier
	*
	* @returns				the NounPhrase identifier string
	*/
	public String getIdentifier()
	{
		return identifier;
	}
	/**
	* Sets this NounPhrases identifier 
	*
	* @param identifier		the new identifier for this NounPhrase
	*/
	public void setIdentifier(String identifier)
	{
		this.identifier = identifier;
	}
	/**
	* Makes an addition to the beginning of the Identifier property.
	* This is neccessay because single objects may be referenced using 
	* two nouns.
	*
	* @param ident			the addition to the indentifier property
	*/
	public void addIdentifier(String ident)
	{
		this.identifier = ident + " " + this.identifier;
	}
	
	
	
	/**
	* Returns the pronoun status of this NounPhrase 
	* (whether this NP represents a pronoun)
	*
	* @returns				the pronoun status of the NounPhrase
	*/
    public boolean getPronounStatus()
    {
        return pronounStatus;
    }
    /**
    * Sets the pronoun status (whether this NP represents a pronoun)
    *
    * @param pronounStatus	the new pronoun status of the NounPhrase
    */
    public void setPronounStatus(boolean pronounStatus)
    {
        this.pronounStatus = pronounStatus;
    }

	/**
	* Returns the NounPhrases count
	*
	* @returns				the NounPhases count property
	*/
	public int getCount()
	{
		return count;
	}
	
	/**
	* Sets the NounPhrases count
	*
	* @param count			the new count of this NounPhrase
	*/
	public void setCount(int count)
	{
		this.count = count;
	}
	
	/**
	*
	*/
    public int getArgument()
    {
        return argument;
    }
    
    /**
	*
	*/
    public void setArgument(int argument)
    {
        this.argument = argument;
    }

	/**
	*
	*/
    public byte getModifier()
    {
        return modifier;
    }
    
    /**
	*
	*/
    public void setModifier(byte modifier)
    {
        this.modifier = modifier;
    }

	/**
	*
	*/
	public String getModifierString()
	{
		return modifierString;
	}
	
	/**
	*
	*/
	public void setModifierString(String modifierString)
	{
		this.modifierString = modifierString;
	}
	
	/**
	*
	*/
    public void addAdjective(String adjective)
    {
        adjectives.addElement(adjective);
    }

	/**
	*
	*/
    public void addNoun(Atom noun)
    {
        nouns.addElement(noun);
    }
    
    /**
	*
	*/
    public void setNoun(Atom noun)
    {
    	this.noun = noun;
    }
    
    /**
	*
	*/
    public Atom getNoun()
    {
    	return noun;
    }
}
