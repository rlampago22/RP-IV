# Relatório semanal — Bernardo Dorneles

- **Semana:** 1 (entrega 08/09/2026)
- **Issue:** #13
- **Trilha:** ControleReator, medições, PersistenciaReator
- **Data:** 2026-09-09

## O que entreguei

- Revisei o diagrama/classes do núcleo em `docs/marco1/06-classes-projeto-mvp.md` e o PlantUML `classes-projeto-mvp.puml`
- Confirmei o recorte Must da minha trilha (sem classes novas): `Reator`, `Sensor`, `MedicaoReator`, `ReatorFacade`, `Limiar`, `ReatorRepository`
- Confirmei no esqueleto Java que `MedicaoReator.registrarMedicao(...)` já existe (feedback APS)
- Documentei o recorte Bernardo na seção 0 do doc de classes

## Evidências

- Issue: https://github.com/rlampago22/RP-IV/issues/13
- Arquivos:
  - `docs/marco1/06-classes-projeto-mvp.md`
  - `docs/marco1/diagramas/classes-projeto-mvp.puml` (revisado; sem mudança estrutural)
  - `src/main/java/.../controlereator/MedicaoReator.java` (contrato ok)
  - `docs/semanas/2026-09-08/bernardo.md` (este relatório)

## O que vou codar nas próximas semanas (5–8 linhas)

1. Semana 2: `ReatorFacade.receberLeitura` registrando medição em memória  
2. Usar `MedicaoReator.registrarMedicao` no fluxo real  
3. Manter `Reator`/`Sensor` só como entidades do núcleo  
4. Semana 3: histórico consultável (lista em memória)  
5. Ajustar SEQ-UC01 se o código mudar  
6. Semana 4: checklist curto da demo + 2–3 medições seed  
7. Persistência em arquivo só se sobrar tempo (não bloqueia)

## Bloqueios / dúvidas

- Nenhum. Dependência natural: EventBus (Bruno) e Strategy de limiar (José) nas semanas seguintes.

## Próxima semana (previsto)

- Issue #18 — `receberLeitura` + medição em memória
