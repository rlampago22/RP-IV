# Instruções para GitHub Copilot / GPT

Este projeto é acadêmico (AL0343 — RP IV). Sistema: Controle de Usina Nuclear com arquitetura EDA.

## Antes de sugerir código ou docs

1. Considere `AGENTS.md` como fonte de verdade.
2. Trabalhe como se estivesse na branch `desenvolvimento`.
3. Respeite o MoSCoW e o MVP em `docs/mvp/` e `docs/requisitos/`.
4. Não sugira alterações na `main` além de consolidação final.

## Foco até Marco 1 (05/10/2026)

- Requisitos funcionais e não funcionais
- Priorização MoSCoW
- Proposta de MVP
- Diagramas de pacotes e componentes refatorados
- Artefatos alinhados (casos de uso, classes, sequência do núcleo)

## Estilo de sugestão

- Prefira Java simples por módulo (`src/main/java/.../usina/`).
- EventBus in-process no MVP.
- Explique padrões GoF quando aparecerem (Facade, Strategy, Factory, Observer/Pub-Sub).
- Conventional Commits.
- Não reescreva artefatos de outro membro sem pedido explícito.

## Não fazer

- Expandir para evacuação completa, RH amplo, IA preditiva (Won't do MVP)
- Incluir pasta `ai/` no versionamento
- Imitar commits/push em repositórios de outros grupos
