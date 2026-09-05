# Checklist — Feedback APS → ações no RP4 Marco 1

**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Legenda: **Feito** = endereçado nos docs do Marco 1 | **Backlog** = documentado para depois | **N/A** = domínio descartado (feedback militar)

---

## A) Casos de uso / modelo (regras; domínio usina)

| Feedback | Ação | Status |
|----------|------|--------|
| `if` no fluxo principal | Fluxos principais lineares; condições em alternativas (UC01–UC04) | Feito |
| Alerta sem destinatário | UC02 notifica Operador **e** Supervisão Central | Feito |
| Misturar dois UCs | UC01/UC02/UC03 separados; include explícito | Feito |
| System Boundary ausente | Retângulo `SistemaControleUsina` no diagrama | Feito |
| Atores secundários ausentes | Sensor, Supervisão no modelo | Feito |
| include vs extend | Detecção/registro **include** Emitir Alarme e Auditar | Feito |
| Domínio drones/satélites/invasões | Ignorado (outro sistema) | N/A |

---

## B) Sequência × classes (nota 0)

| Feedback | Ação | Status |
|----------|------|--------|
| `Alarme.emitirAlerta` / `registrarEvento` | Incluídos em classes + SEQ-UC02 | Feito |
| `MedicaoReator.registrarMedicao` | Incluído + SEQ-UC01 | Feito |
| `ProtocoloEmergencia` ausente | Classe `«future»` / Could documentada | Feito (doc) / Backlog código |
| `RegistroAcesso` ausente | Classe Should documentada | Feito (doc) |
| `Emergencia.criarEmergencia` | Documentado em Could | Feito (doc) |
| `HistoricoSubstituicao` | Listado em backlog classes | Backlog |
| Registro só no controller (materiais) | Regra: registro na entidade; materiais fora do Must | Backlog RF15 |

---

## C) Modelo relacional (nota 4) — núcleo

| Feedback | Ação | Status |
|----------|------|--------|
| Usar VARCHAR não String | ER núcleo usa VARCHAR | Feito |
| Enums → tabelas | status/tipo sensor, alarme, operacional | Feito |
| FK Sensor–Alarme | `alarme.id_sensor` | Feito |
| FK Medicao→Sensor | `medicao_reator.id_sensor` | Feito |
| FK Turbina→Reator (não o inverso) | `turbina.id_reator` | Feito |
| FK StatusOperacional em Reator | `reator.id_status_operacional` | Feito |
| Emergencia_Alarme N:N injustificado | Removido do núcleo | Feito |
| Sem agregação no relacional | Só FKs/cardinalidades | Feito |
| Comparação de chaves nos exemplos | Seção 5 do doc 08 | Feito |
| RH/Capacitacao/Alocacao/Acesso.id_cargo | Won't / Should sem id_cargo | Backlog / parcial |
| Material/Movimentacao FKs | Fora do núcleo MVP | Backlog |

---

## D) Entregáveis pedidos pelo professor (Marco 1)

| Item | Arquivo | Status |
|------|---------|--------|
| RF e RNF | 01-requisitos-rf-rnf.md | Feito |
| Priorização | 02-priorizacao-moscow.md | Feito |
| Proposta MVP | 03-proposta-mvp.md | Feito |
| Pacotes | diagramas/pacotes.puml + 04 | Feito |
| Componentes lógicos | diagramas/componentes-logicos.puml | Feito |
| Componentes físicos | diagramas/componentes-fisicos.puml | Feito |
| UCs / classes / sequências / ER | 05–08 | Feito |
| Esqueleto Java | src/main/java/... | Feito |
| PDF consolidado | ENTREGA-MARCO1.md (exportar PDF) | Feito |
