# Entrega Marco 1 — Bernardo Dorneles

- **Data apresentação:** 05/10/2026
- **Issue:** [#55](https://github.com/rlampago22/RP-IV/issues/55)
- **Branch:** `bernardo` (sincronizada com `desenvolvimento`)
- **Trilha:** Medições / T02 Sensores — telemetria ao vivo

## Critério do professor

Prioridade: **código funcionando**. TOK apoiado em telemetria real (não mock): seeds → EventBus → `/api/estado` → T02.

## 1. O que foi feito (código já em Dev — #50)

Não há feature nova nesta semana. O valor da #55 é **demonstrar** o que a S4 entregou:

| Peça | Onde |
|------|------|
| Seeds Normal / Observação | `CenariosMedicao` + `POST /api/cenarios/{normal\|observacao}` |
| Histórico (6 amostras) | `SensorEstado.historico` no snapshot |
| T02 UI | `SensoresPage.jsx` via `EstadoContext` |
| Testes | `TestesApiEstado` (seeds + historico) + HIST-01..03 em `TestesMvp` |

## 2. Evidência de execução (04/10)

```text
POST /api/cenarios/normal  → status=ESTAVEL  temp=310.5  historico=6
POST /api/cenarios/observacao → status=ATENCAO temp=328.0 historico=6
TestesApiEstado → [SUCESSO] seeds #50 100%
```

## 3. Roteiro ao vivo (#55) — ~2 min

**Antes:** `mvp\4-EXECUTAR-API-ESTADO.bat` + `cd frontend && npm run dev`  
Confirmar header **ONLINE** (não MOCK).

| Passo | Ação | O que dizer / provar |
|-------|------|----------------------|
| 1 | Aba **Sensores** | T02 = mesma fonte da T01 (`/api/estado`) |
| 2 | **Condição · Normal** | 6 leituras estáveis; sparkline + tabela; núcleo ESTÁVEL |
| 3 | **Condição · Observação** | Temp **328 °C**; pill Atenção; núcleo ATENÇÃO |
| 4 | Voltar **Overview** | Mesmo status (poll 3s) — um só estado EDA |
| 5 | (se perguntarem) | `receberLeitura` → `MedicaoRegistrada` → buffer `historico` |

Plano B (API cai): botões ainda aplicam mock local alinhado aos seeds — avisar que a demo TOK é com ONLINE.

## 4. DoD #55

- [x] Código T02/seeds em `desenvolvimento` (#50)
- [x] Branch `bernardo` sincronizada com Dev
- [x] Smoke Normal / Observação + `TestesApiEstado`
- [x] Relatório + roteiro nesta pasta
- [ ] Demo ao vivo no dia 05/10 (apresentação)

## 5. Fora de escopo

Slides individuais; login Spring; T05/T03 (colegas #56/#57).
