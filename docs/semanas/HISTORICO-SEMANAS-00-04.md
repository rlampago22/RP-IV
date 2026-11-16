# Histórico — Semanas 0 a 4 (05/09 a 03/10/2026, pré-Marco 1)

Resumo consolidado do que cada semana entregou antes do Marco 1 (05/10/2026).
Substitui as pastas individuais `docs/semanas/2026-09-07/`, `2026-09-08/`,
`2026-09-14/`, `2026-09-21/`, `2026-09-22/` e `2026-09-28/` — os relatórios
completos de cada integrante continuam no histórico do git (`git log --all --
docs/semanas/2026-09-*`), só não ficam mais soltos na árvore de trabalho.

## Semana 0 (05–07/09) — alinhar escopo

Objetivo: **não** fechar o Marco 1 ainda. Cada integrante listou o que pretendia
fazer na trilha própria (Álvaro: requisitos/MoSCoW; Bruno: EDA/EventBus;
Bernardo: núcleo/medições; José: alarmes/GoF; Marcus: UCs/sequência/auditoria).

## Semana 1 (08/09) — RF/RNF, pacotes, classes, GoF, UCs

Pedido do professor: cada um entrega uma parte do Marco 1.

| Aluno | Issue | Entregou |
|-------|-------|----------|
| Álvaro | [#11](https://github.com/rlampago22/RP-IV/issues/11) | RF/RNF + MoSCoW + MVP |
| Bruno | [#12](https://github.com/rlampago22/RP-IV/issues/12) | Pacotes + componentes L/F |
| Bernardo | [#13](https://github.com/rlampago22/RP-IV/issues/13) | Classes do núcleo revisadas |
| José | [#14](https://github.com/rlampago22/RP-IV/issues/14) | GoF de alarmes previstos |
| Marcus | [#15](https://github.com/rlampago22/RP-IV/issues/15) | UC01 linear + SEQ-UC01 |

## Semana 2 (14/09) — núcleo Must em Java (`mvp/`)

| Aluno | Issue | Resultado |
|-------|-------|-----------|
| Álvaro | #16 | Spec RF/MoSCoW/MVP + checklist aceite |
| Bruno | #17 | EventBus (integrado via grupo) |
| Bernardo | #18 | `ReatorFacade.receberLeitura` + histórico em memória |
| José | #19 | Alarmes/Strategy/Factory/Facade em `mvp/` |
| Marcus | #20 | `AuditoriaSubscriber` append-only + hash encadeado (SHA-256) + suíte `TestesMvp` 11/11 PASS |

Eventos EDA tocados: `MedicaoRegistrada`, `AlarmeEmitido`, `AlarmeReconhecido`, `AlarmeResolvido`.

## Semana 3 (21/09) — entrega vertical (back + front Opção A)

PRs mergeadas em `desenvolvimento`: [#60](https://github.com/rlampago22/RP-IV/pull/60) bernardo ·
[#61](https://github.com/rlampago22/RP-IV/pull/61) marcus · [#62](https://github.com/rlampago22/RP-IV/pull/62) jose ·
[#63](https://github.com/rlampago22/RP-IV/pull/63) bruno · [#64](https://github.com/rlampago22/RP-IV/pull/64) alvaro

| Aluno | Issue | Back | Front |
|-------|-------|------|-------|
| Álvaro | #43 | Checklist Must RF01–RF06 (11/11 PASS) | Aceite visual Opção A + walkthrough T01–T05 |
| Bruno | #41/#44 | `EstadoAgregador` + `ApiEstadoHttpServer` (`GET /api/estado`) | T01 Overview ligado a `EstadoContext` |
| Bernardo | #45 | `ReatorRepository` + histórico na Facade | T02 Sensores (cards, sparkline, histórico) |
| José | #46 | Ciclo de alarme Must | T03 Alarmes ACK/Resolver |
| Marcus | #47 | SEQ-UC02 + auditoria revisada | T04 Auditoria + T05 cenários |

Débito aberto ao fim da semana: Figma T01–T05 (#40, depois fechado como fora de escopo).

## Auditoria intermediária (22/09)

Checagem geral PDF APS × repo × issues antes do Marco 1 — decisão: Marco 1 = MVP
Must **completo** (não só esqueleto/docs). Fechou as issues #41/#43/#44/#45/#47
(todas já mergeadas) e identificou os gaps que viraram a Semana 4: T05 não
publicava no EventBus/API (#52) e o checklist de aceite Must ainda sem evidência
(#48). Detalhe completo: `git log` da pasta `2026-09-22/` original.

## Semana 4 (22/09–03/10) — fechar gaps para o Marco 1

PRs: [#65](https://github.com/rlampago22/RP-IV/pull/65) bernardo · jose (rebase) · [#67](https://github.com/rlampago22/RP-IV/pull/67) marcus

| Aluno | Issue | Resultado |
|-------|-------|-----------|
| Álvaro | #48 | Pendente — checklist Must PASS com evidência |
| Bruno | #49 | Pendente — polish T01 + README |
| Bernardo | #50 | **Merge #65** — seeds HTTP + T02 ligado à API |
| José | #51 | Código em `desenvolvimento` — T03 live + `POST /api/demo/anomalia` |
| Marcus | #52 | **Merge #67** — T05 completo (`/api/cenarios/...`) |

Login Spring (branch `alvaro`) ficou fora do Must dessa fase — só entrou depois,
no backend de autenticação (RF07, Marco 2).

## Onde ver o Marco 1 fechado

- Pacote de entrega: [`../marco1/PACOTE-ENTREGA-MARCO1.md`](../marco1/PACOTE-ENTREGA-MARCO1.md)
- Entrega final: [`../marco1/ENTREGA-MARCO1.md`](../marco1/ENTREGA-MARCO1.md) / `.pdf`
