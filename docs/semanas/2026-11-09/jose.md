# Entrega semanal — José Guilherme Monteiro

- **Semana / sprint:** Marco 2 (2026-11-09)
- **Issue:** [#80](https://github.com/rlampago22/RP-IV/issues/80)
- **Branch:** `jose`
- **Trilha:** Alarmes / limiar / GoF e T03

## 1. O que foi feito

- Explicitei no roteiro e na arquitetura EDA quem publica e quem observa cada evento do ciclo de alarme.
- Ampliei `TestesApiEstado` para verificar que `EstadoAgregador` observa `ALARME_EMITIDO`, `ALARME_RECONHECIDO` e `ALARME_RESOLVIDO` na timeline.
- Registrei o fluxo esperado para o banner T01 e a T03 consumirem o mesmo snapshot publicado por SSE.

## 2. Como foi feito

`AlarmeFacade` assina `MedicaoRegistrada`, aplica `AvaliadorLimiar` (Strategy), cria a entidade por `AlarmeFactory` (Factory) e publica os eventos de emissão, reconhecimento e resolução. `EstadoAgregador`, `AuditoriaSubscriber` e `HistoricoSubscriber` são assinantes globais (`assinarTodos`): respectivamente atualizam estado/timeline, persistem auditoria e derivam o histórico operacional.

Os três asserts em `TestesApiEstado` tornam visível que os eventos publicados no ciclo aparecem no estado derivado. A matriz atualizada está em `mvp/ROTEIRO-APRESENTACAO.md` e `docs/wiki/Arquitetura-EDA.md`.

## 3. Integração do push de alarme

O fluxo de aceite é: cenário crítico → `ALARME_EMITIDO` → snapshot SSE → `estadoObservavel.publicar(snapshot)` → `update()` do observador React → banner T01 e lista T03 atualizados sem aguardar polling.

**Dependência de integração:** na branch `jose` deste checkout, a V1 do observável e a V2 SSE ainda não estão incorporadas. As PRs [#82](https://github.com/rlampago22/RP-IV/pull/82) e [#83](https://github.com/rlampago22/RP-IV/pull/83) estão abertas; a #83 depende da #82. Portanto, o smoke test visual do push permanece pendente até essas mudanças chegarem à branch de integração e serem sincronizadas aqui. Não atribuo ao código desta branch uma validação que ainda não foi executada.

Após a integração, validar com os três serviços ativos: login, abrir T01 e T03, disparar o cenário crítico e confirmar o banner/lista atualizados pelo `update()`; reconhecer e resolver o alarme e confirmar as transições também sem espera pelo próximo ciclo de polling.

## 4. Evidência e DoD

- [x] Relação entre Observer, Strategy e Factory documentada com os assinantes/publicadores concretos.
- [x] Teste de estado cobre os três eventos do ciclo de vida do alarme.
- [x] `.\mvp\executar-mvp.ps1 -Testar`: suíte Must, ciclo HTTP de alarme, RF08, histórico e consulta HTTP passaram.
- [x] `TestesApiEstado`: asserts dos três eventos de alarme passaram.
- [x] `npm run build` em `frontend/`: build Vite concluído.
- [ ] Smoke test visual SSE depois da integração das PRs #82 e #83.
- [ ] PR da branch `jose` para `desenvolvimento`.

## 5. Bloqueios

- Smoke test SSE depende da integração das PRs #82 (Observer V1) e #83 (push V2). A #83 deve entrar depois da #82.

## 6. Próxima ação

- Sincronizar com `desenvolvimento` quando a V1/V2 for integrada e executar o cenário crítico com o navegador, confirmando o `update()` nas telas T01/T03.
