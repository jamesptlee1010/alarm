@echo off
setlocal
cd /d "%~dp0"

if not exist local.properties if exist "%LOCALAPPDATA%\Android\Sdk" (
  set "SDK_PATH=%LOCALAPPDATA:\=/%/Android/Sdk"
  > local.properties echo sdk.dir=%SDK_PATH%
)

echo Building Math Wake Alarm debug APK...
call gradlew.bat clean test assembleDebug
if errorlevel 1 (
  echo.
  echo Build failed. Open the folder in Android Studio to see the full error and confirm the Android SDK is installed.
  pause
  exit /b 1
)

echo.
echo Build complete:
echo %~dp0app\build\outputs\apk\debug\app-debug.apk
explorer "%~dp0app\build\outputs\apk\debug"
pause
