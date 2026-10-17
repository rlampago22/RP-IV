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
| Shell + tokens Opção A | Grupo / Bruno | #40 CLOSED (Figma fora; UI = React) | — |
| T01 Overview | Bruno | #44 | #49 |
| T02 Sensores | Bernardo | #45 | #50 |
| T03 Alarmes | José | #46 | #51 |
| T04 Auditoria | Marcus | #47 | #52 (polish) |
| T05 Demo | Marcus | — | #52 |
| UX / aceite visual | Álvaro | #43 | #48 |

---

## Atualização v2 (17/10/2026 — Bernardo)

Remodelagem visual completa das 6 telas (Login + T01–T05), mantendo a Opção A (identidade SCADA escura, tokens, EDA/EventBus, PR [#72](https://github.com/rlampago22/RP-IV/pull/72)) — não é uma nova direção, é a mesma direção com execução mais elevada:

- **Logo do projeto:** marca própria em átomo (núcleo + 3 órbitas), substituindo o antigo monograma "RN". Componente `AtomLogo` em [`frontend/src/components/icons.jsx`](../../frontend/src/components/icons.jsx).
- **Removido do cabeçalho:** o texto "Opção A — SCADA escuro" (jargão interno da equipe, não faz sentido como copy do produto para quem assiste à demo).
- **Núcleo do reator (T01):** virou um gauge radial (anel de progresso + leitura central) em vez do círculo pulsante simples.
- **Sensores (T01/T02):** ícone próprio por grandeza (termômetro, manômetro, radiação, gota) com mini-anel de status coloridos por OK/Atenção/Crítico.
- **Linha do tempo (T01/T04):** feed vertical com ponto colorido por tipo de evento, no lugar da tabela simples em T01 (Auditoria mantém a tabela formal + um cabeçalho com ícone).
- **Ícones:** conjunto próprio em SVG inline (`frontend/src/components/icons.jsx`), sem dependência de CDN/Google Fonts — importante para a demo não depender de internet na sala de apresentação.
- **Design system:** tokens CSS existentes (`--bg`, `--card`, `--crit`, etc.) mantidos sem renomear; novas classes utilitárias (`.panel-heading`, `.ring`/`.ring-sm`/`.ring-lg`, `.stat-tile`, `.sensor-tile2`, `.timeline2`, `.action-btn2`, `.tab2`) em [`frontend/src/styles/app.css`](../../frontend/src/styles/app.css).
- **Zero mudança de lógica:** `EstadoContext`, `authApi`, `estado.js` e toda a integração RF07/RF08 validada no PR #72 continuam iguais — só a camada de apresentação mudou. Re-testado end-to-end (login, cenário crítico, ACK de alarme, acesso UC04, auditoria) sem regressão, e `mvp\2-TESTAR-MVP.bat` + `TestesAcesso` seguem 100% PASS.
