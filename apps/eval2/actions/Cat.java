// $Id: Cat.java,v 1.8 1999/03/11 10:41:08 alex Exp $
// The Cat
// James Fryer, 3 Aug 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;

/** Start Catting
*/
public class CatStart
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
        if (current instanceof Thing)
            {
            Thing thing = (Thing)current;
            thing.addWatcher(new CatWatcher(world, thing, true));
            world.timerEvent((Mobile)current, thing, 3);
            }
        }

    class CatWatcher extends Watcher
        {
        private World world;

        public CatWatcher(World world, Thing thing, boolean isLead)
            {
            super(thing, isLead);
            this.world = world;
            }

        protected void doOutput(String msg, Event event)
            {
            // If the event is null, or not a say packet, or speaker is a cat, do nothing
            if (event == null)
                return;
            msg = msg.toLowerCase();
            if (!msg.startsWith("type=say"))
                return;
            if (event.getActor().isDescendantOf(event.getWorld().getDatabase().getAtom("cat")))
                return;

            // Match the message against several buzzwords and meow if one of them is recognised.
            final String buzzWords[] = { "cat", "milk", "fish", "carruthers" };
            for (int i = 0; i < buzzWords.length; i++)
                {
                if (msg.indexOf(buzzWords[i]) >= 0)
                    {
                    world.parseCommand("SAY Meow!", (Mobile)thing);
                    break;
                    }
                }
            }
        }
    }

public class CatTimer
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
		// Get a random number from 1 to 100
		int r = AtomUtil.rollDice(100);

		// Half the time, purr or mewel depending on the "happy" flag
		if (r <= 50)
		    {
            boolean isHappy = current.getBool("is_happy");
            String msgID = isHappy ? "purr_msg" : "mewel_msg";
            container.output(new Output("misc", "msg",
                    event.formatOutput(current.getString(msgID))), event, (Mobile)current);
            }

		// 10% of the time scratch at the floor
		else if (r <= 60)
		    {
            container.output(new Output("misc", "msg",
                    event.formatOutput(current.getString("scratch_msg"))), event, (Mobile)current);
		    }

		// The rest of the time attempt to move in a random direction
        else {
            final String directions[] = { "n", "s", "e", "w", "ne", "nw", "se", "sw", "u", "d", };
            r = AtomUtil.rollDice(directions.length) - 1;
            world.parseCommand("GO " + directions[r], (Mobile)current);
            }

		// Next cat event in 2d20 secs
        world.timerEvent(actor, current, AtomUtil.rollDice(20, 2));
        }
    }

public class StrokeCat
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
        // Stroking makes the cat happy
        current.setBool("is_happy", true);
        container.output(new Output("misc", "msg", event.formatOutput(current.getString("stroke_omsg"))), event, actor);
        event.setResult(current.getString("stroke_msg"));
        }
    }

public class KickCat
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
        // Kicking makes the cat unhappy
        current.setBool("is_happy", false);
        container.output(new Output("misc", "msg", event.formatOutput(current.getString("kick_omsg"))), event, actor);
        event.setResult(current.getString("kick_msg"));
        }
    }
