@echo off
setlocal EnableDelayedExpansion

set WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_PROPERTIES%" (
  echo [ERROR] Missing %WRAPPER_PROPERTIES%
  exit /b 1
)

if not exist "%WRAPPER_JAR%" (
  set WRAPPER_URL=
  for /f "usebackq tokens=1,* delims==" %%A in ("%WRAPPER_PROPERTIES%") do (
    if /I "%%A"=="wrapperUrl" set WRAPPER_URL=%%B
  )
  if "!WRAPPER_URL!"=="" set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

  if "!WRAPPER_URL!"=="" (
    echo [ERROR] wrapperUrl not found in maven-wrapper.properties
    exit /b 1
  )

  echo Downloading Maven Wrapper from !WRAPPER_URL!
  powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '!WRAPPER_URL!' -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 (
    echo [ERROR] Failed to download maven-wrapper.jar
    exit /b 1
  )
)

set MAVEN_PROJECTBASEDIR=%~dp0
java -classpath "%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
