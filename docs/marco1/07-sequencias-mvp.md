# 07 — Diagramas de Sequência (núcleo MVP)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Todas as mensagens referenciam métodos existentes no código do MVP e em [06-classes-projeto-mvp.md](06-classes-projeto-mvp.md).

Fonte UML editável: [Marcus-UML-MVP.asta](../marcus/diagramas/Marcus-UML-MVP.asta). As imagens foram exportadas pelo Astah.

---

## SEQ-UC01 — Registrar Medição (fluxo principal até include de alarme)

**Código (S3):** `ReatorFacade.receberLeitura` → `MedicaoReator.registrarMedicao` →
`ReatorRepository.salvarMedicao` (histórico curto em memória) → `EventBus.publicar(MedicaoRegistrada)`.
`AlarmeFacade` e `AuditoriaSubscriber` consomem o evento no barramento (EDA).

Consulta T02: `consultarHistorico()`, `consultarHistorico(sensorId)`, `consultarHistoricoRecente(n)`.

![Diagrama UML de sequência UC01](<../marcus/diagramas/Marcus-UML-MVP/02 - SEQ UC01 Registrar Medição.png>)

PlantUML espelho: [seq-uc01-medicao.puml](diagramas/seq-uc01-medicao.puml).

---

## SEQ-UC02 — Emitir Alarme (limiar violado)

![Diagrama UML de sequência UC02](<../marcus/diagramas/Marcus-UML-MVP/03 - SEQ UC02 Emitir Alarme.png>)

As notificações a Operador e Supervisão Central representam a saída de console realizada por `AlarmeFacade`; ainda não existe serviço externo de notificação nem persistência de alarme em banco.

---

## SEQ-UC02-A1 — Limiar não violado (alternativa)

Fluxo: medição registrada e auditada; `AvaliadorLimiar` retorna não violado; nenhum `AlarmeEmitido`.

---

## SEQ-UC02-A2 / exceção — Falha de sensor

`FalhaSensorDetectada` publicada no EventBus e registrada na auditoria (sem necessariamente emitir alarme ALTO).
