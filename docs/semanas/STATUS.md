# Status semanal — RP4

Plano: [`../plano-marco1.md`](../plano-marco1.md) · Arquitetura: **EDA only** · UI: [`../ui/idealizacao-telas.md`](../ui/idealizacao-telas.md)

## Direção atual

Entrega **vertical** (Java back + React front). Swing em `mvp/` = legado.  
Resumo Semana 2: [`2026-09-14/RESUMO-MERGE.md`](2026-09-14/RESUMO-MERGE.md)

## Semana 2 — 14/09 (consolidada)

| Aluno | Issues | Status |
|-------|--------|--------|
| Álvaro | #11 #16 | Entregue |
| Bernardo | #13 #18 | Entregue |
| Marcus | #15 #20 | Entregue (MVP em `mvp/`) |
| Bruno | #12 #17 | Pendente evidência / fechar |
| José | #14 #19 | Código no MVP; fechar evidência |

## Semana 3 — 21/09 (próxima · vertical)

Cada um: pedaço **API/domínio** + pedaço **tela React** (T0X) + relatório no template vertical.

| Aluno | Issue | Back | Front |
|-------|-------|------|-------|
| Álvaro | #21 | Checklist APS núcleo | — / critérios UI |
| Bruno | #22 | Componentes físicos + EventBus estável | Shell / overview data |
| Bernardo | #23 | Histórico medições | T02 Sensores |
| José | #24 | Alarme completo | T03 Alarmes |
| Marcus | #25 | SEQ-UC02 | T04 Auditoria |

## Como rodar

```bat
mvp\1-EXECUTAR-MVP.bat
cd frontend && npm install && npm run dev
```
