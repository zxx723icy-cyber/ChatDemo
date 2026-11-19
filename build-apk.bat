@echo off
setlocal
cd /d "%~dp0android"
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle not found. Install Android Studio and Gradle 8.9 or newer.
  exit /b 1
)
call gradle --no-daemon assembleDebug
if errorlevel 1 exit /b 1
if not exist ..\dist mkdir ..\dist
copy app\build\outputs\apk\debug\app-debug.apk ..\dist\ChatDemo-debug.apk
echo APK created at dist\ChatDemo-debug.apk
