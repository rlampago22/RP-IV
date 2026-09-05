# 06 — Diagrama de Classes de Projeto (núcleo MVP)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Corrige inconsistências do feedback APS (classes/métodos faltantes nas sequências). Classes `«future»` listadas para rastreio, sem obrigatoriedade de sequência no Marco 1.

Fonte PlantUML: [diagramas/classes-projeto-mvp.puml](diagramas/classes-projeto-mvp.puml)

---

## 1. Classes do núcleo (Must)

### InfraestruturaEventos

| Classe | Responsabilidade | Métodos principais |
|--------|------------------|--------------------|
| `EventBus` | Pub/Sub in-process | `publicar(EventoDominio)`, `assinar(tipo, IEventSubscriber)` |
| `EventoDominio` | Fato imutável | `getTipo()`, `getTimestamp()`, `getPayload()` |
| `IEventPublisher` | Porta de publicação | `publicar(...)` |
| `IEventSubscriber` | Porta de consumo | `onEvento(EventoDominio)` |

### ControleReator

| Classe | Responsabilidade | Métodos principais |
|--------|------------------|--------------------|
| `ReatorFacade` | Fachada do módulo | `receberLeitura(...)`, `consultarHistorico(...)` |
| `Reator` | Entidade agregadora | `getId()`, `getStatusOperacional()`, `associarSensor(Sensor)` |
| `Sensor` | Entidade | `getId()`, `getTipo()`, `getStatus()`, `obterLocalizacao()` |
| `MedicaoReator` | Entidade | **`registrarMedicao(...)`**, `getValor()`, `getTimestamp()`, `getSensor()` |
| `AvaliadorLimiar` | Strategy | `avaliar(MedicaoReator, Limiar): ResultadoAvaliacao` |
| `Limiar` | Configuração | `getParametro()`, `getValorMax()`, `getValorMin()` |

### Alarmes

| Classe | Responsabilidade | Métodos principais |
|--------|------------------|--------------------|
| `AlarmeFacade` | Fachada | `processarAvaliacao(ResultadoAvaliacao)` |
| `Alarme` | Entidade | **`emitirAlerta(destinatarios)`**, **`registrarEvento()`**, `getStatus()`, `getTipo()` |
| `AlarmeFactory` | Factory | `criar(MedicaoReator, TipoAlarme): Alarme` |

### PersistenciaReator / Auditoria

| Classe | Métodos |
|--------|---------|
| `ReatorRepository` | `salvarMedicao`, `salvarAlarme`, `buscarHistorico` |
| `AuditoriaSubscriber` | `onEvento` |
| `RegistroAuditoria` | `registrar(EventoDominio)` |

---

## 2. Classes Should / Could (corrigem feedback; sequências só se implementadas)

| Classe | Marco | Métodos |
|--------|-------|---------|
| `RegistroAcesso` | Should | `registrar(credencial, area, resultado)`, `getResultado()` |
| `AcessoFacade` | Should | `solicitarAcesso(...)` |
| `Emergencia` | Could | **`criarEmergencia(...)`**, `getStatus()`, `getTipo()` |
| `ProtocoloEmergencia` | Could | `ativar()`, `getPassos()`, `associarEmergencia(Emergencia)` |
| `HistoricoSubstituicao` | Could/backlog | `registrarSubstituicao(...)` |
| `Localizacao` | backlog material | `atualizarPosicao(...)`, `getNivelSeguranca()` |
| `MaterialRadioativo` | backlog | `getTipo()`, `getLocalizacao()` |
| `Movimentacao` | backlog | **`registrarMovimentacao(...)`** na entidade (não só no controller) |

---

## 3. Regras de consistência (checklist do professor)

1. Toda mensagem em diagrama de sequência do MVP referencia método existente nesta lista.
2. Lógica de registro de domínio fica na **entidade** (`registrarMedicao`, `registrarEvento`, `registrar` em `RegistroAcesso`), não apenas no controller/facade.
3. Destinatários de `emitirAlerta` incluem Operador e Supervisão Central (modelo de análise / UC).

---

## 4. Relacionamentos principais (núcleo)

```text
Reator 1──* Sensor
Sensor 1──* MedicaoReator
MedicaoReator *──1 Limiar (por parâmetro avaliado)
Alarme *──1 MedicaoReator (origem)
Alarme *──1 TipoAlarme (tabela de domínio no ER)
EventBus ◄── ReatorFacade, AlarmeFacade, AuditoriaSubscriber
```
