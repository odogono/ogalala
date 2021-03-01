# $Id: Makefile,v 1.1 1998/11/06 17:04:19 matt Exp $
# Makefile for command line script tools
# Matthew Caldwell, 2 October 1998
# Copyright (C) Ogalala Ltd. <info@ogalala.com>

#####################################################################

# This file should make a working tools.jar on any suitably
# configured Unix platform (ie, with the JDK correctly installed
# and in the PATH). It has only been tested under Linux with
# GNU make, but it's pretty straightforward stuff.

# It is written to be made from within the root directory of
# the CVS tools module (currently "cliTools"). It will *not*
# work from within the tools directory in the full CVS source
# tree, so don't even bother trying.

#####################################################################

# Macros

JC=javac
JFLAGS=-nowarn -classpath $(CLASSPATH) -d .
JAVAC=$(JC) $(JFLAGS)

JAR=jar
JARFLAGS=-cf
JARC=$(JAR) $(JARFLAGS)

#####################################################################

# targets and dependencies

# Each package is a separate target, with a dummy .tgt file to
# provide a tangible target (with a mod date) for make to reference.

# In the current CVS "client" module, the directory structure
# deviates from the package structure in various ways (reflected
# below). This is not ideal and in the long run I'd prefer to
# see them more closely matched, since it would make the build
# process easier, but I'll work with what we've got for now.

all: tools.jar

# create the jar file
tools.jar: tools.tgt
	@echo
	@echo BUILDING JAR FILE
	$(JARC) tools.jar com

# make the tools package
tools.tgt: util.tgt atom.tgt *.java
	@echo
	@echo COMPILING TOOLS PACKAGE
	$(JAVAC) *.java
	touch tools.tgt

# make the atoms stuff
atom.tgt: util.tgt atoms/*.java MuaDummies.java
	@echo
	@echo COMPILING ATOMS PACKAGE
	$(JAVAC) atoms/*.java MuaDummies.java
	touch atom.tgt

# make the util package -- at present only the
# TableParser class is actually used by the tools,
# but others may be later so let's include them all
util.tgt: util/*.java
	@echo
	@echo COMPILING UTIL PACKAGE
	$(JAVAC) util/*.java
	touch util.tgt

# remove binaries and .tgt placeholders
clean:
	@echo
	@echo REMOVING BINARIES AND INTERMEDIATE FILES
	-rm -rf com
	-rm -f tools.jar
	-rm -f *.tgt