# Documentação de Engenharia — RP-IV

Hub único (inspirado no padrão Sentinela) que aponta para os artefatos oficiais do grupo.  
**Não duplica** o conteúdo longo: cada seção linka a fonte canônica.

**Sistema:** Central de Supervisão — Usina Nuclear  
**Arquitetura:** EDA (EventBus in-process)  
**UI oficial:** Opção A — SCADA escuro ([`ui/direcao-opcao-a.md`](ui/direcao-opcao-a.md))  
**Marco 1:** 05/10/2026

---

## 1. Requisitos

| Artefato | Link |
|----------|------|
| RF / RNF | [`marco1/01-requisitos-rf-rnf.md`](marco1/01-requisitos-rf-rnf.md) |
| MoSCoW | [`marco1/02-priorizacao-moscow.md`](marco1/02-priorizacao-moscow.md) |
| Spec consolidada MVP | [`mvp/especificacao-rf-rnf-moscow-mvp.md`](mvp/especificacao-rf-rnf-moscow-mvp.md) |
| Checklist aceite Must | [`mvp/checklist-aceite-must.md`](mvp/checklist-aceite-must.md) |

---

## 2. Proposta de MVP

| Artefato | Link |
|----------|------|
| Proposta | [`marco1/03-proposta-mvp.md`](marco1/03-proposta-mvp.md) |
| Fluxo demo (roteiro) | [`../mvp/ROTEIRO-APRESENTACAO.md`](../mvp/ROTEIRO-APRESENTACAO.md) |

**Must:** medições RF-1 + limiar/alarme + auditoria SHA-256 + UI supervisão (web Opção A).

---

## 3. Arquitetura

| Artefato | Link |
|----------|------|
| EDA (canônico) | [`marco1/04-arquitetura-eda.md`](marco1/04-arquitetura-eda.md) |
| Backend Java (direção) | [`backend-java.md`](backend-java.md) |
| Índice arquitetura | [`arquitetura/README.md`](arquitetura/README.md) |

---

## 4. Casos de uso e sequências

| Artefato | Link |
|----------|------|
| UCs MVP | [`marco1/05-casos-uso-mvp.md`](marco1/05-casos-uso-mvp.md) |
| Classes | [`marco1/06-classes-projeto-mvp.md`](marco1/06-classes-projeto-mvp.md) |
| Sequências | [`marco1/07-sequencias-mvp.md`](marco1/07-sequencias-mvp.md) |
| PlantUML | [`marco1/diagramas/`](marco1/diagramas/) |

---

## 5. Interface (Opção A)

| Artefato | Link |
|----------|------|
| Direção visual | [`ui/direcao-opcao-a.md`](ui/direcao-opcao-a.md) |
| Aceite visual | [`ui/aceite-visual.md`](ui/aceite-visual.md) |
| Protótipo HTML | [`ui/propostas/opcao-a.html`](ui/propostas/opcao-a.html) |
| Figma | [`ui/FIGMA.md`](ui/FIGMA.md) |
| Idealização T01–T05 | [`ui/idealizacao-telas.md`](ui/idealizacao-telas.md) |

---

## 6. Rastreabilidade implementação

[`implementacao/00-INDICE.md`](implementacao/00-INDICE.md) — o que já existe em `mvp/` e `frontend/`.

---

## 7. Análise / lacunas

[`analise/00-INDICE-E-METODO.md`](analise/00-INDICE-E-METODO.md)

---

## 8. Entrega Marco 1 (pacote)

Índice do rascunho: [`marco1/README.md`](marco1/README.md)  
Pacote consolidado (quando fechado): `marco1/ENTREGA-MARCO1.md`
