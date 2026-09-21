@echo off
cd /d "%~dp0"
if not exist "dados\auditoria.log" (
  echo Execute primeiro o arquivo 1-EXECUTAR-MVP.bat.
  pause
  exit /b 1
)
start "" notepad.exe "dados\auditoria.log"
