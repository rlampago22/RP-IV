# Entrega semanal — Bernardo Dorneles

- **Semana / sprint:** S4 (2026-09-28)
- **Issue:** [#50](https://github.com/rlampago22/RP-IV/issues/50)
- **Branch:** `bernardo`
- **Trilha:** Medições / seeds + T02 alinhada à T01 via `/api/estado`

## 1. O que foi feito

### Backend (`mvp/`)
- `CenariosMedicao` — seeds reproduzíveis Normal (6 passos) e Observação (Normal + temp 328 °C).
- `POST /api/cenarios/normal` e `POST /api/cenarios/observacao` no stub HTTP (`ApiEstadoHttpServer`), aplicando leituras via `ReatorFacade.receberLeitura` (histórico real no EventBus/`ReatorRepository`).
- `SensorEstado.historico` (últimas 6) no `EstadoAgregador` + serialização em `EstadoJson`.
- Testes extras em `TestesApiEstado` (histórico + seeds).

### Frontend (`frontend/`)
- T02 consome só `EstadoContext` (mesmo contrato da T01).
- Botões Seed Normal / Observação; sparkline e tabela a partir de `sensores[].historico`.
- Status/cores OK · Atenção · Crítico (Opção A) + badge do núcleo no lead.
- Fallback mock offline com os mesmos valores dos seeds.

### Docs
- `docs/api-estado-contrato.md` — rotas de cenário + campo `historico`.
- Este relatório.

## 2. Como foi feito

- Seeds passam pela Facade → `MedicaoRegistrada` / `ObservacaoRegistrada` (EDA); agregador não chama a Facade.
- T02 não inventa buffer próprio quando a API está up — usa o snapshot.
- Fora de escopo: T05 (#52), Figma, UML.

## 3. Como será aplicado (demo)

1. `mvp\4-EXECUTAR-API-ESTADO.bat`
2. `cd frontend && npm run dev` → aba **Sensores**
3. Clicar **Seed · Normal** → núcleo ESTÁVEL, sparkline estável
4. Clicar **Seed · Observação** → temp 328 °C, status Atenção / núcleo ATENÇÃO
5. Overview (T01) reflete o mesmo estado (polling 3s)

## 4. O que foi entregue (DoD #50)

- [x] Seeds Normal/Observação previsíveis na API
- [x] T02 100% alinhada à T01 via `/api/estado` / EstadoContext
- [x] Histórico/sparkline a partir do estado
- [x] Evidência em `docs/semanas/2026-09-28/bernardo.md`
- [x] PR → `desenvolvimento`

## 5. Bloqueios

- Nenhum para #50. T05 integrado (#52 Marcus) fora do escopo desta semana.

## 6. Próxima semana (S5 / Marco 1 #55)

- Demo medições T02 no pacote Marco 1 (roteiro + evidência ao vivo).
