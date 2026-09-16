# Frontend — React (Vite)

UI oficial da Central de Supervisão — **Opção A (SCADA escuro)**.

## Referências

| Doc | Link |
|-----|------|
| Direção visual | [`../docs/ui/direcao-opcao-a.md`](../docs/ui/direcao-opcao-a.md) |
| Aceite | [`../docs/ui/aceite-visual.md`](../docs/ui/aceite-visual.md) |
| Protótipo HTML | [`../docs/ui/propostas/opcao-a.html`](../docs/ui/propostas/opcao-a.html) |
| Idealização T01–T05 | [`../docs/ui/idealizacao-telas.md`](../docs/ui/idealizacao-telas.md) |

## Rotas

| Rota | Tela |
|------|------|
| `/` | T01 Overview |
| `/sensores` | T02 Sensores |
| `/alarmes` | T03 Alarmes |
| `/auditoria` | T04 Auditoria |
| `/demo` | T05 Cenários |

## Setup

```bash
cd frontend
npm install
npm run dev
```

## Nota

A UI Swing em `mvp/` é **legado de fluxo** (roteiro/cenários). O visual final é este frontend alinhado à Opção A.
