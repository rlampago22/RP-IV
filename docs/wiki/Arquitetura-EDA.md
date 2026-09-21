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

| Evento | Origem típica |
|--------|----------------|
| `MedicaoRegistrada` | ControleReator |
| `ObservacaoRegistrada` | UC01 Alt. 1 |
| `FalhaSensorDetectada` | UC01 Exceção |
| `AlarmeEmitido` | Alarmes / limiar |
| `AlarmeReconhecido` | Operador |
| `AlarmeResolvido` | Operador / engenheiro |

## Onde está no código

- Executável Must: pasta `mvp/`
- Spec: [`docs/marco1/04-arquitetura-eda.md`](https://github.com/rlampago22/RP-IV/blob/desenvolvimento/docs/marco1/04-arquitetura-eda.md)
- Destino API HTTP: `backend/` (migração gradual)

**Não** introduzir segunda arquitetura (ex.: hexagonal paralelo) sem decisão do grupo.
