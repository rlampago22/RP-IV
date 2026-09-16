# Status semanal — RP4

**Plano vigente:** [`../plano-entregas.md`](../plano-entregas.md)  
Arquitetura: **EDA only** · UI: [`../ui/idealizacao-telas.md`](../ui/idealizacao-telas.md) · Figma: [`../ui/FIGMA.md`](../ui/FIGMA.md)

## Direção

Entrega **vertical** (Java + React) toda segunda. Swing em `mvp/` = legado de fluxo.  
Resumo Semana 2: [`2026-09-14/RESUMO-MERGE.md`](2026-09-14/RESUMO-MERGE.md)

Issues antigas abertas (#12–#38) foram **fechadas em 15/09** e substituídas pelo conjunto abaixo.

---

## Histórico

| Sprint | Data | Status |
|--------|------|--------|
| S1 | 08/09 | Docs/UML — Álvaro/Bernardo/Marcus ok; **débito Bruno/José** |
| S2 | 14/09 | Código Java Must parcial — ver RESUMO-MERGE |

---

## Agora → 21/09 — Idealização + débito

| Issue | Dono | Foco |
|-------|------|------|
| [#40](https://github.com/rlampago22/RP-IV/issues/40) | Grupo | Figma T01–T05 + shell React |
| [#41](https://github.com/rlampago22/RP-IV/issues/41) | Bruno | Pacotes/EventBus + T01 mock |
| [#42](https://github.com/rlampago22/RP-IV/issues/42) | José | GoF/Strategy + T03 mock |

---

## S3 — 21/09 (vertical)

| Aluno | Issue | Back | Front |
|-------|-------|------|-------|
| Álvaro | [#43](https://github.com/rlampago22/RP-IV/issues/43) | Aceite Must + APS | UX T0X |
| Bruno | [#44](https://github.com/rlampago22/RP-IV/issues/44) | API/estado EventBus | T01 |
| Bernardo | [#45](https://github.com/rlampago22/RP-IV/issues/45) | Histórico + SEQ-UC01 | T02 |
| José | [#46](https://github.com/rlampago22/RP-IV/issues/46) | Alarme completo | T03 |
| Marcus | [#47](https://github.com/rlampago22/RP-IV/issues/47) | SEQ-UC02 + auditoria | T04 |

## S4 — 28/09

| Aluno | Issue |
|-------|-------|
| Álvaro | [#48](https://github.com/rlampago22/RP-IV/issues/48) |
| Bruno | [#49](https://github.com/rlampago22/RP-IV/issues/49) |
| Bernardo | [#50](https://github.com/rlampago22/RP-IV/issues/50) |
| José | [#51](https://github.com/rlampago22/RP-IV/issues/51) |
| Marcus | [#52](https://github.com/rlampago22/RP-IV/issues/52) |

## Marco 1 — 05/10

| Aluno | Issue |
|-------|-------|
| Álvaro | [#53](https://github.com/rlampago22/RP-IV/issues/53) |
| Bruno | [#54](https://github.com/rlampago22/RP-IV/issues/54) |
| Bernardo | [#55](https://github.com/rlampago22/RP-IV/issues/55) |
| José | [#56](https://github.com/rlampago22/RP-IV/issues/56) |
| Marcus | [#57](https://github.com/rlampago22/RP-IV/issues/57) |

---

## Como rodar

```bat
mvp\1-EXECUTAR-MVP.bat
cd frontend && npm install && npm run dev
```
