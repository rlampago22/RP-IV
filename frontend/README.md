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

## Estado da integração

- T05 dispara quatro cenários demonstráveis: normal, observação, falha de sensor e anomalia crítica.
- T01, T02, T03 e T04 consomem o mesmo estado React; por isso, telemetria, alarmes e auditoria mudam em conjunto durante a apresentação.
- Os hashes exibidos no frontend são demonstrativos. A cadeia SHA-256 real permanece no domínio Java em `mvp/dados/auditoria.log`.
- A integração HTTP React → Java ainda é evolução arquitetural; o frontend não afirma estar conectado ao `EventBus` enquanto essa API não existir.
