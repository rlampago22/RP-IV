# Roteiro de demo — Bernardo, 09/11/2026 (Marco 2)

Referência completa: [`00-feedback-professor-04-11.md`](00-feedback-professor-04-11.md).
Sua issue: [#79](https://github.com/rlampago22/RP-IV/issues/79) · Seu PR: [#82](https://github.com/rlampago22/RP-IV/pull/82) (mergeado).

~3 min de fala + demo ao vivo. Checkout único para apresentar: **`desenvolvimento`**
(mesma convenção do Marco 1) — já tem seu V1 **e** o V2 do Bruno integrados hoje.

## Antes de entrar em sala

```powershell
git checkout desenvolvimento
git pull
```

Subir os 3 serviços, nesta ordem, cada um numa janela:

```powershell
cd mvp
.\4-EXECUTAR-API-ESTADO.bat
```
```powershell
cd backend
$env:APP_BOOTSTRAP_PASSWORD = "troque-esta-senha"
mvn spring-boot:run
```
```powershell
cd frontend
npm run dev
```

Abrir `http://localhost:5173`, login `operador@usina.local` / `troque-esta-senha`.

## Fala (1/3) — o problema e o diagnóstico

> "O senhor pediu, no áudio de 04/11, pra gente parar de ficar consultando o
> estado por timer e implementar Observer de verdade — o objeto observado
> guarda a lista de telas e só chama `update()` quando o dado muda.
>
> Antes de mexer em qualquer coisa, eu verifiquei o que já existia: o
> back-end (`EventBus.java`, no domínio Java) **já era Observer** — pub/sub
> clássico, com lista de assinantes e isolamento de falha. O problema estava
> só no front-end: o `EstadoContext.jsx` fazia `setInterval` de 3 em 3
> segundos, buscando o estado inteiro da API **independente de ter mudado
> algo** — exatamente o anti-padrão que o senhor descreveu com o exemplo dos
> 50 monitores travando."

(Opcional, se quiser mostrar código: abrir `frontend/src/state/EstadoObservavel.js`
e apontar o `publicar()` comparando o snapshot anterior antes de notificar.)

## Fala (2/3) — o que eu fiz (V1) e o que o Bruno fez em cima (V2)

> "Eu criei um Subject explícito — `EstadoObservavel` — que mantém a lista de
> telas assinantes e só chama `update()` de cada uma quando o dado muda de
> verdade. Deixei a leitura periódica da API como um **produtor isolado**,
> plugado nesse sujeito, pensado pra ser trocável sem mexer nas telas.
>
> O Bruno pegou exatamente esse ponto de troca e, essa semana, entregou o
> V2: um canal SSE autenticado no Spring Boot que empurra o evento assim que
> ele acontece no back-end — zero timer, zero polling. As telas não
> precisaram mudar uma linha, porque o contrato `assinar()`/`update()`
> continuou o mesmo. É o Observer funcionando como projeto, não só como
> conceito."

## Fala (3/3) — como o grupo chegou nisso hoje

> "Depois de mapear o feedback, dividi o trabalho nas 5 issues do Marco 2
> (#77 a #81) e, hoje, simulei os merges de todas as branches antes de
> integrar de verdade, pra não adivinhar onde ia dar conflito. Achei um
> conflito real — esperado, porque eu e o Bruno mexemos no mesmo arquivo pra
> trocar o produtor — resolvi mantendo a versão dele (que já incorpora a
> minha), e outro conflito bobo (1 linha de indentação) na branch do José.
> Está tudo integrado em `desenvolvimento` agora: meu V1, o V2 do Bruno, a
> spec de 2FA e arquitetura híbrida do Álvaro, e os testes do José."

## Demo ao vivo (mostrar, não só falar)

1. Tela **Visão Geral** carregada, status ESTÁVEL, histórico rodando — sem
   nenhum timer visível piscando.
2. Ir em **Demo / Cenários** → clicar **"Simular Anomalia (Crítico)"**.
3. Voltar pra **Visão Geral**: o núcleo virou CRÍTICO, o banner de alarme
   apareceu e o histórico recebeu os eventos **na hora** — isso agora é o
   push real (SSE) do back-end, não mais um fetch disfarçado.
4. Clicar **"Validar / Reconhecer"** no banner — mostrar que o alarme é
   reconhecido com a identidade do operador logado (RF07).
5. Ir em **Auditoria** — mostrar a trilha com hash encadeado registrando tudo
   (medição → alarme emitido → alarme reconhecido), com o nome do operador.

## Se perguntarem "e o polling ainda existe?"

> "Não mais no caminho principal. O meu V1 isolou o ponto de produção de
> estado; o V2 do Bruno, que acabou de ser mergeado, trocou esse ponto por
> um push real via SSE. As telas nunca precisaram saber a diferença."

## Se perguntarem sobre os outros pontos do feedback

- **2FA por celular + arquitetura híbrida documentada** — Álvaro, issue
  [#77](https://github.com/rlampago22/RP-IV/issues/77) — **spec entregue e
  já mergeada** (PR #85); falta a implementação do 2FA em si.
- **Observer V2 (push real)** — Bruno, issue
  [#78](https://github.com/rlampago22/RP-IV/issues/78) — **entregue e
  mergeado** (PR #83).
- **Alarmes como 1º consumidor do push + testes/GoF** — José, issue
  [#80](https://github.com/rlampago22/RP-IV/issues/80) — **testes entregues
  e mergeados** (PR #86).
- **Auditoria do 2FA + roteiro de demo** — Marcus, issue
  [#81](https://github.com/rlampago22/RP-IV/issues/81) — tem um PR de
  correções (#69) em aberto como rascunho, aguardando OK do grupo antes de
  mesclar; ainda não entrou em `desenvolvimento`.

Diagnóstico e divisão completos: [`00-feedback-professor-04-11.md`](00-feedback-professor-04-11.md).
