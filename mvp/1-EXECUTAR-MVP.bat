@echo off
chcp 65001 >nul
title MVP - Controle de Usina Nuclear
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0executar-mvp.ps1"
echo.
echo Pressione qualquer tecla para fechar...
pause >nul
