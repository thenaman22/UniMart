@echo off
setlocal

set SCRIPT_DIR=%~dp0
set PROPS_FILE=%SCRIPT_DIR%gradle\wrapper\gradle-wrapper.properties

for /f "tokens=1,* delims==" %%A in (%PROPS_FILE%) do (
  if /I "%%A"=="distributionUrl" set DIST_URL=%%B
)

if "%DIST_URL%"=="" (
  echo Could not read distributionUrl from %PROPS_FILE%
  exit /b 1
)

set DIST_URL=%DIST_URL:\=%
for %%F in ("%DIST_URL%") do set DIST_FILE=%%~nxF
set DIST_NAME=%DIST_FILE:-bin.zip=%
set DIST_DIR=%SCRIPT_DIR%.gradle-wrapper\dists\%DIST_NAME%
set ZIP_PATH=%SCRIPT_DIR%.gradle-wrapper\dists\%DIST_FILE%
set GRADLE_CMD=%DIST_DIR%\bin\gradle.bat

if not exist "%GRADLE_CMD%" (
  echo Downloading %DIST_URL%
  if not exist "%SCRIPT_DIR%.gradle-wrapper\dists" mkdir "%SCRIPT_DIR%.gradle-wrapper\dists"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%';" ^
    "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%SCRIPT_DIR%.gradle-wrapper\dists' -Force"
  if errorlevel 1 (
    echo Failed to download or extract Gradle distribution.
    exit /b 1
  )
)

call "%GRADLE_CMD%" %*
endlocal
