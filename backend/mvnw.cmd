@echo off
setlocal

set WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_PROPERTIES%" (
  echo [ERROR] Missing %WRAPPER_PROPERTIES%
  exit /b 1
)

if not exist "%WRAPPER_JAR%" (
  for /f "usebackq delims=" %%I in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$line = Get-Content '%WRAPPER_PROPERTIES%' | Where-Object { $_ -like 'wrapperUrl=*' } | Select-Object -First 1; if ($line) { $line.Substring('wrapperUrl='.Length) }"`) do set WRAPPER_URL=%%I

  if "%WRAPPER_URL%"=="" (
    echo [ERROR] wrapperUrl not found in maven-wrapper.properties
    exit /b 1
  )

  echo Downloading Maven Wrapper from %WRAPPER_URL%
  powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 (
    echo [ERROR] Failed to download maven-wrapper.jar
    exit /b 1
  )
)

set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%
java -classpath "%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR% org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
