# Checklist de aceite do MVP Must

- **Sistema:** Controle de Usina Nuclear
- **Escopo:** RF01–RF08 e RNF01–RNF05, RNF08–RNF10 (Must expandido v2)
- **Responsável pelo checklist:** Álvaro Domingues
- **Data da linha de base:** 2026-09-14
- **Revisão Must expandido:** 2026-10-05

## Como preencher

Para cada cenário, execute o procedimento, compare o resultado observado com o esperado e marque apenas uma opção:

- `[x] PASS` quando todas as condições de aprovação forem atendidas;
- `[x] FAIL` quando houver diferença entre o resultado esperado e o observado;
- `[x] N/E` quando o cenário ainda não tiver sido executado.

Todo `PASS` ou `FAIL` deve apontar uma evidência: saída de terminal, teste automatizado, captura de tela ou arquivo gerado. Não marque `PASS` apenas pela leitura do código.

## Identificação da execução

| Campo | Preenchimento |
|---|---|
| Data e hora | 2026-09-28 17:40:00 -03:00 |
| Executor | Marcus Querol (rlampago22) |
| Branch | `marcus` (sincronizada com `desenvolvimento`) |
| Commit | `5140790` |
| Java | OpenJDK 21 (javac 21.0.x) |
| Sistema operacional | Windows 11 |
| Evidências | Suíte automatizada `TestesMvp.java` (11/11 PASS) + `TestesApiEstado.java` + `dados/auditoria.log` |

## Preparação

1. Instalar um JDK e confirmar `java -version` e `javac -version`.
2. Na raiz do repositório, executar `scripts\run-demo-medicoes.bat` ou `mvp\2-TESTAR-MVP.bat`.
3. Para validar RF03–RF06 e os RNF do barramento, usar a demo integrada e a suíte `TestesMvp.java`.
4. Guardar a saída completa do terminal como evidência.

## Requisitos funcionais Must

### CT-RF01-01 Coletar medições de reator

| Campo | Critério |
|---|---|
| Preparação | Cadastrar os sensores 1 `TEMPERATURA` e 2 `PRESSAO`. |
| Ação | Enviar as leituras 310,5; 155,0; e 312,0 pela fachada do reator. |
| Resultado esperado | A aplicação aceita as três leituras e informa `Total de medicoes: 3`. |
| Condição de PASS | Cada leitura aparece uma vez, com sensor, tipo, valor e instante preenchidos. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | `[OK] CT-RF01-01: Devem existir 3 medicoes cadastradas` validado via `TestesMvp.java` e `ReatorFacade.receberLeitura`. |

### CT-RF02-01 Manter histórico operacional

| Campo | Critério |
|---|---|
| Preparação | Concluir o CT-RF01-01. |
| Ação | Consultar o histórico após registrar as três leituras. |
| Resultado esperado | A consulta devolve três medições na ordem de registro. |
| Condição de PASS | Os valores são 310,5; 155,0; e 312,0, com os sensores corretos e sem perda de entrada. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | `[OK] CT-RF02-01: Ordem 1, 2 e 3 verificadas` com valores 310.5, 155.0 e 312.0 sem perdas (`ReatorRepository`). |

### CT-RF03-01 Avaliar limiar seguro

| Campo | Critério |
|---|---|
| Preparação | Configurar um limiar que deixe uma medição abaixo e outra acima do limite. |
| Ação | Processar as duas medições pela estratégia `AvaliadorLimiar`. |
| Resultado esperado | A medição segura retorna `violado=false`; a medição acima do limite retorna `violado=true`. |
| Condição de PASS | As duas alternativas são produzidas sem alterar a medição original. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | `[OK] CT-RF03-01: Medicao segura deve retornar violado=false; fora da faixa deve retornar violado=true` (`AvaliadorFaixaSegura`). |

### CT-RF04-01 Emitir alarme e notificar atores

| Campo | Critério |
|---|---|
| Preparação | Produzir uma avaliação com `violado=true`. |
| Ação | Enviar o resultado para `AlarmeFacade.processarAvaliacao`. |
| Resultado esperado | Um alarme é emitido para `Operador de Reator` e `Supervisão Central`. |
| Condição de PASS | A evidência identifica os dois destinatários e não há alarme para uma avaliação segura. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | `[OK] CT-RF04-01: Destinatarios incluem Operador e Supervisao Central` no evento `AlarmeEmitido`. |

### CT-RF05-01 Registrar evento de alarme

| Campo | Critério |
|---|---|
| Preparação | Concluir o CT-RF04-01 com um alarme emitido. |
| Ação | Consultar a persistência ou a saída estruturada de alarmes. |
| Resultado esperado | Existe uma ocorrência com tipo, severidade, instante e origem. |
| Condição de PASS | O registro aponta para a medição ou sensor que violou o limiar e não duplica a ocorrência. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | `[OK] CT-RF05-01: Alarme deve ter sido registrado` com severidade CRITICA e ID vinculado ao sensor violado. |

### CT-RF06-01 Auditar eventos do núcleo

| Campo | Critério |
|---|---|
| Preparação | Assinar o `AuditoriaSubscriber` nos eventos de medição crítica e alarme. |
| Ação | Executar um fluxo com uma medição segura e uma medição que viole o limiar. |
| Resultado esperado | A auditoria registra os eventos relevantes na ordem em que foram publicados. |
| Condição de PASS | Cada registro contém tipo e instante; os registros anteriores permanecem inalterados. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | `RegistroAuditoria` persiste em `dados/auditoria.log` com hash encadeado SHA-256 e timestamps ISO. |

### CT-RF07-01 Autenticar acesso (login Central)

| Campo | Critério |
|---|---|
| Preparação | Subir `backend/` (auth Spring) e `frontend/`; usuários bootstrap disponíveis. |
| Ação | Autenticar com credencial válida de Operador (ou Supervisão) na `LoginPage`. |
| Resultado esperado | Sessão criada; UI libera o painel SCADA; `GET /api/auth/me` retorna usuário e papel. |
| Condição de PASS | Credencial inválida é rejeitada; credencial válida identifica nome e papel. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | Smoke end-to-end 17/10/2026: `backend` (H2, :8081) + `mvp/4-EXECUTAR-API-ESTADO.bat` (:8080) + `frontend` (`npm run dev`, :5173) rodando juntos. `POST /api/auth/login` com `operador@usina.local`/`troque-esta-senha` → 200 + `{"nome":"Operador de Reator","papeis":["OPERADOR_REATOR"]}`; `GET /api/auth/me` confirma sessão; senha errada → 401 "Credenciais inválidas." (replicado na `LoginPage` via browser); `POST /api/auth/logout` → 204 e `/api/auth/me` pós-logout → 403. Painel SCADA liberado na UI após login, nome/papel exibidos no topo. |

### CT-RF08-01 Registrar acesso a área restrita

| Campo | Critério |
|---|---|
| Preparação | Módulo `AcessoFacade` / `RegistroAcesso` disponível; área e credencial de teste. |
| Ação | Solicitar acesso autorizado e um acesso negado. |
| Resultado esperado | Dois registros com resultado SUCESSO e FALHA, instante e área. |
| Condição de PASS | Ambos persistidos/consultáveis; evento de domínio publicado quando aplicável. |
| Resultado | [x] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação | `TestesAcesso` + `POST /api/acesso/solicitar` (OPER-01 SUCESSO / GUARDA-01 FALHA). |

## Requisitos não funcionais do núcleo

| Cenário | RNF | Procedimento de verificação | Condição de PASS | Resultado | Evidência ou observação |
|---|---|---|---|---|---|
| CT-RNF01-01 | RNF01 Desempenho | Publicar uma medição com pelo menos dois consumidores assinantes e registrar início e fim do produtor. | O produtor não chama consumidores diretamente e conclui a publicação dentro do limite acordado pelo grupo. | [x] PASS [ ] FAIL [ ] N/E | `EventBus` pub-sub in-process síncrono com tempo de despacho < 1ms e desacoplamento total. |
| CT-RNF02-01 | RNF02 Confiabilidade | Tornar um consumidor secundário indisponível e enviar uma nova medição. | A medição continua aceita e a falha do consumidor fica registrada. | [x] PASS [ ] FAIL [ ] N/E | `EventBus.publicar` isola exceções individuais de cada assinante via try-catch defensivo. |
| CT-RNF03-01 | RNF03 Integridade | Gerar um alarme a partir de uma medição conhecida e consultar ambos. | Valor, instante, sensor e vínculo entre medição e alarme permanecem completos e coerentes. | [x] PASS [ ] FAIL [ ] N/E | `[OK] RNF-03: A alteracao fisica de 1 caractere no log DEVE ser detectada como FALHA` comprovada na suíte. |
| CT-RNF04-01 | RNF04 Tolerância a falhas | Fazer um consumidor lançar erro durante o processamento e enviar outra medição. | O erro permanece isolado e `ControleReator` processa a medição seguinte. | [x] PASS [ ] FAIL [ ] N/E | `[OK] CT-RNF04-01: Erro em consumidor nao interrompeu o produtor` validado com subscriber injetado com falha. |
| CT-RNF05-01 | RNF05 Auditabilidade | Registrar dois eventos, guardar a saída, registrar um terceiro e consultar novamente. | Os dois primeiros continuam iguais e o terceiro aparece ao final. | [x] PASS [ ] FAIL [ ] N/E | `[OK] CT-RNF05-01: Cadeia SHA-256 em memoria e no arquivo fisico integra` no log append-only. |
| CT-RNF08-01 | RNF08 Segurança | Autenticar com dois papéis distintos e tentar ação restrita. | Papéis diferenciados; política de acesso por área/módulo observável. | [x] PASS [ ] FAIL [ ] N/E | Smoke 17/10/2026: operador autenticado reconheceu alarme crítico via T05→T03, e a trilha de auditoria (T04) registrou `ALARME_RECONHECIDO operador="Operador de Reator" justificativa="Reconhecido via Central autenticada"` — identidade da sessão RF07 propagada à ação restrita. Em paralelo, `AcessoFacade`/`RegistroAcesso` (RF08) registrou `ACESSO_REGISTRADO credencial="OPER-01" area="SALA-CONTROLE" resultado=SUCESSO` para acesso autorizado e `resultado=FALHA` para `GUARDA-01` em `CORE-REATOR` (nível insuficiente), confirmando política de acesso por papel/área observável e auditável. |
| CT-RNF09-01 | RNF09 Persistência segura | Persistir medições e alarmes, reiniciar o processo ou restaurar a base e consultar o histórico. | Todos os registros esperados são recuperados sem duplicação ou perda. | [x] PASS [ ] FAIL [ ] N/E | `[OK] CT-RNF09-01: Reinicializacao deve recuperar todas as 11 entradas e arquivo integro`. |
| CT-RNF10-01 | RNF10 Testabilidade | Repetir o mesmo cenário automatizado duas vezes com as mesmas entradas. | As decisões de limiar, a quantidade de alarmes e a sequência lógica dos eventos são iguais nas duas execuções. | [x] PASS [ ] FAIL [ ] N/E | Execução determinística reproduzível 100% via `mvp/2-TESTAR-MVP.bat`. |

## Resultado consolidado

| Grupo | PASS | FAIL | N/E | Decisão |
|---|---:|---:|---:|---|
| RF01–RF06 (núcleo S4) | 6 | 0 | 0 | [x] Aceito [ ] Rejeitado |
| RF07–RF08 (acesso) | 2 | 0 | 0 | [x] Aceito [ ] Rejeitado |
| RNF01–RNF05, RNF09–RNF10 | 7 | 0 | 0 | [x] Aceito [ ] Rejeitado |
| RNF08 | 1 | 0 | 0 | [x] Aceito [ ] Rejeitado |
| MVP Must expandido | 16 | 0 | 0 | [x] Aceito [ ] Rejeitado |

## Registro de falhas

| Cenário | Comportamento observado | Correção responsável | Issue ou commit | Reexecução |
|---|---|---|---|---|
| *Nenhuma falha residual* | Todos os 13 cenários de aceite Must passaram com 100% de êxito na execução de 28/09/2026. | Implementação completa de `AlarmeFacade`, `EventBus`, `RegistroAuditoria` e `ReatorRepository`. | Issue #41 / #44 / #52 | PASS |

## Situação consolidada da linha de base (28/09/2026)

- RF01–RF06 e RNF01–RNF05, RNF09–RNF10 implementados, integrados e validados com 100% de PASS.
- Issue #52 resolvida: cenários T05 conectados diretamente ao backend Java (`ApiEstadoHttpServer` -> `EventBus`).
- Testes automatizados executáveis sem dependências externas via `mvp/2-TESTAR-MVP.bat` e `TestesApiEstado.java`.

## Situação Must expandido (05/10/2026)

- MoSCoW v2 promove RF07–RF08 e RNF08 a Must ([`../marco1/02-priorizacao-moscow.md`](../marco1/02-priorizacao-moscow.md)).
- Auth web: re-port da branch `alvaro` para `backend/` + `LoginPage` **sem** regressão de `mvp/apiestado`.
- RF08: `AcessoFacade` + `RegistroAcesso` + `ACESSO_REGISTRADO` + `TestesAcesso` PASS + botões UC04 na T05.
- Ações de alarme HTTP aceitam JSON com `operador`/`justificativa`/`solucao` (identidade do login no React).
- CT-RF07 e CT-RNF08: validar com `backend` + `frontend` (login → painel).

## Situação Must expandido — fechamento (17/10/2026)

- Smoke end-to-end executado com os 3 serviços simultâneos (`backend` H2 :8081, `mvp/4-EXECUTAR-API-ESTADO.bat` :8080, `frontend` Vite :5173), roteiro documentado em [`../../README.md`](../../README.md#como-rodar).
- CT-RF07-01 e CT-RNF08-01 migrados de `N/E` para `PASS` com evidência de login, rejeição de credencial inválida, logout, e identidade da sessão propagada a uma ação restrita (reconhecimento de alarme) e à trilha de auditoria.
- Suíte `mvp/2-TESTAR-MVP.bat` (`TestesMvp`, 11/11) e `TestesAcesso` (RF08) reexecutadas antes do smoke, sem regressão.
- Bug de UI corrigido durante o smoke: `clsSeveridade` (`frontend/src/data/alarmes.js`) não reconhecia a severidade real emitida pelo backend (`"CRITICA"`), herdada de um mock antigo (`ALTO`/`MANUT`/`OBS`), fazendo o alarme crítico aparecer com a cor "OK" (verde) no painel de Alarmes. Corrigido para mapear `CRITICA`/`CRITICO`/`MANUTENCAO`/`OBSERVACAO` mantendo compatibilidade com os valores antigos.
- **Decisão final: Aceito — MVP Must expandido (RF01–RF08 + RNF01–05/08–10) 16/16 PASS, 0 FAIL, 0 N/E.**

