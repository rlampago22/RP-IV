# 01 — Requisitos Funcionais e Não Funcionais

**Disciplina:** AL0343 — Resolução de Problemas IV  
**Sistema:** Controle de Usina Nuclear  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol  
**Versão:** 2.1 — Must expandido + 2FA por celular (Marco 2)

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

### Segundo fator de autenticação (Marco 2 — Must)

| ID | Requisito | Descrição |
|----|-----------|-----------|
| RF18 | Confirmar acesso com 2FA por celular | Depois de validar e-mail e senha, o sistema deve solicitar um segundo fator no celular previamente vinculado ao usuário. O acesso à Central só é liberado após a confirmação no celular **ou** a validação do código recebido nesse dispositivo. |

#### UC05 — Confirmar acesso à Central com 2FA por celular

| Campo | Conteúdo |
|-------|----------|
| Objetivo | Concluir a autenticação na Central por meio de um segundo fator vinculado ao celular do usuário. |
| Ator primário | Usuário da Central (Operador de Reator, Supervisão Central, Guarda / Controle de Acesso ou Administrador do Sistema). |
| Ator secundário | Celular previamente vinculado ao usuário. |
| Pré-condições | Usuário ativo, celular vinculado e primeiro fator (e-mail e senha) válido. |
| Pós-condições de sucesso | Sessão autenticada é criada e o usuário pode acessar somente os recursos autorizados ao seu papel. |
| Pós-condições de falha | Nenhuma sessão autenticada é criada; a tentativa fica registrada para auditoria. |

**Fluxo principal:**

1. O usuário informa e-mail e senha na Central.
2. O sistema valida o primeiro fator e cria uma solicitação de segundo fator associada à tentativa de acesso.
3. O sistema envia a solicitação ao celular vinculado.
4. O usuário confirma a solicitação no celular **ou** informa na Central o código recebido no celular.
5. O sistema valida a confirmação ou o código e libera a sessão autenticada.
6. O sistema registra o resultado da autenticação sem expor o código ou o telefone em logs/mensagens.

**Alternativas e exceções:**

| ID | Condição | Tratamento |
|----|----------|------------|
| A1 | Usuário rejeita a solicitação no celular | Sistema nega o acesso e registra a tentativa. |
| A2 | Código informado é inválido, já utilizado ou expirado | Sistema mantém o acesso negado e permite iniciar uma nova solicitação conforme a política de segurança. |
| E1 | Não há celular vinculado ou o envio não pode ser iniciado | Sistema não cria sessão e informa que o segundo fator não pôde ser concluído, sem revelar dados sensíveis. |

**Critérios de aceite:**

- Com e-mail e senha válidos, a Central deve permanecer em estado de autenticação pendente até a confirmação no celular ou a validação do código.
- A confirmação correta no celular ou o código válido deve liberar uma única sessão para o usuário autenticado.
- Rejeição, código inválido, reutilizado ou expirado, e falha de envio não podem liberar sessão nem recursos protegidos.
- A solicitação e o código devem estar vinculados à tentativa de autenticação; o código não pode ser reutilizado.
- O resultado da tentativa deve ser auditável, sem registrar ou exibir código, telefone ou outros dados sensíveis.

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
| RNF08 | Segurança | Políticas de acesso granuláveis por módulo/área; sessão autenticada na Central e segundo fator vinculado ao celular do usuário. | Pacote SegurancaAcesso + auth web |
| RNF09 | Persistência segura do núcleo | Medições e alarmes com backup/recuperação simples no MVP. | Auditoria + schema de auth |
| RNF10 | Testabilidade | Módulos testáveis com eventos simulados. | Event bus mockável |

### Situação no MVP expandido

- **Must de qualidade:** RNF01–RNF05, RNF08–RNF10 entram no aceite. Para RF18, RNF08 exige que a sessão só exista após os dois fatores e que segredos do segundo fator não sejam expostos.
- **Atendidos / demonstráveis:** RNF03, RNF05 e RNF10; isolamento básico RNF02/RNF04 por subscriber.
- **Parciais:** RNF08 (login + papéis implementados; 2FA de RF18 pendente), RNF09 (auditoria em arquivo; medições/alarmes ainda em memória no `mvp/`) e RNF01 (bus in-process).
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
