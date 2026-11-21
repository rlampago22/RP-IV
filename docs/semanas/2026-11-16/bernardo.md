# Semana 16/11 — Bernardo Dorneles

**Contexto:** reunião do grupo com o professor em 16/11. Ele aprovou a entrega
da semana (diagramas Observer V2 + UC04, 5 reatores, abas por sensor,
protocolo de contingência com digital), e deixou duas pautas obrigatórias para
a próxima semana: (1) sair da simulação em memória e persistir reator,
sensores/histórico e auditoria em banco de dados — só a geração do valor do
sensor continua simulada, mas grava no banco; dado crítico dispara push
imediato via trigger, dado rotineiro continua por polling; (2) documentar e
discutir de verdade os padrões de projeto usados na aplicação (GoF + padrões
implícitos de framework), para a apresentação final.

A pedido do usuário, fiz uma auditoria geral do repositório antes de dividir
as tarefas da semana entre os 5 integrantes.

## O que foi feito

1. **Auditoria de branches/PRs** — só a `PR #94` (José) estava aberta; todo o
   resto já mesclado nesta data. Encontrado um problema real: a PR #94 estava
   aberta contra `main` em vez de `desenvolvimento` (viola a convenção do
   `AGENTS.md` e inflava o diff para 23 mil linhas) — corrigido via
   `gh pr edit 94 --base desenvolvimento`. Mesmo corrigida, ela segue com
   conflito porque a branch `jose` está 25 commits atrás da `desenvolvimento`;
   como o conflito toca o código de alarmes dele, decidi deixar a resolução
   para o próprio José em vez de resolver às cegas.
   A branch `alvaro` está 62 commits atrás, mas confirmei que não tem nenhum
   trabalho divergente — é um ponto antigo da própria `desenvolvimento`
   (PR #85 já mesclada), então é um fast-forward trivial, sem risco de
   conflito.

2. **Corrigido o bug que eu mesmo relatei ao professor**: o histórico
   operacional (RF16/RF17, `mvp/dados/historico-operacional.csv`) só
   registrava o reator principal (R-01) — `HistoricoSubscriber` descartava
   explicitamente qualquer evento de outro reator, porque os ids de sensor
   (1 a 4) se repetem em todos os 5 reatores e sem isolar o estado por reator
   o ciclo de um se misturava com o de outro.
   - `HistoricoSubscriber` agora mantém um ciclo de leitura por reator
     (`EstadoCiclo`), em vez de um único estado global.
   - `SnapshotOperacional` e `FiltroHistorico` ganharam o campo `reatorId`;
     `/api/historico`, `/api/historico/dias` e `/api/historico/exportar`
     aceitam `?reator=R-0X` (default: todos os 5).
   - CSV antigo (sem a coluna `reator_id`) continua carregando normalmente —
     linhas legadas são tratadas como R-01, que é o que de fato eram.
   - Toda a suíte de testes do `mvp/` (`TestesMvp`, `TestesReatores`,
     `TestesHistorico`, `TestesApiHistorico`, `TestesApiAlarmes`,
     `TestesApiEstado`, `TestesAcesso`) passa 100% depois da mudança; o teste
     que antes validava o bug como comportamento esperado
     (`TestesReatores`) foi atualizado para validar a cobertura correta dos 5
     reatores.
   - API (`:8080`) recompilada e reiniciada com o fix; trilha de auditoria
     não foi tocada.

3. **Migração completa para banco de dados** — decidi não implementar sozinho
   o schema definitivo das 3 tabelas (reator, leitura/histórico, auditoria):
   é uma decisão de time (o próprio professor perguntou "que banco vocês
   estão pensando em usar?" e não foi respondido com clareza), e a infra
   já existe pronta em `backend/` (Spring Data JPA + Flyway, H2/Postgres) —
   inclusive o protocolo de contingência e o 2FA/biometria já usam essa
   mesma infra. Preferi deixar o design para a divisão de tarefas abaixo, em
   vez de impor um schema sem o time revisar.

## Divisão de tarefas da semana (5 integrantes)

- **Bernardo (eu)** — migrar leituras de sensor/histórico (`mvp/`) para uma
  tabela nova, reaproveitando a infra JPA/Flyway de `backend/`.
- **Bruno** — cadastro de reator em banco (substituindo o `List.of(...)`
  hardcoded) + trigger de push para leitura crítica.
- **José** — resolver o conflito da PR #94 (merge de `desenvolvimento` na
  `jose`) e liderar o documento de padrões de projeto (GoF + implícitos de
  framework) pedido pelo professor, incluindo os maiores candidatos a
  refatoração (`SistemaMvpUI.java`, 1178 linhas, é o maior).
- **Marcus** — migrar a auditoria para banco (mantendo o encadeamento
  SHA-256) + terminar de integrar o protocolo de contingência (já pronto em
  `backend/.../emergencias/`) ao `EventBus`/sistema do `mvp/`.
- **Álvaro** — atualizar a própria branch (fast-forward simples) + apoiar o
  Marcus na integração do protocolo de contingência + revisar os critérios de
  aceite MoSCoW à luz dos novos requisitos de persistência.

## Verificação

- `javac`/`java -cp out br.edu.unipampa.usina.app.Testes*` — todas as 7
  suítes do `mvp/` passando depois da mudança no histórico.
- `curl http://localhost:8080/api/estado` — `200` após reiniciar a API com o
  fix aplicado.
- PR #94: base corrigida (`gh pr view 94`), conflito real confirmado e
  comunicado ao José, não resolvido às cegas.

## Atualização (17/11)

- Fix do histórico multi-reator mesclado em `desenvolvimento` via
  [PR #95](https://github.com/rlampago22/RP-IV/pull/95).
- Conflito da PR #94 (José) resolvido: a branch `jose` estava 25+ commits
  atrás e tinha divergido em `AlarmeEstado.java` (ciclo de vida do alarme,
  campo `resolvidoEm`/`resolvidoPor`) e em telas do front (confirmação de
  alarme vs. seletor de reator). Mesclei `desenvolvimento` em `jose`,
  combinando as duas features (ex.: `comReconhecimento`/`comResolucao` agora
  também carregam `reatorId`), rodei as 7 suítes do `mvp/` + build do
  frontend, e enviei (`push`). PR #94 está `CLEAN`/`MERGEABLE`, pronta pra
  mergear.
- Issues da semana criadas para os 5 integrantes:
  [#96](https://github.com/rlampago22/RP-IV/issues/96) (Bruno),
  [#97](https://github.com/rlampago22/RP-IV/issues/97) (Bernardo),
  [#98](https://github.com/rlampago22/RP-IV/issues/98) (José),
  [#99](https://github.com/rlampago22/RP-IV/issues/99) (Marcus),
  [#100](https://github.com/rlampago22/RP-IV/issues/100) (Álvaro).

## Atualização (21/11) — issue #97 entregue

PR #94 e #95 já foram mescladas pelo time durante a semana. Ao retomar a
[#97](https://github.com/rlampago22/RP-IV/issues/97), encontrei a
`desenvolvimento` 18 commits à frente de `bernardo` — o Marcus já tinha
entregue a [#99](https://github.com/rlampago22/RP-IV/issues/99) (auditoria em
banco) com um padrão diferente do que eu havia esboçado: `mvp/` conecta
**direto** no banco via JDBC (`ConexaoBanco`), sem ponte HTTP pro `backend/`,
com as tabelas no schema `nucleo` criado idempotentemente pelos dois lados
(núcleo e Flyway). Refeito o trabalho do dia para seguir essa mesma convenção,
em vez de duas arquiteturas divergentes para o mesmo problema:

- `RegistroHistorico` reescrito para JDBC puro via `ConexaoBanco` — mesma
  estrutura de `RegistroAuditoria` (`abrirPadrao()`, `garantirTabela()`
  idempotente, import do CSV legado uma única vez, `AutoCloseable`), tabela
  `nucleo.historico_operacional`.
- Migração Flyway `V7__criar_historico_operacional_nucleo.sql` (V6 já estava
  ocupada pela auditoria do Marcus).
- Removida a ponte HTTP que eu tinha prototipado antes de ver o trabalho do
  Marcus: `EncaminhadorHistoricoSpring`, o proxy em `ApiEstadoHttpServer` e o
  pacote `backend/.../historico/` inteiro (entidade/repositório/controllers
  JPA) — sem necessidade, já que o núcleo lê/escreve direto no banco.
- Docker Compose + `.env.example` (Postgres compartilhado do time) mantidos:
  continuam valendo independente da arquitetura de persistência escolhida.
- As 7 suítes do `mvp/` voltaram a passar 100% (rodadas com o driver H2 no
  classpath, já que os testes agora abrem banco de verdade via
  `ConexaoBanco.emMemoria(...)`, mesmo padrão usado pelos testes da auditoria).
- `backend/` compila limpo; a falha de `ProtocoloContingenciaTest` (contexto
  Spring) é pré-existente — confirmada rodando a suíte com minhas mudanças
  stashadas, no mesmo commit de base.

## Fechamento (21/11) — issue #97 concluída

- [PR #102](https://github.com/rlampago22/RP-IV/pull/102) mesclada em
  `desenvolvimento`; issue
  [#97](https://github.com/rlampago22/RP-IV/issues/97) fechada.
- Docker Desktop estava com uma pasta de sockets internos corrompida
  (`AppData\Local\Docker\run`), travando a subida do daemon — resolvido
  renomeando a pasta (apagar os arquivos um a um falhava, a pasta inteira
  não). Com o daemon de pé, `docker compose up -d` subiu o Postgres real
  (porta `5433` nesta máquina — a `5432` padrão já estava em uso por outro
  projeto meu).
- Testado ponta a ponta com Postgres real: backend (perfil `postgres`,
  migrações V1–V7 aplicadas) + núcleo `mvp/` (JDBC direto) + frontend, todos
  apontando pro mesmo container. Disparei um cenário crítico no R-03 pelo
  Demo e confirmei em 3 camadas — `psql` na tabela
  `nucleo.historico_operacional`, `GET /api/historico?reator=R-03` e a tela
  de Histórico — que a linha gravada tem o reator, a temperatura e os
  sensores em alarme corretos.
- Esse teste manual revelou um bug real e pré-existente na tela de
  Histórico do frontend: o seletor de reator nunca repassava `?reator=` pra
  API (ficou faltando desde a PR #95, 16/11), e ainda mostrava um aviso fixo
  dizendo que só cobria o R-01. Corrigido no mesmo PR — ver commit
  `fix(frontend): filtrar historico por reator de verdade (RF16/RF17)`.
