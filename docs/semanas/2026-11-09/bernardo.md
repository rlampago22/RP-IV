# Semana 09/11 — Bernardo Dorneles

**Issue:** [#79](https://github.com/rlampago22/RP-IV/issues/79) — Observer V1 (front-end) + diagnóstico do feedback do professor.
**Branch:** `bernardo` (commits `0b20fbe`, `03510d7`, `f802c60`, `21515a5`).
**Contexto completo:** [`docs/marco2/00-feedback-professor-04-11.md`](../../marco2/00-feedback-professor-04-11.md).

## O que foi feito

1. **Diagnóstico do feedback do professor** (áudio 04/11 + PDF resumo) cruzado
   com o estado real do código: o back-end (`mvp/EventBus.java`) já era
   Observer; o gap era o front-end, que fazia `setInterval(atualizar, 3000)`
   — exatamente o anti-padrão descrito por ele (polling cego, sem checar
   mudança).
2. **Observer V1 implementado** — `EstadoObservavel.js` (Subject genérico:
   lista de observadores, `publicar()` só chama `update()` quando o dado
   muda) + `EstadoContext.jsx` refatorado para se assinar nele em vez de
   fazer polling direto. A leitura periódica da API virou um "produtor"
   isolado e trocável pelo push real (SSE, V2 — issue do Bruno) sem alterar
   o contrato `assinar()`/`update()` que as telas consomem.
3. **Diagnóstico geral do repositório**: branches, issues (todas as do Marco 1
   fechadas), PRs, suíte de testes (`mvp/2-TESTAR-MVP.bat` — 100% PASS).
4. **Atualização das 5 branches individuais** com `desenvolvimento`:
   `bruno`, `jose`, `marcus` sem conflito; `alvaro` teve conflito real
   (login/auth implementado em paralelo) — resolvido mantendo o lado já
   validado de `desenvolvimento`, preservando a execução histórica do Álvaro
   como registro suplementar no checklist de aceite. Detalhe completo
   comentado na [issue #77](https://github.com/rlampago22/RP-IV/issues/77).
5. **5 issues do Marco 2 abertas** (#77–#81), uma por integrante, mantendo a
   trilha de cada um.

## Smoke test (hoje, com os 3 serviços no ar)

Subi `mvp/4-EXECUTAR-API-ESTADO.bat` (8080), `backend` Spring com perfil H2
(8081, `mvn spring-boot:run`) e `frontend` (`npm run dev`, 5173) juntos e
validei no navegador:

- Login com `operador@usina.local` — sessão RF07 autenticada, painel libera.
- Cenário crítico (`Simular Anomalia`, T05) → T01 atualizou para `CRÍTICO`,
  banner de alarme apareceu e o histórico recebeu os eventos — **tudo via o
  Subject (`estadoObservavel`), sem precisar esperar o próximo ciclo de
  polling** porque a ação do operador publica no mesmo observável que a
  leitura periódica.
- Reconhecimento do alarme (ACK) — identidade `"Operador de Reator"`
  propagada corretamente ao evento `ALARME_RECONHECIDO` e à trilha de
  auditoria (T04), cadeia de hash SHA-256 íntegra.
- Acesso UC04 (RF08) e trilha de auditoria conferidas na T04 — sem
  regressão.
- Console do navegador sem erros novos (só os 403 esperados em
  `/api/auth/me` antes do login).
- `npm run build` (frontend) e `mvn -q -o compile` (backend) limpos.

## Pendências

- Observer V2 (push real via SSE) é tarefa do Bruno (issue #78) — o V1 já
  deixa o ponto de troca isolado (`produzirLeituras` em `EstadoContext.jsx`).
- Abrir PR `bernardo` → `desenvolvimento`.
