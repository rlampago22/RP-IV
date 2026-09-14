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
| Data e hora |  |
| Executor |  |
| Branch | `alvaro` |
| Commit |  |
| Java |  |
| Sistema operacional |  |
| Evidências |  |

## Preparação

1. Instalar um JDK e confirmar `java -version` e `javac -version`.
2. Na raiz do repositório, executar `scripts\run-demo-medicoes.bat`.
3. Para validar RF03–RF06 e os RNF do barramento, usar a demo integrada assim que esses módulos estiverem conectados ao fluxo.
4. Guardar a saída completa do terminal como evidência.

## Requisitos funcionais Must

### CT-RF01-01 Coletar medições de reator

| Campo | Critério |
|---|---|
| Preparação | Cadastrar os sensores 1 `TEMPERATURA` e 2 `PRESSAO`. |
| Ação | Enviar as leituras 310,5; 155,0; e 312,0 pela fachada do reator. |
| Resultado esperado | A aplicação aceita as três leituras e informa `Total de medicoes: 3`. |
| Condição de PASS | Cada leitura aparece uma vez, com sensor, tipo, valor e instante preenchidos. |
| Resultado | [ ] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação |  |

### CT-RF02-01 Manter histórico operacional

| Campo | Critério |
|---|---|
| Preparação | Concluir o CT-RF01-01. |
| Ação | Consultar o histórico após registrar as três leituras. |
| Resultado esperado | A consulta devolve três medições na ordem de registro. |
| Condição de PASS | Os valores são 310,5; 155,0; e 312,0, com os sensores corretos e sem perda de entrada. |
| Resultado | [ ] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação |  |

### CT-RF03-01 Avaliar limiar seguro

| Campo | Critério |
|---|---|
| Preparação | Configurar um limiar que deixe uma medição abaixo e outra acima do limite. |
| Ação | Processar as duas medições pela estratégia `AvaliadorLimiar`. |
| Resultado esperado | A medição segura retorna `violado=false`; a medição acima do limite retorna `violado=true`. |
| Condição de PASS | As duas alternativas são produzidas sem alterar a medição original. |
| Resultado | [ ] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação |  |

### CT-RF04-01 Emitir alarme e notificar atores

| Campo | Critério |
|---|---|
| Preparação | Produzir uma avaliação com `violado=true`. |
| Ação | Enviar o resultado para `AlarmeFacade.processarAvaliacao`. |
| Resultado esperado | Um alarme é emitido para `Operador de Reator` e `Supervisão Central`. |
| Condição de PASS | A evidência identifica os dois destinatários e não há alarme para uma avaliação segura. |
| Resultado | [ ] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação |  |

### CT-RF05-01 Registrar evento de alarme

| Campo | Critério |
|---|---|
| Preparação | Concluir o CT-RF04-01 com um alarme emitido. |
| Ação | Consultar a persistência ou a saída estruturada de alarmes. |
| Resultado esperado | Existe uma ocorrência com tipo, severidade, instante e origem. |
| Condição de PASS | O registro aponta para a medição ou sensor que violou o limiar e não duplica a ocorrência. |
| Resultado | [ ] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação |  |

### CT-RF06-01 Auditar eventos do núcleo

| Campo | Critério |
|---|---|
| Preparação | Assinar o `AuditoriaSubscriber` nos eventos de medição crítica e alarme. |
| Ação | Executar um fluxo com uma medição segura e uma medição que viole o limiar. |
| Resultado esperado | A auditoria registra os eventos relevantes na ordem em que foram publicados. |
| Condição de PASS | Cada registro contém tipo e instante; os registros anteriores permanecem inalterados. |
| Resultado | [ ] PASS  [ ] FAIL  [ ] N/E |
| Evidência ou observação |  |

## Requisitos não funcionais do núcleo

| Cenário | RNF | Procedimento de verificação | Condição de PASS | Resultado | Evidência ou observação |
|---|---|---|---|---|---|
| CT-RNF01-01 | RNF01 Desempenho | Publicar uma medição com pelo menos dois consumidores assinantes e registrar início e fim do produtor. | O produtor não chama consumidores diretamente e conclui a publicação dentro do limite acordado pelo grupo. | [ ] PASS [ ] FAIL [ ] N/E |  |
| CT-RNF02-01 | RNF02 Confiabilidade | Tornar um consumidor secundário indisponível e enviar uma nova medição. | A medição continua aceita e a falha do consumidor fica registrada. | [ ] PASS [ ] FAIL [ ] N/E |  |
| CT-RNF03-01 | RNF03 Integridade | Gerar um alarme a partir de uma medição conhecida e consultar ambos. | Valor, instante, sensor e vínculo entre medição e alarme permanecem completos e coerentes. | [ ] PASS [ ] FAIL [ ] N/E |  |
| CT-RNF04-01 | RNF04 Tolerância a falhas | Fazer um consumidor lançar erro durante o processamento e enviar outra medição. | O erro permanece isolado e `ControleReator` processa a medição seguinte. | [ ] PASS [ ] FAIL [ ] N/E |  |
| CT-RNF05-01 | RNF05 Auditabilidade | Registrar dois eventos, guardar a saída, registrar um terceiro e consultar novamente. | Os dois primeiros continuam iguais e o terceiro aparece ao final. | [ ] PASS [ ] FAIL [ ] N/E |  |
| CT-RNF09-01 | RNF09 Persistência segura | Persistir medições e alarmes, reiniciar o processo ou restaurar a base e consultar o histórico. | Todos os registros esperados são recuperados sem duplicação ou perda. | [ ] PASS [ ] FAIL [ ] N/E |  |
| CT-RNF10-01 | RNF10 Testabilidade | Repetir o mesmo cenário automatizado duas vezes com as mesmas entradas. | As decisões de limiar, a quantidade de alarmes e a sequência lógica dos eventos são iguais nas duas execuções. | [ ] PASS [ ] FAIL [ ] N/E |  |

## Resultado consolidado

| Grupo | PASS | FAIL | N/E | Decisão |
|---|---:|---:|---:|---|
| RF01–RF06 |  |  |  | [ ] Aceito [ ] Rejeitado |
| RNF01–RNF05, RNF09–RNF10 |  |  |  | [ ] Aceito [ ] Rejeitado |
| MVP Must |  |  |  | [ ] Aceito [ ] Rejeitado |

## Registro de falhas

| Cenário | Comportamento observado | Correção responsável | Issue ou commit | Reexecução |
|---|---|---|---|---|
|  |  |  |  |  |

## Situação da linha de base em 2026-09-14

- RF01 e RF02 possuem uma demo em memória em `DemoMedicoes`.
- RF03 possui a interface da estratégia, mas ainda precisa de implementação concreta e integração ao fluxo.
- RF04 e RF05 ainda lançam `UnsupportedOperationException("Marco 2")`.
- RF06 depende da implementação e integração do `EventBus`.
- RNF01–RNF05 e RNF09–RNF10 não devem ser marcados como `PASS` antes da demo integrada.
- A tentativa local de executar `scripts\run-demo-medicoes.bat` em 2026-09-14 foi bloqueada porque `java` e `javac` não estavam disponíveis no `PATH`.
