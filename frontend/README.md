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

## Setup (demo Marco 1)

Em um terminal, subir a API Java:

```bat
cd mvp
4-EXECUTAR-API-ESTADO.bat
```

Em outro:

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173/`. O header deve mostrar EventBus **ONLINE** (não MOCK).  
Contrato: [`../docs/api-estado-contrato.md`](../docs/api-estado-contrato.md).  
URL da API: `VITE_API_BASE_URL` (padrão `http://localhost:8080`).

## Nota

A UI Swing em `mvp/` é **legado de fluxo** (plano B se o React falhar). O visual oficial é este frontend Opção A.

## Estado da integração (pós-S4 em `desenvolvimento`)

- T01–T04 leem o mesmo `EstadoContext` → polling `GET /api/estado` (derivado do EventBus).
- T02: condições Normal / Observação → `POST /api/cenarios/normal|observacao` (+ histórico no snapshot).
- T03: reconhecer / resolver alarme → `POST /api/alarmes/{id}/...`.
- T05: Normal / Observação / Falha / Crítico → `POST /api/cenarios/{key}` (reflete em T01–T04).
- Sem a API Java, o front cai em mock local (badge MOCK) — útil offline, **não** é a demo TOK.
- Cadeia SHA-256 real: `mvp/dados/auditoria.log` (T04 mostra a timeline do snapshot).