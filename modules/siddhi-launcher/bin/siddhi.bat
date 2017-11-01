@echo off

REM ---------------------------------------------------------------------------
REM   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
REM
REM   Licensed under the Apache License, Version 2.0 (the "License");
REM   you may not use this file except in compliance with the License.
REM   You may obtain a copy of the License at
REM
REM   http://www.apache.org/licenses/LICENSE-2.0
REM
REM   Unless required by applicable law or agreed to in writing, software
REM   distributed under the License is distributed on an "AS IS" BASIS,
REM   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM   See the License for the specific language governing permissions and
REM   limitations under the License.

rem ---------------------------------------------------------------------------
rem Main Script for Siddhi SDK
rem
rem Environment Variable Prerequisites
rem
rem   SIDDHI_HOME  Home of SIDDHI installation. If not set I will  try
rem                   to figure it out.
rem
rem   JAVA_HOME       Must point at your Java Development Kit installation.
rem
rem   JAVA_OPTS       (Optional) Java runtime options used when the commands
rem                   is executed.
rem ---------------------------------------------------------------------------

rem ----- if JAVA_HOME is not set we're not happy ------------------------------

:checkJava

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
goto checkServer

:noJavaHome
echo "You must set the JAVA_HOME variable before running Siddhi."
goto end

rem ----- set SIDDHI_HOME ----------------------------
:checkServer
rem %~sdp0 is expanded pathname of the current script under NT with spaces in the path removed
set SIDDHI_HOME=%~sdp0..
SET curDrive=%cd:~0,1%
SET siddhiDrive=%SIDDHI_HOME:~0,1%
if not "%curDrive%" == "%siddhiDrive%" %siddhiDrive%:

goto updateClasspath

:noServerHome
echo SIDDHI_HOME is set incorrectly or Siddhi could not be located. Please set SIDDHI_HOME.
goto end

rem ----- update classpath -----------------------------------------------------
:updateClasspath

setlocal EnableDelayedExpansion
set SIDDHI_CLASSPATH=
FOR %%C in ("%SIDDHI_HOME%\lib\*.jar") DO set SIDDHI_CLASSPATH=!SIDDHI_CLASSPATH!;
"%SIDDHI_HOME%\lib\%%~nC%%~xC"

set SIDDHI_HOME="%JAVA_HOME%\lib\tools.jar";%SIDDHI_CLASSPATH%;

FOR %%D in ("%SIDDHI_HOME%\lib\*.jar") DO set SIDDHI_CLASSPATH=!SIDDHI_CLASSPATH!;
"%SIDDHI_HOME%\lib\%%~nD%%~xD"

rem ----- Process the input command -------------------------------------------

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).

:setupArgs
if ""%1""=="""" goto doneStart

if ""%1""==""debug""    goto commandDebug
if ""%1""==""-debug""   goto commandDebug
if ""%1""==""--debug""  goto commandDebug

shift
goto setupArgs


rem ----- commandDebug ---------------------------------------------------------
:commandDebug
shift
set DEBUG_PORT=%1
if "%DEBUG_PORT%"=="" goto noDebugPort
if not "%JAVA_OPTS%"=="" echo Warning !!!. User specified JAVA_OPTS will be ignored, once you give the --debug option.
set JAVA_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%DEBUG_PORT%
echo Please start the remote debugging client to continue...
goto runServer

:noDebugPort
echo Please specify the debug port after the --debug option
goto end

:doneStart
if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal
goto runServer


rem ----------------- Execute The Requested Command ----------------------------

:runServer

set CMD=%*

rem ---------- Add jars to classpath ----------------

set SIDDHI_CLASSPATH=.\lib;%SIDDHI_CLASSPATH%

set JAVA_ENDORSED=".\bre\lib\bootstrap\endorsed";"%JAVA_HOME%\jre\lib\endorsed";"%JAVA_HOME%\lib\endorsed"

set CMD_LINE_ARGS=-Xbootclasspath/a:%SIDDHI_XBOOTCLASSPATH% -Xms256m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath="%SIDDHI_HOME%\heap-dump.hprof"  -Dcom.sun.management.jmxremote -classpath %SIDDHI_CLASSPATH%
%JAVA_OPTS% -Djava.endorsed.dirs=%JAVA_ENDORSED%  -Dsiddhi.home="%SIDDHI_HOME%"  -Djava
.command="%JAVA_HOME%\bin\java" -Djava.opts="%JAVA_OPTS%" -Djava.io.tmpdir="%SIDDHI_HOME%\tmp" -Denable
.nonblocking=false -Dfile.encoding=UTF8 -Djava.util.logging.config
.file="%SIDDHI_HOME%\conf\logging.properties"


:runJava
"%JAVA_HOME%\bin\java" %CMD_LINE_ARGS% org.wso2.siddhi.launcher.Main %CMD%
:end
goto endlocal

:endlocal

:END
