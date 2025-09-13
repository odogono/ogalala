# Parser Architecture

The Ogalala MUD framework uses a sophisticated natural language parser to convert player input into executable game actions. The parser processes English-like commands through multiple stages to resolve ambiguities and create precise event objects.

## Table of Contents

- [Overview](#overview)
- [Parser Pipeline](#parser-pipeline)
- [Core Components](#core-components)
- [Processing Stages](#processing-stages)
- [Sentence Processing](#sentence-processing)
- [Event Generation](#event-generation)
- [Error Handling](#error-handling)

## Overview

The parser architecture follows a **pipeline pattern** where player input flows through sequential processing stages. Each stage transforms the input closer to executable events, with error handling and validation at each step.

### Key Design Principles

- **Natural Language Support**: Handles imperative sentences with verbs, objects, and prepositions
- **Ambiguity Resolution**: Resolves multiple possible interpretations through context
- **Extensible Vocabulary**: Dynamic verb and noun definitions via script commands
- **Privilege System**: Commands filtered by user permission levels
- **Multi-sentence Support**: Processes compound commands separated by periods

## Parser Pipeline

The parser follows an 11-stage pipeline in `Parser.parseSentence()`:

```
Input String → Tokenization → Sentence Loading → Special Commands → 
Word Lookup → Terminator Splitting → Lexical Analysis → 
Conjunction Splitting → Sentence Correction → Verb/Adverb Assignment → 
Template Matching → Event Generation → Event Posting
```

### Pipeline Flow

1. **Tokenization**: Break input into word tokens
2. **Sentence Loading**: Create `Sentence` objects with `Word` components  
3. **Special Commands**: Handle `!Verb`, `!Run`, and other meta-commands
4. **Word Lookup**: Resolve word types using vocabulary
5. **Terminator Splitting**: Split on periods into multiple sentences
6. **Lexical Analysis**: Assign grammatical roles to words
7. **Conjunction Splitting**: Handle "and" connectors
8. **Sentence Correction**: Apply grammatical fixes
9. **Verb/Adverb Assignment**: Match verbs and fold in adverbs
10. **Template Matching**: Find appropriate verb templates
11. **Event Generation**: Create and post events to the world

## Core Components

### Parser Class (`mua/parser/Parser.java`)

The main parser class that orchestrates the pipeline:

```java
public class Parser {
    private World world;           // Back-pointer to game world
    private static Vocabulary vocabulary;  // Word definitions
    private Vector sentenceStore;  // Processed sentences
    private Atom actor;           // Current player
    private int privilege;        // User permission level
}
```

### Vocabulary Class (`mua/parser/Vocabulary.java`)

Manages all word definitions and lookups:

```java
public class Vocabulary {
    protected Dictionary words;    // Master word list
    protected Dictionary verbs;    // Verb definitions
    protected Dictionary nouns;    // Noun definitions
    protected Dictionary adverbs;  // Adverb transformations
}
```

### Sentence Class (`mua/parser/Sentence.java`)

Represents a parsed command with words and grammatical structure:

```java
public class Sentence {
    private Vector words;         // List of Word objects
    private Verb verb;           // Primary verb
    private Vector nounPhrases;  // Noun phrase structures
}
```

### Word Class (`mua/parser/Word.java`)

Individual word with type information:

```java
public class Word {
    private String value;             // Word text
    private int wordType;            // Grammatical type
    private int possibleWordTypes;   // Potential types
    
    // Word type constants
    public static final int WT_UNKNOWN = 0;
    public static final int WT_VERB = 1;
    public static final int WT_NOUN = 2;
    public static final int WT_PREPOSITION = 4;
    // ... other types
}
```

## Processing Stages

### Stage 1-2: Tokenization and Loading

```java
// Tokenize input string
String tokens[] = tokenizeSentence(sentence);

// Load into Sentence object
loadStringIntoSentence(first, tokens);
```

**Handles:**
- String literals in quotes: `"hello world"`
- Numbers in tildes: `~42~`
- Punctuation: commas, periods, exclamation marks
- Special characters for raw verbs starting with `!`

### Stage 3: Special Command Handling

```java
private Sentence handleLowLevelSentences(Sentence sentence) {
    if (sentence.getWordValue(0).equals("!verb")) {
        addVerb(sentence.getWordValue(1, sentence.size()-1));
        throw new ParserPipelineException("Add verb found");
    }
    // ... other special commands
}
```

**Special Commands:**
- `!Verb` - Define new verbs
- `!RawVerb` - Define raw verbs  
- `!Run` - Execute script files
- `!Priv` - Manage privilege levels
- `again`/`g` - Repeat last command

### Stage 4-5: Word Lookup and Splitting

```java
private void lookUpWords(Sentence sentence) {
    for (int i = 0; i < sentence.size(); i++) {
        Word word = sentence.getWord(i);
        int types = vocabulary.getWordTypes(word.getValue());
        word.setPossibleWordTypes(types);
    }
}
```

**Word Type Resolution:**
- Consults vocabulary for each word
- Handles multiple possible types
- Special cases for numbers and directions
- Automatic verb insertion for directions (`north` → `go north`)

### Stage 6-7: Lexical Analysis

```java
private void lexicallyAnalyse(Atom actor, Vector sentences, boolean verbose) {
    // Assign grammatical roles based on context
    // Resolve ambiguities using game world state
    // Validate sentence structure
}
```

**Functions:**
- Assigns definitive word types
- Resolves `current` vs `thing` ambiguities
- Validates sentence grammar
- Handles communicative verbs specially

### Stage 8-10: Verb Processing

```java
private void assignVerbsAndAdverbs(Vector sentences, Vocabulary vocab, int privilege) {
    // Match verbs from vocabulary
    // Apply adverb transformations
    // Check privilege levels
}
```

**Adverb Handling:**
- `quickly walk` → `run`
- `loudly say` → `shout`
- Adverb definitions in vocabulary

### Stage 11: Event Generation

```java
private void generateParserEvents(Parser parser, Vector sentences) {
    // Match verb templates against sentence structure
    // Resolve object references
    // Create Event objects
}
```

## Sentence Processing

### Noun Phrase Resolution

The parser builds noun phrases to handle complex object references:

```java
public class NounPhrase {
    private Vector adjectives;    // Descriptive words
    private Vector nouns;        // Object identifiers  
    private Atom resolvedAtom;   // Final object reference
}
```

**Examples:**
- `red sword` → finds red-colored sword object
- `wooden chest` → finds wooden chest in environment
- `first book` → uses ordinal selection

### Object Resolution

Objects are resolved through multiple search strategies:

1. **Inventory Search**: Player's carried items
2. **Container Search**: Objects in current room
3. **World Search**: Global object lookup (admin commands)
4. **Visibility Checks**: Respects line-of-sight rules

### Template Matching

Verb templates define valid command patterns:

```java
public class VerbTemplate {
    private int argType[];           // Argument types
    private String preposition[];    // Connecting words
    private String propertyID;       // Action to invoke
    private Hashtable modifiers[];   // Property conditions
}
```

**Matching Process:**
1. Try templates in definition order
2. Check argument type compatibility
3. Verify object constraints
4. Validate property conditions
5. Select best match

## Event Generation

### Event Structure

Successful parsing creates `Event` objects:

```java
public class Event {
    private Atom actor;        // Command initiator
    private String verb;       // Action identifier  
    private Atom target;       // Primary object
    private Object[] args;     // Additional arguments
}
```

### Event Posting

Events are queued for execution:

```java
world.postEvent(event);  // Add to event queue
```

The event system handles:
- **Asynchronous Execution**: Events processed in order
- **Action Resolution**: Property lookup and execution
- **Error Propagation**: Failed events return messages
- **Result Handling**: Action outcomes sent to players

## Error Handling

### Parser Exceptions

```java
public class ParserException extends RuntimeException {
    // Thrown for syntax errors, unknown words, etc.
}

public class ParserPipelineException extends Exception {
    // Used to exit pipeline early (not always an error)
}
```

### Error Types

1. **Syntax Errors**: Malformed commands
2. **Unknown Words**: Vocabulary lookup failures  
3. **Ambiguous References**: Multiple object matches
4. **Privilege Violations**: Insufficient permissions
5. **Template Mismatches**: No valid verb patterns

### Error Messages

The parser provides contextual error feedback:

```java
actor.output("I don't understand that.");
actor.output("I don't see that here.");  
actor.output("You can't do that.");
actor.output("Be more specific.");
```

## Configuration

### Vocabulary Loading

Vocabulary is loaded from script files during world initialization:

```
/mua/scripts/verbs     # Verb definitions
/mua/scripts/nouns     # Noun definitions  
/mua/scripts/adverbs   # Adverb transformations
```

### Parser Settings

Parser behavior can be configured:

```java
private static final boolean DEBUG = false;  // Debug output
private int privilege;                        // User permission level
```

## Performance Considerations

### Optimization Strategies

1. **Vocabulary Caching**: Words cached in hashtables
2. **Template Ordering**: Most common patterns first
3. **Early Termination**: Pipeline exits on special commands
4. **Object Filtering**: Search scope limited by context

### Memory Management

- **Sentence Reuse**: Cached sentence objects
- **Word Pooling**: Reused word instances
- **Event Cleanup**: Automatic garbage collection

## Related Classes

- `VerbTemplate.java` - Template pattern matching
- `NounPhrase.java` - Object reference resolution  
- `Adverb.java` - Verb transformation rules
- `World.java` - Event execution and world state
- `Atom.java` - Game object representation

## Usage Examples

### Basic Command Processing

```java
Parser parser = world.newParser();
parser.parseSentence("get sword", player);
// → Creates Event(player, "get", sword_object)
```

### Multi-sentence Commands

```java
parser.parseSentence("get sword. open door. go north", player);
// → Creates three separate events
```

### Complex Object References

```java
parser.parseSentence("put red sword in wooden chest", player);
// → Resolves specific objects by attributes
```