# 04 — Arquitetura Orientada a Eventos (EDA)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol  
**Decisão (atualizada após o feedback do professor de 04/11/2026):** a solução é **híbrida**: EDA organiza o fluxo operacional e a UI de supervisão; o backend Spring de autenticação é um monólito em camadas, com responsabilidades MVC.

---

## 1. Decisão arquitetural híbrida

EDA continua sendo o estilo arquitetural do núcleo operacional: fatos de domínio são publicados e consumidores reagem de forma desacoplada. Isso não é substituído pelo backend web.

O backend de autenticação em `backend/` tem outra responsabilidade: expor a API HTTP da Central e aplicar autenticação, autorização e persistência de seus registros. Por ser um monólito Spring, ele é organizado em camadas/MVC, sem introduzir microserviços ou um segundo barramento de domínio.

| Parte da solução | Estilo predominante | Responsabilidade |
|------------------|--------------------|------------------|
| `mvp/` | EDA com EventBus in-process | Medições, limiares, alarmes, auditoria e registro de acesso físico. |
| `frontend/` | UI reativa orientada aos eventos/estados operacionais | Apresenta a evolução de medições, alarmes e auditoria nas telas T01–T05. |
| `backend/` | Monólito Spring em camadas/MVC | Endpoints HTTP, sessão, autorização, regras de autenticação e persistência de usuários/registros. |

No núcleo EDA, os produtores e consumidores seguem organizados em torno de **eventos de domínio** publicados e consumidos via barramento:

- Produtores publicam fatos (`MedicaoRegistrada`, `AlarmeEmitido`, …)
- Consumidores reagem de forma desacoplada
- O MVP usa **EventBus in-process** (Java); a EDA permanece válida se o bus evoluir

**Módulos, Facade e persistência dedicada** são mecanismos de realização da EDA no núcleo. Controller, serviço e repositório são mecanismos de organização do backend Spring; não alteram os contratos de eventos do domínio.

---

## 2. Componentes lógicos (EDA)

| Elemento | Papel na EDA |
|----------|----------------|
| Sensor / simulador | Produtor de `MedicaoRegistrada` |
| InfraestruturaEventos (EventBus) | Barramento; desacopla pub/sub |
| ControleReator | Orquestra medição; pode publicar e disparar avaliação |
| Alarmes | Consome/avalia; publica `AlarmeEmitido` |
| AuditoriaLogs | Consumidor append-only |
| RegistroAuditoria | Persistência em arquivo append-only com cadeia SHA-256 |
| Frontend React | UI reativa: apresenta estados e eventos operacionais nas telas T01–T05 |
| API HTTP | Adaptadores para a UI: `mvp/apiestado` para estado do núcleo e `backend/` para autenticação |

---

## 3. Ligação aos RNFs

| RNF | Mecanismo EDA |
|-----|----------------|
| RNF01 Desempenho | **Parcial:** publish síncrono no MVP; assíncrono permanece como evolução |
| RNF02 Disponibilidade | **Parcial:** exceções de assinantes são isoladas, mas não há redundância |
| RNF03 Integridade | Evento como fato; validação antes do publish |
| RNF04 Tolerância a falhas | Isolamento por assinante/módulo |
| RNF05 Auditabilidade | Consumidor dedicado de auditoria |
| RNF06 Manutenibilidade | Novos consumidores sem alterar produtores |
| RNF07 Escalabilidade | Contratos permitem novos consumidores; escala distribuída ainda não existe |
| RNF09 Persistência | **Parcial:** auditoria persiste; medições/alarmes ficam em memória |
| RNF10 Testabilidade | Eventos simulados e 11 cenários automatizados locais |

---

## 4. Fora do MVP (`«future»`)

GestaoRH, Evacuacao, AnaliseRelatorios, rastreio amplo de materiais, etc. — legado APS, **não** entram no Must. Podem aparecer no diagrama de pacotes só como `«future»`.

---

## 5. Diagramas essenciais (versionados)

| Diagrama | Arquivo |
|----------|---------|
| Pacotes (EDA) | [diagramas/pacotes.puml](diagramas/pacotes.puml) |
| Componentes lógicos | [diagramas/componentes-logicos.puml](diagramas/componentes-logicos.puml) |
| Componentes físicos | [diagramas/componentes-fisicos.puml](diagramas/componentes-fisicos.puml) |
| Casos de uso MVP | [Astah editável](../marcus/diagramas/Marcus-UML-MVP.asta) |
| Classes núcleo | [diagramas/classes-projeto-mvp.puml](diagramas/classes-projeto-mvp.puml) |
| SEQ UC01 / UC02 | [Astah editável](../marcus/diagramas/Marcus-UML-MVP.asta) |

Outros artefatos (Astah APS completo, ER auxiliar) ficam em arquivo legado / não são entrega ativa.

---

## 6. Backend Spring: camadas/MVC

O backend mantém controllers finos e separa as responsabilidades hoje presentes em `backend/src/main/java`:

| Camada | Elementos atuais | Papel |
|--------|------------------|-------|
| Controller | `AutenticacaoController`, `CentralController` | Recebem HTTP, validam a entrada de borda e devolvem respostas HTTP. |
| Serviço/aplicação | `AutenticacaoService` | Carrega usuário ativo, mapeia papéis e apresenta o contrato de saída. |
| Domínio/persistência | `Usuario`, `Papel`, `RegistroAutenticacao` e seus repositórios | Modelam e persistem usuários, papéis e registros de autenticação. |
| Infraestrutura | `SecurityConfig`, Flyway e Spring Security | Configuram sessão, autorização, CORS, hash de senha e schema. |

RF18 seguirá essa separação: controller recebe as etapas de login/validação; serviço coordena a regra de segundo fator; repositórios persistem apenas os dados necessários; o adaptador de comunicação com o celular fica isolado da regra de negócio. A criação da sessão ocorre somente após a confirmação do segundo fator.

## 7. Stack de realização

- **Núcleo de domínio:** Java (EDA + EventBus)
- **Frontend:** React (Vite) — supervisão web reativa aos estados/eventos expostos pela integração
- **Backend de autenticação:** Spring Boot monolítico em camadas/MVC
- **Integração:** API HTTP entre UI, `mvp/apiestado` e `backend/`; a evolução para eventos reais do Java permanece prevista
- **GoF:** Observer/Pub-Sub, Strategy, Factory, Facade
