# Figma — Central de Supervisão RP-IV

## Status no Cursor (este workspace)

O MCP **plugin-figma-figma** ainda **não está habilitado** no projeto RP4 (ferramentas disponíveis hoje: browser + Canva).

### Como ativar (necessário uma vez)

1. Cursor → **Settings → MCP / Plugins**
2. Ativar o plugin **Figma**
3. Autenticar a conta Figma
4. Reabrir o chat neste workspace (`RP4`)
5. Pedir de novo: “cria os frames T01–T05 no Figma”

Enquanto isso, a idealização está em [`idealizacao-telas.md`](idealizacao-telas.md) e o export T01 em [`exports/t01-overview-supervisao.png`](exports/t01-overview-supervisao.png).

---

## Spec dos frames (para `use_figma` / create_new_file)

**Arquivo:** `RP-IV Usina — Supervisão MVP`  
**Page:** `MVP Screens`  
**Desktop:** 1440 × 900

| Frame | Nome | Conteúdo |
|-------|------|----------|
| T01 | Overview | Topbar + alarm banner + nav + 4 KPIs + mimic leve |
| T02 | Sensores | Tabela/cards sensores + sparkline |
| T03 | Alarmes | Lista priorizada + ações reconhecer/resolver |
| T04 | Auditoria | Log append-only |
| T05 | Demo | Botões de cenário (normal / anomalia / falha) |

**Tokens de cor (sóbrios):**

- bg `#1A1D23`
- panel `#232833`
- text `#E8EAED`
- muted `#9AA3B2`
- ok `#3D9A6A`
- warn `#C9922A`
- crit `#C44B4B`

**Tipografia:** Inter Regular / Semi Bold

**Componentes:** `TopBar`, `AlarmBanner`, `SideNav`, `KpiCard`, `AlarmRow`, `SensorTile`, `StatusPill`

---

## Ligação com entrega vertical

| Frame | Dono front |
|-------|------------|
| T01 | Bruno |
| T02 | Bernardo |
| T03 | José |
| T04 | Marcus |
| T05 | Marcus (+ Bruno API) |
| Validação UX | Álvaro |
