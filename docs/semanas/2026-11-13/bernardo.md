# Semana 13/11 — Bernardo Dorneles

**Contexto:** professor gostou da entrega da semana passada (Observer V1/V2) e
pediu dois itens novos — diagramas de sequência entre os objetos (ideal: Astah)
e uma aba por sensor no front-end. Feito sozinho, sem dividir com o grupo desta
vez.

## O que foi feito

1. **2 diagramas de sequência novos** (não duplicam SEQ-UC01/UC02 que já
   existiam):
   - `docs/marco1/diagramas/seq-observer-v2-push.puml`/`.png` — o fluxo
     completo do push de estado via SSE entregue semana passada: `EventBus` →
     `EncaminhadorEventosSpring` → `EventoPushController` → `SseEmitter` →
     `EventSource` → `EstadoObservavel` → UI.
   - `docs/marco1/diagramas/seq-uc04-acesso.puml`/`.png` — UC04 (acesso
     físico, RF08), uma lacuna conhecida que nunca tinha diagrama:
     `AcessoFacade.solicitarAcesso` → autorização → `RegistroAcesso` →
     publica `AcessoRegistrado` → `AuditoriaSubscriber` audita mesmo em caso
     de acesso negado.
   - Renderizados via o servidor público do PlantUML (sem precisar instalar
     nada local — baixar e rodar o `.jar` do PlantUML localmente foi negado
     pelo classificador de segurança, por boa razão: é código de terceiros não
     confiável).
   - Documentados em `docs/marco2/03-diagramas-sequencia.md`, com o roteiro
     passo a passo (participantes + mensagens, na ordem exata) pra recriar no
     Astah (`docs/marcus/diagramas/Marcus-UML-MVP.asta`) — não consigo operar
     o Astah diretamente (app desktop de GUI), então a entrega "bonita" nessa
     ferramenta ainda depende de um passo manual.

2. **Uma aba por sensor** no front-end (`/sensores/temperatura`,
   `/sensores/pressao`, `/sensores/radiacao`, `/sensores/fluxo-resfriamento`),
   como sub-navegação dentro da seção Sensores (pra não poluir a barra de
   navegação principal com mais 4 itens):
   - Extraído o card de um sensor de `SensoresPage.jsx` pra um componente
     reaproveitável, `frontend/src/components/SensorCard.jsx`.
   - Nova página genérica `frontend/src/pages/sensores/SensorDetailPage.jsx`
     (rota dinâmica `/sensores/:slug`), em vez de 4 páginas quase idênticas.
   - `SensoresPage.jsx` virou a visão geral, reaproveitando o mesmo
     `SensorCard` + a nova sub-nav (`SensorSubNav.jsx`).
   - Nenhuma mudança em `EstadoContext`/`estado.js`/back-end — o dado já
     chegava com `tipo`/`id` prontos pra filtrar por sensor.
   - De brinde: corrigido um detalhe pré-existente (não introduzido agora) —
     o sensor de fluxo aparecia como código genérico `S-4` em vez de
     `F-COOL-01` por faltar 1 entrada no mapa `CODIGO_POR_TIPO`
     (`frontend/src/data/sensores.js`).

3. **Gráfico de tendência melhor** na aba de cada sensor (a sparkline minúscula
   + tabela crua não mostrava bem as últimas leituras): `ReadingsChart.jsx`,
   um SVG próprio (sem nova dependência) com os valores no contexto da faixa
   operacional (eixo Y = min–max do sensor, não só o range das 6 amostras),
   zona de atenção sombreada, área preenchida, rótulo direto do valor atual, e
   crosshair + tooltip ao passar o mouse (valor exato + "Amostra -N"/"Atual").
   A tabela de valores continua logo abaixo como alternativa acessível (tudo
   que o tooltip mostra também está lá, sem precisar do mouse).

## Verificação

- `npm run build` (frontend) limpo em cada etapa.
- Testado ao vivo no navegador: login, Visão Geral sem regressão, as 4 abas
  novas mostrando cada uma só o sensor certo (valor, status, faixa, código
  correto), sub-nav destacando a aba ativa.
- Gráfico testado com cenário "Normal" (6 leituras) nas 4 abas: zonas de
  atenção na posição certa, hover/crosshair/tooltip respondendo com o valor
  exato de cada amostra.
- Console sem erros novos.

## Pendências

- Recriar os 2 diagramas no Astah (`Marcus-UML-MVP.asta`) — passo manual, não
  automatizável; roteiro completo já documentado em
  `docs/marco2/03-diagramas-sequencia.md`.
- Commit/push deste trabalho (branch `bernardo`) ainda não feito.
