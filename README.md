# RP-IV — Sistema de Controle de Usina Nuclear

Repositório da disciplina **AL0343 — Resolução de Problemas IV** (UNIPAMPA Alegrete).

## Equipe e papéis

| Integrante | Trilha |
|------------|--------|
| Álvaro Domingues | Requisitos, MoSCoW, aceite, checklist feedback |
| Bruno Rocha | Arquitetura EDA, EventBus, pacotes/componentes |
| Bernardo Dorneles | ControleReator, medições, persistência |
| José Guilherme Monteiro | Alarmes, limiares, padrões GoF |
| Marcus Querol | UCs/sequências, auditoria, demo, acesso (Should) |

## Como trabalhamos (importante)

Entregamos **semana a semana**, com evidência **individual** para a verificação parcial.

- **Plano operacional:** [`docs/plano-semanal-rp4.md`](docs/plano-semanal-rp4.md)
- **Status da semana:** [`docs/semanas/STATUS.md`](docs/semanas/STATUS.md)
- **Semana atual (0):** [`docs/semanas/2026-09-07/`](docs/semanas/2026-09-07/)

### Sobre `docs/marco1/`

O conteúdo em [`docs/marco1/`](docs/marco1/) (incluindo PDF/HTML) é **rascunho / draft**.  
Serve de base para validar e corrigir até o **Marco 1 oficial em 05/10/2026**. Não tratar como entrega fechada: cada aluno adota a parte da sua trilha nas Semanas 0–4.

## Sistema

Monitoramento e controle lógico de usina nuclear: medições de reatores, alarmes, auditoria de eventos; depois acesso restrito e (se couber) contingência.

## Arquitetura

Mantida do legado APS:

- Arquitetura Orientada a Eventos (EDA)
- Módulos independentes
- Persistência dedicada por módulo

## Escopo do MVP

**Must:** medições + histórico + alarmes por limiar + persistência mínima + auditoria  
**Should:** controle de acesso (`RegistroAcesso`)  
**Could:** protocolo de contingência  
**Won't:** evacuação completa, RH, conformidade ampla, IA preditiva

## Stack

- Java (pacotes por módulo)
- Barramento de eventos in-process no MVP
- Persistência simples (arquivo/H2/SQLite) conforme implementação semanal

## Estrutura

```text
docs/plano-semanal-rp4.md   # calendário + papéis + template
docs/semanas/               # entregas individuais por semana
docs/marco1/                # RASCUNHO até 05/10
src/main/java/              # esqueleto → implementação semana a semana
```

## Marcos

| Marco | Data | Foco |
|-------|------|------|
| 1 | 05/10/2026 | Docs validados + demo parcial Must |
| 2 | 16/11/2026 | Núcleo + GoF + Should parcial |
| 3 | 16–17/12/2026 | Demo final |

## Licença acadêmica

Material para avaliação na UNIPAMPA — uso interno do grupo e da disciplina.
