@echo off
rem $Id: odlnile.bat,v 1.1 1998/11/18 10:05:03 jim Exp $
rem Compile all ODL files on a dos box
rem James Fryer, 18 Nov 98
rem Copyright (c) 1998 Ogalala Ltd. <www.ogalala.com>

set ODL_BASE=d:\work\ogalala\java

REM Build MUA scripts
echo BUILDING MUA SCRIPTS
set ODL_OUT1=%ODL_BASE%\mua\scripts\compiled
md %ODL_OUT1%
echo Y | del %ODL_OUT1% > nul
cd %ODL_BASE%\mua\odl
call odl *.odl
for %%k in (*.atom *.exit *.thing) do move %%k %ODL_OUT1%

REM Build Nile scripts
echo BUILDING NILE SCRIPTS
set ODL_OUT2=%ODL_BASE%\apps\nileServer\scripts\compiled
md %ODL_OUT2%
echo Y | del %ODL_OUT2% > nul
cd %ODL_BASE%\apps\nileServer\odlcore
call odl *.odl
for %%k in (*.atom *.exit *.thing) do move %%k %ODL_OUT2%
cd %ODL_BASE%\apps\nileServer\odl
call odl *.odl
for %%k in (*.atom *.exit *.thing) do move %%k %ODL_OUT2%

REM Use dependency compiler to create WORLD script
echo BUILDING DEPENDENCY SCRIPT
set ODL_DEP=%ODL_BASE%\apps\nileServer\scripts\world
rem call dep -o %ODL_DEP% %ODL_BASE%\mua\scripts\*.* %ODL_BASE%\apps\nileServer\scripts\*.* %ODL_OUT1%\*.* %ODL_OUT2%\*.*
call dep -o %ODL_DEP% %ODL_OUT1%\*.* %ODL_OUT2%\*.*

REM Clean up
:exit
cd %ODL_BASE%
set ODL_BASE=
set ODL_OUT1=
set ODL_OUT2=
set ODL_DEP=

