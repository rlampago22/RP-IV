# Planejamento de Desenvolvimento — RP-IV

Documento canônico de calendário e papéis (padrão Sentinela: um `PLANEJAMENTO_*` na raiz de `docs/`).

**Detalhe operacional / DoD:** [`plano-entregas.md`](plano-entregas.md)  
**Status vivo das issues:** [`semanas/STATUS.md`](semanas/STATUS.md)  
**UI:** Opção A — [`ui/direcao-opcao-a.md`](ui/direcao-opcao-a.md)

---

## Regra de entrega (vertical)

Toda segunda cada aluno abre **1 PR** `branch pessoal → desenvolvimento` com:

1. Backend / domínio / doc da trilha  
2. Frontend da tela T0X (chrome Opção A)  
3. Relatório em `docs/semanas/AAAA-MM-DD/`

Template: [`semanas/_template-entrega-vertical.md`](semanas/_template-entrega-vertical.md)

---

## Calendário até Marco 1

| Sprint | Segunda | Foco | Issues |
|--------|---------|------|--------|
| Idealização | agora → 21/09 | Shell React + Figma Opção A | [#40](https://github.com/rlampago22/RP-IV/issues/40) |
| S3 | 21/09 | 1ª fatia vertical back+front | [#43](https://github.com/rlampago22/RP-IV/issues/43)–[#47](https://github.com/rlampago22/RP-IV/issues/47) |
| S4 | 28/09 | Integração + ensaio 8–10 min | [#48](https://github.com/rlampago22/RP-IV/issues/48)–[#52](https://github.com/rlampago22/RP-IV/issues/52) |
| Marco 1 | 05/10 | Apresentação + demo Must | [#53](https://github.com/rlampago22/RP-IV/issues/53)–[#57](https://github.com/rlampago22/RP-IV/issues/57) |

### Débito

| Issue | Dono |
|-------|------|
| [#41](https://github.com/rlampago22/RP-IV/issues/41) | Bruno — pacotes/EventBus |
| [#42](https://github.com/rlampago22/RP-IV/issues/42) | José — GoF/Strategy |

---

## Papéis × telas (Opção A)

| Aluno | Back | Front |
|-------|------|-------|
| Álvaro | Aceite Must / APS | UX + aceite visual |
| Bruno | EventBus / API estado | T01 Overview |
| Bernardo | Medições / histórico | T02 Sensores |
| José | Alarmes / limiar | T03 Alarmes |
| Marcus | Auditoria / UCs | T04 + T05 Demo |

---

## Branches

| Branch | Uso |
|--------|-----|
| `main` | Limpa — consolidação final |
| `desenvolvimento` | Integração do grupo |
| `alvaro` `bernardo` `bruno` `jose` `marcus` | Trabalho individual |
| `fix/*` | Ajustes estruturais/docs (ex.: este alinhamento) |

Hooks locais: `git config core.hooksPath .githooks`
