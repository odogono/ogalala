# $Id: Makefile,v 1.31 1998/08/27 11:25:38 matt Exp $
# Makefile for Session Server
# James Fryer, 3 July 98
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

#JCLASSPATH=.:./util:./net:./crypt:./command:./database:./protocol:/misc/ODI/pse.jar

# I *think* is the only classpath addition we need
JCLASSPATH=/misc/ODI/pse.jar

JC=javac
JFLAGS=-nowarn -classpath $(JCLASSPATH):$(CLASSPATH) -d .
JAVAC=$(JC) $(JFLAGS)

JAR=jar
JARFLAGS=-c0f
JARC=$(JAR) $(JARFLAGS)

#######################################################################

# targets and dependencies

# the targets roughly correlate to the separate packages, though
# there's a certain amount of deviation from this scheme

# there's a dummy .tgt file for all the package targets, since the
# packages themselves may consist of many different class files

all: server.jar

# actions and atom definitions are loaded dynamically
server.jar: mua.tgt action.tgt atomdef.tgt
	@echo CREATING JAR FILE
	$(JAR) -c0f Server.jar com

# the actions package seems to be split across two directories
# -- this will need to be revised in future
action.tgt: util.tgt mua.tgt eval2/actions/*.java mua/action/*.java
	@echo MAKING ACTIONS PACKAGE
	$(JAVAC) eval2/actions/*.java mua/action/*.java
	touch action.tgt

# this target will probably vanish in the mix
atomdef.tgt: eval2/atomDefinitions/AtomDefinition.java
	@echo MAKING ATOM DEFINITIONS
	$(JAVAC) eval2/atomDefinitions/AtomDefinition.java
	touch atomdef.tgt
	
mua.tgt: util.tgt server.tgt mua/*.java
	@echo MAKING MUA PACKAGE
	$(JAVAC) mua/*.java
	touch mua.tgt

server.tgt: util.tgt database.tgt crypt.tgt server/*.java server/protocol/PasswordLogin.java
	@echo MAKING SERVER PACKAGE
	$(JAVAC) server/*.java server/protocol/PasswordLogin.java
	touch server.tgt

command.tgt: util.tgt database.tgt server/command/*.java
	@echo MAKING COMMAND PACKAGE
	$(JAVAC) server/command/*.java
	touch command.tgt

database.tgt: server/database/*.java
	# it's possible we may need to compile server/database/UserNotFoundException.java
	# first in order to avoid import errors, but with luck javac will take care of
	# that itself...
	@echo MAKING DATABASE PACKAGE
	$(JAVAC) server/database/*.java
	touch database.tgt

parser.tgt: mua/parser/*.java
	@echo MAKING PARSER PACKAGE
	$(JAVAC) mua/parser/*.java
	touch parser.tgt

# the net target may not be needed in this Makefile at all
# in which case the net directory should be removed from
# this and future server modules	
#net.tgt: util.tgt net/*.java
#	@echo MAKING NET PACKAGE
#	$(JAVAC) net/*.java
#	touch net.tgt
	
util.tgt: util/*.java server/protocol/ChannelUtil.java
	@echo MAKING UTIL PACKAGE
	$(JAVAC) util/*.java server/protocol/ChannelUtil.java
	touch util.tgt 

crypt.tgt: crypt/*.java server/protocol/PasswordValidator.java server/protocol/AuthenticationException.java
	@echo MAKING CRYPT PACKAGE
	$(JAVAC) crypt/*.java server/protocol/PasswordValidator.java server/protocol/AuthenticationException.java
	touch crypt.tgt

# remove binaries and .tgt files
	@echo REMOVING BINARIES AND INTERMEDIATE FILES
	-rm -Rf com
	-rm -f Server.jar
	-rm *.tgt
