# Object Definition Language (ODL) Reference

The Object Definition Language (ODL) is a domain-specific language used in the Ogalala MUD framework to define game objects, their properties, inheritance relationships, and behaviors. ODL files are compiled into script files that create the game world structure.

## Table of Contents

- [Overview](#overview)
- [Basic Syntax](#basic-syntax)
- [Object Types](#object-types)
- [Property Types](#property-types)
- [Inheritance](#inheritance)
- [Built-in Atoms](#built-in-atoms)
- [Comments and Documentation](#comments-and-documentation)
- [Compilation Process](#compilation-process)
- [Examples](#examples)

## Overview

ODL provides a declarative way to define game objects without writing Java code. The ODL compiler (`tools/ODLCompiler.java`) translates ODL files into script commands that create objects in the game world.

### Key Features

- **Object-Oriented**: Supports inheritance and composition
- **Property-Based**: Objects defined by their properties and values
- **Type-Safe**: Strong typing for different value types
- **Hierarchical**: Atoms can inherit from multiple parents
- **Extensible**: Easy to add new object types and behaviors

### File Structure

ODL files typically use the `.odl` extension and contain:
- Atom definitions (class-like templates)
- Thing definitions (world instances)
- Property assignments
- Documentation comments

## Basic Syntax

### Object Definition

```odl
atom object_name : parent1 parent2 {
    property_name = value
    another_property = "string value"
}
```

### Thing Definition  

```odl
thing instance_name : atom_template {
    _where = $container_object
    custom_property = 42
}
```

### Property Assignment

```odl
property_name = value
```

## Object Types

### Atoms

Atoms are templates or classes that define object types:

```odl
atom weapon : thing portable {
    name = "weapon"
    description = "A generic weapon."
    damage = 10
    weight = 5
}
```

**Characteristics:**
- Can inherit from other atoms
- Define default properties
- Not instantiated in the world
- Used as templates for things

### Things

Things are actual instances that exist in the game world:

```odl
thing excalibur : weapon {
    _where = $armory
    name = "Excalibur"
    description = "A legendary sword with mystical powers."
    damage = 50
    weight = 3
}
```

**Characteristics:**
- Must specify location with `_where`
- Inherit properties from atom parents
- Exist as actual game objects
- Can override inherited properties

## Property Types

### String Values

Text enclosed in double quotes:

```odl
name = "leather armor"
description = "Well-crafted leather armor that provides decent protection."
material = "leather"
```

**Features:**
- Support escape sequences: `\"`, `\\`, `\n`
- Can contain template variables: `"You see {name} here."`
- Multi-line strings allowed

### Integer Values

Whole numbers without quotes:

```odl
damage = 25
weight = 10
capacity = 100
health = -5
```

**Usage:**
- Statistics and counters
- Coordinates and positions  
- Timing values
- Boolean alternatives (0 = false, non-zero = true)

### Boolean Values

Truth values using unquoted keywords:

```odl
is_closed = true
is_locked = false
is_obvious = TRUE
can_fly = False
```

**Keywords:** `true`, `false`, `TRUE`, `FALSE` (case-insensitive)

### Action References

Java action classes prefixed with `!`:

```odl
open = !OpenDoor
close = !CloseDoor
attack = !WeaponAttack
on_create = !InitializeObject
```

**Usage:**
- Link verbs to Java implementations
- Define event handlers
- Specify behavioral responses

### Atom References

References to other objects prefixed with `$`:

```odl
container = $backpack
key = $brass_key
destination = $throne_room
factory_template = $gold_coin
```

**Usage:**
- Object relationships
- Container hierarchies
- Template references
- Event targets

### Lists

Arrays of values enclosed in square brackets:

```odl
exit_directions = [north south east west]
damage_types = ["slashing" "piercing" "bludgeoning"]
skill_bonuses = [2 4 1 3]
related_objects = [$sword $shield $helmet]
```

**Features:**
- Mixed types allowed
- Space-delimited elements
- Nested structures supported

### Dictionaries

Key-value pairs for complex data:

```odl
skill_bonuses = {
    "strength" = 5
    "dexterity" = 3
    "intelligence" = 1
}

room_connections = {
    "north" = $forest_path
    "south" = $village_square
}
```

**Usage:**
- Configuration data
- Lookup tables
- Complex relationships

## Inheritance

### Single Inheritance

```odl
atom container : thing {
    capacity = 10
    is_closed = false
}

atom chest : container {
    capacity = 50          # Override parent value
    is_locked = false      # Add new property
}
```

### Multiple Inheritance

```odl
atom lockable_chest : chest lockable {
    # Inherits from both chest and lockable
    key = $brass_key
}
```

**Resolution Order:**
1. Properties defined directly in the object
2. Properties from rightmost parent
3. Properties from leftmost parent
4. Properties from grandparents (recursive)

### Property Inheritance

```odl
atom base_weapon {
    damage = 10
    weight = 5
    material = "iron"
}

atom sword : base_weapon {
    damage = 15           # Override
    weapon_type = "sword" # Add new
    # weight = 5 (inherited)
    # material = "iron" (inherited)
}
```

## Built-in Atoms

### Core Hierarchy

```odl
atom root {
    # Base of all objects
}

atom described : root {
    name = "unnamed"
    description = "You see nothing special."
}

atom thing : described {
    # Base for all game objects
    size = 10
    weight = 1
}
```

### Container Types

```odl
atom container : thing {
    capacity = 20
    is_closed = false
    contents_suffix_msg = "(in {name})"
}

atom container_on : container {
    contents_suffix_msg = "(on {name})"
}
```

### Interactive Objects

```odl
atom openable : thing {
    open = !Open
    close = !Close
    is_closed = true
    open_msg = "You open {name}."
}

atom lockable : openable {
    lock = !Lock
    unlock = !Unlock  
    is_locked = false
    key = $key
}
```

### Location Types

```odl
atom exit : thing {
    go = !Go
    direction = "nowhere"
    destination = null
}

atom door : exit openable {
    open = !OpenDoor
    close = !CloseDoor
    is_closed = true
}
```

### Character Types

```odl
atom mobile : thing {
    # Base for characters
    health = 100
    start_container = null
}

atom character : mobile {
    # Player characters
    experience = 0
    level = 1
}
```

## Comments and Documentation

### Comment Types

```odl
# Private comments - internal notes
## Documentation comments - for generated docs
### Special tool comments - design metadata
```

### Documentation Comments

```odl
## This atom represents a magical weapon that can be wielded
## by characters with sufficient skill. The weapon provides
## both physical and magical damage bonuses.
atom magic_weapon : weapon {
    ## The magical power level of this weapon (1-10)
    magic_level = 5
    
    ## Spell effects triggered on successful hits  
    spell_effects = ["flame" "ice" "lightning"]
}
```

**Features:**
- Support HTML-like markup: `<b>`, `<i>`, `<u>`
- Can reference properties and methods
- Used by documentation generators
- Describe object purpose and usage

### Comment Examples

```odl
# TODO: Add durability system for weapons
atom sword : weapon {
    ## The sharpness of the blade affects damage output
    sharpness = 8
    
    # Internal note: consider adding rust effects
    condition = "pristine"
}
```

## Compilation Process

### ODL Compiler

The ODL compiler translates `.odl` files into `.atom` script files:

```bash
java com.ogalala.tools.ODLCompiler input.odl output.atom
```

### Compilation Stages

1. **Parsing**: Read ODL syntax and build object tree
2. **Inheritance Resolution**: Flatten inheritance hierarchies  
3. **Property Expansion**: Resolve all property values
4. **Script Generation**: Output script commands
5. **Validation**: Check for circular references and errors

### Generated Script Format

ODL input:
```odl
atom chest : container openable {
    name = "wooden chest"
    capacity = 100
}
```

Generated script output:
```
!atom chest container openable
!set chest.name "wooden chest"
!set chest.capacity 100
```

### Build Integration

ODL compilation is integrated into the build process:

```makefile
# In Makefile
%.atom: %.odl
	$(JAVA) com.ogalala.tools.ODLCompiler $< $@

all: objects.atom behaviors.atom
```

## Examples

### Complete Room Definition

```odl
## A cozy tavern where adventurers gather to rest and share tales
atom tavern_room : container {
    name = "The Prancing Pony"
    description = "A warm, inviting tavern with a roaring fireplace. " +
                  "Wooden tables and chairs are scattered about, and " +
                  "the air is filled with the smell of ale and roasted meat."
    
    ## Ambient lighting level (0-100)
    light_level = 75
    
    ## Background sounds for atmosphere
    ambient_sounds = ["crackling fire" "distant laughter" "clinking mugs"]
    
    ## Special room properties
    is_safe_zone = true
    allows_camping = true
}

## The actual tavern instance in the world
thing prancing_pony : tavern_room {
    _where = $village_square
    
    ## Custom welcome message
    enter_msg = "You push open the heavy wooden door and step into " +
                "the warmth of the tavern."
}
```

### Weapon System

```odl
## Base weapon template with common properties
atom weapon : thing portable {
    ## Combat statistics
    damage = 10
    accuracy = 70
    durability = 100
    
    ## Physical properties  
    weight = 5
    material = "iron"
    
    ## Combat actions
    attack = !WeaponAttack
    parry = !WeaponParry
    
    ## Weapon maintenance
    repair = !RepairWeapon
    clean = !CleanWeapon
}

## Specialized sword class
atom sword : weapon {
    damage = 15
    weapon_type = "slashing"
    required_skill = "swordplay"
    
    ## Sword-specific techniques
    slash = !SwordSlash
    thrust = !SwordThrust
}

## Legendary sword instance
thing excalibur : sword {
    _where = $stone_circle
    
    name = "Excalibur"
    description = "The legendary sword of King Arthur, glowing with " +
                  "an inner light. Mystical runes are etched along " +
                  "the fuller of the blade."
    
    ## Enhanced statistics
    damage = 50
    accuracy = 95
    durability = 999
    
    ## Magical properties
    magic_level = 10
    spell_effects = ["holy_light" "undead_bane"]
    alignment_required = "good"
    
    ## Special abilities
    speak_truth = !SwordTruth
    detect_evil = !DetectEvil
}
```

### Container Hierarchy

```odl
## Base container with opening/closing
atom openable_container : container openable {
    capacity = 20
    is_closed = true
    
    open_msg = "You open {name}."
    close_msg = "You close {name}."
    
    ## Container examination
    look_in = !LookContainer
}

## Lockable container that requires keys
atom secure_container : openable_container lockable {
    is_locked = true
    key = $master_key
    
    lock_msg = "You lock {name} with {arg.name}."
    unlock_msg = "You unlock {name} with {arg.name}."
    open_fail_msg = "You cannot open {name}, it is locked."
}

## Magical container with special properties
atom enchanted_box : secure_container {
    ## Magical features
    is_magical = true
    magic_resistance = 50
    
    ## Enhanced capacity through magic
    capacity = 200
    weight_reduction = 0.5  # Items weigh half inside
    
    ## Special actions
    enchant = !EnchantContainer
    dispel = !DispelMagic
    
    ## Protective measures
    trap_spell = !MagicTrap
    ward_level = 5
}
```

### NPC Definition

```odl
## Base NPC template
atom npc : mobile {
    ## AI and behavior
    on_create = !CreateNPC
    on_tick = !NPCTick
    
    ## Default stats
    health = 100
    level = 1
    
    ## Conversation system
    talk = !NPCTalk
    default_greeting = "Hello there!"
    
    ## Movement patterns
    wander_radius = 3
    movement_frequency = 30  # seconds
}

## Merchant specialization
atom merchant : npc {
    ## Trading functionality
    buy = !MerchantBuy
    sell = !MerchantSell
    
    ## Merchant properties
    buy_rate = 0.5    # Buys at 50% value
    sell_rate = 1.5   # Sells at 150% value
    gold = 1000       # Starting capital
    
    ## Inventory management
    restock_timer = 3600  # 1 hour
    inventory_limit = 50
}

## Specific merchant instance
thing blacksmith : merchant {
    _where = $smithy
    
    name = "Gareth the Blacksmith"
    description = "A burly man with muscled arms from years of " +
                  "working the forge. His leather apron is " +
                  "stained with soot and metal shavings."
    
    ## Specialization
    specializes_in = ["weapons" "armor" "tools"]
    skill_level = 8
    
    ## Custom responses
    greeting = "Welcome to my smithy! I have the finest weapons " +
               "and armor in the kingdom."
    
    farewell = "May your blade stay sharp and your armor strong!"
    
    ## Services offered
    repair = !BlacksmithRepair
    forge = !BlacksmithForge
    appraise = !AppraiseItem
}
```

## Best Practices

### Organization

1. **File Structure**: Group related objects in logical files
2. **Naming**: Use descriptive, consistent naming conventions
3. **Inheritance**: Create reusable base atoms for common types
4. **Documentation**: Comment complex objects and properties

### Property Design

1. **Defaults**: Set sensible default values in base atoms
2. **Validation**: Use appropriate types for property values
3. **Relationships**: Use atom references for object connections
4. **Extensibility**: Design properties for future expansion

### Performance

1. **Inheritance Depth**: Avoid excessive inheritance chains
2. **Property Count**: Balance detail with memory usage
3. **Circular References**: Prevent infinite loops in relationships
4. **Compilation Time**: Consider compile speed for large files

## Related Tools

- **ODL Compiler**: `tools/ODLCompiler.java`
- **Script System**: Loads compiled `.atom` files
- **Documentation Generator**: `tools/DocCompiler.java`
- **Property Definition**: `tools/PropertyDefinition.java`