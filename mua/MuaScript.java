// $Id: MuaScript.java,v 1.3 1999/04/19 17:43:03 matt Exp $
// Core functions for the MUA scripting facility.
// Matthew Caldwell, 14 April 1999
// Copyright (C) HotGen Studios Ltd <www.hotgen.com>

package com.ogalala.mua;

import java.util.*;
import com.ogalala.util.*;

/**
 *  This class implements some shared functionality of the
 *  scripting structures used by the NPC classes and the
 *  string substitution process. It does not (at present)
 *  provide any functions for actually executing script commands.
 *  That is done in the NPCBase action class.
 */
public class MuaScript
{
	//--------------------------------------------------------------
	//  class variables
	//--------------------------------------------------------------
	
	public static final String NONE = "none";
	public static final String IF = "if";
	public static final String CASE = "case";
	public static final String INC = "inc";
	public static final String LOOP = "loop";
	public static final String RANDOM = "random";
	public static final String PROPERTY_START = "{";
	public static final String PROPERTY_END = "}";

	private static Random rng = new Random ();
	
	//--------------------------------------------------------------
	//  class methods
	//--------------------------------------------------------------

	/**
	 *  Determine the "outcome" of the control structures
	 *  in a script (ie, the element in the script that is
	 *  to be executed or output). This method selects only
	 *  from the top-level script list. If the selected item
	 *  is itself a list, that is what is returned. If you want
	 *  to descend into sub-lists, use the <tt>recursiveSelect()</tt>
	 *  method.
	 *  <p>
	 *  If the supplied script is not a Vector, it is itself
	 *  returned. Otherwise, the first element is treated as
	 *  an <i>operator</i> and used to determine which of the
	 *  subsequent elements is returned. The following operators
	 *  are supported:
	 *  <ul>
	 *  <li>"none", <tt>null</tt>, any object which the AtomData
	 *      class evaluates as an integer less than 1: <tt>null</tt>
	 *      is returned
	 *  <li>"random": one of the remaining elements in the vector
	 *      is chosen at random and returned
	 *  <li>"if{<i>property</i>}": if the property is TRUE returns
	 *      the object at index 1 in the vector, otherwise returns the
	 *      object at index 2
	 *  <li>"case{<i>property</i>}": returns the object at the specified
	 *      element in the vector, or <tt>null</tt> if there aren't
	 *      enough elements
	 *  <li>"inc{<i>property</i>}": the same as "case", but it also
	 *      increments the value of the property (without looping)
	 *  <li>"loop{<i>property</i>}": the same as "case", but it also
	 *      increments the value of the property, looping back to 1
	 *      when the value exceeds the number of items in the list
	 *  <li>"{property}": gets the property's value and treats is as
	 *      below
	 *  <li>any other object: if the AtomData class determines that
	 *      the object is a BOOLEAN, it is treated as in the "if" case
	 *      above; if the AtomData class determines that the object is
	 *      an INTEGER, it is treated as in the "case" case above;
	 *      if the AtomData class determines that the object is NULL,
	 *      it is treated as the "none" case above; otherwise, the object
	 *      itself is returned.
	 *  </ul>
	 */
	public static Object select ( Object script,
								  Event event )
	{
		// non-vectors just get returned
		if ( script == null
			 || ! (script instanceof Vector) )
			return script;
		
		Vector list = (Vector) script;
		
		if ( list.size() <= 0 )
			return null;
		
		Object control = list.elementAt(0);
		
		if ( control instanceof Boolean )
			return ifSelect ( list, ((Boolean) control).booleanValue() );
		
		if ( control instanceof Integer )
			return caseSelect ( list, ((Integer) control).intValue() );
		
		if ( control instanceof String )
		{
			String str = ((String) control).trim().toLowerCase();
			
			// operator NONE
			if ( str.equals(NONE) )
				return null;
			
			// operator RANDOM
			if ( str.equals(RANDOM) )
			{
				// randomly select one of the remaining elements in the
				// field
				if ( list.size() <= 1 )
					return null;
				else
					return list.elementAt ( Math.abs ( rng.nextInt() ) % ( list.size() - 1 ) + 1 );
			}
			
			// operator IF
			if ( str.startsWith ( IF ) )
				return ifSelect ( list,
								  AtomData.toBool ( getProperty ( str, event ) ) );
			
			// operator CASE
			if ( str.startsWith ( CASE ) )
				return caseSelect ( list,
									AtomData.toInt ( getProperty ( str, event ) ) );
			
			// operator INC
			if ( str.startsWith ( INC ) )
			{
				int index = AtomData.toInt ( getProperty ( str, event ) );
				setProperty ( str, event, new Integer ( index + 1 ) );
				return caseSelect ( list, index );
			}
			
			// operator LOOP
			if ( str.startsWith ( LOOP ) )
			{
				int index = AtomData.toInt ( getProperty ( str, event ) );
				
				if ( index >= list.size() )
					setProperty ( str, event, new Integer ( 1 ) );
				else
					setProperty ( str, event, new Integer ( index + 1 ) );
				
				return caseSelect ( list, index );
			}
			
			// any property
			if ( str.startsWith ( PROPERTY_START ) )
			{
				control = getProperty ( str, event );
			}	
		}
		
		// any arbitrary control object -- note that unhandled
		// strings will get here, but there currently is no
		// checking for strings like "TRUE" and "5"
		switch ( AtomData.getFieldType ( control ) )
		{
			// booleans are treated as IF statements
			case AtomData.BOOLEAN:
				return ifSelect ( list, AtomData.toBool ( control ) );
			
			// integers are treated as CASE statements
			case AtomData.INTEGER:
				return caseSelect ( list, AtomData.toInt ( control ) );
			
			// in all other cases, the control object itself is returned
			default:
				return control;
		}
	}
	
	//--------------------------------------------------------------
	
	/**
	 *  Determine the "outcome" of the control structures in a
	 *  script, descending into any resulting lists until a
	 *  non-vector outcome is achieved. For details of the
	 *  operators supported, see the notes for the <tt>select()</tt>
	 *  method.
	 */
	public static Object recursiveSelect ( Object script,
										   Event event )
	{
		while ( script instanceof Vector )
			script = select ( script, event );
		
		return script;
	}
	
	//--------------------------------------------------------------
	
	/**
	 *  Return either the second or the third item in a list
	 *  depending on a supplied boolean value. If there are
	 *  less items in the list than the condition requires,
	 *  <tt>null</tt> is returned.
	 */
	public static Object ifSelect ( Vector list, boolean condition )
	{
		if ( condition )
			return caseSelect ( list, 1 );
		else
			return caseSelect ( list, 2 );
	}
	
	//--------------------------------------------------------------
	
	/**
	 *  Return the element at the specified index in a list.
	 *  If the index is less than 1 or greater than the index
	 *  of the last item in the list, <tt>null</tt> is returned.
	 */
	public static Object caseSelect ( Vector list, int index )
	{
		if ( index < 1 || index >= list.size() )
			return null;
		
		return list.elementAt ( index );
	}
	
	//--------------------------------------------------------------
	
	/**
	 *  Extract a property specification from the supplied
	 *  string and get its value in the context of the given
	 *  event. The property specification is assumed to be
	 *  contained between the first matching pair of curly
	 *  braces in the string. All other characters are
	 *  stripped, so this call can be used with strings like
	 *  "case { current.index }". The actual interpretation
	 *  of the spec is handled by the LValue class.
	 */
	public static Object getProperty ( String propertySpec,
									   Event event )
	{
		if ( propertySpec == null
			 || propertySpec.indexOf ( PROPERTY_START ) == -1
			 || propertySpec.indexOf ( PROPERTY_START ) + 1 >= propertySpec.length() )
			return null;
		
		propertySpec = propertySpec.substring ( propertySpec.indexOf( PROPERTY_START ) + 1 );
		
		if ( propertySpec.indexOf ( PROPERTY_END ) != -1 )
			propertySpec = propertySpec.substring ( 0, propertySpec.indexOf ( PROPERTY_END ) );
		
		LValue eval = new LValue ( event.getCurrent(), event );
		eval.parse ( propertySpec );
		
		return eval.getAtom().getProperty ( eval.getFieldID() );
	}
	
	//--------------------------------------------------------------
	
	/**
	 *  Exttract a property specification from the supplied
	 *  string and set its value to the supplied value. The
	 *  property specification is assumed to be contained
	 *  between the first matching pair of curly braces in
	 *  the string. All other characters are stripped, so
	 *  this call can be used with strings like 
	 *  "inc { actor.count }". The actual interpretation of
	 *  the spec is handled by the LValue class.
	 */
	public static void setProperty ( String propertySpec,
									 Event event,
									 Object value )
	{
		if ( propertySpec == null
			 || propertySpec.indexOf ( PROPERTY_START ) == -1
			 || propertySpec.indexOf ( PROPERTY_START ) + 1 >= propertySpec.length() )
			return;
		
		propertySpec = propertySpec.substring ( propertySpec.indexOf( PROPERTY_START ) + 1 );
		
		if ( propertySpec.indexOf ( PROPERTY_END ) != -1 )
			propertySpec = propertySpec.substring ( 0, propertySpec.indexOf ( PROPERTY_END ) );
		
		LValue eval = new LValue ( event.getCurrent(), event );
		eval.parse ( propertySpec );
		
		eval.getAtom().setProperty ( eval.getFieldID(), value );		
	}
	
	//--------------------------------------------------------------
}