@echo off
REM $Id: odl_checkin.bat,v 1.2 1998/11/12 14:39:24 steve Exp $
REM Check in ODL files for use with ODL Designer
REM James Fryer 12 November 1998
REM Copyright (C) 1998 Ogalala Ltd (www.ogalala.com)

if %1.==. goto usage


REM Add the file just in case, update it then check it in
cd odl
cvs add %1
cvs up %1
cvs ci %1
cd ..

goto exit

:usage
echo Usage: ODL_CHECKIN filename

:exit
