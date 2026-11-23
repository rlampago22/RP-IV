# Entrega semanal - José Guilherme Monteiro

- **Semana / sprint:** 16/11/2026
- **Issue:** [#98](https://github.com/rlampago22/RP-IV/issues/98)
- **Branch:** `jose`
- **Trilha:** Alarmes / padrões de projeto / qualidade de código

## 1. O que foi feito

- Documentei em um documento próprio os padrões GoF realmente utilizados no
  código, explicando o problema resolvido e o papel concreto de cada um.
- Relacionei Observer/Pub-Sub, Strategy, Factory e Facade aos produtores,
  consumidores e fachadas existentes em `mvp/`.
- Documentei Spring DI/IoC, Spring Data `Repository` e composição de componentes
  React como padrões implícitos dos frameworks.
- Levantei os dois maiores candidatos a refatoração: `SistemaMvpUI.java` e
  `ApiEstadoHttpServer.java`, incluindo a direção de migração.
- Mantive a discussão limitada ao Must e à arquitetura vigente: EDA no núcleo,
  MVC/camadas no backend Spring e Opção A no frontend.

## 2. Como foi feito

O documento canônico foi criado em
[`docs/marco2/04-padroes-projeto.md`](../../marco2/04-padroes-projeto.md).

Os padrões foram confirmados no código antes da descrição:

- `EventBus` implementa assinaturas por tipo e assinaturas globais.
- `AlarmeFacade` usa `AvaliadorLimiar`, uma `AvaliadorFaixaSegura` e
  `AlarmeFactory`.
- `ReatorFacade` e `AlarmeFacade` oferecem operações de alto nível aos módulos.
- `AutenticacaoService`, controllers e repositórios Spring demonstram DI/IoC.
- `EstadoObservavel` e `EstadoContext` reproduzem Observer no frontend.
- A composição React foi conferida no shell e nas páginas T01-T05.

Na issue, os números citados eram 1.178 linhas para `SistemaMvpUI.java` e
641 linhas para `ApiEstadoHttpServer.java`. Na revisão atual, a medição direta
encontrou 1.002 e 632 linhas, respectivamente. A diferença mostra que o código
evoluiu desde o registro da issue; os totais atuais prevalecem nesta entrega.

## 3. Refatorações propostas

- **Swing:** migrar `SistemaMvpUI` para MVC/MVP, com View/Presenter separada,
  componentes por painel e ações delegadas a um controlador. O domínio continua
  nas fachadas EDA.
- **API HTTP:** dividir `ApiEstadoHttpServer` em Front Controller/Router,
  handlers por recurso e funções comuns de CORS/parsing/resposta. Strategy ou
  Command em cada rota evita crescimento de um dispatcher monolítico.

As propostas não reimplementam a arquitetura nem movem a API para Spring. Elas
reduzem concentrações de responsabilidade dentro da stack Java atual.

## 4. Evidência e DoD

- [x] Padrões GoF associados a classes e responsabilidades reais.
- [x] Spring DI/IoC, Spring Data Repository e composição React documentados.
- [x] Candidatos a refatoração identificados com padrões de destino.
- [x] Riscos e critérios de preservação descritos.
- [x] Documento vinculado no índice de engenharia.
- [ ] Revisão do grupo.
- [ ] PR desta entrega para `desenvolvimento`.

## 5. Bloqueios

- Nenhum bloqueio técnico para a documentação.
- Refatorar `SistemaMvpUI.java` e `ApiEstadoHttpServer.java` não faz parte desta
  issue; esta entrega documenta uma proposta segura que deve ser executada em
  issues separadas.

## 6. Próxima ação

- Solicitar revisão do texto em PR.
- Usar o documento como base da discussão de qualidade na apresentação final.
- Converter a refatoração da API ou do Swing em issue específica somente depois
  de confirmar prioridade no backlog.
