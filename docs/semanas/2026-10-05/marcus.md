# Entrega semanal — Marcus Querol

- **Semana / sprint:** Marco 1 (2026-10-05)
- **Issue:** [#57](https://github.com/rlampago22/RP-IV/issues/57)
- **Branch:** `marcus`
- **Trilha:** T05 cenários + T04 auditoria ao vivo

## 1. O que foi feito

- Backend: cenários Normal, Observação, Falha e Crítico acionáveis por `POST /api/cenarios/{key}`, publicando no `EventBus` do núcleo Java ([PR #67](https://github.com/rlampago22/RP-IV/pull/67), issue #52, mergeado em `desenvolvimento` em 04/10).
- Frontend: T05 dispara os cenários na API e o resultado aparece em T01, T03 e T04.
- Docs: checklist de aceite Must preenchido (13 critérios) e guia de apresentação em `docs/marcus/GUIA-APRESENTACAO-PROFESSOR.md`.
- Revisão do fluxo da demo e correções (PR a abrir após o Marco 1):
  - um sensor fora da faixa mantém um único alarme aberto (antes duplicava);
  - ao resolver o alarme, o sensor volta ao ponto normal (antes o status ia a ESTÁVEL com 372 °C na tela);
  - falha de sensor leva o reator a ATENÇÃO;
  - tempo real simulado sem alarme falso de pressão;
  - `GET /api/auditoria/integridade` e selo "CADEIA ÍNTEGRA" no T04 (a verificação SHA-256 já existia no núcleo, mas não era exibida);
  - severidade `CRITICA` com destaque correto no T03 e textos de tela operacionais;
  - novo logo da Central (símbolo de radiação em emblema) no cabeçalho e no ícone da aba, no mesmo tamanho do anterior.

## 2. Como foi feito

- Cada cenário injeta as leituras dos quatro sensores pela `ReatorFacade`; a avaliação de limiar, o ciclo do alarme e a auditoria reagem aos eventos, sem acoplamento direto com a API.
- O front consome o mesmo snapshot (`GET /api/estado`, polling) em todas as telas; sem a API ele cai em mock local, identificado na tela.
- Métodos de `AlarmeFacade` e `ReatorFacade` passaram a ser `synchronized`, porque o servidor HTTP e a thread do tempo real acessam as mesmas coleções.

## 3. Como será aplicado

- Demo ao vivo: API e front no ar com o cabeçalho **ONLINE** → T05 percorre os 4 cenários → T01 e T03 atualizam → T03 reconhece e encerra os 2 alarmes → T04 mostra os eventos novos e o selo de integridade → abre `mvp/dados/auditoria.log` (SHA-256 append-only).

## 4. O que foi entregue (DoD)

- [x] Código back (`mvp/`) e front (`frontend/`) do T05 mergeados em `desenvolvimento` (PR #67)
- [x] Suítes Java executadas em 05/10 com as correções: `TestesMvp` (11 verificações), `TestesApiAlarmes` e `TestesApiEstado`
- [x] Roteiro da demo exercitado ponta a ponta (HTTP e interface): Normal → Observação → Falha → Crítico → reconhecer → encerrar → integridade
- [x] Build React (`vite build`) sem erro
- [ ] PR das correções → `desenvolvimento` (aguardando o grupo, para não alterar o checkout da demo)
- [ ] Evidência visual (print/GIF) anexada

## 5. Bloqueios

- Medições e alarmes ficam em memória; só a auditoria persiste em arquivo.
- App Swing e API gravam no mesmo `auditoria.log`: não rodar os dois juntos (a cadeia de hash bifurca).
- O login (branch `alvaro`) não está em `desenvolvimento`; a demo do Marco 1 roda sem autenticação.

## 6. Próxima semana

- Abrir o PR das correções e alinhar com José (`AlarmeFacade`) antes do merge.
- Integrar o login ao fluxo da Central depois do Marco 1.
