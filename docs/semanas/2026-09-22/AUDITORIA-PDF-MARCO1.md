# Auditoria PDF APS 8ª × repo × issues × Marco 1

**Data:** 2026-09-22  
**Branch auditada:** `desenvolvimento`  
**Decisão:** Marco 1 (05/10) = **MVP Must completo** (não só esqueleto/docs).  
**PDF:** APS — Sistema de Controle de Usina Nuclear — 8ª entrega (99 págs.)

Detalhes: [`../marco1/matriz-pdf-aps-vs-rp4.md`](../marco1/matriz-pdf-aps-vs-rp4.md) · [`../marco1/PACOTE-ENTREGA-MARCO1.md`](../marco1/PACOTE-ENTREGA-MARCO1.md)

---

## 1. Scorecard MVP completo (A–L)

| # | Critério | Status | Evidência / gap |
|---|----------|--------|-----------------|
| A | RF01 coletar medições | **PASS** | `ReatorFacade.receberLeitura` · `TestesMvp` CT-RF01 |
| B | RF02 histórico | **PASS** | `ReatorRepository` · HIST-01..03 OK em 22/09 |
| C | RF03 limiar Strategy | **PASS** | `AvaliadorLimiar` / `AvaliadorFaixaSegura` · CT-RF03 |
| D | RF04–05 alarme + notificar | **PASS** (Java) / **PARCIAL** (demo web) | Facade + destinatários OK; T03 via API OK; roteiro live #51 |
| E | RF06 auditoria SHA-256 | **PASS** | `AuditoriaSubscriber` · arquivo `dados/auditoria.log` |
| F | EDA produtor ≠ consumidor | **PASS** | `EventBus` · isolamento falha CT-RNF04 |
| G | ≥3 GoF | **PASS** | Facade, Observer/Pub-Sub, Strategy, Factory |
| H | UI Opção A T01–T05 | **PARCIAL** | Shell + T01–T04 OK · **T05 não dispara API** (#52) |
| I | Ponte Java↔React | **PASS** | `ApiEstadoHttpServer` · `/api/estado` · `EstadoContext` |
| J | Artefatos professor | **PASS*** | **UML/diagramas canônicos = PDF APS 8ª** (não redesenhar no repo). Docs MoSCoW/MVP + código bastam; pasta `marco1/diagramas` = legado/apoio |
| K | Pacote entrega + aceite Must | **GAP** | Checklist ainda N/E / texto 14/09 (#48) |
| L | Apresentação 8–10 min | **PARCIAL** | Issues #53–#57 abertas · depende fechar H/K |

**Suíte 22/09:** `[SUCESSO] 11 verificacoes Must + historico curto` via `executar-mvp.ps1 -Testar`.

---

## 2. Gaps críticos até Marco 1

1. **T05 integrado** — botões só alteram estado local; não publicam no EventBus/API → #52  
2. **Aceite Must preenchido com PASS** — checklist desatualizado → #48  
3. **Docs mentindo DoD** — “Marco 1 = esqueleto / API futura” → corrigido nesta auditoria  
4. **Board sujo** — S3 fechadas nesta auditoria  
5. **Seeds/T02 polish** — histórico front alinhado API → #50  
6. **Figma #40** — desejável; **não bloqueia** MVP  
7. **UML no repo** — **não é mais trilha**: fonte = PDF APS; issues não pedem PlantUML/Astah novos |

---

## 3. Issues — ações

### Fechar (feito nesta auditoria)

| Issue | Motivo |
|-------|--------|
| #43 Álvaro S3 | PR #64 mergeado |
| #44 Bruno S3 | PR #63 mergeado |
| #45 Bernardo S3 | PR #60 mergeado |
| #47 Marcus S3 | PR #61 mergeado |
| #41 Débito Bruno | EventBus + pacotes + API evidenciados |

### Manter / reduzir escopo

| Issue | Ação |
|-------|------|
| #40 Figma | OPEN só item Figma (shell já feito) — não bloqueia M1 |

### S4 = fechar gaps A–L (reescritas)

| Issue | Gap | DoD resumido |
|-------|-----|--------------|
| #48 Álvaro | K (+ slides) | Checklist Must PASS + slides MoSCoW/MVP |
| #49 Bruno | I (+ T01) | T01/API polish — **sem** redesenhar UML |
| #50 Bernardo | A/B demo | Seeds + T02 histórico alinhado `/api/estado` |
| #51 José | D (+ G fala) | T03 + roteiro alarme no fluxo live |
| #52 Marcus | H | T05 → cenário crítico atualiza T01/T03/T04 + README demo |

### Marco 1 #53–#57 = só apresentar

Sem feature nova. Bloqueadas até S4 fechar gaps H/K.

---

## 4. Docs corrigidos nesta auditoria

- `docs/marco1/03-proposta-mvp.md` §6 — M1 = MVP Must completo  
- `docs/marco1/ENTREGA-MARCO1.md` §3 — idem + API stub  
- `docs/plano-entregas.md` — API não é mais “só futura”  
- `docs/semanas/STATUS.md` — foco S4 / M1  
- `docs/marco1/diagramas/er-nucleo-mvp.puml` — restaurado do arquivo  
- `docs/marco1/matriz-pdf-aps-vs-rp4.md` · `PACOTE-ENTREGA-MARCO1.md`

---

## 5. O que NÃO fazer até Marco 1

Evacuação, RH, materiais radioativos, IA preditiva, biometria real, cluster/99,99%, microserviços, implementar ER em banco de produção.

---

## 6. Critério de pronto 05/10

- Scorecard sem GAP crítico (H e K em PASS)  
- Demo: Normal → Anomalia → ACK → Resolver → auditoria (T05→T01→T03→T04)  
- `2-TESTAR-MVP.bat` + `npm run build` OK  
- Board: S3 fechada · S4 fechada · M1 = apresentação  
- UML na fala = **PDF APS** (sem PRs de diagrama)  
- Professor recebe MVP Must **completo** + PDF APS para diagramas, sem prometer o sistema APS inteiro
