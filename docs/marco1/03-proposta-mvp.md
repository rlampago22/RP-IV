# 03 — Proposta de MVP

**Sistema:** Controle de Usina Nuclear  
**Disciplina:** AL0343 — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol  
**Versão:** 2.0 — MVP Must expandido (núcleo + acesso + demo integrada)

---

## 1. Problema

Operadores precisam acompanhar parâmetros de reatores em tempo quase real, receber alarmes quando limites seguros são violados, manter trilha auditável dos eventos e **autenticar-se** na Central antes de agir — sem acoplar o monitoramento a módulos secundários (RH, evacuação, relatórios regulatórios amplos).

## 2. Solução MVP

Um software modular com **arquitetura orientada a eventos (EDA)**:

1. Sensores simulados (ou cenários via API) enviam leituras ao **ControleReator**.
2. O **ControleReator** mantém medições e publica eventos no barramento.
3. O módulo **Alarmes** consome as medições, avalia limiares, mantém alarmes e notifica Operador e Supervisão Central.
4. O módulo **AuditoriaLogs** consome eventos e grava trilha append-only encadeada por SHA-256.
5. A **API de estado** (`apiestado`) agrega o snapshot para a UI React (T01–T05).
6. O módulo de **acesso/autenticação** valida identidade (login web + registro de acesso) e amarra o operador às ações de alarme.

Persistência Must: auditoria em arquivo; autenticação com schema próprio no `backend/` (Spring). Banco completo para medições/alarmes e mensageria durável permanecem evoluções pós-Must.

## 3. Escopo incluso (Must)

- RF01–RF08
- RNF01–RNF05, RNF08–RNF10
- API HTTP `/api/*` + UI Opção A T01–T05
- Ciclo de alarme reconhecer → resolver com identidade
- Diagramas de pacotes e componentes (lógico + físico)
- Casos de uso e sequências do núcleo + UC04 (acesso)
- Mapeamento relacional do núcleo (+ auth)

## 4. Escopo excluído (Won't nesta disciplina)

Evacuação, RH/treinamentos, conformidade ampla, IA preditiva, rastreio completo de materiais, implantação em cluster.

## 5. Critérios de aceite do MVP

| # | Critério |
|---|----------|
| 1 | É possível simular N medições e consultá-las no histórico / UI durante a execução |
| 2 | Medição acima do limiar gera alarme, notificação aos atores e evento na auditoria |
| 3 | Eventos passam pelo barramento (produtor não chama consumidor diretamente) |
| 4 | Pelo menos 3 padrões GoF documentados e visíveis no código |
| 5 | README permite executar o fluxo ponta a ponta (API + UI) |
| 6 | Login autentica operador/supervisão; sessão identifica ações de alarme |
| 7 | Tentativa de acesso restringido gera `RegistroAcesso` persistido |

## 6. Entregas por marco

| Marco | Entrega |
|-------|---------|
| **1 (05/10/2026) + evolução imediata** | **MVP Must expandido:** RF01–RF08 + RNFs, GoF, auditoria, API estado, UI T01–T05, auth, demo completa |
| **2** | Should (emergência/protocolo), polish de integração e persistência além do núcleo |
| **3** | Ampliações Could e preparação de demonstração final da disciplina |

## 7. Riscos e mitigação

| Risco | Mitigação |
|-------|-----------|
| Escopo herdar todos os UCs da APS | MoSCoW vigente em `02-priorizacao-moscow.md`; `«future»` nos diagramas |
| Segunda stack (Spring auth vs `mvp/` JDK) | Auth só no `backend/`; telemetria permanece em `mvp/apiestado` — sem merge cego da branch `alvaro` |
| Inconsistência classes × sequência | Checklist; sequências só com métodos existentes |
| Atraso | Must expandido primeiro; RF09–RF10 opcionais |

## 8. Valor pedagógico

O MVP demonstra o que a disciplina avalia: **padrão arquitetural escolhido e justificado** + **padrões de projeto** + **fluxo operacional completo** (incluindo identidade), sem redesenhar a arquitetura nem absorver o APS inteiro.
