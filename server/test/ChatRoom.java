// $Id: ChatRoom.java,v 1.5 1999/09/01 16:07:01 jim Exp $
// Chat room for "Yet another chat application".
// James Fryer, 2 June 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.apps;

import java.io.*;
import java.util.*;

/** A chat room represents a public area for people to chat in
*/
public class ChatRoom
    {
    /** An ID for this room
    */
    private String roomID;
    
    /** The people in this room, indexed by their person ID.
    */
    Hashtable people = new Hashtable();
    
    public ChatRoom(String roomID)
        {
        this.roomID = roomID;
        }
    
    /** Open the chat room
    */
    public synchronized void open()
        {
        }
        
    /** Close the chat room
    */
    public synchronized void close()
        {
        // Broadcast closing down message to everyone 
        //###
        }
        
    /** Find a person in the room
    */
    public ChatPerson getPerson(String userID)
        {
        return (ChatPerson)people.get(userID);
        }
    
    /** Enumerate the people
    */
    public final Enumeration getPeople()
        {
        return people.elements();
        }
    
    /** Add a person to the room
    */
    public synchronized void addPerson(ChatPerson person)
        {
        people.put(person.getPersonID(), person);
        }
        
    /** Remove a person from the room
    */
    public synchronized void removePerson(ChatPerson person)
        {
        people.remove(person.getPersonID());
        }
        
    /** Send a message to the room
    */
    public synchronized void broadcast(String s)
        {
        Enumeration enum = people.elements();
        while (enum.hasMoreElements())
            {
            ChatPerson person = (ChatPerson)enum.nextElement();
            person.output(s);
            }
        }

    /** Send a message to the room, except one user
    */
    public synchronized void broadcast(String s, ChatPerson exceptMe)
        {
        Enumeration enum = people.elements();
        while (enum.hasMoreElements())
            {
            ChatPerson person = (ChatPerson)enum.nextElement();
            if (person != exceptMe)
                person.output(s);
            }
        }

    /** Start the chat session
    */
    public void startSession(ChatPerson person)
        {
        broadcast(person.getPersonID() + " has arrived.", person);
        person.output("Hello. Welcome to chat room " + roomID + ".");
        }

    /** End the chat session 
    */
    public void stopSession(ChatPerson person)
        {
        broadcast(person.getPersonID() + " has left.", person);
        person.output("Bye. Thanks for visiting " + roomID + ".");
        }

    /** SAY command
    */
    public synchronized void say(ChatPerson person, String s)
        {
        person.output("You say: " + s);
        String userName = person.getPersonID();
        broadcast(userName + " says: " + s, person);
        }

    /** WHISPER command
    */
    public synchronized void whisper(ChatPerson person, ChatPerson otherPerson, String s)
        {
        otherPerson.output(person.getPersonID() + " whispers: " + s);
        person.output("You whisper to " + otherPerson.getPersonID() +": " + s);
        }

    /** WHO command
    */
    public synchronized void who(ChatPerson person)
        {
        Enumeration enum = people.elements();
        while (enum.hasMoreElements())
            {
            ChatPerson otherPerson = (ChatPerson)enum.nextElement();
            person.output(otherPerson.getPersonID());
            }
        }
    }
