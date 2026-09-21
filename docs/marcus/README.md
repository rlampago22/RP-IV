# Marcus — trilha de UCs, sequências, auditoria e demo

Esta pasta organiza os artefatos de responsabilidade do Marcus sem mover ou reescrever entregas dos demais integrantes.

## Situação das tarefas

| Prazo | Issue | Situação nesta branch | Próximo passo |
|-------|-------|-----------------------|---------------|
| Semana 1 | [#15](https://github.com/rlampago22/RP-IV/issues/15) | Corrigida: Mermaid substituído por fonte Astah e imagens UML | Levar o `.asta` e os PNGs para a apresentação |
| Semana 2 | [#20](https://github.com/rlampago22/RP-IV/issues/20) | Entregue: `AuditoriaSubscriber` e log append-only | Manter testes verdes |
| Semana 3 — 21/09 | [#47](https://github.com/rlampago22/RP-IV/issues/47) | Implementada no [PR #61](https://github.com/rlampago22/RP-IV/pull/61): SEQ-UC02, T04 dinâmica e auditoria | Revisar e integrar em `desenvolvimento` |
| Semana 4 — 28/09 | [#52](https://github.com/rlampago22/RP-IV/issues/52) | Adiantada: T05 já controla T01–T04 por estado compartilhado | Integrar React à API Java e polir roteiro |
| Marco 1 — 05/10 | [#57](https://github.com/rlampago22/RP-IV/issues/57) | Preparação iniciada | Ensaiar UCs, auditoria e T04/T05 na demo |

## Artefatos

- [Projeto Astah editável](diagramas/Marcus-UML-MVP.asta)
- [Exportações PNG](diagramas/Marcus-UML-MVP/)
- [Revisão do MVP gerado no Antigravity](revisao-mvp-antigravity.md)
- [Relatório da Semana 3](../semanas/2026-09-21/marcus.md)

## Ordem recomendada de trabalho

1. Entregar a #47 com o Astah, T04 e o relatório semanal.
2. Na #52, substituir o estado simulado do React por chamadas à API Java, sem mudar a EDA interna.
3. Na #57, fechar roteiro, prints e ensaio de 5–10 minutos.
