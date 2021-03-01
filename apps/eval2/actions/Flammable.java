// $Id: Flammable.java,v 1.5 1999/03/11 10:41:08 alex Exp $
// Flammable objects, fires, ashes
// James Fryer, 17 Aug 98
// Copyright (C) Ogalala Ltd <info@ogalala.com>

package com.ogalala.mua.action;

import com.ogalala.mua.*;

/**
*/
public class Light
    extends JavaAction
    {
    // This is the weight that will burn straight away
    public static final int SMALL_WEIGHT = 100;

    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
        // If the actor is holding the thing, tell them to drop it first
        if (((Thing)current).getContainer() == actor)
            {
            event.setResult(current.getString("burn_must_drop_msg"));
            return;
            }

        // Check that we have an argument and that it is an ignitor
        if (event.getArgCount() < 1)
            {
            event.setResult(current.getString("burn_need_ignitor_msg"));
            return;
            }
        Atom ignitor = (Atom)event.getArg(0);
        if (!ignitor.isDescendntOf(world.getDatabase().getAtom("ignitor")))
            {
            event.setResult(current.getString("burn_not_ignitor_msg"));
            return;
            }

        // Test ignitor strength against the object's flammability
        int sparkStrength = ignitor.getInt("spark_strength");
        int ignitability = current.getInt("ignitability");
        if (ignitability < sparkStrength)
            {
            container.output(new Output("misc", "msg", event.formatOutput(current.getString("burn_fail_omsg"))), event, actor);
            event.setResult(current.getString("burn_fail_msg"));
            return;
            }
        // Get weight and size of the object we are burning
        int weight = current.getInt("weight");
        int size = current.getInt("size");

        // If the object is very light, just burn it straight away
        //### ALgorithm not complete -- should factor in size as well
        if (weight < SMALL_WEIGHT)
            {
            //### Note the order of these message is not logical -- this will be fixed when we complete the engine.
            container.output(new Output("misc", "msg", event.formatOutput(current.getString("burn_omsg"))), event, actor);
            container.output(new Output("misc", "msg", event.formatOutput(current.getString("burn_quick_msg"))), event, actor);
            event.setResult(current.getString("burn_quick_msg"));

            // Remove the current atom.
            //### This should be done by the world model so that the correct messages are sent out.
            world.getDatabase().deleteAtom(current);
            return;
            }

        // Else, create a fire object and replace 'current' with it
        else {
            //### Should be an easier way to do this in the world model

            // Create a fire object
            String fireID = world.getDatabase().getUniqueID("fire");
            Atom fireParent = world.getDatabase().getAtom("fire");
            Thing fire = (Thing)world.getDatabase().newAtom(fireID, "Thing", fireParent);

            // Set the fire's properties
            fire.setInt("weight", weight);
            fire.setInt("start_weight", weight);
            fire.setInt("size", size);
            fire.setInt("burn_rate", current.getInt("burn_rate"));

            // Tell the fire to get burning
            world.timerEvent(world.getAdmin(), fire, 0);

            // Tell the user what has happened
            container.output(new Output("misc", "msg", event.formatOutput(current.getString("burn_omsg"))), event, actor);
            event.setResult(current.getString("burn_msg"));

            // Replace the orignial object with the fire
            //### (Again, consistency should be maintained with the world model)
            world.getDatabase().deleteAtom(current);
            container.add(fire);
            }
        }
    }

/**
*/
public class FireTimer
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
        // Get the weight, size and burn rate of the fire
        int weight = current.getInt("weight");
        int size = current.getInt("size");
        int burn_rate = current.getInt("burn_rate");

        // If the fire hasn't burned itself out yet, output a progress message
        weight -= burn_rate;
        if (weight > 0)
            {
            current.setInt("weight", weight);
            container.output(new Output("misc", "msg", event.formatOutput(current.getString("fire_progress_msg"))), event);
            world.timerEvent(event, fireTick());
            }

        // Else, replace the fire with some ash
        else {
            // Tell the users what has happened
            container.output(new Output("misc", "msg", event.formatOutput(current.getString("fire_end_msg"))), event);

            // Make the ashes
            String ashesID = world.getDatabase().getUniqueID("ashes");
            Atom ashesParent = world.getDatabase().getAtom("ashes");
            Thing ashes = (Thing)world.getDatabase().newAtom(ashesID, "Thing", ashesParent);

            // Set the ash's properties
            ashes.setInt("weight", current.getInt("start_weight")/2);
            ashes.setInt("size", size);

            // Tell the ash to start decaying
            world.timerEvent(actor, ashes, AshesTimer.ashesTick());

            // Replace the fire with the ashes
            //### (Again, consistency should be maintained with the world model)
            world.getDatabase().deleteAtom(current);
            container.add(ashes);
            }
        }

    public static int fireTick()
        {
        // About 30 secs
        return AtomUtil.rollDice(20, 3);
        }
    }

/**
*/
public class AshesTimer
    extends JavaAction
    {
    public void execute(World world, Mobile actor, Atom current, Container container, Event event)
        {
        // Get the weight of the ashes
        int weight = current.getInt("weight");

        // Halve the ashes' weight
        weight /= 2;

        // If the weight is big enough to keep around, write the new weight back to it.
        // Don't tell the users anything -- who wants to know about a reducing pile of ash??!?
        if (weight > Light.SMALL_WEIGHT)
            {
            current.setInt("weight", weight);
            world.timerEvent(event, ashesTick());
            }

        // Else, destroy the ashes
        else {
            // Tell the users what has happened
            container.output(new Output("misc", "msg", event.formatOutput(current.getString("ashes_end_msg"))), event);

            // Destroy the ashes
            //### (Again, consistency should be maintained with the world model)
            world.getDatabase().deleteAtom(current);
            }
        }

    public static int ashesTick()
        {
        // 15 minutes
        return 15 * 60;
        }
    }
