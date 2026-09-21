# Central de Supervisão — MVP Usina Nuclear

> **Nota (set/2026):** esta UI **Swing** permanece como legado de fluxo/demo. O visual oficial do grupo passa a ser **React** (`frontend/`) + idealização em [`docs/ui/idealizacao-telas.md`](../docs/ui/idealizacao-telas.md). Arquitetura: **EDA only**.

Aplicação desktop local desenvolvida em Java (Zero External Dependencies) para demonstrar o caminho crítico completo do sistema conforme documentado no projeto oficial da disciplina de Resolução de Problemas IV / Análise e Projeto de Software.

---

## 🚀 Como Executar

### 1. Interface Gráfica (Recomendado para Apresentação)
- Dê dois cliques em **`0-ABRIR-SISTEMA-GRAFICO.vbs`** (ou execute `abrir-sistema.ps1` no PowerShell).
- A **Central de Supervisão do Reator Nuclear 01** será aberta com painel escuro de alta visibilidade e telemetria.

### 2. Suíte de Testes Automatizados
- Dê dois cliques em **`2-TESTAR-MVP.bat`** (ou execute `powershell -File .\executar-mvp.ps1 -Testar`).
- O resultado esperado é:
```text
[SUCESSO] 11 verificacoes de requisitos Must (RF01-RF06 / RNF01-RNF10) passaram com 100% de exito!
```

### 3. Demonstração em Linha de Comando
- Dê dois cliques em **`1-EXECUTAR-MVP.bat`** (ou execute `powershell -File .\executar-mvp.ps1`).

### 4. API de Estado (para o T01 Overview em React)
- Dê dois cliques em **`4-EXECUTAR-API-ESTADO.bat`** para subir `http://localhost:8080/api/estado`.
- Em outro terminal, rode `cd ..\frontend && npm install && npm run dev` para consumir o estado real no T01.
- Contrato JSON e demais rotas: [`../docs/api-estado-contrato.md`](../docs/api-estado-contrato.md).

---

## 🎮 Funcionalidades e Controles na Interface

| Controle / Botão | O que faz no Sistema | Requisito / UC Atendido |
|---|---|---|
| **Iniciar Tempo Real** | Ativa a coleta contínua automática via timer com oscilações físicas realistas. | **UC01 Passo 1 e 2** / **RF-1** |
| **Cenário Normal** | Define todos os 4 sensores (`Temperatura`, `Pressão`, `Radiação`, `Fluxo`) em valores seguros e estáveis. | **RF-1** (Operação Estável) |
| **Simular Observação (Alt. 1)** | Simula valores preventivos na faixa de atenção sem soar alarme crítico, auditando o evento. | **UC01 Fluxo Alternativo 1** |
| **Simular Anomalia (Crítico)** | Ultrapassa limites críticos, acionando alarme visual no núcleo (`CRÍTICO`) e notificando operadores. | **RF-2** / **UC01 Passo 4 a 7** |
| **Falha Sensor (Exceção)** | Simula perda de comunicação no barramento do sensor, emitindo alerta técnico para a equipe de manutenção. | **UC01 Fluxo de Exceções** |
| **Validar / Reconhecer Alerta** | Operador de Reator valida formalmente o alarme ativo no painel, alterando o status para `EM TRATAMENTO`. | **UC01** (Validação do Operador) |
| **Normalizar / Encerrar** | Engenheiro de Segurança registra a normalização dos parâmetros e o reator retorna a `ESTÁVEL`. | **UC01** (Resolução de Alarme) |
| **Registrar Leituras** | Envia manualmente para o barramento os valores ajustados nos seletores numéricos. | **RF-1** (Telemetria Flexível) |
| **Abrir Log** | Abre o arquivo `dados/auditoria.log` persistido em modo append-only com cadeia SHA-256. | **RNF-03 / RNF-05** |

---

## 🏛️ Arquitetura Implementada

```text
[Sensores Físicos Simulados: Temp, Pressão, Radiação, Fluxo]
                    │
                    ▼
            ReatorFacade (ControleReator)
                    │ (MedicaoRegistrada / ObservacaoRegistrada / FalhaSensorDetectada)
                    ▼
          ┌─────────────────────┐
          │  EventBus (EDA)     │ ───> [Isolamento de Falhas RNF-04]
          └─────────────────────┘
             │                │
             │ (Medições)     │ (Todos os Eventos de Domínio)
             ▼                ▼
     AlarmeFacade      AuditoriaSubscriber
     (Avalia Limiar)          │
             │                ▼
     (AlarmeEmitido)   RegistroAuditoria
             │         (dados/auditoria.log)
             │         └── Cadeia SHA-256 com Persistência Multi-Sessão
             ▼             e Verificação Física Anti-Adulteração
     [Notificação:
      Operador de Reator &
      Supervisão Central]
```

- **Padrões de Projeto Aplicados:** Facade, Observer/Pub-Sub, Strategy, Factory.
- **Rastreabilidade Completa:** Consulte `BASE-NO-DOCUMENTO.md`.
- **Roteiro para a Banca:** Consulte `ROTEIRO-APRESENTACAO.md`.
