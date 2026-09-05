# 01 — Requisitos Funcionais e Não Funcionais

**Disciplina:** AL0343 — Resolução de Problemas IV  
**Sistema:** Controle de Usina Nuclear  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

---

## 1. Requisitos Funcionais (RF)

Escopo refinado a partir do legado APS, com foco no que o sistema de controle digital realmente faz (sensores físicos e intervenções médicas/estruturais permanecem fora).

### Núcleo operacional (MVP Must)

| ID | Requisito | Descrição |
|----|-----------|-----------|
| RF01 | Coletar medições de reator | Registrar temperatura, pressão, radiação, fluxo de resfriamento e demais parâmetros operacionais recebidos dos sensores já existentes. |
| RF02 | Histórico operacional | Manter histórico de medições e status operacional do reator para consulta e rastreabilidade. |
| RF03 | Avaliar limiares | Comparar medições com limites seguros configurados. |
| RF04 | Emitir alarme | Quando um limiar for violado, emitir alarme e notificar Operador e Supervisão Central. |
| RF05 | Registrar evento de alarme | Persistir ocorrência do alarme (tipo, severidade, timestamp, sensor/medição de origem). |
| RF06 | Auditar eventos | Registrar trilha de eventos relevantes (medições críticas, alarmes, publicações no barramento). |

### Acesso (MVP Should)

| ID | Requisito | Descrição |
|----|-----------|-----------|
| RF07 | Autenticar acesso a área restrita | Validar credencial (crachá/biometria simulada) para áreas classificadas. |
| RF08 | Registrar acesso | Persistir tentativa/resultado de acesso em `RegistroAcesso` (sucesso ou falha). |

### Contingência (MVP Could)

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
| RNF05 | Auditabilidade | Histórico imutável (ou append-only) de eventos relevantes. | Pacote AuditoriaLogs + eventos |
| RNF06 | Manutenibilidade | Alterar limiares/estratégia de alarme sem reescrever todo o sistema. | Módulos + Strategy (Marco 2) |
| RNF07 | Escalabilidade | Possibilidade futura de escalar consumidores independentemente. | Persistência dedicada + bus |
| RNF08 | Segurança | Políticas de acesso granuláveis por módulo. | Pacote SegurancaAcesso |
| RNF09 | Persistência segura do núcleo | Medições e alarmes com backup/recuperação simples no MVP. | DB dedicado do módulo reator |
| RNF10 | Testabilidade | Módulos testáveis com eventos simulados. | Event bus mockável |

---

## 3. Atores

| Ator | Tipo | Papel |
|------|------|-------|
| Operador de Reator | Primário | Monitora medições, recebe alarmes, pode acionar contingência |
| Supervisão Central | Secundário | Recebe alertas e visão agregada |
| Sensor (sistema externo) | Secundário | Publica leituras (simulado no MVP) |
| Guarda / Controle de Acesso | Primário (Should) | Solicita entrada em área restrita |
| Administrador do Sistema | Primário (config) | Configura limiares e parâmetros |
