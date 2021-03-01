// $Id: DummyGameApp.java,v 1.1 1998/07/02 14:20:55 jim Exp $
// Output random valid game packets
// James Fryer, 2 July 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.server.apps;

import java.io.*;
import java.util.*;
import com.ogalala.util.*;
import com.ogalala.server.*;
import com.ogalala.server.apps.chat.*;

/** A Session Server application that implements a chat system
*/
public class DummyGameApp
    implements Application
    {
    /** The ID of this application
    */
    private String appID;

    public DummyGameApp()
        {
        }

    /** Create a new application. Implementations should avoid overwriting
        existing apps with the same ID.
    */
    public void create(String appID, Enumeration args)
        {
        // Pass on to 'open' as there is no persistent info
        open(appID, args);
        }

    /** Open an existing application
    */
    public void open(String appID, Enumeration args)
        {
        this.appID = appID;
        }

    /** Close this application. Preserve state, if appropriate.
    */
    public void close()
        {
        }

    /** Remove an application's persistent state.
        (USE WITH CARE!)
    */
    public void delete(String appID)
        {
        // Do nothing
        }

    /** Does an application called 'appID' exist?
    */
    public boolean exists(String appID)
        {
        // Always true because there is no persistent info
        return true;
        }

    /** Get the ID of this application
    */
    public String getID()
        {
        return appID;
        }

    /** Create a new channel
    */
    public Channel createChannel()
        {
        return new DummyGameChannel(this);
        }

    /** Get some information about this application
    */
    public String getInfo()
        {
        return "Fake game";
        }
    }

class DummyGameChannel
    implements Channel
    {
    DummyGameApp app;
    String channelID;
    String personID = "blah";
    User user;
    
    public DummyGameChannel(DummyGameApp app)
        {
        this.app = app;
        }
        
    /** Open the channel 
    */
    public void open(String channelID, User user, Enumeration args)
        throws ChannelOpenException
        {
        // Set up instance variables
        this.channelID = channelID;
        this.user = user;
        
        // Add to user and app 
        user.addChannel(this);
        }
    
    /** Close the channel and remove it from the user's list.
    */
    public void close()
        {
        user.removeChannel(this);
        }
    
    /** Get the channel ID, e.g. "nile/miss_marple".
        <p>
        Note this is named to avoid confusion with other IDs that may be posessed
        by the application object that implements the Channel interface.
    */
    public String getChannelID()
        {
        return channelID;
        }
    
    public String getUserID()
        {
        return personID;
        }
    
    /** Send input to the application
    */
    public void input(String s)
        {
        outputRandomPacket();
        }
    
    /** Receive output from the application
    */
    public void output(String s)
        {
        user.outputChannel(this, s);
        }
    
    /** Get the user
        ### (Do we need this???)
    */
    public User getUser()
        {
        return user;
        }
    
    private void outputRandomPacket()
        {
        final int N_PACKET_TYPES = 10;
        switch (random(N_PACKET_TYPES))
            {
        case 0: outputMisc(); break;
        case 1: outputError(); break;
        case 2: outputWhisper(); break;
        case 3: outputSay(); break;
        case 4: outputShout(); break;
        case 5: outputPerson(); break;
        case 6: outputThing(); break;
        case 7: outputRoom(); break;
        case 8: outputInventory(); break;
        case 9: outputStatus(); break;
            }
        }
        
    private void outputMisc()
        {
        switch (random(3))
            {
        case 0: output("type=misc msg='Phil drops the rock'"); break;
        case 1: output("type=misc msg='You feel a bit tired'"); break;
        case 2: output("type=misc msg='The floor looks too flimsy for your weight'"); break;
            }
        }

    private void outputError()
        {
        try {
            switch (random(3))
                {
            // These are deliberate errors!
            case 0: { Object o = null; o.toString(); } break;
            case 1: Debug.assert(false, "false"); break;
            case 2: { int y = 0; int x = 2/y; } break;
                }
            }
        catch (Exception e)
            {
            output("type=error msg='" + e + "'");
            }
        }
    
    private void outputWhisper()
        {
        switch (random(2))
            {
        case 0: 
            output("type=whisper from=" + getRandomName() + " to=" + getRandomName() + " msg="+ getRandomSpeech());
            break;
        case 1: 
            output("type=whisper from=" + getRandomName() + " to=" + getRandomName());
            break;
            }
        }

    private void outputSay()
        {
        switch (random(2))
            {
        case 0: 
            output("type=say from=" + getRandomName() + " to=" + getRandomName() + " msg="+ getRandomSpeech());
            break;
        case 1: 
            output("type=say from=" + getRandomName() + " msg="+ getRandomSpeech());
            break;
            }
        }

    private void outputShout()
        {
        switch (random(2))
            {
        case 0: 
            output("type=shout from=" + getRandomName() + " to=" + getRandomName() + " msg="+ getRandomSpeech());
            break;
        case 1: 
            output("type=shout from=" + getRandomName() + " msg="+ getRandomSpeech());
            break;
            }
        }

    private void outputPerson()
        {
        output("type=person name=" + getRandomName() + " description=" + getRandomSpeech());
        }

    private void outputThing()
        {
        output("type=thing name=" + getRandomName() + " description=" + getRandomSpeech());
        }

    private void outputRoom()
        {
        String peopleList = getRandomList(6);
        String thingList = getRandomList(6);
        String exitList = "[ 'north' 'a passage leads west' ]";
        
        switch (random(10))
            {
        case 0: case 1: case 2: case 3:
            output("type=room name=" + getRandomName() + " description=" + 
                    getRandomSpeech() + " people=" + peopleList + " contents=" + 
                    thingList + " exits=" + exitList);
            break;
        case 4:
            output("type=room name=" + getRandomName() + " update=add people=" + peopleList);
            break;
        case 5:
            output("type=room name=" + getRandomName() + " update=remove people=" + peopleList);
            break;
        case 6:
            output("type=room name=" + getRandomName() + " update=add contents=" + thingList);
            break;
        case 7:
            output("type=room name=" + getRandomName() + " update=remove contents=" + thingList);
            break;
        case 8:
            output("type=room name=" + getRandomName() + " update=add exits=" + exitList);
            break;
        case 9:
            output("type=room name=" + getRandomName() + " update=remove exits=" + exitList);
            break;
            }
        }

    private void outputInventory()
        {
        String thingList = getRandomList(10);
        switch (random(4))
            {
        case 0: case 1:
            output("type=inv contents=" + thingList);
            break;
        case 2:
            output("type=inv update=add contents=" + thingList);
            break;
        case 3:
            output("type=inv update=remove contents=" + thingList);
            break;
            }
        }

    private void outputStatus()
        {
        output("type=status score=0 msg='Status is a moveable feast'");
        }

    private String getRandomName()
        {
        final String names[] = 
            { 
            "tom","dick","harry","fred","burt","james","matter","fish",
            "craggy","island","niccy","henry","home","french","kiss",
            "glasses","brains","pop","stone","sexpot","miss","mr","man","black",
            "cafe","glasses","rob","rita","niel","suzeq","suzzy","lavish","zank",
            "zankman","zankLady","pure","steve","bird","tits","nipples","blanket",
            "dopey","basset","bob","bertie","bingo","bimbo","limbo","knickers",
            "man","boy","grandad","woman","girl","grandmother","keith","richards",
            "wotULookingAt","phone","spk","pure","window","widow","poster","car",
            "van","lorry","chair","glass","desk","button","punch","ian","blue","green"
            };
        return names[random(names.length)];
        }

    private String getRandomSpeech()
        {
        final String speechStrings[] = 
            {
"Refreshed by a brief blackout, I got to my feet and went next door.",
"The honeymoon is over when he phones to say he'll be late for supper and she's already left a note that it's in the refrigerator. -- Bill Lawrence",
"The abuse of greatness is when it disjoins remorse from power.",
"Don't quit now, we might just as well lock the door and throw away the key.",
"It's a poor workman who blames his tools.",
"One advantage of talking to yourself is that you know at least somebody's listening.",
"Love is staying up all night with a sick child, or a healthy adult.",
"Tell the truth or trump--but get the trick.",
"JAPAN is a WONDERFUL planet -- I wonder if we'll ever reach their level of COMPARATIVE SHOPPING ...",
"Laundry is the fifth dimension!!  ... um ... um ... th' washing machine is a black hole and the pink socks are bus drivers who just fell in!!",
"Only God can make random selections.",
"One would like to stroke and caress human beings, but one dares not do so, because they bite.",
"Due to circumstances beyond your control, you are master of your fate and captain of your soul.",
"I bet the human brain is a kludge.",
"Of course you have a purpose -- to find a purpose.",
"Garbage In -- Gospel Out.",
"You never know how many friends you have until you rent a house on the beach.",
"Nothing takes the taste out of peanut butter quite like unrequited love.",
"I ain't broke, but I'm badly bent.",
"People don't usually make the same mistake twice -- they make it three times, four time, five times...",
"Anything that is good and useful is made of chocolate.",
"One seldom sees a monument to a committee.",
"Why am I so soft in the middle when the rest of my life is so hard?",
"Nothing shortens a journey so pleasantly as an account of misfortunes at which the hearer is permitted to laugh.",
"Monogamy is the Western custom of one wife and hardly any mistresses.",
"Barbie says, Take quaaludes in gin and go to a disco right away!",
"But Ken says, WOO-WOO!!  No credit at 'Mr. Liquor'!!",
"A lie is an abomination unto the Lord and a very present help in time of trouble.",
"Truth is stranger than fiction, because fiction has to make sense.",
"Decorate your home.  It gives the illusion that your life is more interesting than it really is.",
"The eleventh commandment was `Thou Shalt Compute' or `Thou Shalt Not Compute' -- I forget which.",
"Yow!  I threw up on my window!",
"Never sleep with a woman whose troubles are worse than your own.",
"A complex system that works is invariably found to have evolved from a simple system that works.",
"The better the state is established, the fainter is humanity.",
"To make the individual uncomfortable, that is my task.",
"Tact, n.:	The unsaid part of what you're thinking.",
"A is for Apple.",
"Let me put it this way: today is going to be a learning experience.",
"I think we're in trouble.",
"...there can be no public or private virtue unless the foundation of action is the practice of truth.",
"Nearly every complex solution to a programming problem that I have looked at carefully has turned out to be wrong.",
"History is the version of past events that people have decided to agree on.",
"In Tennessee, it is illegal to shoot any game other than whales from a moving automobile.",
"You could live a better life, if you had a better mind and a better body.",
"Whoever dies with the most toys wins.",
"On the road, ZIPPY is a pinhead without a purpose, but never without a POINT.",
"Hope is a good breakfast, but it is a bad supper.",
"Don't hit a man when he's down -- kick him; it's easier.",
"Don't try to outweird me, three-eyes.  I get stranger things than you free with my breakfast cereal.",
            };
        return "\"" + speechStrings[random(speechStrings.length)] + "\"";
        }
    
    private String getRandomList(int n)
        {
        StringBuffer result = new StringBuffer("[ ");
        for (int i = 1; i < random(n) + 1; i++)
            result.append(getRandomName() + " ");            
        result.append("]");            
        return result.toString();
        }
        
    private int random(int n)
        {
        double r = Math.random();
        int result = (int)(r * (double)n);
        return result;
        }
    }
