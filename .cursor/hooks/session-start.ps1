# Injeta contexto do RP-IV no início da sessão do agente.
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$agents = Join-Path $root 'AGENTS.md'
$readme = Join-Path $root 'README.md'
$plano = Join-Path $root 'docs\plano-semanal-rp4.md'

$parts = @()
$parts += 'CONTEXTO RP-IV: leia AGENTS.md antes de editar. Branch de trabalho = desenvolvimento. Main limpa. Nao alterar repos de outros grupos.'
$parts += 'Semana 1 (professor): RF/RNF, priorizacao MoSCoW, MVP, diagramas de pacotes e componentes, artefatos extras.'
$parts += 'Marco 1 oficial: 05/10/2026. docs/marco1 e rascunho.'

if (Test-Path $agents) {
  $parts += 'Arquivos-chave: AGENTS.md, README.md, docs/plano-semanal-rp4.md, docs/requisitos/, docs/mvp/, docs/diagramas/.'
}

$context = ($parts -join ' ')
$payload = @{ additional_context = $context } | ConvertTo-Json -Compress
[Console]::Out.Write($payload)
exit 0
