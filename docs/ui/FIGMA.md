# Figma — Central de Supervisão RP-IV (Opção A)

## Direção oficial

**Opção A — SCADA escuro** (DNA do MVP Marcus).  
Spec: [`direcao-opcao-a.md`](direcao-opcao-a.md) · Aceite: [`aceite-visual.md`](aceite-visual.md) · Protótipo: [`propostas/opcao-a.html`](propostas/opcao-a.html)

**Arquivo:** [RP-IV Usina — Supervisão MVP](https://www.figma.com/design/2rlHQret0408QGTMTibBI4)  
**Page:** `MVP Screens` · reconstruir T01–T05 no chrome escuro da Opção A

### Como ativar Figma no Cursor

1. **Cursor Settings → Tools & MCP** → Figma **Connect**
2. Abrir chat Agent novo

---

## Spec dos frames (Opção A)

**Desktop:** 1440 × 900 · fundo `#0B0F19`

| Frame | Nome | Conteúdo |
|-------|------|----------|
| T01 | Overview | Topbar + badge + tabs + núcleo/radar + 4 sensores + contadores EDA + timeline + alarmes + ações Tempo Real/ACK/Resolver |
| T02 | Sensores | Cards/tabela RF-1 (temp, pressão, radiação, fluxo) + barras + status |
| T03 | Alarmes | Caixa/lista priorizada + Validar/Reconhecer + Normalizar/Encerrar |
| T04 | Auditoria | Tabela append-only (#, timestamp, evento, hash) |
| T05 | Demo | 4 cenários: Normal · Observação · Falha Sensor · Anomalia Crítica |

**Tokens:** ver `direcao-opcao-a.md`  
**Tipografia:** Segoe UI / Inter · números Consolas

---

## Ligação com entrega vertical

| Frame | Dono | S3 | S4 |
|-------|------|----|----|
| Shell Opção A | Grupo (#40) + Bruno | #40 #44 | #49 |
| T01 | Bruno | #44 | #49 |
| T02 | Bernardo | #45 | #50 |
| T03 | José | #46 | #51 |
| T04 | Marcus | #47 | #52 |
| T05 | Marcus | — | #52 |
| UX | Álvaro | #43 | #48 |
