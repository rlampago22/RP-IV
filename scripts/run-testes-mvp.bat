@echo off
setlocal
cd /d "%~dp0.."

set "OUT=out"
if not exist "%OUT%" mkdir "%OUT%"

echo Pasta: %CD%
echo Compilando suite de testes e modulos do MVP...
javac -encoding UTF-8 -d "%OUT%" ^
  src\main\java\br\edu\unipampa\usina\infraestruturaeventos\*.java ^
  src\main\java\br\edu\unipampa\usina\auditorialogs\*.java ^
  src\main\java\br\edu\unipampa\usina\alarmes\*.java ^
  src\main\java\br\edu\unipampa\usina\controlereator\*.java ^
  src\main\java\br\edu\unipampa\usina\app\*.java

if errorlevel 1 (
  echo ERRO na compilacao dos testes.
  exit /b 1
)

echo.
echo Executando suite formal de testes do checklist de aceite (Must)...
echo.
java -cp "%OUT%" br.edu.unipampa.usina.app.TestesMvp
