# Status semanal — RP4

**Planejamento canônico:** [`../PLANEJAMENTO_DESENVOLVIMENTO.md`](../PLANEJAMENTO_DESENVOLVIMENTO.md) · **Hub engenharia:** [`../DOCUMENTACAO_DE_ENGENHARIA.md`](../DOCUMENTACAO_DE_ENGENHARIA.md)  
**Plano vigente (detalhe):** [`../plano-entregas.md`](../plano-entregas.md)  
Arquitetura: **EDA only** · UI oficial: **Opção A SCADA escuro** [`../ui/direcao-opcao-a.md`](../ui/direcao-opcao-a.md) · protótipo [`../ui/propostas/opcao-a.html`](../ui/propostas/opcao-a.html) · Figma: [`../ui/FIGMA.md`](../ui/FIGMA.md)

## Direção

Entrega **vertical** (Java + React) toda segunda. Swing em `mvp/` = legado de fluxo (roteiro/cenários). Visual web = Opção A.  
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
| [#40](https://github.com/rlampago22/RP-IV/issues/40) | Grupo | **Opção A** Figma + shell React |
| [#41](https://github.com/rlampago22/RP-IV/issues/41) | Bruno | Pacotes/EventBus + T01 mock Opção A |
| [#42](https://github.com/rlampago22/RP-IV/issues/42) | José | GoF/Strategy + T03 mock Opção A |

---

## S3 — 21/09 (vertical · UI Opção A)

| Aluno | Issue | Back | Front |
|-------|-------|------|-------|
| Álvaro | [#43](https://github.com/rlampago22/RP-IV/issues/43) | Aceite Must + APS | UX aceite visual Opção A |
| Bruno | [#44](https://github.com/rlampago22/RP-IV/issues/44) | API/estado EventBus | T01 Overview SCADA |
| Bernardo | [#45](https://github.com/rlampago22/RP-IV/issues/45) | Histórico + SEQ-UC01 | T02 Sensores |
| José | [#46](https://github.com/rlampago22/RP-IV/issues/46) | Alarme completo | T03 Alarmes ACK/Resolver |
| Marcus | [#47](https://github.com/rlampago22/RP-IV/issues/47) | SEQ-UC02 + auditoria | T04 Auditoria |

## S4 — 28/09

| Aluno | Issue |
|-------|-------|
| Álvaro | [#48](https://github.com/rlampago22/RP-IV/issues/48) slides + aceite visual |
| Bruno | [#49](https://github.com/rlampago22/RP-IV/issues/49) T01 polish |
| Bernardo | [#50](https://github.com/rlampago22/RP-IV/issues/50) T02 polish + seeds |
| José | [#51](https://github.com/rlampago22/RP-IV/issues/51) T03 polish + roteiro alarme |
| Marcus | [#52](https://github.com/rlampago22/RP-IV/issues/52) **T05 cenários** + README demo |

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
