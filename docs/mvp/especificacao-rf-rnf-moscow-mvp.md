# Especificação de requisitos MoSCoW e MVP

## Sistema de Controle de Usina Nuclear

- **Disciplina:** AL0343 — Resolução de Problemas IV
- **Versão:** 1.0
- **Data:** 2026-09-14
- **Status:** linha de base do MVP para validação do grupo
- **Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro e Marcus Querol

## Finalidade

Este documento reúne em Markdown os requisitos funcionais, os requisitos não funcionais, a priorização MoSCoW e o recorte do MVP. A numeração e as prioridades seguem a linha de base já registrada em `docs/marco1/01-requisitos-rf-rnf.md`, `docs/marco1/02-priorizacao-moscow.md` e `docs/marco1/03-proposta-mvp.md`.

O núcleo Must é formado por RF01–RF06 e pelos critérios de qualidade RNF01–RNF05, RNF09 e RNF10. Os critérios verificáveis para a demonstração estão em [`checklist-aceite-must.md`](checklist-aceite-must.md).

## Escopo do sistema

O sistema recebe medições de sensores existentes, mantém histórico operacional, avalia limites seguros, emite e registra alarmes e conserva uma trilha de auditoria. A instalação ou manutenção de sensores e demais equipamentos físicos não pertence ao software.

### Dentro do MVP Must

- coleta e histórico de medições do reator;
- avaliação de limiares configurados;
- emissão e registro de alarmes;
- auditoria dos eventos do núcleo;
- persistência mínima e recuperável;
- módulos testáveis por meio de eventos e dados simulados.

### Fora do escopo do software

- instalação, calibração e manutenção de sensores físicos;
- procedimentos médicos de descontaminação;
- construção de barreiras físicas de contenção;
- execução física de manutenção ou evacuação.

## Requisitos funcionais

### Must

| ID | Requisito | Descrição |
|---|---|---|
| RF01 | Coletar medições de reator | Registrar temperatura, pressão, radiação, fluxo de resfriamento e demais parâmetros recebidos dos sensores existentes. |
| RF02 | Manter histórico operacional | Conservar as medições e o estado operacional para consulta e rastreabilidade. |
| RF03 | Avaliar limiares | Comparar cada medição com os limites seguros configurados. |
| RF04 | Emitir alarme | Emitir um alarme quando um limite seguro for violado e notificar Operador e Supervisão Central. |
| RF05 | Registrar evento de alarme | Persistir tipo, severidade, instante e medição ou sensor de origem do alarme. |
| RF06 | Auditar eventos | Registrar medições críticas, alarmes e publicações relevantes do barramento de eventos. |

### Should

| ID | Requisito | Descrição |
|---|---|---|
| RF07 | Autenticar acesso restrito | Validar crachá ou biometria simulada para uma área classificada. |
| RF08 | Registrar acesso | Persistir o resultado de cada tentativa de acesso. |

### Could

| ID | Requisito | Descrição |
|---|---|---|
| RF09 | Criar emergência | Permitir que um operador autorizado registre uma emergência. |
| RF10 | Ativar protocolo de contingência | Associar e acionar um protocolo em uma emergência ativa. |

### Won't nesta disciplina

| ID | Requisito | Motivo |
|---|---|---|
| RF11 | Gestão completa de evacuação | Escopo maior que o necessário para demonstrar o núcleo EDA. |
| RF12 | RH, cargos e treinamentos | Não é necessário para a demonstração do fluxo Must. |
| RF13 | Conformidade regulatória ampla | Pode consumir os eventos do núcleo em uma etapa posterior. |
| RF14 | Predição de falhas por IA | Depende de dados históricos e não faz parte do foco da disciplina. |
| RF15 | Rastreio completo de material radioativo | Permanece no backlog após o núcleo. |

## Requisitos não funcionais

| ID | Atributo | Requisito | Evidência esperada no MVP |
|---|---|---|---|
| RNF01 | Desempenho e tempo de resposta | A publicação de uma medição não deve bloquear o produtor enquanto consumidores independentes processam o evento. | Execução da demo sem chamada direta do produtor para os consumidores e sem espera indevida observável. |
| RNF02 | Confiabilidade e disponibilidade | Uma falha em consumidor secundário não deve interromper a recepção de medições. | Cenário com consumidor de auditoria ou relatório indisponível e nova medição ainda aceita. |
| RNF03 | Integridade | Medições e alarmes devem manter valor, instante e origem rastreáveis. | Consulta ou log relacionando o alarme à medição e ao sensor que o originaram. |
| RNF04 | Tolerância a falhas | A falha de um módulo deve permanecer isolada do ControleReator. | Injeção de falha em consumidor seguida por nova medição processada pelo núcleo. |
| RNF05 | Auditabilidade | Eventos relevantes devem ser registrados em ordem de acréscimo, sem alteração dos registros anteriores. | Log antes e depois de novo evento, preservando todas as entradas anteriores. |
| RNF06 | Manutenibilidade | Estratégias e módulos devem poder mudar com baixo impacto nos demais componentes. | Revisão arquitetural e de código em marcos posteriores. |
| RNF07 | Escalabilidade | Consumidores devem poder evoluir independentemente. | Avaliação posterior ao núcleo in-process. |
| RNF08 | Segurança | O módulo de acesso deve permitir políticas específicas por área. | Critério da entrega Should, fora do checklist Must. |
| RNF09 | Persistência segura do núcleo | Medições e alarmes devem possuir mecanismo documentado de gravação e recuperação. | Reinício ou restauração seguida de consulta aos registros esperados. |
| RNF10 | Testabilidade | O núcleo deve aceitar sensores, medições e eventos simulados de maneira repetível. | Roteiro de demo executável com entradas e resultados esperados explícitos. |

## Priorização MoSCoW

| Prioridade | Regra no projeto | Tratamento |
|---|---|---|
| Must Have | Necessário para demonstrar o fluxo principal e a arquitetura EDA. | Integra o MVP e precisa de aceite pass/fail. |
| Should Have | Tem valor, mas o núcleo funciona sem ele nesta etapa. | Implementar somente depois de estabilizar o Must. |
| Could Have | Melhoria opcional, condicionada à capacidade do grupo. | Considerar no Marco 3. |
| Won't Have | Excluído do MVP da disciplina. | Manter documentado para impedir expansão do escopo. |

## Definição do MVP

O MVP é uma aplicação Java modular que demonstra o seguinte fluxo:

1. um simulador representa sensores e envia medições;
2. `ControleReator` registra cada medição e mantém o histórico;
3. o núcleo avalia os limiares configurados;
4. uma violação produz um alarme para Operador e Supervisão Central;
5. o alarme e os eventos relevantes são persistidos e auditados;
6. os módulos se comunicam pelo barramento de eventos, sem chamada direta entre produtor e consumidores.

O barramento é in-process no MVP acadêmico. Microserviços reais, cluster e infraestrutura física permanecem fora desta versão.

## Aceite do MVP

O MVP Must será aceito quando todos os cenários RF01–RF06 e RNF01–RNF05, RNF09–RNF10 estiverem marcados como `PASS`, com evidência reproduzível. Um resultado `FAIL` deve registrar o comportamento observado e gerar uma correção antes de nova execução.

Documento de execução: [`checklist-aceite-must.md`](checklist-aceite-must.md).

## Rastreabilidade

| Tema | Fonte no repositório |
|---|---|
| RF e RNF | [`../marco1/01-requisitos-rf-rnf.md`](../marco1/01-requisitos-rf-rnf.md) |
| MoSCoW | [`../marco1/02-priorizacao-moscow.md`](../marco1/02-priorizacao-moscow.md) |
| Proposta de MVP | [`../marco1/03-proposta-mvp.md`](../marco1/03-proposta-mvp.md) |
| Checklist de execução | [`checklist-aceite-must.md`](checklist-aceite-must.md) |
