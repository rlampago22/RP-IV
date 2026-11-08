# Status semanal — RP4

**Planejamento canônico:** [`../PLANEJAMENTO_DESENVOLVIMENTO.md`](../PLANEJAMENTO_DESENVOLVIMENTO.md) · **Hub engenharia:** [`../DOCUMENTACAO_DE_ENGENHARIA.md`](../DOCUMENTACAO_DE_ENGENHARIA.md)  
**Plano vigente (detalhe):** [`../plano-entregas.md`](../plano-entregas.md)  
**Auditoria PDF × MVP (22/09):** [`2026-09-22/AUDITORIA-PDF-MARCO1.md`](2026-09-22/AUDITORIA-PDF-MARCO1.md) · **Pacote M1:** [`../marco1/PACOTE-ENTREGA-MARCO1.md`](../marco1/PACOTE-ENTREGA-MARCO1.md)  
**Resumo S4:** [`2026-09-28/RESUMO-MERGE.md`](2026-09-28/RESUMO-MERGE.md)

Arquitetura: **EDA only** · UI oficial: **Opção A SCADA escuro** · **Marco 1 = MVP Must completo** (05/10)

## Direção

Entrega **vertical** (Java + React) toda segunda. Swing em `mvp/` = legado de fluxo. Visual web = Opção A.  
Resumo S2: [`2026-09-14/RESUMO-MERGE.md`](2026-09-14/RESUMO-MERGE.md) · S3: [`2026-09-21/RESUMO-MERGE.md`](2026-09-21/RESUMO-MERGE.md) · S4: [`2026-09-28/RESUMO-MERGE.md`](2026-09-28/RESUMO-MERGE.md)

---

## Histórico

| Sprint | Data | Status |
|--------|------|--------|
| S1 | 08/09 | Docs/UML |
| S2 | 14/09 | Código Java Must parcial |
| S3 | 21/09 | Vertical + API estado — PRs #60–#64 **mergeados** |
| Auditoria | 22/09 | PDF APS × scorecard A–L · gaps → S4 |
| S4 | 28/09–03/10 | Seeds T02 + T03 live + T05→API — PRs #65 #66/#67 **mergeados** |

---

## Débito / idealização

| Issue | Dono | Status |
|-------|------|--------|
| [#40](https://github.com/rlampago22/RP-IV/issues/40) | Grupo | **CLOSED** — Figma fora de escopo; telas = React `frontend/` |
| [#41](https://github.com/rlampago22/RP-IV/issues/41) | Bruno | **CLOSED** |
| [#42](https://github.com/rlampago22/RP-IV/issues/42) | José | **CLOSED** |

---

## S4 — 28/09 (código mergeado em `desenvolvimento`)

| Aluno | Issue | Status |
|-------|-------|--------|
| Álvaro | [#48](https://github.com/rlampago22/RP-IV/issues/48) | **CLOSED** — checklist/aceite em Dev; login Spring fora da demo |
| Bruno | [#49](https://github.com/rlampago22/RP-IV/issues/49) | **CLOSED** — T01/API + READMEs ONLINE |
| Bernardo | [#50](https://github.com/rlampago22/RP-IV/issues/50) | **CLOSED** · [PR #65](https://github.com/rlampago22/RP-IV/pull/65) |
| José | [#51](https://github.com/rlampago22/RP-IV/issues/51) | **CLOSED** |
| Marcus | [#52](https://github.com/rlampago22/RP-IV/issues/52) | **CLOSED** · [PR #67](https://github.com/rlampago22/RP-IV/pull/67) |

## Marco 1 — 05/10 (OPEN · TOK = código funcionando)

Critério do professor: prioridade ao sistema rodando; artefatos só se justificarem. Roteiro: [`2026-09-28/ENTREGA-SEGUNDA.md`](2026-09-28/ENTREGA-SEGUNDA.md).

| Aluno | Issue | Demo ao vivo |
|-------|-------|----------------|
| Álvaro | [#53](https://github.com/rlampago22/RP-IV/issues/53) | Escopo Must + checklist PASS + UI ONLINE |
| Bruno | [#54](https://github.com/rlampago22/RP-IV/issues/54) | EventBus → `/api/estado` → T01 + tempo real |
| Bernardo | [#55](https://github.com/rlampago22/RP-IV/issues/55) | T02 Normal/Observação + histórico API · roteiro [`2026-10-05/bernardo.md`](2026-10-05/bernardo.md) |
| José | [#56](https://github.com/rlampago22/RP-IV/issues/56) | Crítico → ACK → Resolver + GoF |
| Marcus | [#57](https://github.com/rlampago22/RP-IV/issues/57) | T05 4 cenários + T04 + `auditoria.log` |

Slides: cada um prepara **individualmente**. Checkout único: `desenvolvimento`.

---

## Marco 2 — 09/11 (OPEN · feedback do professor 04/11)

Diagnóstico completo + divisão em [`../marco2/00-feedback-professor-04-11.md`](../marco2/00-feedback-professor-04-11.md).
Pedido: padrão Observer (front parava de fazer polling), duas versões
(V1 front-end / V2 push real), expandir o MVP (2FA por celular), arquitetura
híbrida explícita (EDA + camadas/MVC).

| Aluno | Issue | Tarefa | Branch |
|-------|-------|--------|--------|
| Álvaro | [#77](https://github.com/rlampago22/RP-IV/issues/77) | Spec 2FA + arquitetura híbrida | `alvaro` — **diverge de `desenvolvimento`, merge dá conflito** (login/auth próprio); resolver manualmente |
| Bruno | [#78](https://github.com/rlampago22/RP-IV/issues/78) | Observer V2 — push real (SSE) | `bruno` — atualizada com `desenvolvimento` |
| Bernardo | [#79](https://github.com/rlampago22/RP-IV/issues/79) | Observer V1 — front-end | `bernardo` — **feito**, commit `0b20fbe`, falta PR |
| José | [#80](https://github.com/rlampago22/RP-IV/issues/80) | Alarmes consumindo o push + GoF | `jose` — atualizada com `desenvolvimento` |
| Marcus | [#81](https://github.com/rlampago22/RP-IV/issues/81) | Auditoria 2FA + roteiro de demo | `marcus` — já em dia com `desenvolvimento` |

---

## Como rodar (MVP completo)

```bat
mvp\4-EXECUTAR-API-ESTADO.bat
cd frontend && npm install && npm run dev
mvp\2-TESTAR-MVP.bat
```

Guia demo: [`../marcus/GUIA-APRESENTACAO-PROFESSOR.md`](../marcus/GUIA-APRESENTACAO-PROFESSOR.md)
