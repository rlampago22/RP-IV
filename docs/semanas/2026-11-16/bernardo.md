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

## Pendências

- José ainda precisa resolver o conflito da PR #94 e mergear.
- Migração para banco (reator/sensor-histórico/auditoria) é trabalho da
  semana, não concluído ainda — este doc define só a divisão, não a entrega.
- Commit/push deste trabalho (branch `bernardo`) ainda não feito.
