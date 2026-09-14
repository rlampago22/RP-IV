# Plano até Marco 1 — entregas às **segundas**

**Fonte do domínio:** [`legado-aps/APS-RPIV-ATUALIZADO-Diagrama-Implantacao.pdf`](legado-aps/APS-RPIV-ATUALIZADO-Diagrama-Implantacao.pdf)  
**Recorte de entrega:** MVP Must em [`marco1/`](marco1/) — **não** o APS inteiro.

| Marco | Data |
|-------|------|
| Marco 1 | **05/10/2026 (segunda)** |

## Regra

Toda segunda: **1 issue por pessoa**, branch pessoal → PR `desenvolvimento` + relatório em `docs/semanas/AAAA-MM-DD/<nome>.md`.

| Aluno | Branch | Trilha | Must do PDF/MVP |
|-------|--------|--------|-----------------|
| Álvaro | `alvaro` | RF/RNF, MoSCoW, aceite | Escopo Must RF01–RF06 |
| Bruno | `bruno` | EDA, EventBus, pacotes/componentes | Arquitetura + bus |
| Bernardo | `bernardo` | ControleReator / medições | RF01–RF02 |
| José | `jose` | Alarmes / limiar / GoF | RF03–RF05 |
| Marcus | `marcus` | UCs / sequência / auditoria | RF06 + UML núcleo |

## Calendário (segundas reais)

| Entrega | Data | Issues | Foco |
|---------|------|--------|------|
| Semana 1 | 08/09 | #11–#15 | Pacote do professor (docs/UML) |
| **Semana 2** | **14/09 (hoje)** | #16–#20 + [#38](https://github.com/rlampago22/RP-IV/issues/38) | 1ª fatia código Must + apresentação |
| Semana 3 | 21/09 | #21–#25 | Alarme completo + UML=código |
| Semana 4 | 28/09 | #26–#30 | Ensaio / polish |
| Marco 1 | 05/10 | #31–#35 | Apresentação oficial |

## MVP Must (do PDF, recortado)

Medições de reator → histórico → limiares → alarme (Operador + Supervisão) → auditoria, via **EDA**.

**Won't até Marco 1:** evacuação, RH/treinamentos, conformidade ampla, IA preditiva, rastreio completo de materiais.

## Semana 2 — DoD

Demo mínima: registrar medições, violar limiar, criar alarme, ver log de auditoria (mesmo que console).
