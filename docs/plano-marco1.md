# Plano até Marco 1 — issues e entregas

**Stack decidida pelo grupo:** **Java** (esqueleto em `src/main/java/.../usina/`).  
**Fonte de domínio:** [`legado-aps/APS-RPIV-ATUALIZADO-Diagrama-Implantacao.pdf`](legado-aps/APS-RPIV-ATUALIZADO-Diagrama-Implantacao.pdf)  
**Recorte:** MVP Must em [`marco1/`](marco1/) — não o APS inteiro.

| Marco | Data |
|-------|------|
| Marco 1 | **05/10/2026 (segunda)** |

---

## Regras de entrega (toda segunda)

1. Cada aluno tem **uma issue** da semana.
2. Trabalha na **branch pessoal** → PR para `desenvolvimento`.
3. Relatório em `docs/semanas/AAAA-MM-DD/<nome>.md`.
4. Aceite = checklist da issue marcado + PR revisável.

| Aluno | Branch | Trilha | RF Must |
|-------|--------|--------|---------|
| Álvaro Domingues | `alvaro` | RF/RNF, MoSCoW, aceite | Escopo RF01–RF06 |
| Bruno Rocha | `bruno` | EDA, EventBus, pacotes/componentes | Barramento |
| Bernardo Dorneles | `bernardo` | ControleReator / medições | RF01–RF02 |
| José Guilherme Monteiro | `jose` | Alarmes / limiar / GoF | RF03–RF05 |
| Marcus Querol | `marcus` | UCs / sequência / auditoria | RF06 + UML |

---

## Calendário

| Semana | Segunda | Issues | Objetivo do grupo |
|--------|---------|--------|-------------------|
| 1 | 08/09 | #11–#15 | Pacote do professor (docs/UML) |
| **2** | **14/09** | #16–#20 | 1ª fatia **código Java** Must |
| 3 | 21/09 | #21–#25 | Alarme completo + UML alinhada ao código |
| 4 | 28/09 | #26–#30 | Ensaio / polish / roteiro |
| Marco 1 | 05/10 | #31–#35 | Apresentação + demo |

---

## Semana 2 (14/09) — DoD do grupo

Fluxo mínimo em Java (pode ser console):

1. Registrar medições (`receberLeitura`)
2. Publicar/consumir no EventBus (ou stub documentado se Bruno ainda integrar)
3. Avaliar limiar → criar Alarme
4. Auditoria append-only no log

**Demo Bernardo:** `scripts\run-demo-medicoes.bat`

---

## Semana 3 (21/09) — DoD do grupo

- Alarme notifica Operador + Supervisão (console ok)
- SEQ-UC01 / SEQ-UC02 batem com o código
- Checklist feedback APS do núcleo atualizado
- Componentes físicos = o que a demo realmente empacota

---

## Semana 4 (28/09) — DoD do grupo

- Ensaio interno 8–10 min
- Slides/roteiro RF+MoSCoW+MVP+EDA
- Seeds + README “como rodar”
- Lista GoF com ponteiro de classe Java

---

## Marco 1 (05/10) — DoD do grupo

Apresentação oficial: escopo + diagramas + demo Must parcial rodando. Sem módulo Won't.

---

## Won't até Marco 1

Evacuação completa, RH/treinamentos, conformidade ampla, IA preditiva, rastreio completo de materiais, microserviços/cluster.
