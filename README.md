# RP-IV — Sistema de Controle de Usina Nuclear

Repositório da disciplina **AL0343 — Resolução de Problemas IV** (UNIPAMPA Alegrete).

## Equipe

- Álvaro Domingues
- Bruno Rocha
- Bernardo Dorneles
- José Guilherme Monteiro
- Marcus Querol

## Sistema

Sistema de monitoramento e controle lógico de uma usina nuclear, com foco em medições de reatores, alarmes, auditoria de eventos e (em etapas posteriores) controle de acesso e protocolos de contingência.

## Arquitetura

Mantida do projeto de Análise e Projeto de Software:

- Arquitetura Orientada a Eventos (EDA)
- Módulos independentes
- Persistência dedicada por módulo

## Escopo do MVP

**Must (núcleo):**

1. Ingestão e histórico de medições de reatores
2. Emissão e registro de alarmes por limiar
3. Persistência mínima do módulo de reator
4. Traço de eventos (auditoria/log)

**Should:** controle de acesso a áreas restritas  
**Could:** ativação manual de protocolo de contingência  
**Won't (nesta disciplina):** evacuação completa, RH/treinamentos, conformidade regulatória ampla, predição por IA

## Stack

- Java (módulos por pacote de negócio)
- Barramento de eventos in-process no MVP
- Persistência inicial simples (H2/SQLite/arquivo — a definir na implementação do Marco 2)

## Estrutura do repositório

```text
docs/marco1/     # Entrega Marco 1 (requisitos, MVP, diagramas, UCs, classes, sequências, ER)
src/main/java/   # Esqueleto de pacotes alinhado à arquitetura
```

## Entrega Marco 1

- Documentação: [`docs/marco1/`](docs/marco1/)
- Checklist feedback APS: [`docs/marco1/checklist-feedback-aps.md`](docs/marco1/checklist-feedback-aps.md)
- Consolidado: [`docs/marco1/ENTREGA-MARCO1.md`](docs/marco1/ENTREGA-MARCO1.md)
- PDF: [`docs/marco1/ENTREGA-MARCO1.pdf`](docs/marco1/ENTREGA-MARCO1.pdf)
- HTML: [`docs/marco1/ENTREGA-MARCO1.html`](docs/marco1/ENTREGA-MARCO1.html)
- Repositório: https://github.com/rlampago22/RP-IV

## Como acompanhar o progresso

| Marco | Foco |
|-------|------|
| 1 | Requisitos, priorização, MVP, pacotes/componentes, artefatos refatorados |
| 2 | Implementação do núcleo + padrões GoF |
| 3 | Fluxo extra (acesso ou contingência) + polish |

## Licença acadêmica

Material produzido para avaliação na UNIPAMPA — uso interno do grupo e da disciplina.
