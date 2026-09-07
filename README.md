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

## Semana 1 — pedido do professor

- Lista de RF e RNF
- Priorização desses requisitos
- Proposta de MVP
- Projeto refatorado (pacotes + componentes lógico/físico)
- Outros artefatos necessários (UCs, classes, sequência, etc.)

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
