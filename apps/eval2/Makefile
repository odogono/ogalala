# $Id: Makefile,v 1.24 1998/08/28 14:13:07 matt Exp $
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

# build the jar file
server.jar: app.tgt action.tgt
	@echo CREATING JAR FILE
	$(JAR) -c0f Server.jar com

# the actions package seems to be split across two directories
action.tgt: util.tgt mua.tgt actions/*.java mua/action/*.java atomDefinitions/AtomDefinition.java
	@echo MAKING ACTIONS PACKAGE
	$(JAVAC) actions/*.java mua/action/*.java atomDefinitions/AtomDefinition.java
	touch action.tgt

app.tgt: chat.tgt mua_app.tgt

chat.tgt: server.tgt server/test/ChatPerson.java server/test/ChatRoom.java server/test/ChatApp.java
	@echo MAKING CHAT APPLICATION
	$(JAVAC) server/test/ChatPerson.java server/test/ChatRoom.java server/test/ChatApp.java
	touch chat.tgt

mua_app.tgt: mua.tgt mua/apps/MuaApp.java
	@echo MAKING MUA APPLICATION
	$(JAVAC) mua/apps/MuaApp.java
	touch app.tgt

mua.tgt: util.tgt server.tgt mua/*.java
	@echo MAKING MUA PACKAGE
	$(JAVAC) mua/*.java
	touch mua.tgt

server.tgt: util.tgt database.tgt crypt.tgt server/*.java server/command/*.java server/protocol/PasswordLogin.java
	@echo MAKING SERVER PACKAGE
	$(JAVAC) server/*.java server/protocol/PasswordLogin.java server/command/*.java
	touch server.tgt

#command.tgt: util.tgt database.tgt server/command/*.java
#	@echo MAKING COMMAND PACKAGE
#	$(JAVAC) server/command/*.java
#	touch command.tgt

database.tgt: server/database/*.java
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

crypt.tgt: server/crypt/*.java server/protocol/PasswordValidator.java server/protocol/AuthenticationException.java
	@echo MAKING CRYPT PACKAGE
	$(JAVAC) server/crypt/*.java server/protocol/PasswordValidator.java server/protocol/AuthenticationException.java
	touch crypt.tgt

# remove binaries and .tgt files
clean:
	@echo REMOVING BINARIES AND INTERMEDIATE FILES
	-rm -Rf com
	-rm -f Server.jar
	-rm -f *.tgt

