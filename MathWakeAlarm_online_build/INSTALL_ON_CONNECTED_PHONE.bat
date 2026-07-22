@echo off
setlocal
cd /d "%~dp0"
set "APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"

if not exist "%APK%" (
  echo APK not found. Run BUILD_DEBUG_APK.bat first.
  pause
  exit /b 1
)

where adb >nul 2>&1
if errorlevel 1 (
  if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else (
  set "ADB=adb"
)

if not defined ADB (
  echo adb was not found. Install Android Studio platform-tools or use Android Studio's Run button.
  pause
  exit /b 1
)

"%ADB%" install -r "%APK%"
if errorlevel 1 (
  echo Installation failed. Confirm USB debugging is enabled and approve the phone prompt.
  pause
  exit /b 1
)

echo Math Wake Alarm installed successfully.
pause
