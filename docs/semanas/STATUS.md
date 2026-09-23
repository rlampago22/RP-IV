# Status semanal — RP4

**Planejamento canônico:** [`../PLANEJAMENTO_DESENVOLVIMENTO.md`](../PLANEJAMENTO_DESENVOLVIMENTO.md) · **Hub engenharia:** [`../DOCUMENTACAO_DE_ENGENHARIA.md`](../DOCUMENTACAO_DE_ENGENHARIA.md)  
**Plano vigente (detalhe):** [`../plano-entregas.md`](../plano-entregas.md)  
**Auditoria PDF × MVP (22/09):** [`2026-09-22/AUDITORIA-PDF-MARCO1.md`](2026-09-22/AUDITORIA-PDF-MARCO1.md) · **Pacote M1:** [`../marco1/PACOTE-ENTREGA-MARCO1.md`](../marco1/PACOTE-ENTREGA-MARCO1.md)

Arquitetura: **EDA only** · UI oficial: **Opção A SCADA escuro** · **Marco 1 = MVP Must completo** (05/10)

## Direção

Entrega **vertical** (Java + React) toda segunda. Swing em `mvp/` = legado de fluxo. Visual web = Opção A.  
Resumo S2: [`2026-09-14/RESUMO-MERGE.md`](2026-09-14/RESUMO-MERGE.md) · S3: [`2026-09-21/RESUMO-MERGE.md`](2026-09-21/RESUMO-MERGE.md)

---

## Histórico

| Sprint | Data | Status |
|--------|------|--------|
| S1 | 08/09 | Docs/UML |
| S2 | 14/09 | Código Java Must parcial |
| S3 | 21/09 | Vertical + API estado — PRs #60–#64 **mergeados** |
| Auditoria | 22/09 | PDF APS × scorecard A–L · gaps → S4 |

---

## Débito / idealização (atualizado 22/09)

| Issue | Dono | Status |
|-------|------|--------|
| [#40](https://github.com/rlampago22/RP-IV/issues/40) | Grupo | **CLOSED** — Figma fora de escopo; telas = React `frontend/` |
| [#41](https://github.com/rlampago22/RP-IV/issues/41) | Bruno | **CLOSED** |
| [#42](https://github.com/rlampago22/RP-IV/issues/42) | José | **CLOSED** |

---

## S3 — 21/09 (mergeado · fechar issues)

| Aluno | Issue | Status |
|-------|-------|--------|
| Álvaro | [#43](https://github.com/rlampago22/RP-IV/issues/43) | PR #64 · fechar |
| Bruno | [#44](https://github.com/rlampago22/RP-IV/issues/44) | PR #63 · fechar |
| Bernardo | [#45](https://github.com/rlampago22/RP-IV/issues/45) | PR #60 · fechar |
| José | [#46](https://github.com/rlampago22/RP-IV/issues/46) | **CLOSED** |
| Marcus | [#47](https://github.com/rlampago22/RP-IV/issues/47) | PR #61 · fechar |

---

## Agora → S4 — 28/09 (fechar gaps do MVP completo)

| Aluno | Issue | Gap A–L |
|-------|-------|---------|
| Álvaro | [#48](https://github.com/rlampago22/RP-IV/issues/48) | K — aceite Must PASS + slides |
| Bruno | [#49](https://github.com/rlampago22/RP-IV/issues/49) | I — T01 + API (sem UML; PDF APS) |
| Bernardo | [#50](https://github.com/rlampago22/RP-IV/issues/50) | A/B — seeds + T02 ↔ API · **em PR (branch `bernardo`)** |
| José | [#51](https://github.com/rlampago22/RP-IV/issues/51) | D — T03 + roteiro alarme live |
| Marcus | [#52](https://github.com/rlampago22/RP-IV/issues/52) | H — **T05 → EventBus/API** + README demo |

## Marco 1 — 05/10 (só apresentar o MVP fechado)

| Aluno | Issue |
|-------|-------|
| Álvaro | [#53](https://github.com/rlampago22/RP-IV/issues/53) escopo MoSCoW |
| Bruno | [#54](https://github.com/rlampago22/RP-IV/issues/54) EDA no código + PDF APS |
| Bernardo | [#55](https://github.com/rlampago22/RP-IV/issues/55) demo medições T02 |
| José | [#56](https://github.com/rlampago22/RP-IV/issues/56) demo alarme + GoF |
| Marcus | [#57](https://github.com/rlampago22/RP-IV/issues/57) UCs/auditoria + T05 |

---

## Como rodar (MVP completo)

```bat
mvp\4-EXECUTAR-API-ESTADO.bat
cd frontend && npm install && npm run dev
mvp\2-TESTAR-MVP.bat
```
