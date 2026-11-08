# Resumo da semana 3 — merge em desenvolvimento

- **Período:** 15/09–21/09/2026
- **Data do merge:** 2026-09-21
- **PRs mescladas:** [#60](https://github.com/rlampago22/RP-IV/pull/60) bernardo · [#61](https://github.com/rlampago22/RP-IV/pull/61) marcus · [#62](https://github.com/rlampago22/RP-IV/pull/62) jose · [#63](https://github.com/rlampago22/RP-IV/pull/63) bruno · [#64](https://github.com/rlampago22/RP-IV/pull/64) alvaro
- **Escopo:** entrega vertical S3 (Must parcial + UI Opção A) + fechamento de débito #40–#42

## Visão geral

O grupo fechou a Semana 3 em `desenvolvimento`: API de estado HTTP no Java (`apiestado`), histórico curto de medições, ciclo de alarme no front, auditoria/cenários T04–T05 compartilhados, e aceite visual Must + walkthrough Opção A. Débito de idealização (#40 shell) e mocks T01/T03 avançaram; **Figma T01–T05** permanece pendente do grupo.

## Por integrante

| Aluno | Issue | Back | Front | Resultado |
|-------|-------|------|-------|-----------|
| Álvaro | #43 | Checklist Must RF01–RF06 revisado (PASS só com evidência) | Aceite visual Opção A + walkthrough T01–T05 | Entregue (PR #64); JDK local bloqueou execução Java |
| Bruno | #41 #44 | `EstadoAgregador` + `ApiEstadoHttpServer` (`GET /api/estado`, ações tempo-real/alarme) | T01 Overview ligado a `EstadoContext` (polling) | Entregue (PR #63); contrato em `docs/api-estado-contrato.md` |
| Bernardo | #45 (+ apoio #40–#42) | `ReatorRepository` + histórico na Facade; SEQ-UC01 alinhado | T02 Sensores (cards, sparkline, histórico) | Entregue (PR #60); Figma ainda pendente |
| José | #46 | Ciclo alarme Must já no MVP (Strategy/Factory/Facade) | T03 Alarmes ACK/Resolver + banner Opção A | Entregue (PR #62); stub local até consumir API |
| Marcus | #47 | SEQ-UC02 + Astah UC/SEQ; auditoria revisada | T04 dinâmica + T05 cenários compartilhando estado T01–T04 | Entregue (PR #61) |

## Eventos EDA tocados nesta semana

`MedicaoRegistrada`, `ObservacaoRegistrada`, `FalhaSensorDetectada`, `AlarmeEmitido`, `AlarmeReconhecido`, `AlarmeResolvido` — agregados na API de estado; no front, T03/T05 ainda usam stub/estado compartilhado React onde a API não está plugada.

## Telas T0X tocadas nesta semana

| Tela | Status pós-merge |
|------|------------------|
| T01 Overview | Consome `/api/estado` (Bruno) |
| T02 Sensores | Histórico/UI Opção A (Bernardo); liga à API na S4 |
| T03 Alarmes | ACK/Resolver Opção A (José + apoio débito) |
| T04 Auditoria | Trilha dinâmica (Marcus) |
| T05 Cenários | Dispara estado compartilhado T01–T04 (Marcus) |

Walkthrough: [`docs/ui/walkthrough-s3.md`](../../ui/walkthrough-s3.md) · Débito: [`debito-40-41-42.md`](debito-40-41-42.md)

## Como rodar a demo desta semana

```bat
REM API de estado (Java) — porta 8080
cd mvp
REM compilar/rodar conforme README (ApiEstadoMain)

REM Front Opção A
cd frontend
npm install
npm run dev
```

Roteiro sugerido (Álvaro): iniciar em **T05** → **T01** → **T03** → **T04**.

## Débitos / próxima sprint (S4 · 28/09)

- **Figma** T01–T05 (#40) — pendente grupo
- Integrar T02/T03/T04/T05 de ponta a ponta com a API de estado (#49–#52)
- Álvaro #48: aceite visual final + evidências após integração
- Bruno #49: polish T01 (tempo real default) + diagramas EDA apresentação
- Bernardo #50: seeds + polish T02 na API
- José #51: polish T03 + roteiro alarme
- Marcus #52: README demo + T05 cenários oficiais
- API ainda em memória (sem persistência entre reinícios) — ok MVP, registrar decisão até Marco 1
