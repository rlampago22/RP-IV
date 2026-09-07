# RP-IV — Sistema de Controle de Usina Nuclear

Repositório da disciplina **AL0343 — Resolução de Problemas IV** (UNIPAMPA Alegrete).

> **Importante:** o desenvolvimento acontece na branch [`desenvolvimento`](https://github.com/rlampago22/RP-IV/tree/desenvolvimento).  
> A `main` permanece limpa até a consolidação final.

## Equipe

| Integrante | Branch | Trilha |
|------------|--------|--------|
| Álvaro Domingues | `alvaro` | Requisitos, MoSCoW, aceite, checklist |
| Bruno Rocha | `bruno` | EDA, EventBus, pacotes/componentes |
| Bernardo Dorneles | `bernardo` | ControleReator, medições, persistência |
| José Guilherme Monteiro | `jose` | Alarmes, limiares, GoF |
| Marcus Querol | `marcus` | UCs/sequências, auditoria, demo, acesso |

## Como trabalhar

1. Atualize `desenvolvimento`
2. Trabalhe na sua branch pessoal (`alvaro`, `bernardo`, `bruno`, `jose`, `marcus`)
3. Abra PR para `desenvolvimento`
4. Deixe evidência em `docs/semanas/YYYY-MM-DD/<seu-nome>.md`

Leia também: [`AGENTS.md`](AGENTS.md) (obrigatório para qualquer IA).

## Plano até Marco 1

Documento vigente: [`docs/plano-marco1.md`](docs/plano-marco1.md)

| Entrega | Data |
|---------|------|
| Semana 1 (pedido do professor) | **08/09/2026** |
| Semana 2 | 15/09/2026 |
| Semana 3 | 22/09/2026 |
| Semana 4 (ensaio) | 29/09/2026 |
| **Marco 1** | **05/10/2026** |

Padrão: **1 issue por pessoa por semana** → branch pessoal → PR para `desenvolvimento`.

## Semana 1 — pedido do professor (todos uma parte)

- Álvaro (#11): RF/RNF + MoSCoW + MVP
- Bruno (#12): pacotes + componentes lógico/físico
- Bernardo (#13): classes do núcleo
- José (#14): GoF alarmes
- Marcus (#15): UCs + sequência

## Sistema e arquitetura

Monitoramento lógico de usina nuclear: medições, alarmes, auditoria; depois acesso restrito.

- Arquitetura Orientada a Eventos (EDA)
- Módulos independentes
- Persistência dedicada por módulo

## Estrutura

```text
AGENTS.md / CLAUDE.md / GEMINI.md
.github/copilot-instructions.md
.cursor/rules/  .cursor/hooks/
.githooks/      scripts/
docs/requisitos  docs/mvp  docs/arquitetura  docs/diagramas
docs/marco1/     # rascunho até 05/10/2026
docs/semanas/    # evidências individuais
src/main/java/.../usina/
```

## Marcos

| Marco | Data | Foco |
|-------|------|------|
| 1 | 05/10/2026 | Docs validados + demo parcial Must |
| 2 | 16/11/2026 | Núcleo + GoF + Should parcial |
| 3 | 16–17/12/2026 | Demo final |

## Hooks Git (opcional local)

```bash
git config core.hooksPath .githooks
```

## Licença acadêmica

Material para avaliação na UNIPAMPA — uso interno do grupo e da disciplina.
