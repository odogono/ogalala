# The !Verb Command

The `!Verb` command is a script command used to define verbs dynamically during world initialization or administration in the Ogalala MUD framework. It creates the vocabulary and grammar rules that the parser uses to understand and execute player commands during gameplay.

## Table of Contents

- [Syntax](#syntax)
- [Processing Flow](#processing-flow)
- [Template Components](#template-components)
- [Examples](#examples)
- [Advanced Usage](#advanced-usage)
- [Runtime Behavior](#runtime-behavior)

## Syntax

```
!Verb [privilege] "synonyms" "template>action" ["template>action" ...]
```

### Parameters

- **`privilege`** (optional): Minimum privilege level required to use this verb
  - Values: `Guest`, `Player`, `Builder`, `Admin`, etc.
  - If omitted, uses the default privilege level
  
- **`synonyms`**: Comma-delimited list of verb names
  - Example: `"look,l,peer,peek,spy,gaze"`
  - All synonyms will trigger the same verb behavior
  
- **`template>action`**: Template pattern and action mapping
  - **Template**: Defines the argument pattern and constraints
  - **Action**: Property ID to invoke on the target object
  - Connected by `>` character

## Processing Flow

1. **Script Execution**: When a script file is executed via `World.execScript()`, each line is processed by `Parser.parseSentence()`

2. **Command Recognition**: The parser identifies lines starting with `!verb` as verb definitions

3. **Verb Creation**: Creates a `Verb` object and processes:
   - Privilege level (if specified)
   - Synonym list (adds each to vocabulary)
   - Template definitions (creates `VerbTemplate` objects)

4. **Template Processing**: Each template is parsed into:
   - Argument types and constraints
   - Search modifiers
   - Property conditions
   - Action mappings

## Template Components

### Argument Types

| Type | Description | Example |
|------|-------------|---------|
| `none` | No arguments | "look" |
| `current` | Object being acted upon | "open door" |
| `thing` | Generic object | "get sword" |
| `direction` | Compass direction | "go north" |
| `string` | Text string | "say hello" |
| `numeric` | Number | "buy 5" |

### Search Modifiers

| Modifier | Description | Usage |
|----------|-------------|-------|
| `(see)` | Object must be visible | `current(see)` |
| `(touch)` | Object must be reachable | `thing(touch)` |
| `(inv)` | Search only inventory | `current(inv)` |
| `(room)` | Search only current room | `thing(room)` |
| `(world)` | Search entire world | `thing(world)` |

### Property Conditions

| Condition | Description | Example |
|-----------|-------------|---------|
| `{property}` | Property must be true | `{is_closed}` |
| `{!property}` | Property must be false | `{!locked}` |

## Examples

### Basic Look Command

```
!Verb "look,l,peer,peek,spy,gaze" \
    "none(see)>look" \
    "current(see)>examine" \
    "at current(see)>examine" \
    "direction(see)>look_far"
```

This creates a "look" verb with multiple synonyms and templates:
- `look` (no arguments) → calls `look` action on root
- `look sword` → calls `examine` action on sword
- `look at sword` → calls `examine` action on sword  
- `look north` → calls `look_far` action on north exit

### Open Command with Conditions

```
!Verb "open" \
    "current(see){is_closed}>open" \
    "current{is_closed} with thing>open_with" \
    "direction(touch){is_closed}>open"
```

This creates an "open" verb that:
- Opens closed objects you can see
- Opens objects with a tool/key
- Opens closed exits/doors

### Administrative Command

```
!Verb (Admin) "teleport,tp" \
    "thing(world)>teleport_to" \
    "current(world) to thing(world)>teleport_thing"
```

This creates admin-only teleport commands:
- `teleport kitchen` → teleport self to kitchen
- `teleport player to garden` → teleport player to garden

### Combat Command

```
!Verb "attack,hit,strike" \
    "current(see)>attack" \
    "thing with current(inv)>attack" \
    "thing at current(inv)>attack"
```

Creates attack commands:
- `attack goblin` → attack with bare hands
- `attack goblin with sword` → attack using weapon
- `attack goblin at sword` → attack using weapon

### Communication Command

```
!Verb "say,'" \
    "string>say" \
    "to current string>say"
```

Creates communication:
- `say hello` → say to everyone in room
- `say to john hello` → say directly to john

### Container Manipulation

```
!Verb "put,place" \
    "current(inv) in current(see)>put_in" \
    "current(inv) on current(see)>put_on" \
    "current(inv) under current(see)>put_under"
```

Creates put commands:
- `put sword in bag` → put sword into bag
- `put book on table` → put book onto table
- `put key under rock` → put key under rock

## Advanced Usage

### Complex Templates with Multiple Conditions

```
!Verb "unlock" \
    "current(see){is_closed}{is_locked} with thing(inv)>unlock"
```

This requires the target to be:
- Visible to the player
- Currently closed
- Currently locked
- Using an item from inventory

### Privilege-Based Commands

```
!Verb (Builder) "create" \
    "thing>create_thing" \
    "room>create_room" \
    "exit from current(world) to current(world)>create_exit"

!Verb (Admin) "shutdown" \
    "none>shutdown_server"
```

### Direction-Based Movement

```
!Verb "go,move,walk" \
    "direction>go" \
    "to current(see)>go_to"
```

Allows both:
- `go north` → use north exit
- `go to kitchen` → find path to kitchen

## Runtime Behavior

### Verb Matching Process

When a player types a command like "open door":

1. **Verb Lookup**: Parser finds "open" in vocabulary
2. **Template Matching**: Tries each template against input:
   - Checks if "door" matches `current(see){is_closed}`
   - Verifies door is visible and closed
3. **Object Resolution**: Locates door object in environment
4. **Action Execution**: Calls door's `open` property (→ `!OpenDoor` action)

### Error Handling

If matching fails, the system provides appropriate feedback:
- Verb not found: "I don't understand that."
- Object not found: "I don't see that here."
- Condition not met: "The door is already open."
- Privilege insufficient: Command is ignored

### Template Priority

Templates are matched in the order they appear in the `!Verb` definition. More specific templates should be listed first:

```
!Verb "get" \
    "current(inv)(see)>get_from_inventory" \  # More specific
    "current(see)>get"                        # Less specific
```

## File Locations

Verb definitions are typically found in:
- `/mua/scripts/verbs` - Main verb definitions
- `/mua/scripts/inner_core` - Core action mappings
- `/mua/odl/behaviours.odl` - Object behavior definitions

## Related Documentation

- [Parser Architecture](parser-architecture.md)
- [Action System](action-system.md)
- [Object Definition Language](odl-reference.md)
- [Vocabulary Management](vocabulary.md)