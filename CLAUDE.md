# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Ogalala codebase - a Java-based MUD (Multi-User Dungeon) server framework with tools and utilities. The project appears to be from the late 1990s and uses a modular architecture with separate server framework, game engine (MUA), and development tools.

## Build Commands

The project uses traditional Make-based builds with multiple JAR outputs:

**Server Build:**
```bash
cd server
make all        # Builds server.jar with all server components
make clean      # Remove binaries and intermediate files
```

**Tools Build:**
```bash
cd tools
make all        # Builds tools.jar containing development utilities
make clean      # Remove binaries and intermediate files
```

**Individual Application Builds:**
```bash
cd apps/nileServer && make
cd apps/design && make
cd apps/eval2 && make
```

## Architecture Overview

**Core Components:**
- `server/` - Network server framework with connection management, login handling, and protocol support
- `mua/` - MUA (Multi-User Application) engine providing the game world, event system, and scripting
- `nile/` - Database and world persistence layer
- `tools/` - Development utilities including dependency compiler, documentation generator, and ODL compiler
- `apps/` - Specific applications built on the framework

**Key Architectural Concepts:**
- **Server Framework**: Connection-based architecture where each user connection is encapsulated in a Connection class
- **Event System**: Central event processing with EventQueue, EventProcessor, and Timer classes
- **World Model**: Object-oriented world representation with atoms, actions, and properties
- **Protocol Layer**: Extensible login and communication protocols
- **Scripting**: MuaScript provides in-game scripting capabilities

**Key Classes:**
- `server/Server.java` - Main server framework class
- `mua/World.java` - Core game world implementation  
- `mua/Event.java` - Event system foundation
- `tools/ODLCompiler.java` - Object Definition Language compiler

## Development Notes

- **Java Version**: Built for late 1990s Java (likely JDK 1.1/1.2 era)
- **Database**: Uses ODI (Object Database Interface) with PSE.jar dependency
- **Build System**: Traditional Make with manual dependency management
- **CVS Integration**: Original version control was CVS (historical artifact)

**Build Dependencies:**
- Requires `/misc/ODI/pse.jar` in classpath for database functionality
- Uses standard JDK tools (javac, jar)
- Make must be run from specific directories as noted in Makefiles