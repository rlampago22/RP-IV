# Relatório semanal — Marcus Querol

- **Semana:** 1 (entrega de 08/09/2026)
- **Trilha:** Casos de uso e diagramas de sequência
- **Data:** 2026-09-07
- **Issue:** [#15 — UCs e sequência do núcleo](https://github.com/rlampago22/RP-IV/issues/15)

## O que entreguei

- UC01 com fluxo principal linear, sem decisões condicionais no caminho principal.
- Fronteira `SistemaControleUsina` preservada no diagrama de casos de uso.
- Inclusão obrigatória de UC02 (avaliar/emitir alarme) e UC03 (auditar evento).
- Separação da consulta ao histórico, pois ela representa outro objetivo de caso de uso.
- SEQ-UC01 alinhada às assinaturas do esqueleto Java, usando `receberLeitura(sensorId, valor)`.
- Entidade `RegistroAuditoria` representada em lifeline própria na sequência.

## Evidências

- Commit: `94f7506` (`docs: alinhar UC01 e sequencia ao nucleo`).
- Arquivos:
  - `docs/marco1/05-casos-uso-mvp.md`
  - `docs/marco1/07-sequencias-mvp.md`
  - `docs/marco1/diagramas/casos-de-uso-mvp.puml`
  - `docs/marco1/diagramas/seq-uc01-medicao.puml`
- Validação: `git diff --check` sem erros e conferência das mensagens contra as classes Java do núcleo.

## Bloqueios / dúvidas

- Nenhum bloqueio para a entrega desta semana.
- A implementação executável do fluxo permanece planejada para a Semana 2, conforme o escopo do Marco 1.

## Próxima semana (previsto)

- Implementar e demonstrar o `AuditoriaSubscriber` append-only da issue #20.
- Consumir eventos de medição e alarme e registrar a trilha de auditoria.
