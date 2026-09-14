@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0.."

set "OUT=out"
if not exist "%OUT%" mkdir "%OUT%"

set "SRC=src\main\java"

set "SOURCES="
for /r "%SRC%" %%f in (*.java) do set "SOURCES=!SOURCES! "%%f""

echo Pasta: %CD%
echo Compilando...
javac -encoding UTF-8 -d "%OUT%" ^
  !SOURCES!

if errorlevel 1 (
  echo.
  echo ERRO na compilacao.
  echo Se "javac" nao for reconhecido, abra um NOVO terminal apos instalar o Java.
  java -version
  javac -version
  exit /b 1
)

echo.
echo Rodando DemoMedicoes...
echo.
java -cp "%OUT%" br.edu.unipampa.usina.controlereator.DemoMedicoes
