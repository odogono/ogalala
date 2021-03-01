@echo off
cd c:\design
echo *** You will have to press RETURN to complete the file transfer. ***
ftp claremont < update_tools.ftp_script
pkzip25 -extract -overwrite=all -dir build.zip

