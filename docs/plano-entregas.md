# Como o sistema funciona e como entregamos

**Arquitetura (única):** EDA — EventBus Java in-process.  
**Stack:** Backend Java (`mvp/` hoje) + Frontend React (`frontend/`).  
**UI oficial:** web **Opção A — SCADA escuro** ([`ui/direcao-opcao-a.md`](ui/direcao-opcao-a.md), protótipo [`ui/propostas/opcao-a.html`](ui/propostas/opcao-a.html)) — Swing é legado de fluxo.

---

## 1. Fluxo do MVP (Must)

![Componentes lógicos do MVP](marco1/diagramas/componentes-logicos-mvp.png)

Fonte UML: [`marco1/diagramas/componentes-logicos-mvp.puml`](marco1/diagramas/componentes-logicos-mvp.puml). Stub HTTP entregue (S3 Bruno): `mvp/4-EXECUTAR-API-ESTADO.bat` → `GET /api/estado`. T05 cenários integrados = gap S4 (#52).

| Camada | O que faz |
|--------|-----------|
| Domínio Java | Publica/consome eventos; mantém medições e alarmes em memória |
| API estado | Expõe snapshot derivado do EventBus ao front (`apiestado`) |
| React | Telas T01–T05 Opção A (`EstadoContext`) |

A auditoria é a persistência existente nesta versão: arquivo `mvp/dados/auditoria.log` com encadeamento SHA-256.

Telas: ver [`ui/idealizacao-telas.md`](ui/idealizacao-telas.md).

---

## 2. Regra de entrega semanal (vertical)

Toda **segunda** cada aluno entrega **1 issue** com:

1. **Backend** (Java / evento / doc de arquitetura da trilha)  
2. **Frontend** (pedaço da tela React T0X ou Figma → código)  
3. Relatório no template [`semanas/_template-entrega-vertical.md`](semanas/_template-entrega-vertical.md)  
4. PR da branch pessoal → `desenvolvimento`

Ao fechar a sprint: preencher [`semanas/_template-resumo-merge.md`](semanas/_template-resumo-merge.md).

| Aluno | Branch | Back (trilha) | Front (tela) |
|-------|--------|---------------|--------------|
| Álvaro | `alvaro` | Aceite, RF/MoSCoW, checklist | Critérios UX / validação T0X |
| Bruno | `bruno` | EventBus, pacotes/componentes, API eventos | T01 Overview (shell + dados) |
| Bernardo | `bernardo` | Medições / histórico | T02 Sensores |
| José | `jose` | Alarmes / limiar / GoF | T03 Alarmes |
| Marcus | `marcus` | Auditoria / UCs-SEQ | T04 Auditoria (+ apoio T05 Demo) |

---

## 3. Calendário até Marco 1

| Sprint | Segunda | Foco do grupo | Issues |
|--------|---------|---------------|--------|
| Idealização | **agora → 21/09** | Opção A no Figma + shell React (#40) | Epic [#40](https://github.com/rlampago22/RP-IV/issues/40) |
| S3 | **21/09** | 1ª fatia vertical back+front por pessoa | 5 issues S3 |
| S4 | **28/09** | Integração, polish, ensaio 8–10 min | 5 issues S4 |
| Marco 1 | **05/10** | Apresentação + demo Must | 5 issues M1 |

### DoD S3 (21/09)
Cada um com PR: domínio da trilha **e** tela React mínima (mesmo mock). Eventos da trilha documentados.

### DoD S4 (28/09)
Fluxo integrado: demo dispara cenário → overview/alarmes/auditoria atualizam. README de execução. Diagramas essenciais = código real.

### DoD Marco 1 (05/10)
**MVP Must completo:** RF01–RF06 + RNFs + GoF + auditoria + API estado + UI T01–T05 + aceite Must PASS + apresentação 8–10 min.  
**Diagramas/UML:** usar o **PDF APS 8ª entrega** (não redesenhar no repo).  
Pacote: [`marco1/PACOTE-ENTREGA-MARCO1.md`](marco1/PACOTE-ENTREGA-MARCO1.md) · Auditoria: [`semanas/2026-09-22/AUDITORIA-PDF-MARCO1.md`](semanas/2026-09-22/AUDITORIA-PDF-MARCO1.md).

---

## 4. Débito técnico a não esquecer

| Item | Dono | Status 22/09 |
|------|------|--------------|
| Evidência S1 pacotes/componentes | Bruno | Feito (#41 + #44) — fechar issue |
| Evidência S1 GoF | José | Feito (#42 CLOSED) |
| API HTTP Java → React | Bruno | Stub `/api/estado` entregue — polish #49 |
| T05 → API/EventBus | Marcus | Gap H — #52 |
| Aceite Must PASS | Álvaro | Gap K — #48 |
| Figma frames oficiais | Grupo | Opcional (#40); não bloqueia M1 |
| Migrar `mvp/` → `backend/` | Contínuo | Pós–Marco 1 OK |

---

## 5. O que NÃO fazer até Marco 1

Evacuação, RH, IA preditiva, conformidade ampla, microserviços, visual Swing como entrega final, **redesenhar UML/diagramas no repositório** (fonte = PDF APS).
