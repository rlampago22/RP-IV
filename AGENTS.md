# AGENTS.md — Contexto obrigatório para qualquer IA

## Leia isto antes de alterar o repositório

Este repositório é da disciplina **AL0343 — Resolução de Problemas IV** (UNIPAMPA Alegrete).
O sistema é o **Controle de Usina Nuclear**, legado da APS, com arquitetura **EDA** (eventos).

**Branch de trabalho:** `desenvolvimento`  
**Branch limpa:** `main` (só consolidação final)  
**Branches individuais:** `alvaro`, `bernardo`, `bruno`, `jose`, `marcus` (baseadas em `desenvolvimento`)

Não faça commits diretos na `main`. Não altere o repositório de referência de outros grupos.

## Equipe

| Integrante | Branch | Trilha |
|------------|--------|--------|
| Álvaro Domingues | `alvaro` | Requisitos, MoSCoW, aceite, checklist feedback |
| Bruno Rocha | `bruno` | Arquitetura EDA, EventBus, pacotes/componentes |
| Bernardo Dorneles | `bernardo` | ControleReator, medições, persistência |
| José Guilherme Monteiro | `jose` | Alarmes, limiares, padrões GoF |
| Marcus Querol | `marcus` | UCs/sequências, auditoria, demo, acesso (Should) |

## Objetivo do apoio por IA

Aprendizado do grupo. Sugestões devem explicar o *porquê*, alinhar requisitos ↔ UML ↔ código ↔ padrões, e preservar autoria dos membros.

## Diretrizes

1. Antes de editar: ler `README.md`, este arquivo e `docs/plano-semanal-rp4.md`.
2. Entrega da **Semana 1** (pedido do professor): RF/RNF, priorização, MVP, projeto refatorado (pacotes + componentes), artefatos extras necessários.
3. `docs/marco1/` é **rascunho** até o Marco 1 oficial (**05/10/2026**).
4. Preferir mudanças pequenas e rastreáveis; evidência individual em `docs/semanas/`.
5. Não inventar requisitos fora do MoSCoW acordado.
6. Em dúvida: registrar a dúvida; não inventar contexto.

## Preservação das contribuições

- Não reescrever ideias de outro membro sem pedido explícito.
- Preferir organização/formatação; mudanças de conteúdo só quando solicitadas.
- Inconsistências: apontar e sugerir, sem sobrescrever silenciosamente.

## Estrutura relevante

```text
docs/plano-semanal-rp4.md   # calendário e papéis
docs/semanas/               # evidências individuais
docs/marco1/                # rascunho Marco 1
docs/requisitos/            # RF/RNF e priorização
docs/mvp/                   # proposta de MVP
docs/arquitetura/           # EDA e decisões
docs/diagramas/             # UML (pacotes, componentes, etc.)
src/main/java/.../usina/    # esqueleto modular EDA
```

## Restrições

- Pasta `ai/` é local e **não** deve ir para o Git.
- Não fazer force push na `main`.
- Não modificar repositórios de outros grupos (apenas referência de estilo).
- Conventional Commits nos commits (`feat:`, `fix:`, `docs:`, `chore:`, etc.).

## Arquivos por ferramenta

| Ferramenta | Arquivo |
|------------|---------|
| Qualquer agente / Cursor | `AGENTS.md` + `.cursor/rules/` |
| Claude | `CLAUDE.md` |
| Gemini | `GEMINI.md` |
| GitHub Copilot / GPT | `.github/copilot-instructions.md` |
