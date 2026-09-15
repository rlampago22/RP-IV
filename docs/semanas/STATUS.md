# Status semanal — RP4

Plano: [`../plano-marco1.md`](../plano-marco1.md) · Stack: **Java** · Fonte: [`../legado-aps/`](../legado-aps/)

## Semana 2 — 14/09/2026

| Aluno | Issue | Entrega | Status |
|-------|-------|---------|--------|
| Álvaro | #16 | Critérios de aceite Must | **Entregue** |
| Bruno | #17 | EventBus + MedicaoRegistrada | Pendente |
| Bernardo | #18 | receberLeitura + demo memória | **Entregue** |
| José | #19 | Strategy limiar + criar Alarme | Pendente |
| Marcus | #20 | AuditoriaSubscriber + MVP Completo | **Entregue** ([mvp/](../../mvp/)) |

## Semana 1 — 08/09

| Aluno | Issue | Status | Relatório |
|-------|-------|--------|-----------|
| Álvaro | #11 | **Entregue** (baseline em `docs/mvp/`) | [alvaro.md](2026-09-14/alvaro.md) |
| Bruno | #12 | Pendente evidência Git | — |
| Bernardo | #13 | **Entregue** | [bernardo.md](2026-09-08/bernardo.md) |
| José | #14 | Pendente evidência Git | — |
| Marcus | #15 | **Entregue** | [marcus.md](2026-09-08/marcus.md) |

## Próximas

| Data | Semana | Issues |
|------|--------|--------|
| 21/09 | S3 | #21–#25 |
| 28/09 | S4 | #26–#30 |
| 05/10 | Marco 1 | #31–#35 |

## Como rodar o MVP (pasta dedicada `mvp/`)

Todo o MVP funcional da usina nuclear (com telemetria dos 4 sensores, auditoria append-only SHA-256 e console SCADA) está centralizado na pasta [`mvp/`](../../mvp/):

- **Abrir a Central de Supervisão SCADA**:
  - Duplo-clique em `mvp\0-ABRIR-SISTEMA-GRAFICO.vbs` (ou execute `mvp\abrir-sistema.ps1`).
- **Rodar a suíte dos 11 testes de aceite Must**:
  - Duplo-clique em `mvp\2-TESTAR-MVP.bat` (ou execute `mvp\executar-mvp.ps1 -Testar`).
- **Demonstração em linha de comando**:
  - Duplo-clique em `mvp\1-EXECUTAR-MVP.bat`.

## Como rodar a demo em memória da Semana 2 (Bernardo)

```bat
scripts\run-demo-medicoes.bat
```
