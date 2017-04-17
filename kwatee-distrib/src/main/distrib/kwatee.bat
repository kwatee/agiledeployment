@Echo Off
REM If JAVA_HOME is not defined in your environment, define it below
REM SET JAVA_HOME=

if NOT [%JAVA_HOME%] == [] goto checkJava
Echo "JAVA_HOME is undefined"
Exit /B 1

:checkJava 
if exist "%JAVA_HOME%\bin\java.exe goto okJava
Echo "%JAVA_HOME%\bin\java.exe NOT FOUND"
Exit /B 1

:okJava
# Absolute path to this script
Set KWATEE_HOME=%~dp0
cd %KWATEE_HOME% && %JAVA_HOME%\bin\java.exe -jar kwatee-${project.version}.jar --spring.config.name=kwatee
