# Visão Geral

## O que é

Central de **supervisão acadêmica** de um reator nuclear (Reator-01):

1. Coleta contínua dos **4 sensores RF-1** (temperatura, pressão, radiação, fluxo)
2. Avaliação de limiar → emissão de **alarmes**
3. Ciclo operador: **reconhecer** → **resolver**
4. **Auditoria** append-only com hash SHA-256 encadeado
5. UI web de supervisão (**Opção A — SCADA escuro**)

## MVP Must (demo)

```text
T05 Cenário → EventBus → T01 Overview
                      → T03 Alarmes (ACK / Resolver)
                      → T04 Auditoria (trilha)
```

Cenários oficiais (roteiro Marcus):

| Cenário | Evento principal |
|---------|------------------|
| Normal / Tempo Real | `MEDICAO_REGISTRADA` |
| Observação (Alt. 1) | `OBSERVACAO_REGISTRADA` |
| Falha Sensor | `FALHA_SENSOR_DETECTADA` |
| Anomalia Crítica | `ALARME_EMITIDO` |

## Telas (T01–T05)

| ID | Tela | Dono |
|----|------|------|
| T01 | Overview | Bruno |
| T02 | Sensores | Bernardo |
| T03 | Alarmes | José |
| T04 | Auditoria | Marcus |
| T05 | Demo / Cenários | Marcus |

Detalhes: [UI Opção A](UI-Opcao-A.md) · [Marco 1](Marco-1.md)
