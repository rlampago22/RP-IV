# 04 — Arquitetura Orientada a Eventos (EDA)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol  
**Decisão:** a **única** arquitetura do projeto é **EDA** (Event-Driven Architecture).

---

## 1. Decisão (EDA only)

Não adotamos arquitetura híbrida, MVC como estilo principal, nem microserviços/cluster no MVP.

Tudo no sistema se organiza em torno de **eventos de domínio** publicados e consumidos via barramento:

- Produtores publicam fatos (`MedicaoRegistrada`, `AlarmeEmitido`, …)
- Consumidores reagem de forma desacoplada
- O MVP usa **EventBus in-process** (Java); a EDA permanece válida se o bus evoluir

**Módulos, Facade e persistência dedicada** não são “outras arquiteturas”: são **mecanismos de realização da EDA** (isolamento de produtores/consumidores e de dados do núcleo).

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
| Frontend React | T01–T05 integradas por estado demonstrativo compartilhado |
| API HTTP | Adaptador futuro entre a interface web e o núcleo Java |

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

## 6. Stack de realização

- **Backend:** Java (EDA + EventBus)
- **Frontend:** React (Vite) — supervisão web; hoje usa estado demonstrativo compartilhado
- **Integração futura:** API HTTP para substituir o estado simulado por eventos reais do Java
- **GoF:** Observer/Pub-Sub, Strategy, Factory, Facade
