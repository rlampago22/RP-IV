$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$saida = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $saida | Out-Null
$fontes = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java'

& javac -encoding UTF-8 -d $saida $fontes.FullName
if ($LASTEXITCODE -ne 0) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show('Falha ao compilar o MVP. Confirme a instalação do Java JDK.', 'MVP Usina Nuclear')
    exit 1
}

Start-Process -FilePath 'javaw.exe' -ArgumentList @('-cp', $saida, 'br.edu.unipampa.usina.app.SistemaMvpUI') -WorkingDirectory $PSScriptRoot
