# CLAUDE.md — Instruções para Claude

Antes de qualquer mudança neste repositório, leia nesta ordem:

1. `AGENTS.md`
2. `README.md`
3. `docs/plano-semanal-rp4.md`
4. O artefato da trilha relacionada à tarefa (em `docs/marco1/`, `docs/requisitos/`, `docs/mvp/` ou `docs/diagramas/`)

## Contexto

- Disciplina: AL0343 — Resolução de Problemas IV
- Sistema: Controle de Usina Nuclear (APS → RP IV)
- Arquitetura: EDA (módulos + EventBus + persistência dedicada)
- Trabalho ativo na branch `desenvolvimento` (nunca na `main` limpa)
- Marco 1: 05/10/2026

## Semana 1 (pedido do professor)

Entregar/refinar:

- Lista de RF e RNF
- Priorização (MoSCoW)
- Proposta de MVP
- Projeto refatorado: diagrama de pacotes + componentes (lógico e físico)
- Outros artefatos se necessário (UCs, classes, sequência)

## Como trabalhar

- Explicar decisões (padrão, trade-off, impacto no MVP).
- Manter coerência requisitos ↔ diagramas ↔ pacotes Java.
- Não expandir escopo além do Must/Should acordado.
- Preservar autoria; sugerir correções em vez de reescrever silenciosamente.
- Commits no estilo Conventional Commits.

## O que evitar

- Alterar `main` com código de desenvolvimento
- Inventar módulos Won't do MoSCoW
- Copiar/alterar repositórios de outros grupos
- Enviar pasta `ai/` ao Git
