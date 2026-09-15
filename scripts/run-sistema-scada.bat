@echo off
setlocal
cd /d "%~dp0.."

set "OUT=out"
if not exist "%OUT%" mkdir "%OUT%"

echo Pasta: %CD%
echo Compilando Central de Supervisao SCADA...
javac -encoding UTF-8 -d "%OUT%" ^
  src\main\java\br\edu\unipampa\usina\infraestruturaeventos\*.java ^
  src\main\java\br\edu\unipampa\usina\auditorialogs\*.java ^
  src\main\java\br\edu\unipampa\usina\alarmes\*.java ^
  src\main\java\br\edu\unipampa\usina\controlereator\*.java ^
  src\main\java\br\edu\unipampa\usina\app\*.java

if errorlevel 1 (
  echo ERRO na compilacao da interface.
  exit /b 1
)

echo.
echo Iniciando Central Digital de Supervisao SCADA (Reator Nuclear 01)...
start "" javaw -cp "%OUT%" br.edu.unipampa.usina.app.SistemaMvpUI
