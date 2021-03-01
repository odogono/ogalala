# $Id: Makefile,v 1.16 1999/04/13 09:07:09 jim Exp $
# Makefile for Nile Server
# James Fryer, 3 Nov 98
# Copyright (C) Ogalala Ltd. <info@ogalala.com>

#######################################################################

# This file should make a working server.jar on any suitably
# configured Unix platform (ie, with the JDK correctly installed
# and in the PATH). It has only been tested under Linux with
# GNU make, but it's pretty straightforward stuff.

# It is written to be made from within the root directory of
# the CVS server module (currently "eval2"). It will *not*
# work from within the server directory in the full CVS source
# tree.

#######################################################################

# macros
JCLASSPATH=/misc/ODI/pse.jar

JC=javac
JFLAGS=-nowarn -classpath $(JCLASSPATH):$(CLASSPATH) -d .
JAVAC=$(JC) $(JFLAGS)

JAR=jar
JARFLAGS=-c0f
JARC=$(JAR) $(JARFLAGS)

JDOC=javadoc
JDOCFLAGS=-classpath $(JCLASSPATH):$(CLASSPATH) -d docs
JAVADOC=$(JDOC) $(JDOCFLAGS)

# locations of the java source files for the different targets
# these have been extracted from the targets because they
# appear in multiple places
SRC_UTIL=util/*.java server/protocol/ChannelUtil.java
SRC_CRYPT=server/crypt/*.java server/protocol/PasswordValidator.java server/protocol/AuthenticationException.java
SRC_DB=server/database/*.java
SRC_PARSER=mua/parser/*.java
SRC_SERVER=server/*.java server/protocol/PasswordLogin.java server/command/*.java
SRC_MUA=mua/*.java mua/atom/*.java $(SRC_PARSER)
SRC_MUA_APP=mua/apps/MuaApp.java
SRC_CHAT=server/test/ChatPerson.java server/test/ChatRoom.java server/test/ChatApp.java
SRC_ACTION=action/*.java mua/action/*.java
SRC_TOOLS=mua/tools/*.java
SRC_NILE=nilesrc/*.java

SRC_ALL=$(SRC_UTIL) $(SRC_CRYPT) $(SRC_DB) $(SRC_SERVER) $(SRC_MUA) $(SRC_MUA_APP) $(SRC_CHAT) $(SRC_ACTION) $(SRC_TOOLS) $(SRC_NILE)

# locations of the application scripts
SCRIPTS_MUA=mua/scripts
SCRIPTS_NILE=scripts

# locations of the odl source files
ODL_MUA=mua/odl
ODL_NILE=odl
ODL_NILECORE=odlcore

# where to put the compiled atom/thing/exit files
THING_MUA=$(SCRIPTS_MUA)/compiled
THING_NILE=$(SCRIPTS_NILE)/compiled

# where to put the compiled dependency script
WORLD=$(SCRIPTS_NILE)/world

# the main script (this runs WORLD, among other things)
MAIN=main

# the script search path
SCRIPT_PATH=$(SCRIPTS_NILE):$(SCRIPTS_MUA):$(THING_NILE):$(THING_MUA)

#######################################################################

# java targets and dependencies

# the targets roughly correlate to the separate packages, though
# there's a certain amount of deviation from this scheme

# there's a dummy .tgt file for all the package targets, since the
# packages themselves may consist of many different class files

all: Server.jar $(WORLD)

# build the jar file
Server.jar: app.tgt action.tgt tools.tgt
	@echo CREATING JAR FILE
	$(JAR) -c0f Server.jar com

# the tools package depends on everything and is used by nothing
# so it gets done last
tools.tgt: app.tgt action.tgt $(SRC_TOOLS)
	@echo MAKING TOOLS PACKAGE
	$(JAVAC) $(SRC_TOOLS)
	touch tools.tgt

nilesrc.tgt: util.tgt mua.tgt $(SRC_NILE)
	@echo MAKING NILESRC PACKAGE
	$(JAVAC) $(SRC_NILE)
	touch nilesrc.tgt

# the actions package seems to be split across two directories
action.tgt: util.tgt mua.tgt nilesrc.tgt $(SRC_ACTION)
	@echo MAKING ACTIONS PACKAGE
	$(JAVAC) $(SRC_ACTION)
	touch action.tgt

app.tgt: chat.tgt mua_app.tgt
	touch app.tgt

chat.tgt: server.tgt $(SRC_CHAT)
	@echo MAKING CHAT APPLICATION
	$(JAVAC) $(SRC_CHAT)
	touch chat.tgt

mua_app.tgt: mua.tgt $(SRC_MUA_APP)
	@echo MAKING MUA APPLICATION
	$(JAVAC) $(SRC_MUA_APP)
	touch mua_app.tgt

mua.tgt: util.tgt server.tgt $(SRC_MUA)
	@echo MAKING MUA PACKAGE
	$(JAVAC) $(SRC_MUA)
	touch mua.tgt

server.tgt: util.tgt database.tgt crypt.tgt $(SRC_SERVER)
	@echo MAKING SERVER PACKAGE
	$(JAVAC) $(SRC_SERVER)
	touch server.tgt

database.tgt: $(SRC_DB)
	@echo MAKING DATABASE PACKAGE
	$(JAVAC) $(SRC_DB)
	touch database.tgt

# is this target ever used? it seems to be included in mua.tgt anyway
parser.tgt: $(SRC_PARSER)
	@echo MAKING PARSER PACKAGE
	$(JAVAC) $(SRC_PARSER)
	touch parser.tgt

util.tgt: $(SRC_UTIL)
	@echo MAKING UTIL PACKAGE
	$(JAVAC) $(SRC_UTIL)
	touch util.tgt 

crypt.tgt: $(SRC_CRYPT)
	@echo MAKING CRYPT PACKAGE
	$(JAVAC) $(SRC_CRYPT)
	touch crypt.tgt

#######################################################################

# scripts

# generate a single script that runs all the others
$(WORLD): odl_mua.tgt odl_nilecore.tgt odl_nile.tgt
	@echo MAKING WORLD SCRIPT
	jre -cp Server.jar com.ogalala.tools.DependencyCompiler -o $(WORLD) \
		$(THING_MUA)/* $(THING_NILE)/*

# compile all the generic ODL files
odl_mua.tgt: Server.jar $(ODL_MUA)/*
	@echo MAKING GENERIC MUA SCRIPTS
	jre -cp Server.jar com.ogalala.tools.ODLCompiler $(ODL_MUA)/*.odl
	-mkdir -p $(THING_MUA)
	-mv $(ODL_MUA)/*.atom $(THING_MUA)
	-mv $(ODL_MUA)/*.thing $(THING_MUA)
	-mv $(ODL_MUA)/*.exit $(THING_MUA)
	touch odl_mua.tgt

# compile the in-progress nile-specific ODL files
odl_nile.tgt: Server.jar $(ODL_NILE)/*
	@echo MAKING IN-PROGRESS NILE SCRIPTS
	jre -cp Server.jar com.ogalala.tools.ODLCompiler $(ODL_NILE)/*.odl
	-mkdir -p $(THING_NILE)
	-mv $(ODL_NILE)/*.atom $(THING_NILE)
	-mv $(ODL_NILE)/*.thing $(THING_NILE)
	-mv $(ODL_NILE)/*.exit $(THING_NILE)
	touch odl_nile.tgt

# compile the core nile-specific
odl_nilecore.tgt: Server.jar $(ODL_NILECORE)/*
	@echo MAKING CORE NILE SCRIPTS
	jre -cp Server.jar com.ogalala.tools.ODLCompiler $(ODL_NILECORE)/*.odl
	-mkdir -p $(THING_NILE)
	-mv $(ODL_NILECORE)/*.atom $(THING_NILE)
	-mv $(ODL_NILECORE)/*.thing $(THING_NILE)
	-mv $(ODL_NILECORE)/*.exit $(THING_NILE)
	touch odl_nilecore.tgt

#######################################################################

# documentation targets
doc:
	@echo MAKING DOCUMENTATION
	make javadoc
	make atomdoc

# make javadoc makes all the javadoc documentation
# this is pretty unwieldy, since the directory structure
# doesn't match the package structure and so each file has
# to be individually specified, but it's all thankfully
# hidden by macros and the joy of shell wildcards
javadoc:
	@echo MAKING JAVA DOCUMENTATION
	-mkdir docs
	$(JAVADOC) $(SRC_ALL)

# make atomdoc makes all the atom documentation
# this requires all the atom script files to have
# been compiled and placed in the appropriate locations
# since to all intents and purposes it is starting
# everything up as if it were the actual game
atomdoc: $(WORLD)
	@echo MAKING ATOM DOCUMENTATION
	-mkdir -p docs/atom
	jre -cp Server.jar com.ogalala.tools.DocCompiler -e docs/atom/log \
		-d doc.db -s $(SCRIPT_PATH) \
		-p docs/atom/ -t -f $(MAIN)

#######################################################################

# remove binaries and .tgt files

clean:
	@echo REMOVING BINARIES AND INTERMEDIATE FILES
	-rm -Rf com
	-rm -Rf docs
	-rm -Rf $(THING_MUA) $(THING_NILE)
	-rm -f $(WORLD)
	-rm -f Server.jar
	-rm -f *.db
	-rm -f *.tgt

#######################################################################
