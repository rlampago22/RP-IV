# Entrega semanal — Bernardo Dorneles

- **Semana / sprint:** S3 (2026-09-21)
- **Issue:** [#45](https://github.com/rlampago22/RP-IV/issues/45)
- **Branch:** `bernardo`
- **Trilha:** Medições / histórico + T02 Sensores (Opção A)

## 1. O que foi feito

### Backend (`mvp/`)
- Criado `ReatorRepository` — persistência em memória do histórico curto (capacidade 200), alinhado ao SEQ-UC01 (`salvarMedicao`).
- `ReatorFacade` passou a usar o repositório; expõe:
  - `consultarHistorico()`
  - `consultarHistorico(sensorId)`
  - `consultarHistoricoRecente(n)`
  - `listarSensores()`
- Documentação SEQ-UC01 atualizada (`07-sequencias-mvp.md` + PlantUML): limiar/alarme via assinante EDA (`AlarmeFacade`), não chamada síncrona na Facade.
- Testes extras HIST-01..03 na suíte `TestesMvp`.

### Frontend (`frontend/`)
- Contrato compartilhado T01/T02 em `src/data/sensores.js`.
- **T02 Sensores:** cards RF-1 com valor, barra, status OK/Atenção/Crítico, sparkline e tabela de histórico curto.
- T01 Overview consome o mesmo contrato (KPIs alinhados).
- **Débito grupo:** T03 Alarmes mock + `PlantContext` (ACK/Resolver) — ver [`debito-40-41-42.md`](debito-40-41-42.md).

## 2. Como foi feito

- Mantida arquitetura EDA: medição publica `MedicaoRegistrada`; histórico fica no módulo ControleReator.
- UI no chrome **Opção A** (SCADA escuro) já presente no shell.
- Mock tipado no front até existir API HTTP (#44 Bruno).

## 3. Como será aplicado

- Fluxo: `receberLeitura` → repositório + EventBus → T02 consulta histórico (hoje mock; depois API).
- Demo: operador vê os 4 sensores e tendência curta na aba Sensores.

## 4. O que foi entregue (DoD)

- [x] Código back em `mvp/` (ReatorRepository + Facade)
- [x] Código front T02 em `frontend/`
- [x] SEQ-UC01 alinhado ao código / docs
- [x] Evidência: suíte MVP + build frontend
- [x] PR → `desenvolvimento` (#60)
- [x] Apoio débito #40/#41/#42 (front + evidência; Figma fica pendente)

## 5. Bloqueios

- API HTTP de estado ainda a cargo do Bruno (#44); front usa mock compartilhado.
- Figma T01–T05 (#40) ainda pendente do grupo.

## 6. Próxima semana (S4 #50)

- Seeds/cenários reproduzíveis + polish T02 ligado à API quando disponível.
