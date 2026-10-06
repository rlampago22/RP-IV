# Contrato — API de Estado (EventBus → T01 Overview)

**Trilha:** Bruno (EventBus, API de estado) · Bernardo (seeds T02) · **Issues:** [#41](https://github.com/rlampago22/RP-IV/issues/41), [#44](https://github.com/rlampago22/RP-IV/issues/44) (S3), [#50](https://github.com/rlampago22/RP-IV/issues/50) (S4)
**Fonte:** `mvp/src/br/edu/unipampa/usina/apiestado/` · **Stub HTTP** (`com.sun.net.httpserver`, JDK puro — Zero External Dependencies, mesmo padrão do resto do `mvp/`)
**Consumidor:** `frontend/src/api/estado.js` (T01 Overview + T02 Sensores, Opção A)

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
| `POST` | `/api/cenarios/normal` | Pausa tempo-real e aplica seed Normal (`CenariosMedicao` → 6 leituras estáveis por sensor) |
| `POST` | `/api/cenarios/observacao` | Seed Observação (Normal + temp 328 °C → `ObservacaoRegistrada`) |
| `POST` | `/api/cenarios/falha` | Simula falha de telemetria no sensor de pressão (`FalhaSensorDetectada`) |
| `POST` | `/api/cenarios/critico` | Anomalia crítica: temp 372 °C + fluxo 420 → `AlarmeEmitido` |
| `POST` | `/api/demo/anomalia` | Alias do roteiro T03: mesmo efeito de `/api/cenarios/critico` |
| `POST` | `/api/alarmes/{id}/reconhecer` | Publica `AlarmeReconhecido` para o alarme `{id}` (operador fixo "Operador de Reator (UI Web)") |
| `POST` | `/api/alarmes/{id}/resolver` | Publica `AlarmeResolvido` para o alarme `{id}` (responsável fixo "Engenheiro de Turno (UI Web)") |
| `GET` | `/api/historico` | **RF16** — consulta filtrada do histórico operacional (ver seção 5) |
| `GET` | `/api/historico/exportar` | **RF17** — baixa o recorte filtrado em CSV (mesmos parâmetros da consulta) |
| `POST` | `/api/turno?operador=&papeis=` | Declara quem está de serviço, para o histórico registrar o operador |
| `POST` | `/api/turno/liberar` | Fim de turno: volta ao estado não identificado |

Todas as respostas (exceto erro de rota) devolvem o snapshot completo em JSON, para o front sempre re-sincronizar após uma ação. CORS liberado (`Access-Control-Allow-Origin: *`) para consumo local do Vite.

`/api/demo/anomalia` é somente um gatilho do roteiro acadêmico. Não representa uma operação disponível ao operador em produção.

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
      "atualizadoEm": "2026-09-20T20:37:44.331Z",
      "historico": [308.0, 309.0, 309.5, 310.0, 310.2, 310.5]
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
| `sensores[].historico` | `number[]` | Últimas até 6 leituras do sensor (antigo → atual), buffer no `EstadoAgregador` a partir de `MedicaoRegistrada` — alimenta sparkline/tabela da T02 |
| `contadores.medicoes` | `int` | Total de `MedicaoRegistrada` desde o start do processo |
| `contadores.alarmes` | `int` | Total de `AlarmeEmitido` (não decresce ao resolver) |
| `contadores.auditoria` | `int` | Total de eventos de qualquer tipo publicados no bus (equivalente ao que `AuditoriaSubscriber` grava) |
| `alarmes[]` | array | Todos os alarmes emitidos na sessão, com `status` `ATIVO \| RECONHECIDO \| RESOLVIDO`; `operador`/`solucao` só preenchidos após reconhecer/resolver |
| `eventos[]` | array | Timeline dos últimos 20 eventos do bus, mais recente primeiro (`MEDICAO_REGISTRADA`, `OBSERVACAO_REGISTRADA`, `FALHA_SENSOR_DETECTADA`, `ALARME_EMITIDO`, `ALARME_RECONHECIDO`, `ALARME_RESOLVIDO` — os 6 eventos obrigatórios de [`ui/direcao-opcao-a.md`](ui/direcao-opcao-a.md)) |

## 5. Consulta do histórico operacional (`GET /api/historico`) — RF16

Fonte: `mvp/src/.../historicooperacional/`. O `HistoricoSubscriber` grava uma linha a cada **ciclo completo** dos 4 sensores RF-1 em `mvp/dados/historico-operacional.csv` (append-only, recuperável entre sessões — RNF09). A rota consulta esse arquivo.

> **Por que snapshot por ciclo, e não uma linha por medição:** uma `MedicaoRegistrada` pertence a um sensor só. No modelo por medição, filtrar "temperatura entre X e Y **e** pressão entre W e Z" retornaria sempre vazio. Gravando os 4 parâmetros do mesmo ciclo numa linha, o filtro combinado passa a fazer sentido.

### Parâmetros (todos opcionais, combinam com **E**)

| Parâmetro | Tipo | Observação |
|---|---|---|
| `de` / `ate` | ISO-8601 | Limites do período, inclusivos |
| `temperaturaMin` / `temperaturaMax` | decimal | Faixa em °C |
| `pressaoMin` / `pressaoMax` | decimal | Faixa em bar |
| `radiacaoMin` / `radiacaoMax` | decimal | Faixa em mSv/h |
| `fluxoMin` / `fluxoMax` | decimal | Faixa em m³/h |
| `operador` | texto | Casa por trecho, sem distinguir maiúsculas |
| `limite` | int | Padrão 500, teto 5000 |

Critério malformado (faixa não numérica, data fora do ISO-8601, faixa ou período invertidos) devolve **HTTP 400** com `{"erro":"..."}` — nunca 500, e nunca um resultado vazio silencioso que o operador leria como "não há registros".

### Resposta

```json
{
  "total": 142,
  "retornados": 142,
  "truncado": false,
  "filtro": {
    "de": "2026-10-05T12:00:00Z", "ate": null,
    "temperatura": { "min": 310.0, "max": 320.0 },
    "pressao": { "min": null, "max": null },
    "radiacao": { "min": null, "max": null },
    "fluxo": { "min": null, "max": null },
    "operador": null
  },
  "registros": [
    {
      "instante": "2026-10-05T14:04:17.383Z",
      "temperatura": 312.0, "pressao": 155.17,
      "radiacao": 2.41, "fluxoResfriamento": 1102.14,
      "alarmesAtivos": 0, "alarmesReconhecidos": 0, "observacaoPendente": true,
      "status": "ATENCAO",
      "operador": "Ana Operadora",
      "papeisOperador": ["OPERADOR_REATOR"],
      "origemOperador": "DECLARADO_PELA_UI"
    }
  ]
}
```

| Campo | Regra |
|---|---|
| `total` | Linhas que casaram com o filtro |
| `retornados` | Quantas vieram nesta resposta (pode ser menor que `total`) |
| `truncado` | `true` quando o limite cortou — a interface deve avisar, para o operador não achar que viu tudo |
| `status` | **Derivado**, não armazenado: `CRITICO` se há alarme ativo; senão `ATENCAO` se há alarme reconhecido ou observação pendente; senão `ESTAVEL`. Mesma regra do `EstadoAgregador` |
| `origemOperador` | `NAO_IDENTIFICADO` · `DECLARADO_PELA_UI` · `SESSAO_VALIDADA` |

### Exportação (`GET /api/historico/exportar`) — RF17

Aceita **os mesmos parâmetros de filtro** da consulta e devolve o recorte como arquivo:

```
Content-Type: text/csv; charset=utf-8
Content-Disposition: attachment; filename="historico-operacional-2026-10-05T14-30-00Z.csv"
```

O conteúdo usa exatamente o mesmo cabeçalho e formato de linha do arquivo persistido — quem abre o download vê a mesma estrutura de `mvp/dados/historico-operacional.csv`.

Duas decisões registradas:

- **O parâmetro `limite` é ignorado aqui.** A consulta limita para não travar o navegador; a exportação não pode, porque um arquivo cortado em silêncio é pior que nenhum arquivo. Quem exporta leva o conjunto completo que casou com o filtro.
- **O arquivo começa com BOM UTF-8.** Sem ele o Excel não reconhece a codificação e nomes acentuados de operador (ex.: "Supervisão Central", que é um dos papéis do bootstrap) saem corrompidos na planilha.

### Operador de serviço (`POST /api/turno`)

`?operador=<nome>&papeis=<A|B>` (aceita `|` codificado como `%7C`, ou vírgula). `POST /api/turno/liberar` encerra o turno.

> **A origem é sempre `DECLARADO_PELA_UI`.** Esta rota não valida sessão — a autenticação roda em outro processo (Spring Boot em `backend/`, porta 8081, sessão por cookie server-side, fora do alcance do `mvp/`). Por isso ela não pode afirmar `SESSAO_VALIDADA`: senão um `curl` se passaria por identidade verificada. `SESSAO_VALIDADA` fica reservado para quando a autenticação for integrada.
>
> Consequência: o campo `operador` no histórico é **conveniência de leitura**, não prova. A fonte auditável de quem estava de serviço é a tabela `registro_autenticacao` do backend de autenticação, que grava cada login com `registrado_em`.

## 6. Consumo no frontend

`frontend/src/api/estado.js` define `ESTADO_MOCK` (mesmo formato acima) usado como fallback quando o `fetch` falha, e `frontend/src/state/EstadoContext.jsx` faz polling a cada 3s. T01 (`OverviewPage.jsx`), T02 (`SensoresPage.jsx` — seeds Normal/Observação + histórico) e o shell (badge/banner em `main.jsx`) leem exclusivamente desse contexto.

## 7. Testes

```bat
cd mvp
2-TESTAR-MVP.bat
```

O script compila e roda todos os suites em sequência, abortando no primeiro que falhar.

> O `javac` não expande `**`, e o `cmd` também não — um comando como `javac ... src\br\edu\unipampa\usina\**\*.java` falha com `error: Invalid filename`. O `executar-mvp.ps1` resolve isso com `Get-ChildItem -Recurse`. Use o `.bat`.

| Suite | Cobre |
|---|---|
| `TestesMvp` | 11 cenários Must (RF01–RF06, RNF03/04/05/09) + histórico curto do T02 |
| `TestesApiEstado` | `EstadoAgregador`/`EstadoJson`: transições ESTÁVEL → ATENÇÃO → CRÍTICO, ciclo do alarme e campos obrigatórios do JSON |
| `TestesApiAlarmes` | Ciclo HTTP do alarme: anomalia → reconhecer → resolver |
| `TestesHistorico` | Amostragem por ciclo, status derivado, operador de serviço e recuperação após reinício (RNF09) |
| `TestesApiHistorico` | `GET /api/historico`: período, faixas, combinação **E**, operador, limite/truncamento, HTTP 400 em critério inválido, e as rotas de turno |
