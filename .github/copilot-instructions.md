# Instruções para GitHub Copilot / GPT

Este projeto é acadêmico (AL0343 — RP IV). Sistema: Controle de Usina Nuclear com arquitetura **EDA**.

## Antes de sugerir código ou docs

1. Considere `AGENTS.md` como fonte de verdade.
2. Trabalhe como se estivesse na branch `desenvolvimento` (ou `fix/*` para estrutura/docs).
3. Respeite MoSCoW / MVP e o hub [`docs/DOCUMENTACAO_DE_ENGENHARIA.md`](../docs/DOCUMENTACAO_DE_ENGENHARIA.md).
4. UI: **Opção A** — [`docs/ui/direcao-opcao-a.md`](../docs/ui/direcao-opcao-a.md).
5. Não sugira alterações na `main` além de consolidação final.

## Código canônico

- Java Must / demo: `mvp/`
- React: `frontend/`
- API futura: `backend/`
- Não use `src/` (esqueleto legado) para novas features

## Estilo de sugestão

- EventBus in-process no MVP.
- Explique padrões GoF quando aparecerem (Facade, Strategy, Factory, Observer/Pub-Sub).
- Conventional Commits.
- Não reescreva artefatos de outro membro sem pedido explícito.

## Não fazer

- Expandir Won't do MVP (evacuação completa, RH amplo, IA preditiva)
- Reintroduzir Opção B (painel claro) ou tema neon decorativo
- Incluir pasta `ai/` ou `.cursor/mcp.json` no versionamento
- Commit/push em repositórios de outros grupos (SafePlace, Sentinela = só leitura)
