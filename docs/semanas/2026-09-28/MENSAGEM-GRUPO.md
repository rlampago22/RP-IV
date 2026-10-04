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

Pendências antes do dia 05:
- Álvaro #48: checklist Must PASS no sistema atual — **não mergear o login Spring** até depois do Marco 1 (fora do Must e quebra a demo).
- Bruno #49: polish T01 + READMEs (como rodar / sem promessa falsa).
- Slides: cada um faz o seu individualmente; o grupo entrega o sistema rodando.

Guia: `docs/marcus/GUIA-APRESENTACAO-PROFESSOR.md`
Tip atual: branch `desenvolvimento` (após #65/#67).
