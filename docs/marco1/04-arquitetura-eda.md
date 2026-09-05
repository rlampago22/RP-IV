# 04 — Arquitetura EDA (mantida)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

---

## 1. Decisão

A arquitetura **não foi redesenhada**. Mantém-se o estilo híbrido já justificado na APS:

- Arquitetura Orientada a Eventos (**EDA**)
- Módulos independentes (design modular)
- Persistência dedicada por módulo
- Distribuição lógica (no MVP acadêmico: processos/pacotes; sem cluster obrigatório)

## 2. Componentes lógicos da EDA no MVP

| Elemento | Papel |
|----------|--------|
| SensorSimulator | Produtor de eventos `MedicaoRegistrada` |
| InfraestruturaEventos (EventBus) | Barramento in-process; desacopla produtores e consumidores |
| ControleReator | Consome medições, orquestra persistência e avaliação de limiar |
| Alarmes | Emite/registra alarmes; publica `AlarmeEmitido` |
| AuditoriaLogs | Consome eventos relevantes; trilha append-only |
| PersistenciaReator | DB dedicado do núcleo (H2/SQLite no Marco 2) |
| SegurancaAcesso | Stub/`«future»` no Marco 1; Should no Marco 3 |

## 3. Ligação aos RNFs

| RNF | Mecanismo arquitetural |
|-----|------------------------|
| RNF01 Desempenho | Publicação assíncrona; produtor não espera consumidores |
| RNF02 Disponibilidade | Falha em AuditoriaLogs não impede ControleReator |
| RNF03 Integridade | Evento como fato; validação antes do publish |
| RNF04 Tolerância a falhas | Isolamento por módulo |
| RNF05 Auditabilidade | Consumidor dedicado de auditoria |
| RNF06 Manutenibilidade | Limiares/estratégias trocáveis sem reescrever o bus |
| RNF07 Escalabilidade | Consumidores independentes (futuro: mais instâncias) |
| RNF09 Persistência do núcleo | Pacote PersistenciaReator separado |
| RNF10 Testabilidade | Bus mockável; simulador de eventos |

## 4. O que permanece `«future»`

Pacotes do legado APS fora do Must: GestaoRH, Evacuacao, AnaliseRelatorios, GestaoManutencao (exceto se Could entrar), rastreio completo de materiais, etc. Permanecem no diagrama de pacotes para não “sumir” com o legado, mas **não entram no MVP**.

## 5. Diagramas

Fontes PlantUML:

- [diagramas/pacotes.puml](diagramas/pacotes.puml)
- [diagramas/componentes-logicos.puml](diagramas/componentes-logicos.puml)
- [diagramas/componentes-fisicos.puml](diagramas/componentes-fisicos.puml)

Renderização textual também está em [ENTREGA-MARCO1.md](ENTREGA-MARCO1.md) (Mermaid) para leitura sem ferramenta.

## 6. Evolução Marco 2+

No código Java, os pacotes espelham este desenho. Padrões GoF previstos:

- Observer / Pub-Sub (EventBus)
- Strategy (avaliação de limiares)
- Factory (criação de eventos/alarmes)
- Facade (controladores de módulo)
