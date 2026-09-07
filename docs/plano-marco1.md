# Plano até Marco 1 — entregas às segundas

**Disciplina:** AL0343 — Resolução de Problemas IV  
**Sistema:** Controle de Usina Nuclear (EDA)  
**Branch de trabalho:** `desenvolvimento`  
**Marco 1:** **05/10/2026 (segunda)**

## Regra de ouro

Toda semana **os 5** entregam: cada um **uma issue própria**, na **sua trilha**, com evidência em `docs/semanas/AAAA-MM-DD/<nome>.md` + PR da branch pessoal → `desenvolvimento`.

| Aluno | GitHub (se no repo) | Branch | Trilha fixa |
|-------|---------------------|--------|-------------|
| Álvaro Domingues | `AlvaroDomingues` | `alvaro` | Requisitos, MoSCoW, aceite, checklist |
| Bruno Rocha | `brunorochaaluno-eng` | `bruno` | EDA, EventBus, pacotes/componentes |
| Bernardo Dorneles | `bNDorneles` | `bernardo` | ControleReator, medições, persistência |
| José Guilherme Monteiro | *(adicionar ao repo)* | `jose` | Alarmes, limiares, GoF |
| Marcus Querol | *(adicionar ao repo)* | `marcus` | UCs/sequências, auditoria, demo |

## Calendário de entrega (segunda = dia D)

| Entrega | Data | Foco da evolução |
|---------|------|------------------|
| **Semana 1** | **08/09/2026** | Pacote do professor (docs/UML refatorados) — **atraso crítico: amanhã** |
| Semana 2 | 15/09/2026 | Fatia vertical Must rodando (EventBus + medição + limiar + auditoria) |
| Semana 3 | 22/09/2026 | Alarme completo + docs alinhados ao código |
| Semana 4 | 29/09/2026 | Integração, polish, ensaio interno |
| **Marco 1** | **05/10/2026** | Apresentação oficial + demo Must parcial |

> Entre 08/09 e 05/10 são **4 sprints semanais** + dia do Marco 1. Não cabe escopo novo: só núcleo Must.

---

## Semana 1 — entrega 08/09 (pedido do professor)

**Objetivo do grupo:** um pacote único com:

1. RF e RNF  
2. Priorização MoSCoW  
3. Proposta de MVP  
4. Projeto refatorado (pacotes + componentes lógico/físico)  
5. Artefatos extras (UCs, classes, sequência, GoF)

| Aluno | Issue / entrega individual | Arquivos-alvo |
|-------|----------------------------|---------------|
| **Álvaro** | RF/RNF revisados **e** MoSCoW + texto curto de MVP (Must/Should/Could/Won't) | `docs/requisitos/`, `docs/mvp/`, `docs/marco1/01*`, `02*`, `03*` |
| **Bruno** | Diagrama de **pacotes** + **componentes lógico** + **componentes físico** (MVP) | `docs/marco1/diagramas/pacotes.puml`, `componentes-*.puml`, `docs/diagramas/` |
| **Bernardo** | Diagrama/classes do **núcleo** (Reator, Sensor, Medicao, Facade, Limiar) alinhado ao MVP | `docs/marco1/06*`, `classes-projeto-mvp.puml`, listagem em relatório |
| **José** | Mapa GoF do MVP (Strategy/Factory/Facade) ligado a alarmes + esboço classes de alarme | doc GoF em `docs/marco1/` ou `docs/arquitetura/`, `alarmes/*` |
| **Marcus** | UC01 (+ include alarme) e **pelo menos 1 sequência** do núcleo, com system boundary | `docs/marco1/05*`, `07*`, `casos-de-uso-mvp.puml`, `seq-uc01*.puml` |

**Definição de pronto (grupo):** pasta `docs/marco1/` coerente o suficiente para apresentar amanhã + 5 relatórios em `docs/semanas/2026-09-08/`.

---

## Semana 2 — entrega 15/09

**Objetivo:** primeira execução do Must (ainda simples).

| Aluno | Evolução |
|-------|----------|
| **Álvaro** | Critérios de aceite **testáveis** do Must (checklist pass/fail) + STATUS da semana |
| **Bruno** | `EventBus` in-process publicar/assinar + evento `MedicaoRegistrada` |
| **Bernardo** | `ReatorFacade.receberLeitura` + `MedicaoReator` persistindo em memória **ou** arquivo mínimo |
| **José** | `AvaliadorLimiar` (Strategy) + criar `Alarme` quando limiar viola |
| **Marcus** | `AuditoriaSubscriber` append-only consumindo evento de medição/alarme |

**DoD grupo:** um `main`/demo que registra 3 medições, dispara 1 alarme e mostra log de auditoria.

---

## Semana 3 — entrega 22/09

**Objetivo:** fechar fluxo de alarme + corrigir inconsistências APS no núcleo.

| Aluno | Evolução |
|-------|----------|
| **Álvaro** | Checklist feedback APS **só do núcleo** (itens feitos/pendentes) |
| **Bruno** | Componentes **físicos** = o que de fato empacota a demo; EventBus estável |
| **Bernardo** | Persistência consultável (histórico) + SEQ-UC01 **igual ao código** |
| **José** | `emitirAlerta` + `registrarEvento` + notificação console (Operador/Supervisão) |
| **Marcus** | SEQ-UC02 (alarme) + doc UC com destinatários explícitos |

**DoD grupo:** demo estável do fluxo medição → limiar → alarme → auditoria; UML sem contradição grave.

---

## Semana 4 — entrega 29/09

**Objetivo:** integração e ensaio (última segunda antes do Marco 1).

| Aluno | Evolução |
|-------|----------|
| **Álvaro** | Roteiro/slides RF+RNF+MoSCoW+MVP (versão apresentação) |
| **Bruno** | Pacotes/componentes finais = pacotes Java reais; fala EDA 2–3 min |
| **Bernardo** | Script/passos da demo de medições + dados seed |
| **José** | Demo alarme (OK vs violado) + lista GoF com ponteiros de código |
| **Marcus** | README “como rodar a demo” + UCs/sequências finais no pacote |

**DoD grupo:** ensaio interno 8–10 min sem inventar módulo novo.

---

## Marco 1 — 05/10/2026

| Aluno | Papel na apresentação |
|-------|----------------------|
| Álvaro | Escopo, requisitos, MoSCoW, MVP |
| Bruno | Arquitetura EDA + diagramas pacotes/componentes |
| Bernardo | Demo medições/persistência |
| José | Demo alarme + padrões |
| Marcus | UCs/sequências + auditoria na demo |

Empacotar versão “oficial” a partir do que foi validado nas Semanas 1–4 (`docs/marco1/` deixa de ser só draft).

---

## Ritmo diário sugerido (atraso Semana 1)

### Hoje → amanhã (08/09)

1. Cada um puxa a **sua issue da Semana 1** e a branch pessoal atualizada de `desenvolvimento`
2. Editar **só** a trilha (não reescrever a do colega)
3. PR curto + relatório `docs/semanas/2026-09-08/<nome>.md`
4. Alguém (Álvaro/Bruno) une os artefatos num PDF/HTML de apresentação se o professor pedir arquivo único

### Semanas seguintes

- Segunda = entrega  
- Terça–domingo = implementação da issue da semana  
- Domingo à noite = PR aberto no máximo  

---

## Fora de escopo até Marco 1

Evacuação completa, RH, conformidade ampla, IA preditiva, Should de acesso (fica pós-Marco 1, salvo sobra real).
