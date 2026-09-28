# Contrato — API de Estado (EventBus → T01 Overview)

**Trilha:** Bruno (EventBus, API de estado) · **Issues:** [#41](https://github.com/rlampago22/RP-IV/issues/41) (débito), [#44](https://github.com/rlampago22/RP-IV/issues/44) (S3)
**Fonte:** `mvp/src/br/edu/unipampa/usina/apiestado/` · **Stub HTTP** (`com.sun.net.httpserver`, JDK puro — Zero External Dependencies, mesmo padrão do resto do `mvp/`)
**Consumidor:** `frontend/src/api/estado.js` (T01 Overview, Opção A)

---

## 1. Como rodar

```bat
cd mvp
4-EXECUTAR-API-ESTADO.bat
```

Sobe em `http://localhost:8080`. Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

O frontend lê a URL base em `VITE_API_BASE_URL` (`frontend/.env.example`, padrão `http://localhost:8080`). Sem o backend rodando, o T01 cai automaticamente para o **mock local** (mesmo formato do contrato) e mostra um aviso — ver seção 4.

---

## 2. Por que um stub e não a migração completa para `backend/`

A migração `mvp/` → `backend/` (Maven/Gradle) é indicada em [`backend-java.md`](backend-java.md) como direção contínua, não obrigatória até o Marco 1. O DoD das issues #41/#44 aceita explicitamente **"mesmo stub ok se documentado"**. Este stub:

- não introduz dependências externas (mantém a filosofia do `mvp/`);
- expõe o estado **derivado apenas dos eventos do EventBus** (`EstadoAgregador`, que implementa `IEventSubscriber` e não conhece `ReatorFacade`/`AlarmeFacade` diretamente) — reforça a regra EDA de que API/Frontend são adaptadores que observam fatos publicados, não o domínio;
- é suficiente para o React consumir dado real e tipado em vez de valores hardcoded soltos no componente.

## 3. Endpoints

| Método | Rota | Efeito |
|--------|------|--------|
| `GET` | `/api/estado` | Retorna o snapshot atual (ver contrato JSON abaixo) |
| `POST` | `/api/tempo-real/iniciar` | Liga a simulação periódica de leituras (varia os 4 sensores a cada 3s) |
| `POST` | `/api/tempo-real/pausar` | Desliga a simulação periódica |
| `POST` | `/api/alarmes/{id}/reconhecer` | Publica `AlarmeReconhecido` para o alarme `{id}` (operador fixo "Operador de Reator (UI Web)") |
| `POST` | `/api/alarmes/{id}/resolver` | Publica `AlarmeResolvido` para o alarme `{id}` (responsável fixo "Engenheiro de Turno (UI Web)") |

Todas as respostas (exceto erro de rota) devolvem o snapshot completo em JSON, para o front sempre re-sincronizar após uma ação. CORS liberado (`Access-Control-Allow-Origin: *`) para consumo local do Vite.

> Simplificação assumida: as rotas de ação não recebem corpo (operador/justificativa fixos). Documentado aqui para não ser lido como "hardcode não documentado".

## 4. Contrato JSON (`GET /api/estado`)

```json
{
  "status": "ESTAVEL",
  "tempoReal": false,
  "atualizadoEm": "2026-09-20T20:37:47.415Z",
  "sensores": [
    {
      "id": 1,
      "tipo": "TEMPERATURA",
      "unidade": "Celsius",
      "valor": 310.5,
      "limiteMinimo": 0.0,
      "limiteMaximo": 350.0,
      "atualizadoEm": "2026-09-20T20:37:44.331Z"
    }
  ],
  "contadores": { "medicoes": 4, "alarmes": 0, "auditoria": 4 },
  "alarmes": [
    {
      "id": "D46AE2F2",
      "sensorId": 2,
      "severidade": "CRITICA",
      "mensagem": "PRESSAO fora da faixa segura: 200.00 bar",
      "status": "ATIVO",
      "criadoEm": "2026-09-20T20:38:01.000Z",
      "operador": null,
      "solucao": null
    }
  ],
  "eventos": [
    {
      "ocorridoEm": "2026-09-20T20:37:44.428Z",
      "tipo": "MEDICAO_REGISTRADA",
      "resumo": "sensor=4 tipo=FLUXO_RESFRIAMENTO valor=1100.00 m3/h faixa=[500.00, 1500.00]"
    }
  ]
}
```

### Campos

| Campo | Tipo | Regra |
|-------|------|-------|
| `status` | `"ESTAVEL" \| "ATENCAO" \| "CRITICO"` | `CRITICO` se houver alarme `ATIVO`; senão `ATENCAO` se houver alarme `RECONHECIDO` **ou** sensor com `ObservacaoRegistrada` pendente; senão `ESTAVEL` |
| `tempoReal` | `boolean` | Reflete `/api/tempo-real/iniciar` \| `/pausar` |
| `atualizadoEm` | `string` (ISO-8601) | Timestamp do snapshot (não do último evento) |
| `sensores[]` | array | Última leitura conhecida de cada sensor (`MedicaoRegistrada`); vazio até a 1ª leitura |
| `contadores.medicoes` | `int` | Total de `MedicaoRegistrada` desde o start do processo |
| `contadores.alarmes` | `int` | Total de `AlarmeEmitido` (não decresce ao resolver) |
| `contadores.auditoria` | `int` | Total de eventos de qualquer tipo publicados no bus (equivalente ao que `AuditoriaSubscriber` grava) |
| `alarmes[]` | array | Todos os alarmes emitidos na sessão, com `status` `ATIVO \| RECONHECIDO \| RESOLVIDO`; `operador`/`solucao` só preenchidos após reconhecer/resolver |
| `eventos[]` | array | Timeline dos últimos 20 eventos do bus, mais recente primeiro (`MEDICAO_REGISTRADA`, `OBSERVACAO_REGISTRADA`, `FALHA_SENSOR_DETECTADA`, `ALARME_EMITIDO`, `ALARME_RECONHECIDO`, `ALARME_RESOLVIDO` — os 6 eventos obrigatórios de [`ui/direcao-opcao-a.md`](ui/direcao-opcao-a.md)) |

## 5. Consumo no frontend

`frontend/src/api/estado.js` define `CONTRATO_MOCK` (mesmo formato acima) usado como fallback quando o `fetch` falha, e `frontend/src/state/EstadoContext.jsx` faz polling a cada 3s. O T01 (`OverviewPage.jsx`) e o shell (badge/banner em `main.jsx`) leem exclusivamente desse contexto — não há mais valores de sensor/alarme hardcoded soltos no componente.

## 6. Testes

```bat
cd mvp
javac -encoding UTF-8 -d out src\br\edu\unipampa\usina\**\*.java
java -cp out br.edu.unipampa.usina.app.TestesApiEstado
```

`TestesApiEstado` cobre: status inicial `ESTAVEL`, transição para `ATENCAO` via `ObservacaoRegistrada`, transição para `CRITICO` via `AlarmeEmitido`, volta para `ATENCAO`/limpeza ao reconhecer/resolver, e presença dos campos obrigatórios no JSON gerado. Não substitui `2-TESTAR-MVP.bat` (11/11 Must) — é específico do agregador de estado.
