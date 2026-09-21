# Entrega semanal — Marcus Querol

- **Semana / sprint:** S3 (2026-09-21)
- **Issue:** [#47](https://github.com/rlampago22/RP-IV/issues/47)
- **Branch:** `marcus`
- **Trilha:** UCs/sequências, auditoria, T04–T05

## 1. O que foi feito

- Backend/docs: SEQ-UC02 alinhado ao código e projeto Astah com UC01–UC03, SEQ-UC01 e SEQ-UC02.
- Frontend: T04 Auditoria passou a exibir a trilha compartilhada da demo; T05 passou a atualizar T01, T02, T03 e T04.
- Revisão: comparação do MVP local do Antigravity com `mvp/` e com o PDF legado.

## 2. Como foi feito

- UML criada como elementos nativos do Astah e exportada para PNG.
- React usa `useReducer` como estado compartilhado temporário para os quatro cenários.
- A documentação foi corrigida para diferenciar o que já roda do que ainda é evolução: EventBus síncrono, persistência apenas da auditoria e API HTTP pendente.

## 3. Como será aplicado

- O fluxo UC02 consome `MedicaoRegistrada`, avalia o limiar, cria o alarme, notifica Operador/Supervisão e publica `AlarmeEmitido`.
- `AuditoriaSubscriber` consome os eventos e registra a trilha append-only.
- Na demo web, um cenário em T05 modifica imediatamente Overview, Sensores, Alarmes e Auditoria.

## 4. O que foi entregue (DoD)

- [x] Auditoria Java existente revisada e testada em `mvp/`
- [x] T04 dinâmica em `frontend/`
- [x] T05 compartilhando estado com T01–T04
- [x] Astah e PNGs versionáveis em `docs/marcus/diagramas/`
- [x] Build React executada com sucesso
- [x] Testes Java do MVP executados com sucesso
- [x] PR [`#61`](https://github.com/rlampago22/RP-IV/pull/61) `marcus` → `desenvolvimento`

## 5. Bloqueios

- A API HTTP Java ainda não existe; a integração web usa estado simulado e explicitamente identificado.
- Medições e alarmes ainda não possuem repositório persistente; somente a auditoria sobrevive a reinicializações.

## 6. Próxima semana

- Concluir a issue #52: README passo a passo, integração com API disponível e polish de T04/T05.
