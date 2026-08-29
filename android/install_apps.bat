@echo off
set ADB="C:\Users\mddaw\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if not exist %ADB% (
    set ADB=adb
)

echo ========================================================
echo   Speedo Ride-Hailing Platform - Multi-App Installer
echo ========================================================
echo.

echo Checking connected Android devices...
%ADB% devices
echo.

echo 1. Installing Speedo Rider App (com.speedo.rider)...
%ADB% install -r rider-app\build\outputs\apk\debug\rider-app-debug.apk
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Rider App installed successfully!
) else (
    echo [ERROR] Failed to install Rider App. Please ensure device is connected.
)
echo.

echo 2. Installing Speedo Captain App (com.speedo.captain)...
%ADB% install -r captain-app\build\outputs\apk\debug\captain-app-debug.apk
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Captain App installed successfully!
) else (
    echo [ERROR] Failed to install Captain App. Please ensure device is connected.
)
echo.

echo 3. Installing Speedo Admin App (com.speedo.admin)...
%ADB% install -r admin-app\build\outputs\apk\debug\admin-app-debug.apk
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Admin App installed successfully!
) else (
    echo [ERROR] Failed to install Admin App. Please ensure device is connected.
)
echo.

echo ========================================================
echo Installation Process Complete!
echo ========================================================
pause
