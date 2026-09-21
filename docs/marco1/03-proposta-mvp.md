# 03 — Proposta de MVP

**Sistema:** Controle de Usina Nuclear  
**Disciplina:** AL0343 — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

---

## 1. Problema

Operadores precisam acompanhar parâmetros de reatores em tempo quase real, receber alarmes quando limites seguros são violados e manter trilha auditável dos eventos — sem acoplar o monitoramento a módulos secundários (RH, evacuação, relatórios).

## 2. Solução MVP

Um software Java modular com **arquitetura orientada a eventos (EDA)**:

1. A aplicação de demonstração envia leituras simuladas ao **ControleReator**.
2. O **ControleReator** mantém medições em memória e publica eventos no barramento.
3. O módulo **Alarmes** consome as medições, avalia limiares, mantém os alarmes em memória e notifica Operador e Supervisão Central pelo console.
4. O módulo **AuditoriaLogs** consome todos os eventos e grava uma trilha append-only encadeada por SHA-256.

Nesta versão, apenas a auditoria possui persistência em arquivo. Banco para medições e alarmes, API HTTP e mensageria durável são evoluções planejadas, não funcionalidades prontas.

## 3. Escopo incluso (Must)

- RF01–RF06
- Diagramas de pacotes e componentes (lógico + físico)
- Casos de uso e sequências do núcleo alinhados às classes
- Mapeamento relacional corrigido do núcleo

## 4. Escopo excluído (Won't nesta disciplina)

Evacuação, RH/treinamentos, conformidade ampla, IA preditiva, rastreio completo de materiais, implantação em cluster.

## 5. Critérios de aceite do MVP (fim da disciplina / Marco 2+)

| # | Critério |
|---|----------|
| 1 | É possível simular N medições e consultá-las no histórico em memória durante a execução |
| 2 | Medição acima do limiar gera alarme em memória, notificação explícita aos atores e evento persistido na auditoria |
| 3 | Eventos passam pelo barramento (produtor não chama consumidor diretamente) |
| 4 | Pelo menos 3 padrões GoF documentados e visíveis no código (ex.: Observer/Pub-Sub, Strategy, Factory, Facade) |
| 5 | README permite executar o fluxo ponta a ponta |

## 6. Entregas por marco

| Marco | Entrega |
|-------|---------|
| **1** | Documentação inicial, arquitetura e esqueleto de pacotes Java |
| **2** | Fluxo Must funcional + padrões de projeto + auditoria persistente |
| **3 (em preparação)** | Interface React T01–T05, integração HTTP e preparação da demonstração |

## 7. Riscos e mitigação

| Risco | Mitigação |
|-------|-----------|
| Escopo herdar todos os UCs da APS | MoSCoW estrito; `«future»` nos diagramas |
| Inconsistência classes × sequência (nota 0 APS) | Checklist; sequências só com métodos existentes |
| Atraso | Núcleo Must primeiro; Should/Could opcionais |

## 8. Valor pedagógico

O MVP demonstra exatamente o que a disciplina avalia: **padrão arquitetural escolhido e justificado** + **aplicação de padrões de projeto** em um problema contínuo da APS, sem redesenhar a arquitetura.
