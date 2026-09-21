# Direção visual oficial — Opção A (SCADA escuro)

**Decisão:** 16/09/2026  
**Protótipo local:** [`propostas/opcao-a.html`](propostas/opcao-a.html)  
**Índice:** [`propostas/index.html`](propostas/index.html)  
**Base de fluxo:** MVP Swing do Marcus (`mvp/.../SistemaMvpUI.java` + `mvp/ROTEIRO-APRESENTACAO.md`)

Opção B (painel claro) foi descartada.

---

## O que é a Opção A

Evolução web do SCADA escuro do Marcus: sala de controle, não dashboard gamer.

| Zona | Conteúdo |
|------|----------|
| Top bar | Marca Reator-01 + badge ESTÁVEL / ATENÇÃO / CRÍTICO |
| Tabs / nav | Visão Geral · Sensores · Alarmes · Auditoria · Demo |
| Alarm banner | Só com alarme ativo · botão Validar/Reconhecer |
| Coluna esquerda (T01) | Núcleo/radar + 4 cards RF-1 com barras |
| Coluna direita (T01) | Contadores EDA + timeline de eventos + caixa de alarmes + ações |
| T05 | Cenários do roteiro: Normal, Observação (Alt.1), Falha Sensor, Anomalia Crítica |

### Tokens

| Token | Hex | Uso |
|-------|-----|-----|
| bg | `#0B0F19` | Fundo |
| card | `#131C2E` | Painéis |
| card2 | `#1A263E` | Nested |
| border | `#22314F` | Bordas |
| text | `#F8FAFC` | Texto |
| muted | `#94A3B8` | Secundário |
| ok | `#10B981` | Estável / resolve |
| cyan | `#06B6D4` | Telemetria |
| warn | `#F59E0B` | Observação / ACK |
| crit | `#EF4444` | Alarme |
| purple | `#A855F7` | Falha/manutenção |

Cor **só** para status/alarme — não decorar o normal com neon em tudo.

### Eventos obrigatórios na UI

`MEDICAO_REGISTRADA` · `OBSERVACAO_REGISTRADA` · `FALHA_SENSOR_DETECTADA` · `ALARME_EMITIDO` · `ALARME_RECONHECIDO` · `ALARME_RESOLVIDO`

### Fluxo demo (aceite)

T05 cenário Crítico → T01 núcleo CRÍTICO + banner → T03 ACK → Resolver → T04 mostra trilha.

---

## Dono por tela (inalterado)

| Tela | Dono | Issue S3 | Issue S4 |
|------|------|----------|----------|
| Shell + tokens Opção A | Grupo / Bruno | #40 | — |
| T01 Overview | Bruno | #44 | #49 |
| T02 Sensores | Bernardo | #45 | #50 |
| T03 Alarmes | José | #46 | #51 |
| T04 Auditoria | Marcus | #47 | #52 (polish) |
| T05 Demo | Marcus | — | #52 |
| UX / aceite visual | Álvaro | #43 | #48 |
