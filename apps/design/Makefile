# $Id: Makefile,v 1.16 1999/04/13 09:54:34 matt Exp $
# Makefile for World Runner
# Matthew Caldwell, 13 Nov 98
# Copyright (C) Ogalala Ltd. <info@ogalala.com>

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

#######################################################################

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
SRC_ACTION=nileStuff/action/*.java mua/action/*.java
SRC_TOOLS=mua/tools/*.java
SRC_WIDGETS=widgets/*.java
SRC_WIDGETS_TEXT=widgets/text/*.java
SRC_CLIENT=client/MenuManager.java client/MessageFormatter.java client/Mapper.java
SRC_DESIGN_TOOLS=*.java
SRC_NILE=nileStuff/nilesrc/*.java

# all sources -- probably don't need this, since this won't run any
# of the doc targets, but still
SRC_ALL=$(SRC_UTIL) $(SRC_CRYPT) $(SRC_DB) $(SRC_SERVER) \
    $(SRC_MUA) $(SRC_MUA_APP) $(SRC_CHAT) $(SRC_ACTION) $(SRC_TOOLS) \
    $(SRC_WIDGETS) $(SRC_WIDGETS_TEXT) $(SRC_CLIENT) $(SRC_DESIGN_TOOLS) \
    $(SRC_NILE)

# locations of the application scripts
SCRIPTS_MUA=mua/scripts
SCRIPTS_NILE=nileStuff/scripts

# locations of the odl source files
ODL_MUA=mua/odl
ODL_NILE=nileStuff/odl
ODL_NILECORE=nileStuff/odlcore

# where to put the compiled atom/thing/exit files
THING_MUA=$(SCRIPTS_MUA)/compiled
THING_NILE=$(SCRIPTS_NILE)/compiled

# where to put the compiled dependency script
WORLD=world

# the main script (this runs WORLD, among other things)
MAIN=main

# the script search path
SCRIPT_PATH=.:$(THING_NILE):$(SCRIPTS_NILE):$(THING_MUA):$(SCRIPTS_MUA)

#######################################################################

# java targets and dependencies

# the targets roughly correlate to the separate packages, though
# there's a certain amount of deviation from this scheme

# there's a dummy .tgt file for all the package targets, since the
# packages themselves may consist of many different class files

all: designTools.jar $(WORLD)

# build the jar file
designTools.jar: designTools.tgt
	@echo CREATING JAR FILE
	$(JAR) -c0f designTools.jar com

# build the design tools 
designTools.tgt: app.tgt action.tgt tools.tgt textWidgets.tgt client.tgt $(SRC_DESIGN_TOOLS)
	@echo BUILDING DESIGN TOOLS
	$(JAVAC) $(SRC_DESIGN_TOOLS)
	touch designTools.tgt

# our subset of the client package is relatively self-contained
# I suspect it doesn't really need the widgets either, but
# I'll err on the side of caution
client.tgt: util.tgt widgets.tgt $(SRC_CLIENT)
	@echo COMPILING CLIENT PACKAGE
	$(JAVAC) $(SRC_CLIENT)
	touch client.tgt

textWidgets.tgt: widgets.tgt util.tgt $(SRC_WIDGETS_TEXT)
	@echo COMPILING TEXT PACKAGE
	$(JAVAC) widgets/text/*.java
	touch textWidgets.tgt

# the widgets package needs to copy over some required resources
widgets.tgt: util.tgt widgets/*.java
	@echo COMPILING WIDGETS PACKAGE
	$(JAVAC) widgets/*.java
	-mkdir com/ogalala/widgets/resources
	-cp widgets/resources/* com/ogalala/widgets/resources
	touch widgets.tgt

# the command-line tools require all the server classes
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
$(WORLD): odl_mua.tgt odl_nilecore.tgt
	@echo MAKING WORLD SCRIPT
	jre -cp designTools.jar com.ogalala.tools.DependencyCompiler \
		-o $(WORLD) $(THING_MUA)/* $(THING_NILE)/*

# compile all the generic ODL files
odl_mua.tgt: designTools.jar $(ODL_MUA)/*
	@echo MAKING GENERIC MUA SCRIPTS
	jre -cp designTools.jar com.ogalala.tools.ODLCompiler $(ODL_MUA)/*.odl
	-mkdir -p $(THING_MUA)
	-mv $(ODL_MUA)/*.atom $(THING_MUA)
	-mv $(ODL_MUA)/*.thing $(THING_MUA)
	-mv $(ODL_MUA)/*.exit $(THING_MUA)
	touch odl_mua.tgt

# compile the core nile ODL files
odl_nilecore.tgt: designTools.jar $(ODL_NILECORE)/*
	@echo MAKING CORE NILE SCRIPTS
	jre -cp designTools.jar com.ogalala.tools.ODLCompiler $(ODL_NILECORE)/*.odl
	-mkdir -p $(THING_NILE)
	-mv $(ODL_NILECORE)/*.atom $(THING_NILE)
	-mv $(ODL_NILECORE)/*.thing $(THING_NILE)
	-mv $(ODL_NILECORE)/*.exit $(THING_NILE)
	touch odl_nilecore.tgt

# core database
core.db: designTools.jar $(WORLD)
	@echo BUILDING WORLD DATABASE
	jre -cp designTools.jar com.ogalala.tools.DatabaseBuilder \
		-d core.db -s $(SCRIPT_PATH)

# core database with doc comments and other useful fields
doc.db: designTools.jar $(WORLD)
	@echo BUILDING DOCUMENTED DATABASE
	jre -cp designTools.jar com.ogalala.tools.DocCompiler \
		-d doc.db -s $(SCRIPT_PATH) -f main -z

# design tools archive
# the process of dosifying the BAT files is pretty kludgy
# in the long term it would be better to have a cleaner
# way of doing this...
build.zip: core.db doc.db
	@echo BUILDING DESIGN TOOLS ARCHIVE
	-mkdir build
	-mkdir build/scripts
	-cp designTools.jar build
	-cp core.* build
	-cp doc.* build
	-dosify <property.attributes >build/property.attributes
	-dosify <designTools.bat >build/designTools.bat
	-dosify <atomBuilder.bat >build/atomBuilder.bat
	-dosify <odl_checkin.bat >build/odl_checkin.bat
	-dosify <odl_checkout.bat >build/odl_checkout.bat
	-dosify <odl_update.bat >build/odl_update.bat
	jar -c0f build.zip build

#######################################################################

# remove binaries and .tgt files

clean:
	@echo REMOVING BINARIES AND INTERMEDIATE FILES
	-rm -Rf com
	-rm -Rf $(THING_MUA)
	-rm -Rf $(THING_NILE)
	-rm -Rf build
	-rm -f build.zip
	-rm -f core.*
	-rm -f doc.*
	-rm -f $(WORLD)
	-rm -f designTools.jar
	-rm -f *.tgt

#######################################################################
