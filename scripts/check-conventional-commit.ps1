param(
  [Parameter(Mandatory = $true)]
  [string]$CommitMsgFile
)

$ErrorActionPreference = 'Stop'
$msg = Get-Content -Raw -Path $CommitMsgFile
$first = ($msg -split "`n")[0].Trim()

if ($first -match '^Merge ') { exit 0 }
if ($first -match '^(feat|fix|docs|chore|refactor|test|style|ci|build|perf)(\(.+\))?: .+') { exit 0 }

Write-Host "Mensagem de commit invalida para Conventional Commits:"
Write-Host "  $first"
Write-Host "Use: tipo(escopo opcional): descricao"
Write-Host "Tipos: feat, fix, docs, chore, refactor, test, style, ci, build, perf"
exit 1
