# Idealização de telas — Central de Supervisão (EDA)

**Stack-alvo:** React (Vite) + backend Java EDA  
**Status:** **Opção A escolhida** — SCADA escuro (DNA Marcus) · [`direcao-opcao-a.md`](direcao-opcao-a.md) · protótipo [`propostas/opcao-a.html`](propostas/opcao-a.html) · issue [#40](https://github.com/rlampago22/RP-IV/issues/40)  
**Legado:** UI Swing em `mvp/.../SistemaMvpUI.java` (fluxo + paleta de base; visual final = React Opção A)

---

## 1. Por que a tela Swing atual fica “estranha”

Pontos observados no código/docs do Swing (`SistemaMvpUI`):

- Tudo em **uma classe monolítica** (~1100 linhas): visual + domínio + animação
- Estética “dashboard gamer/SCADA genérico” (neon, radar animado) — pouco alinhada a HMI nuclear real
- Densidade alta sem hierarquia clara de **visão geral → detalhe → alarme**
- Cor usada demais o tempo todo; em HMI nuclear a cor deve destacar **anomalia**, não decorar o normal
- Não há design system versionado (Figma/tokens); difícil entregar front web consistente

**Conclusão:** aproveitar eventos/cenários do MVP Marcus; **redesenhar** a experiência em web.

---

## 2. Referências de como usinas/SCADA funcionam (UI)

Princípios (NUREG-0700 / modernização de sala de controle / SCADA industrial):

1. **Tarefa do operador primeiro** — monitorar estado, detectar desvio, reconhecer alarme, agir
2. **Zonas fixas de tela** — overview, processo, alarmes, navegação (sempre no mesmo lugar)
3. **Hierarquia** — overview da planta → sistema (reator) → parâmetro/sensor
4. **Mimic simplificado** — fluxo do processo só com o necessário (não desenhar a usina inteira)
5. **Alarmes priorizados** — lista clara; cor/símbolos para severidade; reconhecer/resolver
6. **Tendência** — valores no tempo (mesmo sparkline simples)
7. **Baixa densidade visual** — espaço em branco; tipografia legível; evitar animação decorativa

Fontes de inspiração: guias HFE de control room nuclear, HMIs SCADA (overview + alarm banner + process area).

---

## 3. Telas essenciais do MVP (entrega vertical)

| ID | Tela | Objetivo | Eventos EDA |
|----|------|----------|-------------|
| T01 | **Overview / Supervisão** | Estado do reator + KPIs + banner de alarme | `MedicaoRegistrada`, `AlarmeEmitido` |
| T02 | **Sensores / Medições** | Lista/cards de sensores, histórico curto | `MedicaoRegistrada` |
| T03 | **Alarmes** | Fila de alarmes ativos/reconhecidos | `AlarmeEmitido`, `AlarmeReconhecido`, `AlarmeResolvido` |
| T04 | **Auditoria** | Trilha append-only | qualquer evento consumido por AuditoriaLogs |
| T05 | **Cenários (demo)** | Disparar normal/anomalia/falha (só MVP acadêmico) | publica eventos no bus via API |

Fluxo feliz da demo: T05 → T01 atualiza → T03 mostra alarme → T04 registra trilha.

---

## 4. Layout-base (todas as telas)

```text
+------------------------------------------------------------------+
| TOP BAR: logo Usina | Reator-01 | relógio | status conexão API   |
+------------------------------------------------------------------+
| ALARM BANNER (só se houver crítico/alto)                         |
+--------+---------------------------------------------------------+
| NAV    |  CONTEÚDO PRINCIPAL                                     |
| Over.  |  (mimic leve + KPIs ou tabela)                          |
| Sensor |                                                         |
| Alarme |                                                         |
| Audit. |                                                         |
| Demo   |                                                         |
+--------+---------------------------------------------------------+
| FOOTER: último evento | versão MVP                               |
+------------------------------------------------------------------+
```

**Paleta proposta (sóbrea):** fundo neutro escuro/grafite, texto claro, **âmbar/vermelho só para alarme**, verde só para “normal/ack”. Sem neon ciano em tudo.

---

## 5. Como aplicar no Figma (quando MCP estiver ativo)

Frames sugeridos (Desktop 1440×900):

1. `T01 Overview`
2. `T02 Sensores`
3. `T03 Alarmes`
4. `T04 Auditoria`
5. `T05 Demo / Cenários`
6. Components: `KpiCard`, `AlarmRow`, `SensorTile`, `NavItem`, `StatusPill`

Entregar no repo: link do arquivo Figma + exports PNG em `docs/ui/exports/` (após criação).

---

## 6. Critério de “topzera” para a banca

- Parece **sala de supervisão**, não app genérico
- Operador entende em &lt; 5 s se está OK ou em alarme
- Cada tela tem **um job**; navegação previsível
- Front React espelha os frames; back Java só expõe estado/eventos (EDA)
