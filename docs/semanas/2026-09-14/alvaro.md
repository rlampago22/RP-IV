# Relatório semanal — Álvaro Domingues

- **Semana:** 2 (entrega 14/09/2026)
- **Issue:** #16
- **Trilha:** requisitos, MoSCoW e aceite do MVP
- **Data:** 2026-09-14

## O que entreguei

- versão Markdown consolidada da especificação de RF, RNF, MoSCoW e MVP;
- checklist pass/fail para RF01–RF06;
- critérios verificáveis para RNF01–RNF05, RNF09 e RNF10;
- tabela de resultado consolidado e registro de falhas;
- situação inicial da implementação, sem antecipar aprovação de cenários ainda não executados.

## Evidências

- Issue: [#16](https://github.com/rlampago22/RP-IV/issues/16)
- Arquivos:
  - [`../../mvp/especificacao-rf-rnf-moscow-mvp.md`](../../mvp/especificacao-rf-rnf-moscow-mvp.md)
  - [`../../mvp/checklist-aceite-must.md`](../../mvp/checklist-aceite-must.md)
- Branch: `alvaro`
- Commit: registrado no histórico desta entrega

## Verificação realizada

Foi feita uma tentativa de executar `scripts\run-demo-medicoes.bat`. A execução não iniciou porque `java` e `javac` não estavam disponíveis no `PATH` da máquina. Por isso, nenhum cenário foi marcado como `PASS`; o checklist exige evidência de execução antes da aprovação.

## Bloqueios e dúvidas

- instalar ou configurar um JDK para executar a demo localmente;
- definir com o grupo o limite mensurável de tempo para RNF01;
- executar novamente os cenários RF03–RF06 quando EventBus, limiar, alarme e persistência estiverem integrados.

## Próxima semana

- executar o checklist com a demo integrada;
- registrar resultados `PASS` ou `FAIL` e abrir issues para falhas;
- atualizar o checklist de feedback APS apenas para os itens do núcleo.
