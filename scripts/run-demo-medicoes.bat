@echo off
setlocal
cd /d "%~dp0.."

set "OUT=out"
if not exist "%OUT%" mkdir "%OUT%"

set "SRC=src\main\java\br\edu\unipampa\usina\controlereator"
set "INFRA=src\main\java\br\edu\unipampa\usina\infraestruturaeventos"

echo Pasta: %CD%
echo Compilando...
javac -encoding UTF-8 -d "%OUT%" ^
  "%INFRA%\*.java" ^
  "%SRC%\*.java"

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
