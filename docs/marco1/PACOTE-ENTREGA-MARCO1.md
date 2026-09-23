# Pacote oficial — Entrega Marco 1 (05/10/2026)

**DoD:** MVP Must **completo** (RF01–RF06 + RNFs do núcleo + demo + artefatos).  
**Branch:** `desenvolvimento` (tag sugerida: `marco-1` no dia da entrega).

---

## Checklist do que entregar ao professor

| # | Item | Onde | Status 22/09 |
|---|------|------|--------------|
| 1 | Código MVP Java EDA | `mvp/` | Pronto (suíte Must + HIST OK) |
| 2 | API estado HTTP | `mvp/4-EXECUTAR-API-ESTADO.bat` | Pronto (stub) |
| 3 | Frontend Opção A T01–T05 | `frontend/` | T01–T04 OK · **T05 integrar = #52** |
| 4 | RF/RNF + MoSCoW + MVP | `docs/marco1/01`–`03` | Corrigir marcos (M1=MVP completo) |
| 5 | Pacotes / comp. L+F | `docs/marco1/diagramas/` | OK (revisar PNG vs ApiEstado) |
| 6 | UC / SEQ / classes | Astah + `05`–`07` | OK núcleo |
| 7 | ER núcleo | `08` + `er-nucleo-mvp.puml` | OK (puml restaurado) |
| 8 | PDF consolidado | `ENTREGA-MARCO1.pdf` | Regenerar **após** S4 |
| 9 | Matriz PDF × RP4 | `matriz-pdf-aps-vs-rp4.md` | OK |
| 10 | Aceite Must preenchido | `docs/mvp/checklist-aceite-must.md` | **GAP** — ainda N/E (#48) |
| 11 | README &lt; 5 min | `mvp/README.md` + `frontend/README.md` | Parcial · fechar no #52 |
| 12 | Roteiro 8–10 min | `mvp/ROTEIRO-APRESENTACAO.md` + #53–#57 | Apresentação só |
| 13 | Issues S3 fechadas | GitHub | Fechar na auditoria |
| 14 | Issues S4 = gaps A–L | #48–#52 | Reescritas na auditoria |

---

## Como rodar a demo (entrega)

```bat
REM Terminal 1 — API + EventBus
mvp\4-EXECUTAR-API-ESTADO.bat

REM Terminal 2 — UI
cd frontend
npm install
npm run dev
```

Abrir http://localhost:5173/ · fluxo: T05 (após #52) → T01 → T03 → T04.

Evidência automatizada: `mvp\2-TESTAR-MVP.bat`.

---

## O que NÃO entra no pacote

Itens Won't do MoSCoW RP4 (evacuação, RH, materiais, IA, cluster). Ver [`matriz-pdf-aps-vs-rp4.md`](../marco1/matriz-pdf-aps-vs-rp4.md).
