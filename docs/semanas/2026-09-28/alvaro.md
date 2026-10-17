# Entrega S4 / Marco 1 — Álvaro (escopo e evolução)

## Já na `desenvolvimento` (núcleo Must S4)

- Checklist Must com evidência PASS do núcleo: [`docs/mvp/checklist-aceite-must.md`](../../mvp/checklist-aceite-must.md).
- Aceite visual Opção A: [`docs/ui/aceite-visual.md`](../../ui/aceite-visual.md).
- Sistema rodando: `mvp/` + `frontend/` via `/api/estado`.

**Fala núcleo (#48/#53):** MoSCoW Must vs Won't → abrir UI ONLINE → apontar checklist PASS + `mvp\2-TESTAR-MVP.bat` (11/11). Slides: cada um faz o seu.

## Must expandido (05/10+) — login entra no escopo

Com a revisão MoSCoW v2 ([`docs/marco1/02-priorizacao-moscow.md`](../../marco1/02-priorizacao-moscow.md)), **RF07/RF08/RNF08 são Must**.

| Conteúdo (branch `alvaro`) | Tratamento |
|----------|--------|
| Spring Boot auth (`backend/`, Security, Flyway, usuários) | **Re-portar / cherry-pick** para a linha ativa **sem** trazer regressões de `mvp/apiestado` |
| `LoginPage.jsx` + gate em `main.jsx` | Integrar na demo: **login → telemetria → alarme → ACK → auditoria** |
| `docs/backend-autenticacao.md` | Trazer para `desenvolvimento` como contrato do Must de acesso |

**Não** fazer merge cego da branch `alvaro` inteira (há deleções em `mvp/apiestado` e testes). Auth fica no `backend/`; telemetria permanece no stub `mvp/apiestado`.

## Evidência S3 do Álvaro (JDK / suíte)

A execução local de 21/09 (11/11 Must com JDK) está registrada no relatório S3 [`../2026-09-21/alvaro.md`](../2026-09-21/alvaro.md). O checklist vigente incorpora a suíte Must PASS do núcleo (S4) e cenários novos N/E para RF07–RF08 até o smoke do Must expandido.
