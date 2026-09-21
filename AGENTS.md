# AGENTS.md — Contexto obrigatório para qualquer IA

## Leia isto antes de alterar o repositório

Disciplina **AL0343 — Resolução de Problemas IV** (UNIPAMPA Alegrete).  
Sistema: **Controle de Usina Nuclear** (APS → RP IV).  
Arquitetura **única:** **EDA** (EventBus).

| Branch | Uso |
|--------|-----|
| `desenvolvimento` | Integração |
| `main` | Limpa — só consolidação final |
| `alvaro` `bernardo` `bruno` `jose` `marcus` | Individuais |
| `fix/*` | Ajustes estruturais/docs |

**Stack real:**
- Domínio Java executável: **`mvp/`**
- UI oficial: **`frontend/`** (React) — visual **Opção A** SCADA escuro
- API futura: **`backend/`** (vazio/migração)
- `src/` = esqueleto legado — **não** é a demo

Não faça commits na `main`. Não altere repositórios de terceiros.

## Equipe

| Integrante | Branch | Trilha |
|------------|--------|--------|
| Álvaro Domingues | `alvaro` | Requisitos, aceite, UX Opção A |
| Bruno Rocha | `bruno` | EventBus, API estado, T01 |
| Bernardo Dorneles | `bernardo` | Medições, T02 |
| José Guilherme Monteiro | `jose` | Alarmes/GoF, T03 |
| Marcus Querol | `marcus` | Auditoria/UCs, T04–T05 |

## Diretrizes

1. Ler `README.md`, este arquivo, `docs/PLANEJAMENTO_DESENVOLVIMENTO.md` e `docs/ui/direcao-opcao-a.md`.
2. UI: seguir protótipo `docs/ui/propostas/opcao-a.html` (Opção B descartada).
3. `docs/marco1/` é rascunho até **05/10/2026**.
4. Evidência individual em `docs/semanas/`.
5. Não inventar requisitos fora do MoSCoW.
6. Preferir mudanças pequenas; explicar o *porquê*.

## Estrutura relevante

```text
README.md
CONTRIBUTING.md
docs/DOCUMENTACAO_DE_ENGENHARIA.md
docs/PLANEJAMENTO_DESENVOLVIMENTO.md
docs/ui/direcao-opcao-a.md
docs/implementacao/00-INDICE.md
mvp/                    # Java Must
frontend/               # React Opção A
backend/                # destino API
```

## Restrições

- Pasta `ai/` local — não versionar.
- Sem force push na `main`.
- Conventional Commits (`feat:`, `fix:`, `docs:`, …).

## Arquivos por ferramenta

| Ferramenta | Arquivo |
|------------|---------|
| Qualquer / Cursor | `AGENTS.md` + `.cursor/rules/` |
| Claude | `CLAUDE.md` |
| Gemini | `GEMINI.md` |
| Copilot | `.github/copilot-instructions.md` |
