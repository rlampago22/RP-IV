param(
    [switch]$Testar,
    # Exporta a trilha de auditoria do banco para dados/auditoria-exportada.txt e abre no Bloco de Notas.
    [switch]$ExportarAuditoria
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw 'Java JDK não encontrado. Instale o JDK 17 ou superior e abra um novo terminal.'
}

$saidaCompilacao = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $saidaCompilacao | Out-Null
$fontes = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java'

Write-Host 'Compilando o MVP...' -ForegroundColor Cyan
& javac -encoding UTF-8 -d $saidaCompilacao $fontes.FullName
if ($LASTEXITCODE -ne 0) {
    throw 'Falha na compilação do MVP.'
}

. (Join-Path $PSScriptRoot 'dependencias.ps1')
$classpath = Get-ClasspathMvp $saidaCompilacao

if ($ExportarAuditoria) {
    $destino = Join-Path $PSScriptRoot 'dados/auditoria-exportada.txt'
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.ExportarAuditoriaMain $destino
    if ($LASTEXITCODE -eq 0) { Start-Process notepad.exe $destino }
} elseif ($Testar) {
    Write-Host 'Executando testes...' -ForegroundColor Cyan
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesMvp
    if ($LASTEXITCODE -ne 0) {
        throw 'A suite Must falhou.'
    }
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesApiAlarmes
    if ($LASTEXITCODE -ne 0) {
        throw 'A suite de alarmes via HTTP falhou.'
    }
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesAcesso
    if ($LASTEXITCODE -ne 0) {
        throw 'A suite de acesso (RF08) falhou.'
    }
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesHistorico
    if ($LASTEXITCODE -ne 0) {
        throw 'A suite do historico operacional falhou.'
    }
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesApiHistorico
    if ($LASTEXITCODE -ne 0) {
        throw 'A suite da consulta do historico falhou.'
    }
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesApiEstado
    if ($LASTEXITCODE -ne 0) {
        throw 'A suite da API de estado falhou.'
    }
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesReatores
    if ($LASTEXITCODE -ne 0) {
        throw 'A suite dos 5 reatores falhou.'
    }
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.TestesContingencia
} else {
    Write-Host 'Executando demonstração...' -ForegroundColor Cyan
    & java '-Dfile.encoding=UTF-8' -cp $classpath br.edu.unipampa.usina.app.DemoMvp
}

if ($LASTEXITCODE -ne 0) {
    throw 'A execução terminou com erro.'
}
