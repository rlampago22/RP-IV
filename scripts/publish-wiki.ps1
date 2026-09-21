# Publica docs/wiki → GitHub Wiki (RP-IV.wiki.git)
# Pré-requisito: a aba Wiki precisa ter a 1ª página criada no GitHub
# (qualquer colaborador com write: Wiki → Create the first page → Save).

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "docs\wiki"
$tmp = Join-Path $env:TEMP "rp-iv-wiki-publish"

if (-not (Test-Path $src)) { throw "Pasta docs/wiki não encontrada." }

if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }

Write-Host "Clonando wiki..."
git clone https://github.com/rlampago22/RP-IV.wiki.git $tmp
if ($LASTEXITCODE -ne 0) {
  Write-Host ""
  Write-Host "Falha: wiki ainda não inicializada."
  Write-Host "1) Abra https://github.com/rlampago22/RP-IV/wiki"
  Write-Host "2) Clique em Create the first page, título Home, salve"
  Write-Host "3) Rode este script de novo"
  exit 1
}

Get-ChildItem $src -Filter *.md | ForEach-Object {
  Copy-Item $_.FullName (Join-Path $tmp $_.Name) -Force
}

Set-Location $tmp
git add -A
$status = git status --porcelain
if (-not $status) {
  Write-Host "Wiki já está atualizada."
  exit 0
}

git commit -m "docs: sincronizar wiki a partir de docs/wiki"
git push
Write-Host "Wiki publicada: https://github.com/rlampago22/RP-IV/wiki"
