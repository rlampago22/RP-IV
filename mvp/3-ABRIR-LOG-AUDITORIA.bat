@echo off
chcp 65001 >nul
cd /d "%~dp0"
rem A trilha de auditoria fica no banco (tabela nucleo.auditoria_evento). Este atalho exporta uma
rem copia em texto para dados/auditoria-exportada.txt e abre no Bloco de Notas.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0executar-mvp.ps1" -ExportarAuditoria
if errorlevel 1 pause
