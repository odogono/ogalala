// $Id: HTMLAtomFormatter.java,v 1.9 1999/03/12 15:26:18 matt Exp $
// Class to write atom documentation to HTML
// Matthew Caldwell, 16 November 1998
// Copyright (c) Ogalala Limited <info@ogalala.com>

package com.ogalala.tools;

import com.ogalala.util.*;
import com.ogalala.mua.*;
import java.util.*;
import java.io.*;

/**
 *  A class that formats atom documentation as HTML files.
 */
public class HTMLAtomFormatter
	extends AtomFormatter
{
	//-------------------------------------------------------------
	//  class variables
	//-------------------------------------------------------------

	protected static final String INDEX_TITLE = "Atom Index".intern();
	protected static final String INDEX_FILE = "index.html".intern();
	protected static final String TREE_TITLE = "Atom Hierarchy".intern();
	protected static final String TREE_FILE = "tree.html".intern();
	protected static final String NOUNS_TITLE = "Nouns".intern();
	protected static final String NOUNS_FILE = "nouns.html".intern();
	protected static final String LOCAL_COLOUR = "teal".intern();
	protected static final String INHERITED_COLOUR = "purple".intern();

	protected static final int TABLE_COLUMNS = 5;
	
	//-------------------------------------------------------------
	//  construction
	//-------------------------------------------------------------

	/** Default constructor. */
	public HTMLAtomFormatter ()
	{
		super ();
	}
	
	//-------------------------------------------------------------
	//  usage
	//-------------------------------------------------------------

	/**
	 *  Write documentation on the given atom to an HTML file.
	 *  The name of the resulting file will be the name of the
	 *  atom prefixed with the specified <tt>outputPrefix</tt> and
	 *  suffixed with ".html". Links will be included to any other
	 *  atoms using the assumption that they too have been documented
	 *  by this object using the same <tt>init()</tt> parameters.
	 */
	public void documentAtom ( Atom atom )
	{
		String outName = outputPrefix + atom.getID() + ".html";
		
		String origin = (String) atom.getField("__");
		if ( origin == null )
		{
			origin = "(internal)";
		}
		
		try
		{
			PrintWriter out = new PrintWriter ( new FileWriter( outName ), true );
			
			printHeader ( out, atom.getID() );
			
			// at the top we list the basic details of the atom:
			// name, type, parents, children, nouns, source file
			// and atom comments
						
			// static type
			out.println ( "<b>Static (Java) type:</b> " + atom.getClassName() );
			out.println ( "<p>" );
			
			// inheritance
			out.println ( "<b>Parents:</b>" );
			out.println ( "<ul>" );
			
			Enumeration parents = atom.getParents();
			while ( parents.hasMoreElements() )
			{
				// assume that there is a doc file for the parent
				// that we can link to
				Atom parent = (Atom) parents.nextElement();
				out.println ( "<li><a href=\""
							  + localPrefix + parent.getID() + ".html"
							  + "\">"
							  + parent.getID()
							  + "</a></li>" );
			}
			
			out.println ( "</ul>" );
			out.println ( "<p>" );
			
			if ( atom.hasChildren() )
			{
				out.println ( "<b>Children:</b>" );
				out.println ( "<ul>" );
				
				Enumeration children = atom.getChildren();
				while ( children.hasMoreElements() )
				{
					// assume that there is a doc file for the child
					Atom child = (Atom) children.nextElement();
					out.println ( "<li><a href=\""
								  + localPrefix + child.getID() + ".html"
								  + "\">"
								  + child.getID()
								  + "</a></li>" );
				}
				
				out.println ( "</ul>" );
				out.println ( "<p>" );
			}
			
			// nouns
			Vector nounList = atom.getWorld().getVocabulary().getAtomNouns( atom );
			
			if ( nounList != null
				 && nounList.size() > 0 )
			{
				out.println ( "<a href=\""
							  + localPrefix + "nouns.html"
							  + "\"><b>Nouns:</b></a>" );
				out.println ( "<ul>" );
				
				Enumeration theNouns = nounList.elements();
				while ( theNouns.hasMoreElements() )
				{
					out.println ( "<li>"
								  + theNouns.nextElement()
								  + "</li>" );
				}
				
				out.println ( "</ul>" );
				out.println ( "<p>" );
			}
			
			// source files
			out.println ( "<b>Source script:</b>" );
			if ( origin == null )
				out.println ( "Unknown" );
			else
				out.println ( origin );
			
			out.println ( "<br>" );
			out.println ( "<b>Source ODL:</b>" );
			
			String sourceODL = (String) atom.getField ( "__odl__" );
			if ( sourceODL != null )
				out.println ( sourceODL );
			else
				out.println ( "Unknown" );
			
			out.println ( "<p>" );
					
			// general comments
			String comments = (String) atom.getField( "__" + atom.getID() );
			if ( comments != null )
				out.println ( comments );
			
			out.println ( "<p>" );
			
			// get a table of all the property values
			// (both local and inherited)
			Hashtable properties = new Hashtable();
			atom.getProperties( properties );
			
			// next we put an alphabetical list of all property names,
			// with links to their descriptions
			printPropertyDir ( out, properties.keys() );

			// next we have descriptions of all the properties,
			// organized according to where they're inherited from
			
			// we keep a list of already-documented field names
			// so that we don't document them again
			Vector doneFields = new Vector();
			
			// start with local fields
			
			// only bother to print the "Properties defined by"
			// heading if a property is actually defined
			boolean printedHeading = false;
			
			Enumeration fieldNames = atom.getFieldNames();
			while ( fieldNames.hasMoreElements() )
			{
				String fieldName = ((String) fieldNames.nextElement()).intern();
				
				// skip documentation fields
				if ( fieldName.startsWith("_") )
					continue;
				
				if ( ! printedHeading )
				{
					printedHeading = true;
					out.println ( "<hr>" );
					out.println ( "<h2>Properties defined by "
								  + atom.getID()
								  + "</h2>" );
					out.println ( "<dl>" );
				}
				
				printProperty ( out, atom, fieldName, properties, LOCAL_COLOUR );
				
				doneFields.addElement ( fieldName );
			}
			
			// the local heading opens a definition list, so
			// if that has been done we need to close it
			if ( printedHeading )
				out.println ( "</dl>" );
			
			// properties inherited from ancestors
			Enumeration ancestors = atom.getAncestors();
			
			while ( ancestors.hasMoreElements() )
			{
				// only bother to print the "Properties inherited from"
				// heading if a property actually *is* inherited
				printedHeading = false;
				
				Atom ancestor = (Atom) ancestors.nextElement();
				
				fieldNames = ancestor.getFieldNames();
				
				while ( fieldNames.hasMoreElements() )
				{
					String fieldName = ((String) fieldNames.nextElement()).intern();
					
					// skip documentation/internal fields AND already documented fields
					if ( fieldName.startsWith("_") || doneFields.contains( fieldName ) )
						continue;
					
					doneFields.addElement ( fieldName );
					
					if ( ! printedHeading )
					{
						out.println ( "<hr>" );
						
						out.println ( "<h2>Properties inherited from "
									  + "<a href=\""
									  + localPrefix + ancestor.getID()
									  + ".html\">"
									  + ancestor.getID()
									  + "</a></h2>" );
						
						out.println ( "<dl>" );
						
						printedHeading = true;
					}
					
					// write out the property definition
					printProperty ( out, atom, fieldName, properties, INHERITED_COLOUR );
				}
				
				// the ancestor heading opens the definition list,
				// so if that's done we need to close it
				if ( printedHeading )
					out.println ( "</dl>" );	
			}

			printFooter ( out );
			
			out.flush();
			out.close();			
		}
		catch ( IOException e )
		{
			err.println ( "ERROR: exception writing file "
						  + outName
						  + ": " + e );
		}
	}
	
	//-------------------------------------------------------------

	/**
	 *  Write an alphabetical index of the atoms as "index.html"
	 *  and an indented tree as "tree.html".
	 */
	public void indexAtoms ( boolean includeThings )
	{
		writeAtomList( includeThings );
		writeAtomTree( includeThings );
	}
	
	//-------------------------------------------------------------

	/**
	 *  Write an alphabetical list of atoms, with links to the
	 *  doc files for each atom.
	 */
	public void writeAtomList ( boolean includeThings )
	{	
		String outName = outputPrefix + INDEX_FILE;
		
		Enumeration atoms = world.getAtoms();
		StringVector names = new StringVector();
		
		// gather all the atom names into a list
		// (including only Atoms if necessary)
		while ( atoms.hasMoreElements() )
		{
			Atom anAtom = (Atom) atoms.nextElement();
			if ( (!includeThings)
				 && (! "Atom".equals(anAtom.getClassName()) ) )
				continue;
			
			names.addElement( anAtom.getID() );
		}
		
		// sort the list alphabetically (I hope)
		names.sort();
		
		// dump the list to the index file
		try
		{
			PrintWriter out = new PrintWriter ( new FileWriter( outName ), true );
			
			// header
			printHeader ( out, INDEX_TITLE );;

			// print all atoms
			atoms = names.elements();
			
			out.println ( "<ul>" );
			
			while ( atoms.hasMoreElements() )
			{
				String id = (String) atoms.nextElement();
				
				out.println ( "<li><a href=\""
						  	  + localPrefix + id + ".html"
						  	  + "\">"
						  	  + id
						  	  + "</a>" );
			}
			
			out.println ( "</ul>" );

			printFooter ( out );
			
			out.flush();
			out.close();			

		}
		catch ( IOException x )
		{
			err.println ( "ERROR: exception writing file "
						  + outName
						  + ": " + x );
		}
	}
	
	//-------------------------------------------------------------

	/**
	 *  Write an indented tree of atoms, with links to the
	 *  doc files for each atom.
	 */
	public void writeAtomTree ( boolean includeThings )
	{	
		String outName = outputPrefix + TREE_FILE;
				
		Atom startAtom = world.getRoot();
		Stack stack = new Stack();
		Enumeration e = startAtom.getChildren();
		
		// start traversal
		Atom.clearAllMarks();
		
		try
		{
			PrintWriter out = new PrintWriter ( new FileWriter( outName ), true );
			
			// header
			printHeader ( out, TREE_TITLE );
		
			// print the start atom
			out.println ( "<ul compact type=disc>" );
			
			out.println ( "<li><a href=\""
						  + localPrefix + startAtom.getID() + ".html"
						  + "\">"
						  + startAtom.getID()
						  + "</a>" );
			
			// all the children are a nested sublist
			out.println ( "<ul compact type=disc>" );
			
			// while the enumeration or stack have data
			boolean finished = ! e.hasMoreElements();
			
			while ( !finished )
			{
				// get the next atom
				Atom atom = (Atom) e.nextElement();
				
				// skip Things etc if required
				if ( includeThings
					 || "Atom".equals(atom.getClassName()) )
				{
					out.println ( "<li><a href=\""
								  + localPrefix + atom.getID() + ".html"
								  + "\">"
								  + atom.getID()
								  + "</a>" );
				}
				
				// if the atom isn't marked, we need to process it and
				// iterate its children
				if ( !atom.isMarked() )
				{
					// mark the atom (only traverse children once)
					atom.setMark(true);
					
					if ( atom.hasChildren() )
					{
						// push the current list
						stack.push ( e );
						out.println ( "<ul compact type=disc>" );
						
						e = atom.getChildren();
					}
				}
				
				// if the current enum is finished, unwind the stack
				// until we find an enum that has elements left or
				// the stack is empty
				
				while ( !e.hasMoreElements() && !finished )
				{
					out.println ( "</ul>" );
					
					if ( stack.empty() )
						finished = true;
					else
						e = (Enumeration) stack.pop();
				}
			}

			// close the final list
			out.println ( "</ul>" );

			printFooter ( out );
			
			out.flush();
			out.close();			

		}
		catch ( IOException x )
		{
			err.println ( "ERROR: exception writing file "
						  + outName
						  + ": " + x );
		}
	}

	//-------------------------------------------------------------
	
	/**
	 *  Output the nouns table to a file called "nouns.html"
	 *  (with the outputPrefix, if any, prepended), containing
	 *  links to the atom files that define everything.
	 */
	public void documentNouns ( Hashtable nouns )
	{
		String outName = outputPrefix + NOUNS_FILE;
		
		try
		{
			PrintWriter out = new PrintWriter ( new FileWriter( outName ), true );
			
			// this is a lot simpler than the other one,
			// basically just a list of links
			
			// header
			printHeader ( out, NOUNS_TITLE );
			
			// nouns
			out.println ( "The following list shows which atoms define each noun." );
			
			// this would probably be better done as a table,
			// but for the moment I'm doing it as a list 'cos it's easier
			out.println ( "<ul>" );
			
			// sort the names into alphabetical order
			Enumeration nounNames = nouns.keys();
			StringVector names = new StringVector();
			while ( nounNames.hasMoreElements() )
				names.addElement ( nounNames.nextElement() );
			names.sort();
			nounNames = names.elements();
			
			while ( nounNames.hasMoreElements() )
			{
				// assume that there is a doc file for the atom
				// that we can link to
				String name = (String) nounNames.nextElement();
				String atom = (String) nouns.get( name );
				out.println ( "<li><b>"
							  + name
							  + "</b> : <a href=\""
							  + localPrefix + atom + ".html"
							  + "\">"
							  + atom
							  + "</a></li>" );
			}
			
			out.println ( "</ul>" );
			
			printFooter ( out );
			
			out.flush();
			out.close();			
		}
		catch ( IOException e )
		{
			err.println ( "ERROR: exception writing file "
						  + outName
						  + ": " + e );
		}
	}

	//-------------------------------------------------------------
	//  utilities
	//-------------------------------------------------------------
	
	/**
	 *  Write the HTML file header to specified output stream.
	 */
	protected void printHeader ( PrintWriter out, String title )
	{
		// html header
		out.println ( "<html>" );
		out.println ( "<!-- Auto-generated by DocCompiler"
					  + new Date()
					  + "-->" );
		out.println ( "<head>" );
		out.println ( "<title>" + title + "</title>" );
		out.println ( "</head>" );
		
		// html body
		out.println ( "<body bgcolor=white>" );
		out.println ( "<a name=\"__top__\"></a>" );
		
		// top line cross-references other files
		if ( title == INDEX_TITLE )
			out.println ( "index&nbsp;&nbsp;" );
		else
			out.println ( "<a href=\""
						  + localPrefix
						  + INDEX_FILE
						  + "\">index</a>&nbsp;&nbsp;" );
		
		if ( title == TREE_TITLE )
			out.println ( "tree&nbsp;&nbsp;" );
		else
			out.println ( "<a href=\""
						  + localPrefix
						  + TREE_FILE
						  + "\">tree</a>&nbsp;&nbsp;" );
		
		if ( title == NOUNS_TITLE )
			out.println ( "nouns" );
		else
			out.println ( "<a href=\""
						  + localPrefix
						  + NOUNS_FILE
						  + "\">nouns</a>" );
		
		out.println ( "<hr>" );

		// title
		out.println ( "<h1>" + title + "</h1>" );

	}

	//-------------------------------------------------------------

	/**
	 *  Write the HTML file footer to the specified output stream.
	 */
	protected void printFooter ( PrintWriter out )
	{
		out.println ( "<a name=\"__bottom__\"></a>" );
		out.println ( "<p>" );
		out.println ( "<hr>" );
		out.println ( "<i>Copyright &copy; 1998 Ogalala Ltd</i>" );
		out.println ( "</body>" );
		out.println ( "</html>" );
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Write HTML formatted details of a single field or
	 *  property to the given stream. The property is written
	 *  as a definition list item. It is up to the caller
	 *  to provide the surrounding &lt;dl&gt; and &lt;/dl&gt;
	 *  tags.
	 */
	protected void printProperty ( PrintWriter out,
								   Atom atom,
								   String propertyName,
								   Hashtable properties,
								   String color )
	{
		// each property is a definition list entry
		out.println ( "<dt>" );

		// the property has an anchor which is just the property name
		out.println ( "<a name=\"" + propertyName + "\"></a>" );
		
		out.println ( "<font size=+1 color="
					  + color
					  + "><b>&#149; "
				 	  + propertyName
				 	  + "</b></font><br>" );
		
		out.println ( "<dd>" );
		
		Object fieldValue = properties.get( propertyName );
		String fieldType;
		
		switch ( AtomData.getFieldType( fieldValue ))
		{
			case AtomData.ATOM:
				fieldType = "Atom"; break;
			case AtomData.STRING:
				fieldType = "String"; break;
			case AtomData.INTEGER:
				fieldType = "Integer"; break;
			case AtomData.BOOLEAN:
				fieldType = "Boolean"; break;
			case AtomData.LIST:
				fieldType = "List"; break;
			case AtomData.TABLE:
				fieldType = "Table"; break;
			case AtomData.ACTION:
				fieldType = "Action"; break;
			case AtomData.NULL:
				fieldType = "Null"; break;
				
			case AtomData.UNKNOWN:
			default:
				fieldType = "Unknown";
		}
		
		out.println ( "<b>Type:</b> "
					  + fieldType
					  + "<br>" );
		
		// printing the value may be a mistake, since it could contain
		// characters that will fuck up the HTML -- should add some
		// detox process later
		out.println ( "<b>Value:</b>" );
		
		// Actions have to be treated specially so they look right
		if ( fieldType.equals("Action") )
			out.println ( "!" + fieldValue.toString() );
		else
			out.println ( AtomData.toString( fieldValue ) );
			
		out.println ( "<br>" );
		
		
		// get property comments, if any
		String comments = (String) properties.get ( "__" + propertyName );
		
		if ( comments != null )
			out.println ( comments );
		
		out.println ( "<p>" );
	}
	
	//-------------------------------------------------------------
	
	/**
	 *  Write an alphabetical menu of an atoms property names,
	 *  with links to the definitions.
	 */
	protected void printPropertyDir ( PrintWriter out,
									  Enumeration names )
	{
		// this whole section is omitted if there are no names
		if ( ! names.hasMoreElements() )
			return;
		
		// generate an alphabetized list of the names with
		// links to their definitions
		StringVector nameList = new StringVector ();
		
		while ( names.hasMoreElements() )
		{
			String name = (String) names.nextElement();
			
			// skip doc comments and internal properties
			if ( name.startsWith("_") )
				continue;
			
			nameList.addElement( "<a href=\"#"
								 + name
								 + "\">"
								 + name
								 + "</a>" );
		}
		
		// if there are no properties, skip this whole section
		if ( nameList.size() < 1 )
			return;
		
		nameList.sort();

		// print the section header
		out.println ( "<hr>" );
		out.println ( "<h2>Properties</h2>");
		
		HTMLUtil.tabulate ( out,
							nameList,
							TABLE_COLUMNS,
							"border=0",
							"align=left",
							"width=100" );
							
		out.println ( "<p>" );
	}

	//-------------------------------------------------------------
}