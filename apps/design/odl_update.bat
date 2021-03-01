@echo off
REM $Id: odl_update.bat,v 1.2 1998/11/12 14:39:24 steve Exp $
REM Update ODL files for use with ODL Designer
REM James Fryer 12 November 1998
REM Copyright (C) 1998 Ogalala Ltd (www.ogalala.com)

cd odl
cvs up -dP
cd ..
