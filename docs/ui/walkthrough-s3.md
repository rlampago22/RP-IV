# Walkthrough S3 — Opção A (T01–T05)

**Issue:** #43 · **Responsável:** Álvaro Domingues · **Objetivo:** permitir que uma pessoa
avaliadora reconheça o estado operacional e percorra o fluxo crítico sem procurar controles.

## Leitura em menos de cinco segundos

1. No topo, confirme **Reator-01**, o badge de estado e `EventBus ONLINE`.
2. Se houver ocorrência, leia o banner vermelho e use **Validar / Reconhecer**; sem alarme,
   o banner não é exibido.
3. A aba ativa deve ter borda cyan. O conteúdo deve mudar sem mudar TopBar, navegação ou os
   tokens da Opção A.

`ESTÁVEL` usa verde, `ATENÇÃO` usa âmbar e `CRÍTICO` usa vermelho. Cor comunica estado ou
alarme; não é ornamento para leituras normais.

## Percurso por tela

| Tela | O operador vê | Critério de entendimento |
|---|---|---|
| T01 — Visão Geral | núcleo, quatro leituras RF-1, contadores, timeline EDA e ações | identifica o estado do reator e a existência de uma ocorrência sem navegar. |
| T02 — Sensores | tabela de temperatura, pressão, radiação e fluxo com unidade e status | localiza cada parâmetro RF-1 e distingue OK de atenção. |
| T03 — Alarmes | ocorrência, severidade e ações Reconhecer/Encerrar | entende o ciclo emitido → reconhecido → resolvido. |
| T04 — Auditoria | sequência de eventos, timestamp e hash | verifica que o evento de alarme entrou na trilha append-only. |
| T05 — Demo | Normal, Observação, Falha Sensor e Anomalia Crítica | seleciona o cenário que deve publicar os eventos do fluxo. |

## Roteiro de aceite integrado (reexecutar após #52)

1. Abra T05 e dispare **Simular Anomalia (Crítico)**.
2. Vá para T01. Esperado: núcleo e badge `CRÍTICO`, banner ativo, temperatura/fluxo coerentes
   com a anomalia e `ALARME_EMITIDO` na timeline.
3. Vá para T03 e escolha **Validar / Reconhecer**. Esperado: ocorrência `RECONHECIDA` e evento
   `ALARME_RECONHECIDO`.
4. Escolha **Normalizar / Encerrar**. Esperado: ocorrência resolvida, núcleo/badge `ESTÁVEL` e
   banner oculto.
5. Vá para T04. Esperado, em ordem: evento emitido, reconhecido e resolvido, cada um com
   timestamp e hash; registros anteriores preservados.

Marque o fluxo como PASS somente com execução renderizada desktop e mobile, sem erro relevante de
console e com evidência de cada transição. Enquanto o estado estiver mockado ou local a uma tela,
o resultado é FAIL/N/E — nunca PASS por leitura de código.
