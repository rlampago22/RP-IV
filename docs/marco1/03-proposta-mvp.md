# 03 — Proposta de MVP

**Sistema:** Controle de Usina Nuclear  
**Disciplina:** AL0343 — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

---

## 1. Problema

Operadores precisam acompanhar parâmetros de reatores em tempo quase real, receber alarmes quando limites seguros são violados e manter trilha auditável dos eventos — sem acoplar o monitoramento a módulos secundários (RH, evacuação, relatórios).

## 2. Solução MVP

Um software Java modular com **arquitetura orientada a eventos (EDA)**:

1. Um **simulador de sensores** publica eventos de medição no barramento.
2. O módulo **ControleReator** persiste a medição e avalia limiares.
3. O módulo **Alarmes** emite e registra alarmes, notificando Operador e Supervisão Central.
4. O módulo **AuditoriaLogs** consome eventos relevantes e grava trilha append-only.

A persistência do núcleo fica no pacote **PersistenciaReator** (dedicada). Demais módulos do legado APS permanecem no diagrama como `«future»`.

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
| 1 | É possível simular N medições e vê-las persistidas |
| 2 | Medição acima do limiar gera alarme persistido e notificação explícita aos atores |
| 3 | Eventos passam pelo barramento (produtor não chama consumidor diretamente) |
| 4 | Pelo menos 3 padrões GoF documentados e visíveis no código (ex.: Observer/Pub-Sub, Strategy, Factory, Facade) |
| 5 | README permite executar o fluxo ponta a ponta |

## 6. Entregas por marco

| Marco | Entrega |
|-------|---------|
| **1 (atual)** | Documentação: RF/RNF, MoSCoW, MVP, arquitetura, pacotes, componentes, UCs/classes/sequências/ER do núcleo + esqueleto de pacotes Java |
| **2** | Implementação do fluxo Must + padrões de projeto |
| **3** | Should (acesso) **ou** Could (contingência) + polish/testes |

## 7. Riscos e mitigação

| Risco | Mitigação |
|-------|-----------|
| Escopo herdar todos os UCs da APS | MoSCoW estrito; `«future»` nos diagramas |
| Inconsistência classes × sequência (nota 0 APS) | Checklist; sequências só com métodos existentes |
| Atraso | Núcleo Must primeiro; Should/Could opcionais |

## 8. Valor pedagógico

O MVP demonstra exatamente o que a disciplina avalia: **padrão arquitetural escolhido e justificado** + **aplicação de padrões de projeto** em um problema contínuo da APS, sem redesenhar a arquitetura.
