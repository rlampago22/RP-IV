# 07 — Diagramas de Sequência (núcleo MVP)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Todas as mensagens referenciam métodos existentes em [06-classes-projeto-mvp.md](06-classes-projeto-mvp.md).

Fontes PlantUML: [diagramas/seq-uc01-medicao.puml](diagramas/seq-uc01-medicao.puml), [diagramas/seq-uc02-alarme.puml](diagramas/seq-uc02-alarme.puml)

---

## SEQ-UC01 — Registrar Medição (fluxo principal até include de alarme)

```mermaid
sequenceDiagram
  participant Sensor
  participant ReatorFacade
  participant MedicaoReator
  participant ReatorRepository
  participant EventBus
  participant AvaliadorLimiar
  participant AlarmeFacade
  participant AuditoriaSubscriber

  Sensor->>ReatorFacade: receberLeitura(sensorId, valores)
  ReatorFacade->>MedicaoReator: registrarMedicao(valor, sensor, ts)
  ReatorFacade->>ReatorRepository: salvarMedicao(medicao)
  ReatorFacade->>EventBus: publicar(MedicaoRegistrada)
  EventBus->>AuditoriaSubscriber: onEvento(MedicaoRegistrada)
  AuditoriaSubscriber->>AuditoriaSubscriber: RegistroAuditoria.registrar
  ReatorFacade->>AvaliadorLimiar: avaliar(medicao, limiar)
  AvaliadorLimiar-->>ReatorFacade: ResultadoAvaliacao
  ReatorFacade->>AlarmeFacade: processarAvaliacao(resultado)
```

---

## SEQ-UC02 — Emitir Alarme (limiar violado)

```mermaid
sequenceDiagram
  participant AlarmeFacade
  participant AlarmeFactory
  participant Alarme
  participant ReatorRepository
  participant EventBus
  participant Operador
  participant Supervisao
  participant AuditoriaSubscriber

  AlarmeFacade->>AlarmeFactory: criar(medicao, tipo)
  AlarmeFactory-->>AlarmeFacade: alarme
  AlarmeFacade->>Alarme: emitirAlerta([Operador, Supervisao])
  Alarme-->>Operador: notificação
  Alarme-->>Supervisao: notificação
  AlarmeFacade->>Alarme: registrarEvento()
  AlarmeFacade->>ReatorRepository: salvarAlarme(alarme)
  AlarmeFacade->>EventBus: publicar(AlarmeEmitido)
  EventBus->>AuditoriaSubscriber: onEvento(AlarmeEmitido)
```

---

## SEQ-UC02-A1 — Limiar não violado (alternativa)

1. `AlarmeFacade.processarAvaliacao` recebe `isViolado() == false`
2. Facade encerra sem `emitirAlerta`
3. Opcional: publica `AvaliacaoLimiarOk` para auditoria

---

## SEQ-UC04 (Should — rascunho)

`Guarda -> AcessoFacade.solicitarAcesso -> RegistroAcesso.registrar -> EventBus`  
Implementação e desenho detalhado no Marco 3.
