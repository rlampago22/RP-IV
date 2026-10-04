# Mensagem para o grupo (Whats/Discord) — copiar e colar

S4 mergeada em `desenvolvimento` (03/10):

1. Bernardo #50 → PR #65 (seeds + histórico T02)
2. José #51 → ciclo alarme + `/api/demo/anomalia` (rebase sobre #65)
3. Marcus #52 → PR #67 (T05 → API: Normal/Observação/Falha/Crítico)

Como rodar a demo:
```
mvp\4-EXECUTAR-API-ESTADO.bat
cd frontend && npm run dev
```
Ensaio: T02 Normal → Observação → T05 Crítico → T03 ACK/Resolver → T04.

Para segunda (05/10) — TOK = código funcionando:
- Checkout único: `desenvolvimento` (já tem T01–T05 + API).
- **Não mergear login Spring do Álvaro** (quebra/desvia a demo). Checklist Must já está PASS em Dev; Álvaro apresenta aceite + escopo.
- Bruno: API/T01 já estão em Dev; READMEs atualizados (como rodar ONLINE).
- Slides: cada um faz o seu. Ver `ENTREGA-SEGUNDA.md`.

Guia: `docs/marcus/GUIA-APRESENTACAO-PROFESSOR.md`
Tip: branch `desenvolvimento`.
