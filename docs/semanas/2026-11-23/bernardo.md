# Semana 23/11 — Bernardo Dorneles

**Contexto:** reunião do grupo com o professor em 23/11. Ele revisou a
entrega da semana (histórico/auditoria/cadastro de reator em banco,
documentação de padrões de projeto) e deixou 5 pontos de feedback para a
próxima semana: (1) um trigger real de banco de dados para eventos
críticos, não só o encaminhamento via EventBus/HTTP que já existe; (2)
renomear "retirar" reator para "Desativar", já que o comportamento já é de
desativação suave (preserva histórico); (3) tornar severidade/protocolo
configuráveis por domínio em vez de hardcoded ao cenário nuclear; (4)
continuar a refatoração dos arquivos grandes (`SistemaMvpUI.java`,
`ApiEstadoHttpServer.java`) já mapeada em `docs/marco2/04-padroes-projeto.md`;
(5) — como exploração opcional, o próprio professor disse não ser essencial —
avaliar um app mobile (Flutter) para notificação de alertas críticos.

Antes de dividir as tarefas, levantei o estado real do código para cada um
dos 5 pontos (arquivos, linhas, o que já existe pronto) para que cada issue
aponte para o código de verdade em vez de pedir algo genérico.

## Divisão de tarefas da semana (5 integrantes)

- **Bruno** — trigger real de banco (Postgres) para a contingência,
  substituindo o polling de 1s do `SincronizadorContingencia` por um
  `CREATE TRIGGER`/`pg_notify` que dispara o push já existente
  (`EncaminhadorEventosSpring` → `EventoPushController`/SSE). Precisa
  também atualizar `docs/api-estado-contrato.md` (hoje documenta
  explicitamente que não há trigger de banco).
- **Álvaro** — renomear "retirar"/"retirado" para "Desativar"/"Desativado"
  em todas as camadas (backend, núcleo, frontend) — é rename de
  terminologia, o comportamento (soft-deactivate, histórico preservado,
  R-01 protegido) já está correto. Como exploração secundária e opcional,
  uma nota curta de arquitetura sobre um futuro app mobile (Flutter) para
  alertas críticos, a partir do ponto de extensão já existente
  (`CanalCelular`).
- **José** — severidade configurável por domínio: estender o Strategy
  `AvaliadorLimiar` para usar as faixas de atenção (`atencao_minima`/
  `atencao_maxima`) que já estão salvas no banco mas hoje são ignoradas na
  avaliação de alarme, substituindo a severidade hardcoded `"CRITICA"` por
  um grau real. Atualizar `docs/marco2/04-padroes-projeto.md` de acordo.
- **Marcus** — executar a metade do `ApiEstadoHttpServer.java` do plano de
  refatoração que o José já desenhou (Router/Front-Controller por
  recurso), preservando o contrato `/api/estado` e as suítes de teste.
- **Bernardo (eu)** — a outra metade do item 4: refatorar
  `SistemaMvpUI.java` para um formato mais MVC (`ControladorCentral` +
  componentes Swing separados), mesmos invariantes do doc de padrões.

## Referência

Issues criadas para os 5 integrantes a partir desta divisão; estilo e
convenções seguem `docs/semanas/2026-11-16/bernardo.md` e as issues
#96–#100 e #107.
