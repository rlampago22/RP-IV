# Entrega semanal — Marcus Querol

- **Semana / sprint:** Marco 2, apresentação (2026-11-16)
- **Issue:** #81 — Auditoria do 2FA + roteiro de demo atualizado (**fechada**)
- **Branch:** `marcus` → [PR #69](https://github.com/rlampago22/RP-IV/pull/69) **mergeado** na `desenvolvimento` (commit `e577777`)
- **Trilha:** Segurança / auditoria — RF18, RNF03, RNF08, 4.2.10

## 1. O que foi feito

- **Integração com a versão do grupo:** a `marcus` recebeu a `desenvolvimento` depois dos PRs #85 (spec 2FA e
  arquitetura híbrida, Álvaro) e #86 (testes do Observer, José), sem conflito.
- **Entrega na versão oficial:** o PR #69, aberto desde o Marco 1, passou a reunir o trabalho do Marco 2 (2FA por
  celular, auditoria e digital nas ações críticas) e as correções do Marco 1 (#57). Foi mergeado na
  `desenvolvimento`, e a issue #81 foi fechada.
- **Verificação ponta a ponta antes da apresentação**, no código já integrado (detalhes na seção 4).
- **Roteiro da apresentação de 16/11:** o que mostrar, como foi verificado (estática e dinâmica), respostas para
  perguntas prováveis e cuidados ao vivo com a digital.
- **Protocolo de contingência (RF09/RF10), branch `feat/protocolo-contingencia`:** aba **Emergência** para
  registrar uma emergência e ativar um protocolo (desligamento emergencial, contenção de vazamento ou evacuação).
  Ativar e encerrar são ações críticas e pedem a digital (4.2.10), pelo mesmo `ConfirmacaoBiometrica`; cada passo
  vai para a trilha encadeada. `ProtocoloContingenciaTest` (8 casos), backend 31/31; testado no navegador com a
  digital no celular (protocolo ativado em 16/11 às 11:49).

Trabalho das semanas anteriores que entra nesta entrega:
- 26–27/10: digital (passkey/WebAuthn) como 2º fator das ações críticas, com cadastro só por código de ativação do
  Administrador (relatório `2026-10-26`).
- 09/11: 2FA por celular no login (RF18) com cada etapa na trilha de autenticação encadeada (relatório `2026-11-09`).

## 2. Como foi feito

- Antes de integrar, simulei o merge (`git merge-tree`) para confirmar que não havia conflito.
- Os testes rodaram no código integrado, não na branch isolada, para garantir que o 2FA convive com o Observer
  V1/V2 e com as mudanças de documentação do grupo.
- Teste no navegador com o sistema completo ligado a partir da `marcus` (login 8081 + API do núcleo 8080 +
  tela 5173), conta `admin@usina.local`.
- A digital foi confirmada com a passkey cadastrada no celular (QR code + Bluetooth). A evidência veio do backend:
  o último uso da passkey ficou registrado no mesmo segundo em que o código de ativação foi gerado.

## 3. Como será aplicado

- Na apresentação: login com senha → "Confirme no seu celular" → `/celular` aprova → Central → aba Segurança
  mostra a trilha com cada etapa e o selo **CADEIA ÍNTEGRA** → "Gerar código" para um funcionário pede a digital
  no celular → o código aparece.
- No projeto: a `ConfirmacaoBiometrica` já está pronta para proteger o protocolo de contingência (RF09/RF10)
  quando ele existir.

## 4. O que foi entregue (DoD)

- [x] `mvn test` no backend integrado: **23/23** (`SegundoFatorLoginTest` 6, `LoginBiometriaTest` 15 + 2 aninhados)
- [x] Suítes do núcleo (`mvp`): **5/5** com sucesso, incluindo a detecção de adulteração do `auditoria.log`
- [x] `npm run build` do frontend sem erro
- [x] 2FA no navegador: senha → celular simulado aprova → sessão aberta → trilha com as 4 etapas
  (`SENHA_OK_AGUARDANDO_2FA`, `2FA_SOLICITACAO_ENVIADA`, `2FA_APROVADO_NO_CELULAR`, `2FA_SUCESSO_APROVACAO`)
  e cadeia íntegra (13 registros)
- [x] Digital: "Gerar código" respondeu `428` até a confirmação por passkey no celular; depois disso, o código de
  ativação foi gerado
- [x] PR → `desenvolvimento` (#69, mergeado) e issue #81 fechada

## 5. Bloqueios

- O celular do 2FA continua simulado; envio real (push/SMS) exige provedor externo.
- Se dois navegadores pedem a digital ao mesmo tempo, o celular mostra "ocorreu um erro, reinicie o processo".
  É uma limitação do fluxo de passkey entre aparelhos, não do sistema. Na demo, usar um navegador só.
- O PR #69 foi mergeado sem revisão de outro integrante, para a entrega não ficar fora da versão oficial no dia
  da apresentação.

## 6. Próxima semana

- Revisão do grupo sobre o que entrou no #69 (as correções do Marco 1 estavam aguardando o OK do grupo).
- Alinhar com o Álvaro a mudança do RF18 de "pendente" para "implementado" no RF-RNF.
- Usar a `ConfirmacaoBiometrica` no protocolo de contingência (RF09/RF10) quando ele existir.

---

## Atualização (20–21/11) — issue #99: auditoria em banco + protocolo de contingência ligado ao sistema

Tarefa da divisão de 16/11 (`docs/semanas/2026-11-16/bernardo.md`): pedido do professor de sair do arquivo e
usar banco, e terminar de ligar o protocolo de contingência ao resto do sistema.

### O que foi feito

- **Auditoria do núcleo em banco.** O `RegistroAuditoria` do `mvp/` saiu do `dados/auditoria.log` e passou a
  gravar na tabela `nucleo.auditoria_evento`, no mesmo banco do `backend/` (H2 `backend/data/usina` com
  `AUTO_SERVER`, ou Postgres por `USINA_DB_URL`). A cadeia SHA-256 é a mesma de antes, linha a linha.
  - Migração Flyway `V6__criar_auditoria_nucleo.sql`; o núcleo também cria a tabela (`IF NOT EXISTS`) se subir
    antes do backend. Schema próprio (`nucleo`) para o Flyway não achar o banco "não vazio".
  - Dois processos gravando ao mesmo tempo (API + Swing) deixaram de corromper a cadeia (incidente de 09/11):
    trava `SELECT ... FOR UPDATE` + `sequencia` como chave primária.
  - O `auditoria.log` antigo é importado uma vez, com os mesmos hashes, se estiver íntegro (648 linhas importadas).
  - `mvp/dependencias.ps1` põe o driver H2 no classpath (mesma versão do backend; baixa com conferência de SHA-256
    se faltar). `persistencia/ConexaoBanco` pode ser reaproveitada nas leituras de sensor (#97).
- **Protocolo de contingência ligado ao `EventBus`.** Ativar o protocolo na Central (com digital) agora trava o
  núcleo, em vez de ficar isolado na aba Emergência.
  - Backend: `GET /api/interno/contingencia` (só da própria máquina, sem `Origin`, só leitura).
  - Núcleo: `SincronizadorContingencia` consulta a cada 1 s e publica `ProtocoloContingenciaAtivado` /
    `ProtocoloContingenciaEncerrado`. Daí em diante é EDA: auditoria grava, push leva à tela e o
    `EstadoAgregador` entra em modo de contingência.
  - Modo de contingência: simulação em tempo real pausada; `423` em cenários, anomalia de demonstração e religar o
    tempo real; novos acessos saem como `BLOQUEADO_CONTINGENCIA` (continuam auditados). Monitorar e tratar alarmes
    seguem liberados. Central fora do ar não destrava (falha segura).
- **Tela de contingência.** Painel vermelho na Visão Geral (protocolo, quem ativou, passos do roteiro, o que está
  travado/liberado), selo "CONTINGÊNCIA ATIVA" no topo, faixa vermelha em todas as abas e alerta de tela cheia na
  ativação, com sirene curta e anúncio falado formal por protocolo (voz neural grave com efeito de alto-falante,
  gravada em `frontend/public/audio/contingencia/`; script `frontend/scripts/gerar-audio-contingencia.py`).

### Entregue (DoD)

- [x] Suítes do núcleo: **8/8** (nova `TestesContingencia`, 26 verificações; `TestesMvp` cobre 2 escritores
  simultâneos, alteração e remoção de linha no banco e importação do log antigo)
- [x] Backend: **33/33** (novo `ContingenciaInternaTest`, 3 casos); V6 aplicada pelo Flyway
- [x] Ponta a ponta no banco real: 648 entradas importadas, cadeia íntegra; ativação real com a digital (emergência
  #66) gravada na auditoria e travando cenários/tempo real; encerramento destravando
- [x] `npm run build` do frontend sem erro
- [x] PR → `desenvolvimento` fechando a #99

### Bloqueios / observações

- O schema definitivo de reator e leituras (#96/#97) ainda não existe; a tabela de auditoria ficou separada no
  schema `nucleo` para não atrapalhar essas migrações.
- O som do alerta depende de a pessoa já ter clicado na página (regra do navegador); sem isso o alerta aparece mudo.
- Mergeado sem revisão de outro integrante, a pedido do Marcus, para entrar na versão oficial.
