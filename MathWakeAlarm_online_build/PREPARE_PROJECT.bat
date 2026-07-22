@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not exist local.properties if exist "%LOCALAPPDATA%\Android\Sdk" (
  set "SDK_PATH=%LOCALAPPDATA:\=/%/Android/Sdk"
  > local.properties echo sdk.dir=%SDK_PATH%
)

set "GENERATOR=%~dp0.wrapper-generator"
if exist "%GENERATOR%" rmdir /s /q "%GENERATOR%"
mkdir "%GENERATOR%"
> "%GENERATOR%\settings.gradle" echo rootProject.name = 'wrapper-generator'
(
  echo tasks.wrapper {
  echo     gradleVersion = '8.13'
  echo     distributionType = Wrapper.DistributionType.BIN
  echo }
) > "%GENERATOR%\build.gradle"

echo Preparing the standard Gradle wrapper...
call gradlew.bat -p "%GENERATOR%" wrapper
if errorlevel 1 (
  echo Could not prepare the wrapper. Check your internet connection.
  pause
  exit /b 1
)

copy /y "%GENERATOR%\gradlew" "%~dp0gradlew" >nul
copy /y "%GENERATOR%\gradlew.bat" "%~dp0gradlew.bat" >nul
if not exist "%~dp0gradle\wrapper" mkdir "%~dp0gradle\wrapper"
copy /y "%GENERATOR%\gradle\wrapper\gradle-wrapper.jar" "%~dp0gradle\wrapper\gradle-wrapper.jar" >nul
copy /y "%GENERATOR%\gradle\wrapper\gradle-wrapper.properties" "%~dp0gradle\wrapper\gradle-wrapper.properties" >nul
rmdir /s /q "%GENERATOR%"

echo.
echo Project prepared. You can now open this folder in Android Studio.
pause
