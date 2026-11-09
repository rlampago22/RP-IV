# Marco 2 — Feedback do professor (04/11/2026) e plano de entrega

**Fonte:** áudio WhatsApp de 04/11/2026 (8 min, transcrição automática, trechos com
sobreposição de falas ficaram incertos — ver ressalvas abaixo). Resumo oficial em
`Pedidos_do_professor.pdf` (anexado fora do repo).

**Diagnóstico feito em 08/11/2026** (ver seção 5) antes de dividir as tarefas.

---

## 1. O que o professor pediu

1. **Padrão Observer** — nenhuma tela deve consultar o estado por polling/timer.
   O objeto observado mantém a lista de telas registradas; quando o dado muda,
   chama `update()` de cada uma; se não mudou, nada é notificado. Ele deu o
   exemplo de 50 monitores simultâneos para justificar o problema de desempenho
   do polling.
2. **Duas versões do Observer:**
   - **V1** — Observer só no front-end (objeto observável na própria UI).
   - **V2** — com back-end: comunicação em tempo real (socket/server push). Como
     o back-end é Spring Boot, ele sugeriu pesquisar o equivalente moderno a
     server push no Spring.
3. **Expandir o MVP** — "o projeto está meio esgotado"; pediu funcionalidade
   nova, de preferência no back-end. Aprovou a ideia (já sugerida pelo grupo)
   de segunda camada de autenticação: o sistema manda uma requisição para o
   celular do usuário, que confirma ou digita um código para liberar o acesso.
4. **Arquitetura híbrida** — grupo pode (deve) mostrar mais de uma arquitetura:
   Event-Driven na UI (já descrito) + camadas/MVC no back-end (o monólito
   Spring). O resultado final é uma arquitetura híbrida — isso deve ficar
   explícito nos artefatos, não só implícito no código.
5. **Prazo** — "trazer o resultado na próxima aula"; o trecho de data ficou
   confuso na transcrição ("parece ser daqui a duas semanas").

**Ressalva:** o pedido do time para a próxima entrega foi "segunda-feira agora"
(09/11/2026 — amanhã, a partir da data deste documento). Isso é **1 dia**, não
bate com a leitura de "duas semanas" da transcrição. Ver recomendação na
seção 6.

---

## 2. O que já existe no código (antes de implementar qualquer coisa)

Verificação pedida explicitamente pelo professor como "primeiro passo":

| Camada | Observer já implementado? | Evidência |
|---|---|---|
| **Back-end domínio (`mvp/EventBus.java`)** | **Sim.** Pub/Sub clássico: `assinarTodos`/`assinar(tipo, subscriber)`, lista de assinantes, `publicar()` chama `onEvento()` de cada um, com isolamento de falha por try/catch. | [`mvp/src/.../infraestruturaeventos/EventBus.java`](../../mvp/src/br/edu/unipampa/usina/infraestruturaeventos/EventBus.java) |
| **Front-end (`EstadoContext.jsx`)** | **Não.** É polling: `setInterval(atualizar, 3000)` busca o snapshot inteiro a cada 3s, mude ou não o dado, e refaz `setState` em todo o contexto — exatamente o anti-padrão que o professor descreveu. | [`frontend/src/state/EstadoContext.jsx:18,47-51`](../../frontend/src/state/EstadoContext.jsx) |

**Conclusão:** o pedido 1/2 não é greenfield — é terminar a metade que falta.
O back-end já é Observer; falta (a) o front parar de fazer polling e virar
observador de verdade (V1), e (b) o back empurrar a mudança via push real em
vez do front perguntar de tempo em tempo (V2).

**Stack disponível para V2:** `backend/pom.xml` → Spring Boot **3.4.5**, que
tem suporte nativo a *Server-Sent Events* (`SseEmitter`/`ResponseBodyEmitter`)
sem dependência nova — é o "equivalente moderno a server push" que o professor
pediu para pesquisar, mais simples de implementar e de defender numa banca do
que WebSocket/STOMP (que também é uma opção válida, só mais pesada).

---

## 3. Diagnóstico geral do repositório (08/11/2026)

- **Branches:** `desenvolvimento` é a integração (`PR #76` mergeado —
  T06 Histórico v2); `main` está limpa (1 commit atrás de `desenvolvimento`,
  correto). Branches individuais (`alvaro`, `bruno`, `jose`, `marcus`) estão
  bem atrás de `desenvolvimento` (`bruno` 28 commits atrás) — **cada um
  precisa dar `git pull`/rebase antes de começar a tarefa nova**, senão vai
  trabalhar em cima de código desatualizado.
- **Issues:** todas as issues do Marco 1 (#31–#57, #70) estão **CLOSED**. Não
  há issue aberta descrevendo o Marco 2 ainda — precisa abrir 5 novas (uma por
  pessoa, seguindo o padrão `[Marco 2 · 09/11] Nome — …`).
- **PRs:** só 1 aberto, **#69** (`marcus`, *draft*, parado desde 05/10) — correções
  da demo do Marco 1. Decidir se ainda serve ou se fecha como obsoleto.
- **Suíte de testes (`mvp/2-TESTAR-MVP.bat`):** rodada agora — **100% PASS**
  (11 verificações Must + histórico + ciclo HTTP de alarme + RF08 acesso).
  Nenhuma regressão pendente antes de começar o Marco 2.
- **`docs/semanas/STATUS.md`:** só cobre até o Marco 1 (05/10) — precisa de uma
  seção Marco 2 nova (feito nas seções 4/6 deste documento; falta refletir
  no `STATUS.md` quando as issues forem abertas).
- **Evidência individual (`docs/semanas/`):** só Bernardo tem pasta depois de
  28/09 (`2026-10-05/bernardo.md`). Os outros 4 não registraram evidência
  desde então, embora o código tenha avançado (histórico, polish visual,
  login). Vale cobrar isso junto com a tarefa nova — sem relatório individual
  não dá pra saber quem fez o quê para a correção de amanhã.
- **Arquivo solto:** `Usina-Nuclear-RP-IV-Apresentacao-Final.pptx` está
  untracked na raiz — decidir se entra no repo (`docs/apresentacoes/`?) ou
  fica fora.

---

## 4. Divisão proposta (mantendo a trilha de cada um)

Reaproveitando a trilha já fixada em `docs/plano-entregas.md` (cada um dono de
uma camada do domínio + uma tela), para não largar o que cada pessoa já conhece:

| Quem | Trilha de sempre | Tarefa nova (Marco 2) |
|---|---|---|
| **Álvaro** | Requisitos/aceite/UX | Especificar o **RF novo de 2FA por celular** (RF09?) no MoSCoW/RF-RNF — caso de uso, fluxo (requisição → celular confirma/código → libera acesso), critérios de aceite. Documentar formalmente a **arquitetura híbrida** (EDA na UI + camadas/MVC no back) em `docs/marco1/04-arquitetura-eda.md` ou um `docs/marco2/01-arquitetura-hibrida.md` novo. |
| **Bruno** | EventBus / API estado | **Observer V2**: endpoint de push real no back-end (SSE via `SseEmitter` no `backend/` Spring, ou endpoint dedicado na API estado do `mvp/`) substituindo o polling de 3s por empurrão de evento quando o `EventBus` publica. |
| **Bernardo (eu)** | Medições / T02 | **Observer V1**: refatorar `EstadoContext.jsx` para parar de fazer `setInterval` e virar um *subject* real (lista de observadores/telas, `update()` só quando o dado muda) — e já deixar o gancho pronto para consumir o push do Bruno (SSE) quando ele estiver pronto, trocando o polling pela conexão real. |
| **José** | Alarmes / GoF | Evidenciar e reforçar o Observer já existente no `EventBus` do lado dos **alarmes** (quem assina `AlarmeEmitido`), e ser o primeiro consumidor real do push do Bruno — alarme crítico é o caso mais sensível a atraso de atualização. |
| **Marcus** | Auditoria / UCs | Auditar (RNF03) as novas tentativas de acesso por 2FA — toda confirmação/código deve virar entrada na trilha de auditoria (`AuditoriaLogs`). Consolidar o roteiro de demo/slides do Marco 2 com os pontos novos (Observer V1+V2 ao vivo, 2FA, arquitetura híbrida). |

Cada um abre **1 issue** (`[Marco 2 · 09/11] Nome — …`, seguindo o padrão das
anteriores), evidência em `docs/semanas/2026-11-09/<nome>.md`, PR da branch
pessoal → `desenvolvimento`.

---

## 5. Recomendação sobre o prazo

Implementar **tudo** (Observer V1 + V2 com push real + 2FA funcional +
documentação da arquitetura híbrida) com qualidade em 1 dia, com 5 pessoas
que não mexem no projeto desde 05/10 (branches até 28 commits atrás), é
**irrealista** como entrega "pronta". Duas leituras possíveis do pedido do
professor, e o que entregar em cada uma:

- **Se amanhã é só um checkpoint** ("tragam o resultado", não "terminem"):
  entregar o diagnóstico (este documento), a V1 do Observer funcionando
  (mais simples, só front-end) e a especificação do 2FA + arquitetura híbrida
  documentada — V2 (push real) e a implementação do 2FA ficam para a
  entrega de verdade, 2 semanas depois.
- **Se é entrega fechada mesmo:** priorizar nesta ordem (maior pontuação por
  esforço): (1) Observer V1 — é o que ele disse que "já vai dar trabalho
  grande" sozinho; (2) documentar a arquitetura híbrida (baixo esforço, alto
  retorno); (3) um protótipo mínimo do 2FA (sem precisar estar 100% integrado,
  só mostrar o fluxo); (4) V2 só se sobrar tempo.

**Sugestão:** confirmar com o professor (ou entre vocês) antes de queimar o
dia — a transcrição automática do próprio grupo marcou a data como incerta.

---

## 6. Próximos passos imediatos

1. Abrir as 5 issues do Marco 2 (uma por pessoa, texto acima).
2. Cada branch individual atualizar a partir de `desenvolvimento`.
3. ~~Bernardo começa a V1 do Observer em `EstadoContext.jsx`~~ — **feito**, ver
   seção 7.
4. Atualizar `docs/semanas/STATUS.md` com a seção Marco 2 assim que as issues
   existirem.

---

## 7. V1 do Observer — implementado (08/11/2026)

**Arquivos:**
- [`frontend/src/state/EstadoObservavel.js`](../../frontend/src/state/EstadoObservavel.js) —
  classe `EstadoObservavel` (Subject): `assinar(observador)`/`publicar(novoEstado)`,
  guarda a lista de observadores e só chama `update()` de cada um quando o
  dado muda de verdade (compara snapshot serializado com o último publicado).
  Framework-agnostic — não conhece React nem a API.
- [`frontend/src/state/EstadoContext.jsx`](../../frontend/src/state/EstadoContext.jsx) —
  um único sujeito observável em nível de módulo (`estadoObservavel`); o
  `EstadoProvider` se assina como observador (`update(estado) => setEstado(...)`)
  dentro de um `useEffect`; o produtor (`produzirLeituras`) é o único lugar que
  ainda lê a API em intervalo de 3s — isso é o que a V2 (push real via SSE,
  trilha do Bruno) vai substituir, sem mexer no contrato `assinar()`/`update()`.
  Ações do operador (ACK, resolver alarme, cenários) também publicam no mesmo
  sujeito, então a regra "só notifica quem mudou" vale para elas também.

**Por que ainda tem um `setInterval` dentro do V1:** o pedido do professor era
parar de ficar *consultando o objeto* sem checar mudança — não eliminar toda
leitura periódica, que só desaparece de verdade com push real do back-end
(V2). O ganho do V1 é estrutural: existe agora um Subject explícito com lista
de observadores e `update()` só em mudança real, em vez de cada parte da UI
reagir a um timer cego. Isso também é o que deixa a troca para V2 trivial
(troca o produtor, mantém os assinantes).

**Verificação feita:**
- Teste funcional isolado da classe `EstadoObservavel` (Node, fora do React):
  confirma que `publicar` com o mesmo dado não chama `update()` de novo, que
  dado diferente chama, e que `cancelarAssinatura()` para as notificações.
- `npm run build` do frontend — build limpo, sem erro de import/sintaxe.
- Front subiu local (`npm run dev`), tela de login carregou sem erro de
  console — não deu para testar o fluxo logado nesta passada porque exige
  subir `backend/` (Spring, porta 8081) + `mvp/` (API estado, porta 8080)
  junto; o contrato público do `EstadoContext` (`useEstado()`) não mudou, só
  a implementação interna, então o risco de regressão nas 6 telas é baixo —
  mas falta o smoke test completo (login → T01–T05) antes de considerar
  fechado.

**Pendente para fechar de vez:**
- Rodar o smoke test completo com os 3 serviços no ar (roteiro já documentado
  em sessões anteriores) para confirmar visualmente que as telas atualizam
  normalmente.
- Commit + PR da branch `bernardo` → `desenvolvimento`.
- Bruno encaixar a V2 (SSE) trocando `produzirLeituras` por um `EventSource`
  que chama `estadoObservavel.publicar(...)` a cada mensagem do servidor.
