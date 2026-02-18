@echo off
:: setup.bat — Bootstrap script for Etendo (Windows)
::
:: Flow:
::   1. Require JAVA_HOME
::   2. If githubToken missing -> GitHub Device Flow auth; abort on failure
::   3. Launch gradlew.bat setup.web (always — Gradle task handles resumption)
::
:: State file: .setup-progress
::   Tracks completed phases so re-runs can continue from where they stopped.
::   Phases: auth | setup.web
::   Delete .setup-progress to start over.
::
:: Usage:
::   setup.bat               auto-detect last step and continue
::   setup.bat --fresh       reset progress and start from scratch
::   setup.bat <task>        run a specific gradle task directly

setlocal enabledelayedexpansion

set "STATE_FILE=.setup-progress"
set "PROPS_FILE=gradle.properties"

:: ── --fresh: reset state ──────────────────────────────────────────────────────
if /i "%~1"=="--fresh" (
    if exist "%STATE_FILE%" del /f "%STATE_FILE%"
    powershell -Command "(Get-Content '%PROPS_FILE%') -replace '^githubToken=.*','githubToken=' | Set-Content '%PROPS_FILE%'" 2>nul
    echo Progress reset. Starting fresh.
    call "%~f0"
    exit /b %ERRORLEVEL%
)

set "TASK=%~1"
if "%TASK%"=="" set "TASK=setup.web"

:: ── Helpers ───────────────────────────────────────────────────────────────────
:: phase_done: check if phase is marked done in state file
:: mark_done:  append phase=done to state file if not already there

:: ── 1. Require JAVA_HOME ──────────────────────────────────────────────────────
if not defined JAVA_HOME (
    echo.
    echo ERROR: JAVA_HOME is not set.
    echo   Please set JAVA_HOME to a Java 17+ installation and re-run.
    echo   Example: set JAVA_HOME=C:\Program Files\Java\jdk-17
    echo.
    exit /b 1
)

set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA_CMD%" (
    echo.
    echo ERROR: Java binary not found at: %JAVA_CMD%
    echo   Check that JAVA_HOME points to a valid Java installation.
    echo.
    exit /b 1
)

:: ── 2. Auth phase ─────────────────────────────────────────────────────────────
set "EXISTING="
for /f "tokens=1* delims==" %%A in ('findstr /r /c:"^githubToken=." "%PROPS_FILE%" 2^>nul') do (
    set "EXISTING=%%B"
)

if "!EXISTING!"=="" (
    echo Starting GitHub authentication UI...
    "%JAVA_CMD%" gradle\setup.java
    if !ERRORLEVEL! neq 0 (
        echo.
        echo ERROR: GitHub authentication failed. Setup aborted.
        echo.
        exit /b 1
    )
)
findstr /c:"auth=done" "%STATE_FILE%" >nul 2>&1 || echo auth=done>>"%STATE_FILE%"

:: ── 3. Gradle phase ───────────────────────────────────────────────────────────
:: Always launch Gradle — it handles incremental execution internally.
:: If setup.web previously failed (e.g. at Tomcat), re-running this script
:: skips auth (token already saved) and re-enters setup.web from the last
:: checkpoint tracked by the Gradle task itself.
call gradlew.bat %TASK%
exit /b %ERRORLEVEL%
