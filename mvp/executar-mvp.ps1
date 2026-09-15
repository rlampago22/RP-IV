param(
    [switch]$Testar
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

if ($Testar) {
    Write-Host 'Executando testes...' -ForegroundColor Cyan
    & java '-Dfile.encoding=UTF-8' -cp $saidaCompilacao br.edu.unipampa.usina.app.TestesMvp
} else {
    Write-Host 'Executando demonstração...' -ForegroundColor Cyan
    & java '-Dfile.encoding=UTF-8' -cp $saidaCompilacao br.edu.unipampa.usina.app.DemoMvp
}

if ($LASTEXITCODE -ne 0) {
    throw 'A execução terminou com erro.'
}
