# Action System

The Ogalala MUD framework uses an object-oriented action system where game behaviors are implemented as Java classes that respond to player commands. Actions are dynamically invoked through the event system and can modify world state, send messages, and trigger other actions.

## Table of Contents

- [Overview](#overview)
- [Action Architecture](#action-architecture)
- [Action Types](#action-types)
- [Action Execution](#action-execution)
- [Built-in Actions](#built-in-actions)
- [Creating Custom Actions](#creating-custom-actions)
- [Action Patterns](#action-patterns)
- [Examples](#examples)

## Overview

Actions in Ogalala are Java classes that implement specific game behaviors. When a player types a command, the parser creates an `Event` that triggers the appropriate action on the target object.

### Key Concepts

- **Event-Driven**: Actions are triggered by parser events
- **Object-Oriented**: Each object can have its own action implementations
- **Property-Based**: Actions are stored as object properties (e.g., `open = !OpenDoor`)
- **Inheritance**: Actions inherit from base classes for common patterns
- **Composable**: Actions can call other actions and events

### Action Flow

```
Player Input → Parser → Event → Property Lookup → Action Execution → World Updates
```

## Action Architecture

### Base Action Classes

#### JavaAction (`mua/JavaAction.java`)

The base class for all Java-based actions:

```java
public abstract class JavaAction implements Action {
    protected World world;        // Game world reference
    protected Event event;        // Triggering event
    protected Atom actor;         // Command initiator
    protected Atom current;       // Primary target object
    protected Container container; // Actor's container
    
    public abstract boolean execute();
}
```

#### SimpleAction (`mua/action/OpenableLockable.java`)

Template for basic actions with validation:

```java
public abstract class SimpleAction extends JavaAction {
    public boolean execute() {
        init();                    // Initialize action
        if (!beforeAction()) return true;  // Pre-validation
        if (tryAction()) {
            successAction();       // Success handling
        } else {
            failedAction();        // Failure handling
        }
        return true;
    }
    
    protected abstract boolean tryAction();
    protected void successAction() {}
    protected void failedAction() {}
}
```

#### CheckedAction (`mua/action/OpenableLockable.java`)

For actions requiring state validation:

```java
public abstract class CheckedAction extends SimpleAction {
    protected boolean beforeAction() {
        if (!beforeTest()) return false;
        if (test()) {
            successfulTest();
            return afterTest();
        } else {
            return failedTest();
        }
    }
    
    protected abstract boolean test();  // State validation
}
```

### Action Registration

Actions are registered as object properties using the `!` prefix:

```odl
atom door : exit openable {
    open = !OpenDoor           # Java class OpenDoor
    close = !CloseDoor         # Java class CloseDoor
    description = "A wooden door leading {direction}."
}
```

## Action Types

### Movement Actions

Handle player and object movement through the world:

- **`Go`** - Move through exits
- **`GoIn`** - Enter containers  
- **`GoOut`** - Exit containers
- **`GoRandom`** - Random movement (NPCs)

```java
public class Go extends JavaAction {
    public boolean execute() {
        // Validate movement permissions
        // Move actor to destination
        // Send movement messages
        // Trigger LOOK command
        return world.callEvent(actor, "look", world.getRoot());
    }
}
```

### Object Manipulation

Actions for interacting with objects:

- **`Get`** - Pick up objects
- **`Drop`** - Put down objects
- **`Put`** - Place objects in containers
- **`Give`** - Transfer objects between players

### Container Actions

Specialized actions for openable/lockable objects:

- **`Open`** - Open containers/doors
- **`Close`** - Close containers/doors  
- **`Lock`** - Lock with keys
- **`Unlock`** - Unlock with keys

```java
public class Open extends CheckedAction {
    protected boolean test() {
        return !openable.isOpen(current);  // Must be closed
    }
    
    protected boolean tryAction() {
        openable.open(current);             // Change state
        return true;
    }
    
    protected void successAction() {
        // Send messages to actor and room
        actor.output(openable.open_msg(current));
        container.output(openable.open_omsg(current), actor);
    }
}
```

### Communication Actions

Handle player communication:

- **`Say`** - Speak to room
- **`Tell`** - Private messages
- **`Emote`** - Action expressions
- **`Shout`** - Area-wide communication

### Combat Actions

Implement fighting and conflict:

- **`Attack`** - Basic combat
- **`Shoot`** - Ranged weapons
- **`Load`** - Weapon loading
- **`Block`** - Defensive actions

### Administrative Actions

System management commands:

- **`ModGo`** - Teleportation
- **`ModDig`** - Create exits
- **`ModCreate`** - Create objects
- **`FreezeDatabase`** - System maintenance

## Action Execution

### Event Processing

Actions are executed through the event system:

```java
public boolean callEvent(Atom actor, String verb, Atom target, Object[] args) {
    // Create event
    Event event = newEvent(actor, verb, target, args);
    
    // Push to event stack
    pushEvent(event);
    
    try {
        // Look up property on target
        Object value = target.getProperty(verb);
        
        if (value instanceof Action) {
            Action action = (Action) value;
            return action.execute(event);
        }
        // ... handle other property types
    } finally {
        popEvent();  // Clean up event stack
    }
}
```

### Property Resolution

Actions are found through property inheritance:

1. **Direct Property**: Check target object's properties
2. **Inherited Property**: Check parent atoms
3. **Default Property**: Use fallback behaviors
4. **Error Property**: Handle unknown commands

### Context Information

Actions have access to execution context:

```java
public class ExampleAction extends JavaAction {
    public boolean execute() {
        // Available context
        Atom player = event.getActor();        // Who triggered
        Atom target = event.getTarget();       // Primary object
        Object[] args = event.getArgs();       // Additional arguments
        World world = event.getWorld();        // Game world
        Container room = actor.getContainer(); // Current location
        
        // Perform action logic
        return true;
    }
}
```

## Built-in Actions

### Look System

#### LookRoom (`mua/action/Look.java`)

Displays room descriptions:

```java
public class LookRoom extends LookContainerBase {
    protected void initCurrent() {
        current = actor.getEnclosingContainer();  // Get room
    }
    
    protected void buildPkt() {
        lookBasic();        // Room name/description
        super.buildPkt();   // Contents (things, mobiles, exits)
    }
}
```

#### LookThing (`mua/action/Look.java`)

Examines individual objects:

```java
public class LookThing extends Look {
    protected void buildPkt() {
        outpkt.addField("id", current.getID());
        outpkt.addField("name", getName(current));
        outpkt.addField("description", current.getDescription());
    }
}
```

### Exit System

#### OpenDoor (`mua/action/ExitCommands.java`)

Opens doors on both sides:

```java
public class OpenDoor extends Open {
    public boolean tryAction() {
        door.open(current);  // Opens both sides of door
        return true;
    }
    
    public void successAction() {
        super.successAction();  // Standard messages
        otherRoom.output("the door opens ...");  // Other side
    }
}
```

### Container System

#### FactoryGet (`mua/action/FactoryAction.java`)

Creates objects from templates:

```java
public class FactoryGet extends JavaAction {
    public boolean execute() {
        // Check factory capacity
        if (factory.getInt("factory_count") <= 0) {
            actor.output("There are no more items available.");
            return true;
        }
        
        // Create new object from template
        Atom template = factory.getAtom("factory_template");
        Atom newItem = world.newThing(actor, template);
        
        // Update factory count
        factory.decInt("factory_count");
        
        return true;
    }
}
```

## Creating Custom Actions

### Basic Action Template

```java
package com.ogalala.mua.action;

public class CustomAction extends JavaAction {
    private static final long serialVersionUID = 1;
    
    public boolean execute() {
        // 1. Validate preconditions
        if (!isValidTarget()) {
            actor.output("You can't do that here.");
            return true;
        }
        
        // 2. Perform the action
        doCustomBehavior();
        
        // 3. Send feedback messages
        actor.output("You perform the custom action.");
        container.output(actor.getName() + " does something.", actor);
        
        // 4. Trigger side effects
        triggerSideEffects();
        
        return true;
    }
    
    private boolean isValidTarget() {
        // Validation logic
        return current != null && current.getBool("is_actionable");
    }
    
    private void doCustomBehavior() {
        // Main action logic
        current.setBool("was_acted_upon", true);
    }
    
    private void triggerSideEffects() {
        // Call other actions or events
        world.callEvent(actor, "secondary_action", current);
    }
}
```

### Checked Action Example

```java
public class CustomOpenAction extends CheckedAction {
    private static final long serialVersionUID = 1;
    
    protected boolean test() {
        // Check if action is possible
        return current.getBool("is_closed") && 
               !current.getBool("is_locked");
    }
    
    protected boolean tryAction() {
        // Perform the action
        current.setBool("is_closed", false);
        return true;
    }
    
    protected void successAction() {
        // Success messages
        actor.output("You open the " + current.getName() + ".");
        container.output(actor.getName() + " opens the " + 
                        current.getName() + ".", actor);
    }
    
    protected boolean failedTest() {
        // Handle why action failed
        if (current.getBool("is_locked")) {
            actor.output("The " + current.getName() + " is locked.");
        } else {
            actor.output("The " + current.getName() + " is already open.");
        }
        return false;
    }
}
```

## Action Patterns

### State Management

Actions often modify object state:

```java
// Boolean properties
current.setBool("is_open", true);
current.setBool("is_locked", false);

// Numeric properties  
current.incInt("use_count");
current.decInt("durability");

// String properties
current.setString("last_user", actor.getName());

// Object references
current.setAtom("held_by", actor);
```

### Message Broadcasting

Actions send messages to different audiences:

```java
// To the actor
actor.output("You see a bright flash.");

// To the room (excluding actor)
container.output(actor.getName() + " creates a bright flash.", actor);

// To specific targets
target.output("Someone touches you.");

// To other rooms
otherRoom.output("You hear a loud noise from nearby.");
```

### Event Chaining

Actions can trigger other actions:

```java
// Call action on same object
world.callEvent(actor, "look", current);

// Call action on different object  
world.callEvent(actor, "activate", relatedObject);

// Delayed events via timer
Timer timer = world.getTimer();
timer.addEvent(5000, actor, "delayed_action", current);
```

### Error Handling

```java
public boolean execute() {
    try {
        // Action logic
        performAction();
        return true;
    } catch (ActionException e) {
        // Handle specific action errors
        actor.output("The action failed: " + e.getMessage());
        return true;
    } catch (RuntimeException e) {
        // Handle unexpected errors
        Debug.printStackTrace(e);
        actor.output("Something went wrong.");
        return true;
    }
}
```

## Examples

### Simple Toggle Action

```java
public class ToggleSwitch extends JavaAction {
    public boolean execute() {
        boolean isOn = current.getBool("is_on");
        current.setBool("is_on", !isOn);
        
        String state = isOn ? "off" : "on";
        actor.output("You turn the switch " + state + ".");
        container.output(actor.getName() + " flips the switch.", actor);
        
        // Trigger connected devices
        if (!isOn) {
            world.callEvent(actor, "power_on", current);
        } else {
            world.callEvent(actor, "power_off", current);
        }
        
        return true;
    }
}
```

### Timed Transformation

```java
public class LightFuse extends JavaAction {
    public boolean execute() {
        if (current.getBool("is_lit")) {
            actor.output("The fuse is already burning.");
            return true;
        }
        
        current.setBool("is_lit", true);
        actor.output("You light the fuse. It begins to burn.");
        container.output(actor.getName() + " lights a fuse.", actor);
        
        // Schedule explosion in 10 seconds
        Timer timer = world.getTimer();
        timer.addEvent(10000, world.getRoot(), "explode", current);
        
        return true;
    }
}
```

### Conditional Access

```java
public class UseKeycard extends JavaAction {
    public boolean execute() {
        // Check if player has required keycard
        Atom keycard = current.getAtom("required_keycard");
        if (!AtomUtil.containsDescendant(actor, keycard)) {
            actor.output("You need a keycard to use this device.");
            return true;
        }
        
        // Check keycard access level
        int requiredLevel = current.getInt("access_level");
        int cardLevel = keycard.getInt("access_level");
        if (cardLevel < requiredLevel) {
            actor.output("Your keycard doesn't have sufficient access.");
            return true;
        }
        
        // Grant access
        current.setBool("is_unlocked", true);
        actor.output("The device accepts your keycard and unlocks.");
        
        return true;
    }
}
```

## Integration Points

### With Parser System
- Actions registered via `!Verb` commands
- Template matching determines action parameters
- Property inheritance resolves action lookups

### With Object System  
- Actions stored as object properties
- Inheritance allows action specialization
- State changes persist in object data

### With Event System
- Actions triggered by events
- Can create and post new events
- Event stack tracks execution context

### With Message System
- Actions generate player feedback
- Output packets structure client data
- Message routing handles distribution

## Related Files

- `/mua/action/` - Core action implementations
- `/mua/JavaAction.java` - Base action class
- `/mua/Event.java` - Event system integration
- `/mua/World.java` - Action execution context