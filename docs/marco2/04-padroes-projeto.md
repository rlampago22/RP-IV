# 04 - Padrões de projeto e qualidade de código

**Sistema:** Controle de Supervisão - Usina Nuclear
**Arquitetura:** EDA no núcleo, camadas/MVC no backend Spring
**Contexto:** discussão solicitada pelo professor em 16/11/2026

## 1. Objetivo

Os padrões não são adotados apenas para atender uma lista de nomes. Nesta
apresentação, eles precisam mostrar como uma decisão de projeto reduz acoplamento,
centraliza uma regra ou facilita uma evolução.

A análise abaixo distingue padrões efetivamente implementados no núcleo Java,
padrões de framework usados pela aplicação e pontos candidatos a refatoração.

## 2. Padrões GoF presentes no núcleo

### Observer / Pub-Sub - `EventBus`

`EventBus` mantém listas de assinantes globais e assinantes por tipo de evento.
Produtores publicam fatos de domínio como `MedicaoRegistrada`, `AlarmeEmitido`,
`AlarmeReconhecido` e `AlarmeResolvido`; consumidores como `AlarmeFacade`,
`AuditoriaSubscriber`, `HistoricoSubscriber` e `EstadoAgregador` reagem sem
conhecer o produtor.

Papel real no código:

- `ReatorFacade.receberLeitura()` publica uma medição.
- `AlarmeFacade` assina `MedicaoRegistrada` e avalia o limiar.
- `AuditoriaSubscriber` registra todos os eventos relevantes.
- `EstadoAgregador` deriva o estado atual usado pela API.
- `HistoricoSubscriber` mantém o histórico operacional.

O ganho é adicionar um consumidor sem modificar o sensor ou o reator. A
limitação desta versão é que o barramento é síncrono e in-process; não existe
fila, retry nem persistência do canal de eventos.

No frontend, o mesmo princípio aparece em `EstadoObservavel`: produtor de
polling/SSE publica estado e componentes React registram observadores por meio
do `EstadoContext`. O frontend observa um estado estável, não cada transportador
HTTP separadamente.

### Strategy - avaliação de limiar

`AvaliadorLimiar` define o contrato:

```text
foraDaFaixaSegura(MedicaoRegistrada): boolean
```

`AvaliadorFaixaSegura` contém a implementação atual: valor menor que o mínimo ou
maior que o máximo.

`AlarmeFacade` recebe uma estratégia pelo construtor. Assim, o ciclo de alarme
não depende da fórmula de um tipo específico de limiar. Uma evolução como limiar
por reator, histograma ou tendência poderia fornecer outra implementação sem
duplicar o processo de criação, reconhecimento e resolução do alarme.

O padrão é útil aqui porque separa uma política que pode variar da coordenação
que não varia. A estratégia é intencionalmente pequena: o valor comparado vem
da própria medição e da faixa cadastrada no sensor.

### Factory - criação consistente de alarmes

`AlarmeFactory.criar(MedicaoRegistrada)` concentra a criação de `Alarme`.
`AlarmeFacade` decide se existe uma ocorrência nova, mas delega a construção da
entidade à fábrica.

O ganho de qualidade é impedir que chamadas diferentes dupliquem a inicialização
da entidade. Em uma evolução, o mesmo ponto poderia criar variações de alarme
por severidade ou tipo de sensor.

Essa fábrica é simples e não deve ser confundida com uma hierarquia Complexa de
produtos. Ela é uma justificativa válida de Factory Method/Simple Factory porque
elimina construção espelhada e centraliza invariantes da criação.

### Facade - simplificação dos módulos

`ReatorFacade` oferece o ponto de entrada para registrar sensores, receber
leituras, simular falhas e consultar o histórico. `AlarmeFacade` oferece o ponto
de entrada para consumir medições, reconhecer/resolver alarmes e consultar
pendências.

`ReatorFacade` esconde internamente o repositório em memória, os sensores e a
publicação no EventBus. `AlarmeFacade` esconde a estratégia, a fábrica, a
coleção de alarmes e a publicação dos eventos subsequentes.

O valor do padrão é orientar a utilização: o restante da aplicação não precisa
montar manualmente sensor, registro e barramento em cada fluxo. A fachada não
transforma a aplicação em uma classe única; ela delimita o módulo que o cliente
chama.

## 3. Padrões implícitos dos frameworks

### Spring DI/IoC - inversão de controle

O Spring Boot cria e fornece componentes em `backend/`. Em
`AutenticacaoController`, por exemplo, `AuthenticationManager`,
`AutenticacaoService`, repositórios e configuração de 2FA chegam pelo construtor.
Não há criação direta de todas essas dependências dentro do controller.

A relação é principalmente:

- Anotações como `@Service`, `@Component` e `@RestController` registram o
  componente no contêiner.
- A injeção por construtor declara quais dependências cada componente precisa.
- O contêiner resolve o grafo e cria os objetos de acordo com o ciclo de vida.

Isso aumenta testabilidade e separa composição de comportamento. Em testes, uma
dependência pode ser substituída; em produção, o Spring resolve a implementação
real sem alterar o controller.

### Spring Data `Repository`

Interfaces como `UsuarioRepository`, `PapelRepository`,
`RegistroAutenticacaoRepository` e `EmergenciaRepository` estendem
`JpaRepository`.

Elas não implementam manualmente CRUD e consultas derivadas. O Spring Data cria
um proxy em tempo de execução e implementa métodos como `save`, `findById` e
buscas derivadas como `findByEmailIgnoreCase`.

Esse uso representa duas coisas juntas:

1. Repository: o domínio aplica operações de coleção sem lidar diretamente com
   SQL/JPA em cada serviço.
2. IoC/Proxy: o framework cria a implementação e injeta o objeto na aplicação.

O código deve distinguir o padrão arquitetural de sua realização pelo framework:
a interface é o contrato persistente; o proxy Spring é a infraestrutura que o
realiza.

### Composição de componentes React

As telas React são compostas por componentes pequenos e especializados. O shell
monta provedores e navegação; `EstadoProvider` fornece estado compartilhado;
páginas como `OverviewPage` compõem painel, seletor de reator e visualizações;
componentes como `SensorCard`, `SeletorReator` e `PainelContingencia` possuem
responsabilidades próprias.

Exemplo real:

```text
EstadoProvider
  -> HistoricoProvider
  -> BrowserRouter
  -> Shell
  -> páginas T01-T05
  -> componentes de apresentação e ação
```

Isso é composição de objetos/componentes, não um GoF único obrigatório. A
composição permite reutilizar estrutura e separar dados de apresentação.
`EstadoContext` ainda concentra estado operacional e ações comuns; os
componentes visuais não precisam conhecer polling, SSE ou a URL da API.

## 4. Comparação entre padrões observados

| Padrão | Problema resolvido | Exemplo real | Benefício verificável |
|--------|--------------------|--------------|-----------------------|
| Observer/Pub-Sub | Desacoplar produtor e consumidores | `EventBus` e eventos de alarme | Novo assinante não altera o produtor |
| Strategy | Variar uma decisão sem mudar o fluxo | `AvaliadorFaixaSegura` | Trocar política sem duplicar `AlarmeFacade` |
| Factory | Centralizar criação | `AlarmeFactory` | Construção consistente da entidade |
| Facade | Simplificar acesso a um módulo | `ReatorFacade`, `AlarmeFacade` | Clientes chamam operações de alto nível |
| DI/IoC | Resolver dependências fora da classe | Autenticação Spring | Controller recebe dependências por construtor |
| Repository | Isolar persistência JPA | interfaces Spring Data | Serviços não implementam CRUD manual |
| Composition | Construir UI por componentes | React T01-T05 | Reuso e responsabilidade visual separada |

## 5. Candidatos a refatoração

### `SistemaMvpUI.java` - migrar para MVC/MVP

O arquivo é citado na issue com aproximadamente 1.178 linhas; na revisão atual,
ele tem 1.002 linhas conforme medição direta. Independentemente da diferença
histórica de tamanho, ele concentra janela, paleta,
componentes Swing, eventos, timer, regras de apresentação e acesso às fachadas.
Parte dessa complexidade é visual, mas a página mistura coordenação da tela
com criação dos controles e atualização deles.

Migração recomendada:

- Manter `SistemaMvpUI` como classe fina que monta a `JFrame`.
- Criar um `ControladorCentral` responsável por registrar ações dos botões,
  iniciar/parar o timer e chamar `ReatorFacade`/`AlarmeFacade`.
- Criar componentes Swing separados por responsabilidade:
  painel do reator, cards de sensores, painel de alarmes e tabela de eventos.
- Transformar o agregado mostrado pela tela em um modelo/estado atualizado pelos
  eventos do `EventBus`.

O padrão adequado não é outro GoF isolado. É o modelo **MVC/MVP**, apoiado por
Observer no fluxo de eventos. O painel Swing pode ser o View/Presenter; o
`EventBus` continua notificando mudanças e o domínio continua nas fachadas.

Por que não simplesmente adicionar uma Facade maior: isso esconderia o problema,
mas manteria uma única classe orquestrando apresentação, layout e ações. A
refatoração deve extrair responsabilidades, não apenas reduzir o número de
classes.

### `ApiEstadoHttpServer.java` - extrair um Router/Front Controller

O servidor é citado na issue com aproximadamente 641 linhas; na revisão atual,
ele tem 632 linhas conforme medição direta. Ele registra todas as rotas, mas
também implementa parsing de parâmetros, CORS, validação de métodos, respostas
HTTP e regras específicas de estado, cenário, alarme, acesso e histórico.

Migração recomendada:

- Criar um **Front Controller/Router** que decide a rota e delega.
- Criar handlers ou commands por recurso:
  `EstadoHandler`, `CenarioHandler`, `AlarmeHandler`,
  `HistoricoHandler`, `AcessoHandler` e `AuditoriaHandler`.
- Usar uma `HttpRequestHandler` comum para método e CORS, com handlers
  específicos registrados por padrão de URL.
- Separar leitura de parâmetros e serialização JSON da regra de domínio.

Esse desenho utiliza Strategy/Command em vez de transformar o arquivo em várias
fachadas. Estratégias por rota permitem adicionar/remover endpoints sem aumentar
o switch central. A composição por Route Registry é mais coerente com o JDK
`HttpServer` e permite testes unitários dos handlers sem abrir porta HTTP.

## 6. Métrica e risco da refatoração

Os números atuais foram obtidos diretamente dos arquivos desta revisão:

- `SistemaMvpUI.java`: 1.002 linhas.
- `ApiEstadoHttpServer.java`: 632 linhas.

Os números usados na issue eram estimativas anteriores de 1.178 linhas e
641 linhas. Como o código evoluiu desde o registro da issue, a revisão atual
deve prevalecer para a situação do repositório.

Não basta dividir por quantidade de linhas: o principal defeito é a mistura de
responsabilidades. Antes de concluir que uma refatoração reduziu complexidade,
devem ser preservados:

- o contrato `/api/estado` e das demais rotas;
- os limites de `AlarmeFacade`, `ReatorFacade` e `EstadoAgregador`;
- o ciclo `ATIVO -> RECONHECIDO -> RESOLVIDO`;
- a cadeia de auditoria;
- o modo de contingência;
- as 7 suítes do núcleo e o build do frontend.

## 7. Conclusão para apresentação

A qualidade do projeto não depende de "usar muitos padrões". Ela depende de usar
o padrão no limite correto:

- EventBus desacopla fatos de domínio e consumidores.
- Strategy separa a política de limiar da coordenação dos alarmes.
- Factory centraliza criação consistente.
- Facade simplifica os módulos de reator e alarme.
- Spring DI/IoC e Repository separam composição e persistência no backend.
- React compõe telas por componentes e compartilha estado por Context.
- Classes grandes pedem extração por responsabilidade: MVC/MVP no Swing e
  Router/Handlers na API.

Esta é uma discussão de qualidade real: cada padrão resolve uma mudança
concreta, e a refatoração proposta corrige concentrações de responsabilidade
sem alterar a arquitetura EDA do projeto.
