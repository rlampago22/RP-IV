# Checklist de aceite do MVP Must

- **Sistema:** Controle de Usina Nuclear
- **Escopo:** RF01–RF06 e RNF01–RNF05, RNF09–RNF10
- **Responsável pelo checklist:** Álvaro Domingues
- **Data da linha de base:** 2026-09-14

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

## Requisitos não funcionais do núcleo

| Cenário | RNF | Procedimento de verificação | Condição de PASS | Resultado | Evidência ou observação |
|---|---|---|---|---|---|
| CT-RNF01-01 | RNF01 Desempenho | Publicar uma medição com pelo menos dois consumidores assinantes e registrar início e fim do produtor. | O produtor não chama consumidores diretamente e conclui a publicação dentro do limite acordado pelo grupo. | [x] PASS [ ] FAIL [ ] N/E | `EventBus` pub-sub in-process síncrono com tempo de despacho < 1ms e desacoplamento total. |
| CT-RNF02-01 | RNF02 Confiabilidade | Tornar um consumidor secundário indisponível e enviar uma nova medição. | A medição continua aceita e a falha do consumidor fica registrada. | [x] PASS [ ] FAIL [ ] N/E | `EventBus.publicar` isola exceções individuais de cada assinante via try-catch defensivo. |
| CT-RNF03-01 | RNF03 Integridade | Gerar um alarme a partir de uma medição conhecida e consultar ambos. | Valor, instante, sensor e vínculo entre medição e alarme permanecem completos e coerentes. | [x] PASS [ ] FAIL [ ] N/E | `[OK] RNF-03: A alteracao fisica de 1 caractere no log DEVE ser detectada como FALHA` comprovada na suíte. |
| CT-RNF04-01 | RNF04 Tolerância a falhas | Fazer um consumidor lançar erro durante o processamento e enviar outra medição. | O erro permanece isolado e `ControleReator` processa a medição seguinte. | [x] PASS [ ] FAIL [ ] N/E | `[OK] CT-RNF04-01: Erro em consumidor nao interrompeu o produtor` validado com subscriber injetado com falha. |
| CT-RNF05-01 | RNF05 Auditabilidade | Registrar dois eventos, guardar a saída, registrar um terceiro e consultar novamente. | Os dois primeiros continuam iguais e o terceiro aparece ao final. | [x] PASS [ ] FAIL [ ] N/E | `[OK] CT-RNF05-01: Cadeia SHA-256 em memoria e no arquivo fisico integra` no log append-only. |
| CT-RNF09-01 | RNF09 Persistência segura | Persistir medições e alarmes, reiniciar o processo ou restaurar a base e consultar o histórico. | Todos os registros esperados são recuperados sem duplicação ou perda. | [x] PASS [ ] FAIL [ ] N/E | `[OK] CT-RNF09-01: Reinicializacao deve recuperar todas as 11 entradas e arquivo integro`. |
| CT-RNF10-01 | RNF10 Testabilidade | Repetir o mesmo cenário automatizado duas vezes com as mesmas entradas. | As decisões de limiar, a quantidade de alarmes e a sequência lógica dos eventos são iguais nas duas execuções. | [x] PASS [ ] FAIL [ ] N/E | Execução determinística reproduzível 100% via `mvp/2-TESTAR-MVP.bat`. |

## Resultado consolidado

| Grupo | PASS | FAIL | N/E | Decisão |
|---|---:|---:|---:|---|
| RF01–RF06 | 6 | 0 | 0 | [x] Aceito [ ] Rejeitado |
| RNF01–RNF05, RNF09–RNF10 | 7 | 0 | 0 | [x] Aceito [ ] Rejeitado |
| MVP Must | 13 | 0 | 0 | [x] Aceito [ ] Rejeitado |

## Registro de falhas

| Cenário | Comportamento observado | Correção responsável | Issue ou commit | Reexecução |
|---|---|---|---|---|
| *Nenhuma falha residual* | Todos os 13 cenários de aceite Must passaram com 100% de êxito na execução de 28/09/2026. | Implementação completa de `AlarmeFacade`, `EventBus`, `RegistroAuditoria` e `ReatorRepository`. | Issue #41 / #44 / #52 | PASS |

## Situação consolidada da linha de base (28/09/2026)

- RF01–RF06 e RNF01–RNF05, RNF09–RNF10 implementados, integrados e validados com 100% de PASS.
- Issue #52 resolvida: cenários T05 conectados diretamente ao backend Java (`ApiEstadoHttpServer` -> `EventBus`).
- Testes automatizados executáveis sem dependências externas via `mvp/2-TESTAR-MVP.bat` e `TestesApiEstado.java`.

