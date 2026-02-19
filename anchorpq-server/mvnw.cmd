@REM ----------------------------------------------------------------------------
@REM Maven Wrapper script for Windows
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

@REM Download maven-wrapper.jar if it doesn't exist
if not exist %WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    powershell -Command "(New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"
)

@REM Determine Java command
if defined JAVA_HOME (
    set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_CMD=java
)

@REM Run Maven Wrapper
%JAVA_CMD% %MAVEN_OPTS% -classpath %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %*

endlocal

