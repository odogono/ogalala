# Vocabulary Management

The Ogalala MUD framework uses a comprehensive vocabulary system to manage all words that players can use in commands. The vocabulary handles verbs, nouns, adverbs, prepositions, and other word types, providing the foundation for natural language parsing.

## Table of Contents

- [Overview](#overview)
- [Vocabulary Architecture](#vocabulary-architecture)
- [Word Types](#word-types)
- [Verb Management](#verb-management)
- [Noun Management](#noun-management)
- [Adverb System](#adverb-system)
- [Dynamic Vocabulary](#dynamic-vocabulary)
- [Word Resolution](#word-resolution)
- [Examples](#examples)

## Overview

The vocabulary system serves as the lexicon for the parser, defining which words are recognized and how they should be interpreted. It supports dynamic word addition through script commands and provides efficient lookup mechanisms for real-time parsing.

### Key Features

- **Dynamic Loading**: Words loaded from script files during initialization
- **Type Classification**: Each word can have multiple grammatical types
- **Efficient Lookup**: Hashtable-based word resolution
- **Inheritance Support**: Noun hierarchies mirror object inheritance
- **Privilege Control**: Commands restricted by user permissions
- **Real-time Updates**: Vocabulary can be modified during runtime

### Core Classes

- **`Vocabulary`** (`mua/parser/Vocabulary.java`) - Main vocabulary management
- **`Word`** (`mua/parser/Word.java`) - Individual word representation
- **`Verb`** (`mua/parser/Verb.java`) - Verb definitions with templates
- **`VerbTemplate`** (`mua/parser/VerbTemplate.java`) - Command pattern matching

## Vocabulary Architecture

### Storage Structure

```java
public class Vocabulary {
    // Master word dictionary with type bitmasks
    protected Dictionary words = new Hashtable();
    
    // Specialized collections
    protected Dictionary verbs = new Hashtable();     // Verb objects
    protected Dictionary nouns = new Hashtable();     // Noun mappings  
    protected Dictionary adverbs = new Hashtable();   // Adverb rules
    protected Dictionary rawVerbs = new Hashtable();  // Raw commands
    
    // Reverse lookups
    protected Dictionary nounToAtom = new Hashtable(); // Noun → Object
}
```

### Word Type System

Words are classified using bitmask flags:

```java
public class Word {
    public static final int WT_UNKNOWN = 0;        // Unrecognized
    public static final int WT_VERB = 1;           // Action words
    public static final int WT_NOUN = 2;           // Object names
    public static final int WT_PREPOSITION = 4;    // Connecting words
    public static final int WT_ADJECTIVE = 8;      // Descriptors
    public static final int WT_ADVERB = 16;        // Modifiers
    public static final int WT_DIRECTION = 32;     // Compass directions
    public static final int WT_STRING = 64;        // Literal text
    public static final int WT_NUMERIC = 128;      // Numbers
    public static final int WT_RAWVERB = 256;      // System commands
    public static final int WT_CONNECTOR = 512;    // Conjunctions
    public static final int WT_TERMINATOR = 1024;  // Sentence enders
}
```

### Lookup Mechanism

```java
public int getWordTypes(String word) {
    Word wordObj = (Word) words.get(word.toLowerCase());
    return wordObj != null ? wordObj.getPossibleWordTypes() : WT_UNKNOWN;
}
```

## Word Types

### Verbs

Action words that trigger game behaviors:

```java
// From vocabulary scripts
!Verb "look,l,peer,peek,spy,gaze" \
    "none(see)>look" \
    "current(see)>examine" \
    "direction(see)>look_far"
```

**Characteristics:**
- Have multiple synonyms
- Define command templates
- Map to action properties
- Support privilege restrictions

### Nouns

Object identifiers for game entities:

```java
// Noun definitions
!noun sword weapon blade
!noun door entrance exit portal
!noun chest box container trunk
```

**Features:**
- Multiple synonyms per object
- Automatic inheritance from object hierarchy  
- Case-insensitive matching
- Support for compound nouns

### Prepositions

Connecting words that link command components:

```java
// Common prepositions
!Word "at,to,from,with,in,on,under,over,beside" preposition
```

**Usage:**
- Connect verbs to objects: `put sword in bag`
- Define spatial relationships: `look under table`
- Enable complex commands: `give sword to guard`

### Adjectives

Descriptive words for object disambiguation:

```java
// Adjective examples
!Word "red,blue,green,yellow" adjective
!Word "large,small,tiny,huge" adjective
!Word "wooden,metal,stone,glass" adjective
```

**Functions:**
- Distinguish similar objects: `get red sword`
- Describe object properties
- Support pattern matching

### Adverbs

Modifier words that transform verbs:

```java
// Adverb transformations
!Adverb "quickly + walk > run"
!Adverb "loudly + say > shout"
!Adverb "quietly + say > whisper"
```

**Behavior:**
- Transform verb meaning
- Apply before template matching
- Support emotional expression

### Directions

Compass and relative directions:

```java
// Direction words
!Word "north,n,south,s,east,e,west,w" direction
!Word "northeast,ne,northwest,nw,southeast,se,southwest,sw" direction
!Word "up,u,down,d,in,out" direction
```

**Special Handling:**
- Automatically insert `go` verb: `north` → `go north`
- Support abbreviations
- Handle relative directions

## Verb Management

### Verb Definition

Verbs are created using the `!Verb` script command:

```java
!Verb [privilege] "synonyms" "template>action" [additional_templates...]
```

### Verb Structure

```java
public class Verb {
    private int privilege;              // Required access level
    private Vector templates;           // Command patterns
    private boolean isCommunicative;    // Special text handling
}
```

### Template System

Each verb can have multiple templates for different usage patterns:

```java
// Attack verb with multiple templates
!Verb "attack,hit,strike" \
    "current(see)>attack" \                    # attack goblin
    "thing with current(inv)>attack" \         # attack goblin with sword
    "thing at current(inv)>attack"             # attack goblin at sword
```

### Verb Matching

Template matching follows this priority:

1. **Exact Template Match**: All arguments and prepositions match
2. **Partial Match**: Some arguments match with defaults
3. **Fallback Match**: Generic templates with flexible arguments
4. **Error Handling**: No valid templates found

```java
public VerbTemplate matchTemplate(String prep1, String prep2) {
    for (int i = 0; i < templates.size(); i++) {
        VerbTemplate temp = (VerbTemplate) templates.elementAt(i);
        if (temp.getPreposition(0).equals(prep1) && 
            temp.getPreposition(1).equals(prep2)) {
            return temp;
        }
    }
    return null;
}
```

## Noun Management

### Noun Registration

Nouns are associated with game objects:

```java
// Automatic noun creation from object definitions
atom sword : weapon noun_sword {
    name = "sword"
    // Creates noun "sword" pointing to this atom type
}

// Manual noun definition
!noun door entrance gate portal
```

### Noun Hierarchy

Nouns inherit from object atom hierarchy:

```java
atom weapon : thing {
    // Base weapon class
}

atom sword : weapon {
    // Inherits weapon nouns plus sword-specific nouns
}

thing excalibur : sword {
    // Inherits all sword and weapon nouns
}
```

### Noun Resolution

The parser resolves nouns through multiple strategies:

1. **Direct Match**: Exact noun match to object
2. **Inheritance Match**: Parent class nouns
3. **Adjective Filtering**: Narrow by descriptive words
4. **Context Filtering**: Limit by visibility/accessibility

```java
// Resolution example
"get red sword" 
→ Find objects with noun "sword"
→ Filter by adjective "red"  
→ Check visibility and reachability
→ Return best match
```

## Adverb System

### Adverb Definition

Adverbs transform verbs before template matching:

```java
!Adverb "transformation_rule"

// Examples
!Adverb "quickly + walk > run"
!Adverb "silently + open > sneak_open"
!Adverb "forcefully + push > break"
```

### Transformation Process

```java
public class Adverb {
    private String sourceAdverb;     // "quickly"
    private String sourceVerb;       // "walk"
    private String targetVerb;       // "run"
    
    public String transform(String adverb, String verb) {
        if (adverb.equals(sourceAdverb) && verb.equals(sourceVerb)) {
            return targetVerb;
        }
        return verb;  // No transformation
    }
}
```

### Adverb Application

Adverbs are processed before verb template matching:

```
Input: "quickly walk north"
↓
Adverb Processing: "quickly" + "walk" → "run"
↓  
Result: "run north"
↓
Template Matching: Find templates for "run"
```

## Dynamic Vocabulary

### Runtime Modification

Vocabulary can be updated during gameplay:

```java
// Add new verb
!Verb "newcommand" "none>custom_action"

// Add new noun
vocabulary.addNoun("mystical_sword", swordAtom);

// Remove verb
!RemoveVerb "oldcommand"
```

### Script Loading

Vocabulary is populated from script files:

```java
// Typical loading order
execScript("verbs");      // Core verb definitions
execScript("nouns");      // Object nouns
execScript("adverbs");    // Verb transformations
execScript("inner_core"); // System vocabulary
```

### Privilege Management

Commands can be restricted by user privilege:

```java
// Admin-only commands
!Verb (Admin) "shutdown" "none>shutdown_server"
!Verb (Builder) "create" "string>create_object"
!Verb (Player) "say" "string>say"

// Privilege checking
public Verb getVerb(String verbName, int userPrivilege) {
    Verb verb = getVerb(verbName);
    if (verb != null && verb.getPrivilege() <= userPrivilege) {
        return verb;
    }
    return null;  // Insufficient privilege
}
```

## Word Resolution

### Multi-Step Process

Word resolution follows a systematic approach:

1. **Tokenization**: Split input into word tokens
2. **Case Normalization**: Convert to lowercase
3. **Dictionary Lookup**: Find word type bitmask
4. **Context Analysis**: Determine most likely type
5. **Disambiguation**: Resolve conflicts using grammar rules

### Ambiguity Handling

Some words can have multiple types:

```java
// "light" can be noun or verb
Word lightWord = vocabulary.getWord("light");
int types = lightWord.getPossibleWordTypes();
// types = WT_NOUN | WT_VERB

// Context determines actual type
if (previousWord.isArticle()) {
    lightWord.setWordType(WT_NOUN);  // "the light"
} else if (sentenceStart) {
    lightWord.setWordType(WT_VERB);  // "light torch"
}
```

### Fallback Strategies

When words aren't found:

1. **Partial Matching**: Try substring matches
2. **Similarity Testing**: Check for typos
3. **Context Guessing**: Infer type from position
4. **Default Assignment**: Assume most common type

## Examples

### Basic Vocabulary Setup

```java
// Core verbs
!Verb "get,take,grab,pick" \
    "current(see)>get" \
    "current(see) from current(see)>get_from"

!Verb "drop,put" \
    "current(inv)>drop" \
    "current(inv) in current(see)>put_in" \
    "current(inv) on current(see)>put_on"

// Common nouns
!noun sword weapon blade
!noun bag sack backpack container
!noun door entrance exit portal

// Useful prepositions  
!Word "in,on,under,behind,beside" preposition

// Basic adjectives
!Word "red,blue,green,small,large" adjective
```

### Advanced Verb Definition

```java
// Complex fighting system
!Verb "attack,hit,strike,fight" \
    "current(see)>attack_unarmed" \
    "current(see) with current(inv)>attack_weapon" \
    "current(see) using current(inv)>attack_ranged" \
    "current(see) from direction>attack_distant"

// Magic system
!Verb (Player) "cast,invoke" \
    "string>cast_spell" \
    "string at current(see)>cast_targeted" \
    "string on current(see)>cast_beneficial"

// Administrative commands
!Verb (Admin) "teleport,tp" \
    "current(world)>teleport_to" \
    "current(world) to current(world)>teleport_other"
```

### Noun Hierarchy Example

```java
// Object definitions create automatic nouns
atom container : thing {
    // Creates noun "container"
}

atom chest : container {
    // Inherits "container" noun, adds "chest"
}

atom treasure_chest : chest {
    // Inherits "container" and "chest", adds "treasure_chest"
}

// Manual noun additions
!noun chest box trunk coffer
!noun treasure_chest treasure hoard
```

### Adverb Transformations

```java
// Movement adverbs
!Adverb "quickly + walk > run"
!Adverb "quickly + go > run"
!Adverb "slowly + walk > creep"

// Communication adverbs
!Adverb "loudly + say > shout"
!Adverb "quietly + say > whisper"
!Adverb "angrily + say > yell"

// Action adverbs
!Adverb "carefully + open > slowly_open"
!Adverb "forcefully + attack > brutal_attack"
```

### Dynamic Vocabulary Updates

```java
// Adding specialized profession verbs
if (player.hasSkill("blacksmithing")) {
    vocabulary.addVerb("forge", forgeVerb);
    vocabulary.addVerb("temper", temperVerb);
    vocabulary.addVerb("quench", quenchVerb);
}

// Regional vocabulary
if (player.getLocation().getRegion().equals("pirate_cove")) {
    vocabulary.addVerb("parley", parleyVerb);
    vocabulary.addNoun("cutlass", cutlassAtom);
    vocabulary.addAdverb("swashbucklingly + fight > pirate_fight");
}
```

## Integration Points

### Parser Integration

The vocabulary provides the lexical foundation for parsing:

```java
// Parser uses vocabulary for word classification
public void lookUpWords(Sentence sentence) {
    for (Word word : sentence.getWords()) {
        int types = vocabulary.getWordTypes(word.getValue());
        word.setPossibleWordTypes(types);
    }
}
```

### Object System Integration

Nouns are automatically synchronized with object definitions:

```java
// When objects are created, nouns are registered
public void addNounToVocabulary(String noun, Atom atom) {
    vocabulary.addNoun(noun, atom);
    vocabulary.addWordToDictionary(noun, Word.WT_NOUN);
}
```

### Event System Integration

Verbs trigger events through the action system:

```java
// Verb resolution leads to event creation
VerbTemplate template = verb.matchTemplate(prepositions);
String actionID = template.getPropertyID();
Event event = world.newEvent(actor, actionID, target, args);
world.postEvent(event);
```

## Configuration Files

### Script Locations

- `/mua/scripts/verbs` - Main verb definitions
- `/mua/scripts/nouns` - Noun classifications  
- `/mua/scripts/adverbs` - Adverb transformation rules
- `/mua/scripts/inner_core` - Core vocabulary
- `/mua/odl/*.odl` - Object definitions with automatic nouns

### Loading Order

1. **Core Vocabulary**: Basic word types and system commands
2. **Object Definitions**: Automatic noun generation from ODL
3. **Verb Definitions**: Command patterns and actions  
4. **Specializations**: Regional, profession, or context-specific words
5. **Customizations**: Server-specific vocabulary extensions

## Performance Considerations

### Optimization Strategies

- **Hashtable Lookups**: O(1) word resolution
- **Bitmask Types**: Efficient type checking
- **Template Caching**: Reuse compiled patterns
- **Lazy Loading**: Load vocabulary sections on demand

### Memory Management

- **String Interning**: Reuse common word strings
- **Compact Storage**: Minimize object overhead
- **Garbage Collection**: Clean up unused vocabulary entries
- **Reference Counting**: Track noun usage for cleanup

## Related Documentation

- [Parser Architecture](parser-architecture.md) - How vocabulary integrates with parsing
- [Verb Command Reference](verb-command.md) - Detailed !Verb syntax
- [Action System](action-system.md) - How verbs trigger actions
- [ODL Reference](odl-reference.md) - Object definitions and noun generation