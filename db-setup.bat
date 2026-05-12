@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0db-setup.ps1"
exit /b %ERRORLEVEL%
