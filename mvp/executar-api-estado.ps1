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

Write-Host ''
Write-Host 'Subindo API de estado em http://localhost:8080/api/estado ...' -ForegroundColor Cyan
Write-Host 'Deixe esta janela aberta e rode "cd frontend; npm run dev" em outro terminal.' -ForegroundColor Yellow
Write-Host 'Contrato JSON: docs/api-estado-contrato.md' -ForegroundColor DarkGray
Write-Host ''
& java '-Dfile.encoding=UTF-8' -cp $saidaCompilacao br.edu.unipampa.usina.app.ApiEstadoMain
