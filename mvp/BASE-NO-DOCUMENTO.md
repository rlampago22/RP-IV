# Como o MVP foi Derivado do Documento Oficial do Projeto

Fonte analisada: `ATUALIZADO -Análise e Projeto de Software Oitava Entrega_ Diagrama de Implantação.pdf` (69 páginas).

O documento descreve um sistema amplo de controle para uma usina nuclear. O MVP implementa o **recorte acadêmico de supervisão e telemetria** definido pelo grupo: RF01–RF06 refatorados e evidências locais de integridade, tolerância a falhas, auditabilidade e testabilidade. Alta disponibilidade, backup, API HTTP e persistência completa ainda não são declarados como concluídos.

> A numeração do PDF legado e a numeração do RP IV não são equivalentes. A rastreabilidade abaixo usa o significado funcional do requisito e aponta a implementação real, sem afirmar que todo o sistema de 69 páginas foi entregue.

---

## 1. Matriz de Rastreabilidade (Requisitos do Documento → Implementação no MVP)

| Código | Requisito / Item do Documento | Como está Contemplado no MVP | Evidência / Classe |
|---|---|---|---|
| **RF-1** | **Monitoramento em Tempo Real de Reatores:** Coleta contínua de temperatura, pressão, radiação e fluxo de resfriamento. | Painel com os 4 sensores operacionais em tempo real: `TEMPERATURA` (0–350 °C), `PRESSAO` (0–160 bar), `RADIACAO` (0–5 mSv/h) e `FLUXO_RESFRIAMENTO` (500–1500 m³/h). Suporte a modo contínuo de telemetria automática. | `ReatorFacade.java`, `Sensor.java`, `SistemaMvpUI.java` |
| **RF-2** | **Geração de Alarmes:** Disparo automático de alarmes quando os valores saem dos limites seguros com notificação imediata. | `AvaliadorFaixaSegura` avalia cada medição. Se violar os limites, emite `AlarmeEmitido` com severidade `CRITICA` notificando `Operador de Reator` e `Supervisão Central`. | `AlarmeFacade.java`, `AvaliadorFaixaSegura.java`, `Alarme.java` |
| **UC01 (Principal)** | **Monitoramento de Reatores e Turbinas:** Início de monitoramento, coleta em tempo real, detecção de anomalias e visualização gráfica do estado do núcleo. | Central de Supervisão com telemetria, widget visual do núcleo do reator (mudança cromática `ESTAVEL` ↔ `CRITICO`), linha do tempo e painel de alarmes. | `SistemaMvpUI.java`, `MedicaoRegistrada.java` |
| **UC01 (Alt. 1)** | **Registro de Observação Preventiva:** Detecta valores fora da faixa ideal de alerta, mas dentro dos limites normais, registrando observação sem alarme crítico. | Cada sensor possui limiares de atenção preventiva. Quando atingidos, emite `ObservacaoRegistrada` para auditoria sem soar alarme crítico. | `ObservacaoRegistrada.java`, `ReatorFacade.java` |
| **UC01 (Exceção)** | **Falha na Comunicação com Sensor:** Detecta falha de sensor e emite alerta de manutenção para a equipe técnica. | Método `simularFalhaSensor` dispara `FalhaSensorDetectada` alertando explicitamente a `Equipe Técnica` e o `Engenheiro de Segurança`. | `FalhaSensorDetectada.java`, `ReatorFacade.java` |
| **UC01 (Validação)** | **Validação e Resolução pelo Operador:** Operadores e engenheiros monitoram, validam alertas e encerram ocorrências. | Ciclo de vida do alarme com botões `Validar / Reconhecer Alerta` (operador confirma a ocorrência) e `Normalizar / Encerrar Ocorrência` (solução registrada e reator normalizado para estável). | `Alarme.java`, `AlarmeFacade.java`, `AlarmeReconhecido.java`, `AlarmeResolvido.java` |
| **RNF-01 / RNF-02** | **Desempenho e Confiabilidade:** Arquitetura Orientada a Eventos (EDA) com baixo acoplamento entre produtores e consumidores. | `EventBus` síncrono in-process desacopla `ControleReator`, `Alarmes` e `AuditoriaLogs` e isola falhas dos assinantes. Entrega assíncrona, retenção e alta disponibilidade permanecem pendentes. | `EventBus.java` |
| **RNF-03** | **Integridade dos Dados:** Medições e eventos com integridade verificável; detecção de alterações externas no histórico. | Cadeia de hash criptográfica SHA-256 encadeada ($H_i = \text{SHA256}(i \mid \text{data} \mid \text{tipo} \mid \text{resumo} \mid H_{i-1})$). Se 1 caractere no arquivo for adulterado, o sistema detecta `FALHA`. | `RegistroAuditoria.java`, `EntradaAuditoria.java` |
| **RNF-04** | **Tolerância a Falhas:** Falha em um consumidor isolado não derruba o barramento nem impede novas leituras do reator. | `EventBus.publicar` isola exceções de subscribers via try-catch, garantindo que erros em consumidores não afetem os produtores. | `EventBus.java` |
| **RNF-05** | **Auditabilidade e Rastreabilidade:** Registro append-only com alteração detectável. | `RegistroAuditoria` acrescenta entradas em `dados/auditoria.log`; a cadeia SHA-256 evidencia adulterações, mas não impede edição física do arquivo. | `RegistroAuditoria.java` |
| **RNF-09 (parcial)** | **Persistência Segura e Continuidade Multi-Sessão:** Manter a cadeia de auditoria através de reinicializações. | O log de auditoria é recuperado e continuado. Medições e alarmes ainda são mantidos apenas em memória e precisam de repositório persistente. | `RegistroAuditoria.java`, `ReatorFacade.java`, `AlarmeFacade.java` |
| **RNF-10** | **Testabilidade Automatizada:** Suíte de testes sem frameworks externos. | `TestesMvp.java` executa 11 cenários repetíveis sobre RF01–RF06 e os RNFs verificáveis localmente. | `TestesMvp.java` |

---

## 2. O que Permanece Fora do MVP (Conforme Priorização MoSCoW)

O documento de 69 páginas abrange também os módulos de:
- `ControleAcesso` (catracas, biometria - Should);
- `ProtocoloContingencia` manual e isolamento de áreas (Could);
- `GestaoCombustivel` e rejeitos radioativos (Won't no MVP);
- `GestaoManutencao` completa com compra de peças (Won't no MVP);
- `GestaoRH` e treinamentos obrigatórios (Won't no MVP);
- `AnaliseRelatorios` com IA preditiva (Won't no MVP).

Esses módulos permanecem documentados e explicitamente fora do recorte executável. O MVP atual serve para demonstrar o fluxo do núcleo; não representa uma usina real nem satisfaz requisitos industriais de segurança, disponibilidade ou certificação.
