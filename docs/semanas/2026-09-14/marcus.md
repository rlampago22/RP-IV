# Relatório semanal — Marcus Querol

- **Semana:** 2 (entrega 14/09/2026)
- **Issue:** [#20 — AuditoriaSubscriber append-only](https://github.com/rlampago22/RP-IV/issues/20)
- **Trilha:** auditoria, eventos, UCs e MVP
- **Data:** 2026-09-14

## O que entreguei

- Implementação do `AuditoriaSubscriber` append-only consumindo todos os eventos do `EventBus`;
- Implementação de `RegistroAuditoria` com persistência física em disco (`dados/auditoria.log`), encadeamento de hash SHA-256 (RNF05) e verificação física de adulteração direta no arquivo (RNF03);
- Suporte à continuidade multi-sessão (RNF09), carregando a cadeia existente no startup para preservar o sequencial ($N+1$) e a rastreabilidade histórica;
- Isolamento de falhas no `EventBus` (RNF04), protegendo o barramento contra falhas ou exceções em assinantes defeituosos;
- Eventos de domínio integrados: `MedicaoRegistrada`, `ObservacaoRegistrada` (Fluxo Alternativo 1 do UC01), `FalhaSensorDetectada` (Fluxo de Exceções do UC01), `AlarmeEmitido`, `AlarmeReconhecido` e `AlarmeResolvido` (ciclo de validação do operador e engenheiro do UC01);
- Suíte automatizada com 11 verificações formais (`TestesMvp.java`) cobrindo 100% dos critérios pass/fail do checklist de aceite Must do Álvaro (`docs/mvp/checklist-aceite-must.md`);
- Interface gráfica SCADA industrial de supervisão (`SistemaMvpUI.java`) com renderização vetorial nativa em Java2D, radar animado do núcleo, monitoramento em tempo real contínuo e gestão de alarmes.

## Evidências

- Issue: [#20](https://github.com/rlampago22/RP-IV/issues/20)
- Pasta dedicada do MVP: [`mvp/`](../../../mvp/)
- Código do MVP centralizado em `mvp/src/`:
  - `mvp/src/br/edu/unipampa/usina/auditorialogs/AuditoriaSubscriber.java`
  - `mvp/src/br/edu/unipampa/usina/auditorialogs/RegistroAuditoria.java`
  - `mvp/src/br/edu/unipampa/usina/auditorialogs/EntradaAuditoria.java`
  - `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/EventBus.java`
  - `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/MedicaoRegistrada.java`
  - `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/ObservacaoRegistrada.java`
  - `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/FalhaSensorDetectada.java`
  - `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/AlarmeEmitido.java`
  - `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/AlarmeReconhecido.java`
  - `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/AlarmeResolvido.java`
  - `mvp/src/br/edu/unipampa/usina/alarmes/Alarme.java`
  - `mvp/src/br/edu/unipampa/usina/alarmes/AlarmeFacade.java`
  - `mvp/src/br/edu/unipampa/usina/alarmes/AlarmeFactory.java`
  - `mvp/src/br/edu/unipampa/usina/alarmes/AvaliadorFaixaSegura.java`
  - `mvp/src/br/edu/unipampa/usina/controlereator/Sensor.java`
  - `mvp/src/br/edu/unipampa/usina/controlereator/ReatorFacade.java`
  - `mvp/src/br/edu/unipampa/usina/app/TestesMvp.java`
  - `mvp/src/br/edu/unipampa/usina/app/SistemaMvpUI.java`
  - `mvp/src/br/edu/unipampa/usina/app/DemoMvp.java`
- Scripts e documentação na pasta `mvp/`:
  - `mvp/0-ABRIR-SISTEMA-GRAFICO.vbs`
  - `mvp/1-EXECUTAR-MVP.bat`
  - `mvp/2-TESTAR-MVP.bat`
  - `mvp/3-ABRIR-LOG-AUDITORIA.bat`
  - `mvp/abrir-sistema.ps1`
  - `mvp/executar-mvp.ps1`
  - `mvp/README.md`
  - `mvp/ROTEIRO-APRESENTACAO.md`
  - `mvp/BASE-NO-DOCUMENTO.md`
  - `mvp/DIAGRAMA-ARQUITETURA.txt`

## Verificação realizada

Executada a suíte `TestesMvp.java` via `mvp\2-TESTAR-MVP.bat`:
- **Resultado:** 11/11 verificações passaram com 100% de êxito (`[SUCESSO]`).
- Validados os critérios `CT-RF01` a `CT-RF06`, `CT-RNF01` a `CT-RNF05`, `CT-RNF09`, `CT-RNF10` e a detecção de adulteração intencional em disco com alerta `FALHA`.
- Executada a demo em memória do Bernardo na raiz (`scripts/run-demo-medicoes.bat`) com total compatibilidade mantida.

## Bloqueios / dúvidas

- Nenhum bloqueio. Entrega da Issue #20 e do MVP 100% concluída.

## Próxima semana (previsto)

- Issue #25 — Alinhamento das sequências e apoio na consolidação do Marco 1.
