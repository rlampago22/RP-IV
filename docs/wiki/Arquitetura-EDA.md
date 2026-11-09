# Arquitetura EDA

**Única arquitetura do projeto:** Orientada a Eventos (EDA).

## Ideia

Módulos desacoplados publicam/consomem eventos via **EventBus** in-process (Java).

```text
Sensor → ReatorFacade → MedicaoRegistrada → EventBus
                              ↓
                    AvaliadorLimiar / Alarmes
                              ↓
                         AlarmeEmitido
                              ↓
                      AuditoriaLogs (SHA-256)
                              ↓
                    API estado → React (T01–T05)
```

## Eventos principais

| Evento | Publicador | Consumidores no MVP |
|--------|------------|---------------------|
| `MedicaoRegistrada` | `ReatorFacade` | `AlarmeFacade` (assinatura por tipo), `EstadoAgregador`, `AuditoriaSubscriber`, `HistoricoSubscriber` |
| `ObservacaoRegistrada` | `ReatorFacade` | `EstadoAgregador`, `AuditoriaSubscriber`, `HistoricoSubscriber` |
| `FalhaSensorDetectada` | `ReatorFacade` | `EstadoAgregador`, `AuditoriaSubscriber`, `HistoricoSubscriber` |
| `AlarmeEmitido` | `AlarmeFacade` após avaliar a medição | `EstadoAgregador`, `AuditoriaSubscriber`, `HistoricoSubscriber` |
| `AlarmeReconhecido` | `AlarmeFacade` após ação do operador | `EstadoAgregador`, `AuditoriaSubscriber`, `HistoricoSubscriber` |
| `AlarmeResolvido` | `AlarmeFacade` após ação de resolução | `EstadoAgregador`, `AuditoriaSubscriber`, `HistoricoSubscriber` |
| `AcessoRegistrado` | `AcessoFacade` | `EstadoAgregador`, `AuditoriaSubscriber` |

`AlarmeFacade` assina `MedicaoRegistrada` com `EventBus.assinar(tipo, subscriber)`. Já os três consumidores globais de alarmes usam `EventBus.assinarTodos`: o estado atualiza o snapshot/timeline, a auditoria persiste cada fato e o histórico registra transições operacionais quando os valores de sensores necessários estão disponíveis. Portanto, `AlarmeFacade` publica os três eventos do ciclo de vida; ela não é consumidora desses eventos.

No código, **Strategy** (`AvaliadorLimiar` / `AvaliadorFaixaSegura`) decide se a medição ultrapassa a faixa segura, e **Factory** (`AlarmeFactory`) cria a entidade `Alarme`. A **Facade** (`AlarmeFacade`) orquestra essas etapas e publica os fatos no `EventBus`.

## Onde está no código

- Executável Must: pasta `mvp/`
- Spec: [`docs/marco1/04-arquitetura-eda.md`](https://github.com/rlampago22/RP-IV/blob/desenvolvimento/docs/marco1/04-arquitetura-eda.md)
- Destino API HTTP: `backend/` (migração gradual)

**Não** introduzir segunda arquitetura (ex.: hexagonal paralelo) sem decisão do grupo.
