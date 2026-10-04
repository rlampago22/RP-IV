# Entrega S4 / Marco 1 — Álvaro (o que entra e o que não entra)

## Já na `desenvolvimento` (usar na segunda)

- Checklist Must com evidência PASS: [`docs/mvp/checklist-aceite-must.md`](../../mvp/checklist-aceite-must.md) (preenchido na integração S4; Álvaro é o responsável nominal do aceite).
- Aceite visual Opção A: [`docs/ui/aceite-visual.md`](../../ui/aceite-visual.md).
- Sistema rodando: `mvp/` + `frontend/` via `/api/estado` (sem login).

**Fala #48/#53 (código + aceite):** MoSCoW Must vs Won't → abrir UI ONLINE → apontar checklist PASS + `mvp\2-TESTAR-MVP.bat` (11/11). Slides: cada um faz o seu.

## Na branch `alvaro` — NÃO mergear na demo de 05/10

| Conteúdo | Motivo |
|----------|--------|
| Spring Boot auth (`backend/pom.xml`, Security, Flyway, usuários) | Fora do MoSCoW Must; segunda stack além do stub `mvp/apiestado` |
| `LoginPage.jsx` + gate em `main.jsx` | Quebra o caminho “abrir painel → telemetria” da apresentação |

Esses commits ficam na `alvaro` como **evolução pós–Marco 1** (documentados em `docs/backend-autenticacao.md` na branch dele). Para a segunda, o valor TOK do Álvaro é **escopo + aceite sobre o MVP que já funciona**, não o login.

## Evidência S3 do Álvaro (JDK / suíte)

A execução local de 21/09 (11/11 Must com JDK) está registrada no relatório S3 [`../2026-09-21/alvaro.md`](../2026-09-21/alvaro.md). O checklist vigente em Dev já incorpora a suíte Must PASS atualizada na S4.
