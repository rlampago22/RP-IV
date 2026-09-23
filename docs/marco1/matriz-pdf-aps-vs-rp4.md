# Matriz PDF APS 8ª entrega × MoSCoW RP4

**Data:** 2026-09-22  
**Fontes:** PDF *APS — Sistema de Controle de Usina Nuclear — 8ª entrega* (99 págs.) · [`02-priorizacao-moscow.md`](02-priorizacao-moscow.md) · [`especificacao-rf-rnf-moscow-mvp.md`](../mvp/especificacao-rf-rnf-moscow-mvp.md)

**Regra:** Marco 1 entrega o **MVP Must do RP4** (RF01–RF06 + código/demo), não o PDF inteiro.  
**UML/diagramas:** o próprio PDF APS é a fonte — **não** há trilha de redesenho UML no repositório.

---

## 1. Capítulos do PDF → artefatos no repo

| PDF § | Tema | Artefato RP4 | Status |
|-------|------|--------------|--------|
| 1.1–1.10 | Escopo dentro/fora | `01-requisitos-rf-rnf.md` | OK (recorte) |
| 2 | Requisitos faltantes | checklist + aceite Must | OK |
| 3 | MoSCoW PDF (RF-1…18 / RNF) | `02-priorizacao-moscow.md` | OK com **recorte** (ver §2) |
| 4–5 | Casos de uso amplos | `05-casos-uso-mvp.md` (núcleo) | OK núcleo / fora resto |
| 6 | Classes conceituais/domínio | `06-classes-projeto-mvp.md` | OK núcleo |
| 7 | Atividades | — (PDF APS) | Fora — usar PDF |
| 8–9 | Pacotes / camadas | PDF APS | Canônico no PDF; repo legado |
| 10 | Sequências UC01–UC10 | PDF APS | Canônico no PDF |
| 11 | Componentes L/F | PDF APS | Canônico no PDF |
| 12 | ER | PDF APS (+ texto `08` se útil) | Canônico no PDF |
| 13 | Implantação | PDF APS | Referência |

---

## 2. MoSCoW PDF × RF RP4

| PDF | Prioridade PDF | RP4 | No Marco 1 |
|-----|----------------|-----|------------|
| RF-1 medições tempo real | Must | **RF01** | Obrigatório — PASS no código |
| RF-2 alarmes | Must | **RF04–RF05** | Obrigatório — PASS |
| RF-3 status/histórico reator + manutenção | Must | **RF02** (só histórico de medições) | Obrigatório parcial — manutenção = Won't |
| RF-4 vazamento/contingência | Must | Could/Won't | **Fora** |
| RF-5 acesso biométrico | Must | Should RF07–08 | **Fora código** (doc Should) |
| RF-6 materiais radioativos | Must | Won't | **Fora** |
| RF-7 manutenção agendada | Must | Won't | **Fora** |
| RF-8 radiação ambiental | Should | Won't núcleo | **Fora** |
| RF-9 emergência/evacuação | Must | Could/Won't | **Fora** |
| RF-10 relatórios regulatórios | Should | Won't | **Fora** |
| RF-11–14 capacitação/treinamentos/IA | Should/Must | Won't | **Fora** |
| RF-15 painel visual | Could / “fora” no PDF | UI Opção A T01–T05 | **Obrigatório** no RP4 (supervisão) |
| RF-16–18 notificações/permissões/redundância | Could/Must | Parcial via console + EDA | Console OK; SMS/cluster **Fora** |
| RNF-01…05 PDF | Must | RNF01–05 + EDA | Evidência em `TestesMvp` |

---

## 3. O que NÃO vira issue até Marco 1

Evacuação, RH/treinamentos, materiais, IA preditiva, acesso biométrico real, banco relacional em produção, microserviços, cluster/failover 99,99%.
