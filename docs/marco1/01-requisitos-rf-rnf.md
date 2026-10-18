# 01 — Requisitos Funcionais e Não Funcionais

**Disciplina:** AL0343 — Resolução de Problemas IV  
**Sistema:** Controle de Usina Nuclear  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol  
**Versão:** 2.0 — Must expandido (acesso no MVP)

---

## 1. Requisitos Funcionais (RF)

Escopo refinado a partir do legado APS, com foco no que o sistema de controle digital realmente faz (sensores físicos e intervenções médicas/estruturais permanecem fora).

### Núcleo operacional + acesso (MVP Must)

| ID | Requisito | Descrição |
|----|-----------|-----------|
| RF01 | Coletar medições de reator | Registrar temperatura, pressão, radiação, fluxo de resfriamento e demais parâmetros operacionais recebidos dos sensores já existentes. |
| RF02 | Histórico operacional | Manter histórico de medições e status operacional do reator para consulta e rastreabilidade. |
| RF03 | Avaliar limiares | Comparar medições com limites seguros configurados. |
| RF04 | Emitir alarme | Quando um limiar for violado, emitir alarme e notificar Operador e Supervisão Central. |
| RF05 | Registrar evento de alarme | Persistir ocorrência do alarme (tipo, severidade, timestamp, sensor/medição de origem). |
| RF06 | Auditar eventos | Registrar trilha de eventos relevantes (medições críticas, alarmes, publicações no barramento). |
| RF07 | Autenticar acesso restrito | Validar identidade na Central (login web) e/ou credencial simulada (crachá/biometria) para áreas classificadas (UC04). |
| RF08 | Registrar acesso | Persistir tentativa/resultado de acesso em `RegistroAcesso` (sucesso ou falha). |

### Consulta e exportação do histórico (MVP Could) — proposta, pendente de aprovação do grupo

| ID | Requisito | Descrição |
|----|-----------|-----------|
| RF16 | Consultar histórico operacional | Permitir que o operador filtre o histórico de medições por período e por faixa de valor de cada parâmetro RF-1 (temperatura, pressão, radiação, fluxo de resfriamento), e consulte os alarmes do mesmo período. |
| RF17 | Exportar histórico consultado | Entregar o resultado da consulta em arquivo CSV, sem dependência externa de biblioteca. |

**Por que isso surgiu:** RF02 (Must) já exige manter o histórico *"para consulta e rastreabilidade"*. A metade "manter" foi entregue; a de **consulta** nunca foi exposta. RF16/RF17 realizam essa cláusula, implementados por Bruno (PR #71) como prova de conceito — ainda não promovidos a Should na MoSCoW oficial (`02-priorizacao-moscow.md`).

**Dependência técnica:** exige fechar **RNF09** (hoje `ReatorRepository` é janela em memória de `CAPACIDADE_PADRAO = 200` medições, ~2,5 min com tempo real ligado, e não sobrevive a reinício). Sem persistência durável, filtro por data não tem sentido.

**Decisão de modelagem registrada:** o histórico consultável é gravado como **snapshot por instante** (os 4 parâmetros em uma linha), e não como uma medição por linha. Motivo: uma `MedicaoRegistrada` pertence a um único sensor, logo filtros combinados ("temperatura entre X e Y **e** pressão entre W e Z") retornariam sempre vazio no modelo por medição.

**Delimitação de escopo:** RF16/RF17 são **exportação de histórico operacional**, não relatório de conformidade regulatória — RF13 permanece **Won't**.

### Contingência (MVP Should)

| ID | Requisito | Descrição |
|----|-----------|-----------|
| RF09 | Criar emergência | Permitir que operador autorizado registre uma emergência (`Emergencia.criarEmergencia`). |
| RF10 | Ativar protocolo de contingência | Associar e acionar `ProtocoloEmergencia` a partir de uma emergência ativa. |

### Fora do MVP desta disciplina (Won't — documentados para rastreio)

| ID | Requisito | Motivo |
|----|-----------|--------|
| RF11 | Gestão completa de evacuação | Complexidade alta; fora do Must/Should |
| RF12 | RH, cargos e treinamentos | Não críticos para demonstrar EDA + alarmes |
| RF13 | Relatórios de conformidade regulatória | Pode ser derivado depois |
| RF14 | Predição de falhas por IA | Especulativo; não priorizado |
| RF15 | Rastreio completo de material radioativo | Backlog pós-núcleo |

### Fora do escopo do software (infraestrutura física)

- Instalação/manutenção de sensores físicos
- Procedimentos médicos de descontaminação
- Construção de barreiras físicas de contenção

---

## 2. Requisitos Não Funcionais (RNF)

Alinhados à justificativa da arquitetura EDA mantida.

| ID | Atributo | Descrição | Como a EDA contribui |
|----|----------|-----------|----------------------|
| RNF01 | Desempenho / tempo de resposta | Publicação de medição não deve bloquear o produtor enquanto consumidores processam. | Comunicação assíncrona via barramento |
| RNF02 | Confiabilidade / disponibilidade | Falha em consumidor de relatório não derruba monitoramento crítico. | Baixo acoplamento; fila/retenção de eventos |
| RNF03 | Integridade | Medições e alarmes devem ser precisos e rastreáveis à origem. | Evento como fato; validação no produtor |
| RNF04 | Tolerância a falhas | Isolar falha de módulo (ex.: auditoria) sem parar ControleReator. | Módulos independentes |
| RNF05 | Auditabilidade | Histórico append-only de eventos relevantes com adulteração detectável. | Pacote AuditoriaLogs + eventos |
| RNF06 | Manutenibilidade | Alterar limiares/estratégia de alarme sem reescrever todo o sistema. | Módulos + Strategy |
| RNF07 | Escalabilidade | Possibilidade futura de escalar consumidores independentemente. | Persistência dedicada + bus |
| RNF08 | Segurança | Políticas de acesso granuláveis por módulo/área; sessão autenticada na Central. | Pacote SegurancaAcesso + auth web |
| RNF09 | Persistência segura do núcleo | Medições e alarmes com backup/recuperação simples no MVP. | Auditoria + schema de auth |
| RNF10 | Testabilidade | Módulos testáveis com eventos simulados. | Event bus mockável |

### Situação no MVP expandido

- **Must de qualidade:** RNF01–RNF05, RNF08–RNF10 entram no aceite.
- **Atendidos / demonstráveis:** RNF03, RNF05, RNF08 (login + papéis), RNF10; isolamento básico RNF02/RNF04 por subscriber.
- **Parciais:** RNF09 (auditoria em arquivo; medições/alarmes ainda em memória no `mvp/`); RNF01 (bus in-process).
- **Planejados (não Must):** RNF06/RNF07 plenos, banco para medições/alarmes, fila durável.

---

## 3. Atores

| Ator | Tipo | Papel |
|------|------|-------|
| Operador de Reator | Primário | Autentica-se, monitora medições, recebe e trata alarmes |
| Supervisão Central | Secundário / Primário (visão) | Recebe alertas e visão agregada; autentica-se na Central |
| Sensor (sistema externo) | Secundário | Publica leituras (simulado no MVP) |
| Guarda / Controle de Acesso | Primário (Must — UC04) | Solicita entrada em área restrita |
| Administrador do Sistema | Primário (config) | Configura limiares e parâmetros; papel de admin no login |
