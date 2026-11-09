# Entrega Marco 2 — Bruno Rocha

- **Data:** 09/11/2026
- **Issue:** [#78](https://github.com/rlampago22/RP-IV/issues/78) — Observer V2 (push real via SSE)
- **Branch:** `bruno` (sincronizada com `desenvolvimento` em 09/11)
- **Trilha:** EventBus / API de estado

## Critério do professor

Trocar o polling do frontend pelo "equivalente moderno a server push". O Observer já
existia no domínio (`mvp/EventBus`); o gap era a borda com o navegador.

## 1. Arquitetura

O `SseEmitter` fica no Spring, como a issue pede. Como o `EventBus` vive no processo do
`mvp/` e o Spring não tem referência a ele, há uma ponte entre os dois:

```text
EventBus (mvp, 8080)
   └─ EncaminhadorEventosSpring      assinante global, como auditoria e histórico
         └─ POST localhost:8081/api/eventos/publicar   {tipo, estado}
               └─ EventoPushController (Spring, 8081)
                     └─ SseEmitter  ->  EventSource no navegador
```

| Peça | Onde |
|------|------|
| Encaminhador mvp → Spring | `mvp/apiestado/EncaminhadorEventosSpring.java` (novo) |
| Canal SSE + ponte interna | `backend/eventos/EventoPushController.java` (novo) |
| Liberação da rota interna | `backend/configuracao/SecurityConfig.java` |
| Registro depois do `EstadoAgregador` | `mvp/app/ApiEstadoMain.java` |
| Sujeito observável (`assinar`/`publicar`) | `frontend/src/state/estadoObservavel.js` (novo) |
| Produtor SSE (`EventSource`) | `frontend/src/api/eventos.js` (novo) |
| Provider passa a só observar | `frontend/src/state/EstadoContext.jsx` |
| Contrato das rotas | `docs/api-estado-contrato.md` §6 |

## 2. Decisões

**Canal autenticado.** `GET /api/eventos` exige sessão. Diferente da API do núcleo na
8080, que não tem autenticação nenhuma, só quem fez login recebe o push.

**Rota interna restrita.** `POST /api/eventos/publicar` é `permitAll` no Spring Security,
porque o processo do `mvp/` não tem sessão — mas o controller recusa qualquer chamada que
não venha de loopback.

**Envio assíncrono.** `EventBus.publicar` chama os assinantes em sequência, na mesma
thread. Um POST bloqueante faria cada medição esperar a rede, então o encaminhador usa
`HttpClient.sendAsync`. Com o Spring fora do ar a falha é silenciosa, com um único aviso
no console — o mvp roda sozinho em boa parte da demo.

**Ordem de inscrição.** O encaminhador é registrado **depois** do `EstadoAgregador`,
porque o `EventBus` notifica na ordem de inscrição. Assim o snapshot enviado já contém o
evento que disparou o push.

**Observer preservado.** O contrato `assinar()` / `update()` não mudou; trocou só quem
alimenta o sujeito. O `EstadoContext` virou observador puro: nenhuma linha dele sabe que
a origem é SSE.

## 3. Evidência de execução (09/11)

Spring e mvp no ar, fluxo aberto com `curl` e eventos disparados por outra conexão:

```text
POST /api/auth/login (operador@usina.local)  → HTTP 200
GET  /api/eventos  sem cookie                → HTTP 403   (canal exige sessão)
GET  /api/eventos  com cookie                → fluxo aberto

POST mvp /api/demo/anomalia                  → HTTP 200
POST mvp /api/cenarios/normal                → HTTP 200

28 mensagens recebidas no canal do Spring em 9 s:
  26 × event:MEDICAO_REGISTRADA
   2 × event:ALARME_EMITIDO
```

Ordem do snapshot conferida no push do alarme:

```text
event:ALARME_EMITIDO
data: {"status":"CRITICO", ..., "contadores":{"medicoes":5,"alarmes":1,"auditoria":6}}
```

Já chega `CRITICO`, com o alarme contabilizado — confirma que o encaminhador roda depois
do agregador. Nenhum aviso `[PUSH]` no log do mvp, ou seja, nenhum envio falhou.

## 4. Testes

```text
TestesMvp           → exit 0
TestesApiAlarmes    → exit 0
TestesAcesso        → exit 0
TestesHistorico     → exit 0
TestesApiHistorico  → exit 0
mvn compile         → sucesso
npm run build       → 53 módulos, sem erro
```

Nenhuma suíte precisou mudar: o encaminhador é só mais um assinante, e o
`ApiEstadoHttpServer` não foi tocado.

## 5. Pendências

- O frontend foi validado por build e pela leitura do fluxo via `curl`, **não** por clique
  na tela com os três processos no ar. Falta esse teste manual.
- Não há teste automatizado do canal. Caberia um de integração no Spring abrindo o
  emitter, chamando a rota de publicação e conferindo o bloco recebido.
- A issue referencia `docs/marco2/00-feedback-professor-04-11.md` e a V1 do Bernardo; nada
  disso está em `desenvolvimento` até agora. O sujeito observável foi escrito aqui com o
  contrato que a issue descreve, e pode precisar de ajuste quando a V1 subir.
- Com o Spring fora, a tela não recebe push — cai no fetch inicial e fica parada. Antes,
  com polling, ela seguia atualizando só com o mvp. É consequência de o canal morar na
  Central; vale confirmar com o grupo se é aceitável para a demo.
