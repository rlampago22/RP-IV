# Roteiro de Apresentação do MVP — 5 Minutos (Alinhado ao Documento)

---

## 1. Introdução e Contexto do Projeto (30 segundos)

> “Professor, este MVP foi derivado diretamente do documento oficial de 69 páginas da disciplina (`ATUALIZADO -Análise e Projeto de Software Oitava Entrega`).
> Para a entrega do MVP da Semana 1, implementamos de ponta a ponta o **núcleo crítico do sistema**: a coleta contínua dos **quatro parâmetros operacionais do RF-1** (temperatura, pressão, radiação e fluxo de resfriamento), a detecção de anomalias com emissão de alarmes automáticos (**RF-2**), o ciclo de vida completo do **UC01** (com fluxo principal, alternativo de observação e exceção de falha técnica) e a auditoria com cadeia criptográfica SHA-256 persistente (**RNF-03, RNF-04 e RNF-05**).”

---

## 2. A Central de Supervisão (30 segundos)

*Abra `0-ABRIR-SISTEMA-GRAFICO.vbs` (ou execute `abrir-sistema.ps1`).*

> “Esta é a **Central de Supervisão do Reator Nuclear 01**:
> - À esquerda, temos a telemetria dos **quatro sensores do RF-1**: Temperatura, Pressão, Radiação e Fluxo de Resfriamento com suas faixas operacionais seguras.
> - No topo esquerdo, o indicador visual do **Núcleo do Reator** com feedback cromático (`ESTÁVEL` vs `CRÍTICO`).
> - À direita, os contadores em tempo real, a **Linha do Tempo de Eventos** orientada a EDA e a **Gestão de Alarmes e Notificações** com controle de ciclo de vida.”

---

## 3. Telemetria Contínua em Tempo Real — UC01 Principal (40 segundos)

*Clique no botão **`Iniciar Tempo Real`**.*

> “No UC01, o operador inicia o monitoramento e o sistema passa a coletar dados continuamente.
> Vejam que o sistema atualiza os quatro sensores a cada ciclo com oscilações físicas realistas.
> Cada leitura gera um evento `MEDICAO_REGISTRADA` no barramento `EventBus`, entregue simultaneamente para avaliação de limiar e para a auditoria criptográfica. Como todos os valores estão normais, o núcleo permanece estável e nenhum alarme é emitido.”

*Clique em **`Pausar Tempo Real`** para demonstrar os fluxos específicos.*

---

## 4. Fluxo Alternativo 1 — Observação Preventiva (30 segundos)

*Clique no botão **`Simular Observacao (Alt. 1)`**.*

> “O documento especifica no **UC01 (Fluxo Alternativo 1)** que valores fora da faixa ideal de atenção, mas ainda dentro dos limites normais, devem ser registrados para observação sem disparar alarme de emergência.
> Observem que a tabela recebeu o evento `OBSERVACAO_REGISTRADA` e o contador de auditoria subiu, sem que nenhum alarme crítico fosse disparado desnecessariamente.”

---

## 5. Fluxo de Exceções — Falha na Comunicação com Sensor (30 segundos)

*Clique no botão **`Falha Sensor (Excecao)`**.*

> “No **UC01 (Fluxo de Exceções)**, quando ocorre uma falha de hardware ou timeout em um sensor, o sistema não cai: ele dispara um evento `FALHA_SENSOR_DETECTADA` emitindo um alerta de manutenção específico para a **Equipe Técnica** e o **Engenheiro de Segurança**, isolando o problema conforme exige o requisito de tolerância a falhas (**RNF-04**).”

---

## 6. Alarme Crítico, Notificação e Validação pelo Operador (1 minuto)

*Clique em **`Simular Anomalia (Critico)`**.*

> “Agora simulamos uma anomalia severa: a temperatura salta para 372 °C e o fluxo de resfriamento despenca para 420 m³/h.
> Imediatamente:
> 1. A Strategy de limiar detecta a violação das faixas seguras.
> 2. O núcleo e o status mudam para `CRÍTICO`.
> 3. São emitidos alarmes com notificação explícita para o **Operador de Reator** e a **Supervisão Central**.
> 4. Conforme o UC01, os operadores devem **monitorar e validar os alertas**.”

*Clique em **`Validar / Reconhecer Alerta`**.*

> “O operador clica em reconhecer o alerta: o sistema registra o evento `ALARME_RECONHECIDO` com identificação do operador e muda o status para `EM TRATAMENTO`.”

*Clique em **`Normalizar / Encerrar Ocorrencia`**.*

> “Após a manobra corretiva, o engenheiro encerra a ocorrência: o sistema publica `ALARME_RESOLVIDO`, restabelece os sensores para faixas seguras e o núcleo retorna ao estado verde `ESTÁVEL`.”

### Roteiro curto na UI Web (T05 → T01 → T03)

1. Deixe a API rodando com `mvp/4-EXECUTAR-API-ESTADO.bat` e o React com `cd frontend; npm run dev`.
2. Em **Demo / Cenários**, clique em **Simular Anomalia (Crítico)**. O T05 envia `POST /api/demo/anomalia`; a API registra temperatura de 372 °C, a Strategy detecta a violação e publica `AlarmeEmitido`.
3. O polling de `/api/estado` atualiza o status para `CRÍTICO` e mostra o banner. Abra **Alarmes** e clique em **Validar / Reconhecer**. A T03 envia `POST /api/alarmes/{id}/reconhecer`; o status fica `RECONHECIDO` (em tratamento) e a timeline registra `ALARME_RECONHECIDO`.
4. Clique em **Normalizar / Encerrar**. A T03 envia `POST /api/alarmes/{id}/resolver`; o alarme passa a `RESOLVIDO`, a timeline registra `ALARME_RESOLVIDO` e o status volta a `ESTÁVEL`.

### Padrões GoF no código Java (`mvp/`)

| Padrão | Arquivo | Aplicação |
|--------|---------|-----------|
| Facade | `mvp/src/br/edu/unipampa/usina/controlereator/ReatorFacade.java` | Entrada do caso de uso de medição; publica `MedicaoRegistrada` sem acoplar o produtor à avaliação do alarme. |
| Strategy | `mvp/src/br/edu/unipampa/usina/alarmes/AvaliadorLimiar.java` e `AvaliadorFaixaSegura.java` | Regra intercambiável para verificar violação dos limites seguros. |
| Factory | `mvp/src/br/edu/unipampa/usina/alarmes/AlarmeFactory.java` | Centraliza a criação da entidade `Alarme` a partir da medição crítica. |
| Observer / Pub-Sub | `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/EventBus.java` e `mvp/src/br/edu/unipampa/usina/alarmes/AlarmeFacade.java` | A Facade assina `MedicaoRegistrada`; o barramento distribui os fatos sem chamada direta do produtor para o consumidor. |

**Fala GoF (20 segundos):** “A `ReatorFacade` simplifica a entrada do caso de uso. A Strategy avalia a faixa segura, a Factory cria o alarme e o Observer distribui os eventos pelo `EventBus`. Assim, o produtor da medição não precisa conhecer diretamente quem avalia, notifica ou audita.”

---

## 7. Auditoria Persistente SHA-256 e Continuidade Multi-Sessão (40 segundos)

*Clique no botão **`Abrir Log`**.*

> “Cada evento é persistido em modo append-only em `dados/auditoria.log`.
> Cada linha contém número sequencial, timestamp ISO, tipo de evento, dados da ocorrência e uma cadeia de hash SHA-256 onde cada entrada referencia o hash da anterior.
> Uma das grandes melhorias que fizemos foi a **continuidade multi-sessão (RNF-09)**: se fecharmos a aplicação e abrirmos de novo amanhã, o sistema não zera nem corrompe o log — ele lê a última linha e continua a cadeia perfeitamente.
> Além disso, se qualquer caractere for alterado maliciosamente no arquivo físico, o método `verificarIntegridadeArquivo` detecta a violação na hora e acusa `FALHA` no indicador.”

---

## 8. Conclusão e Testabilidade (30 segundos)

*Mostre a execução de `2-TESTAR-MVP.bat` no terminal.*

> “Para comprovar a qualidade do software, implementamos uma suíte de **11 testes automatizados (RNF-10)** que roda sem dependências externas e valida desde a coleta dos 4 sensores até a detecção de adulteração física do log.
> O MVP cumpre 100% dos requisitos Must acordados na Semana 1 com rigor técnico e arquitetura limpa.”

---

## Possíveis Perguntas da Banca

- **Por que não implementaram evacuação e catracas biométricas no MVP?**
  > *“Conforme o planejamento MoSCoW do documento (páginas 9 a 12), evacuação completa e RH são classificados como Could e Won't no MVP. Nosso objetivo no Marco 1 foi entregar o núcleo crítico com máxima confiabilidade, tolerância a falhas e rastreabilidade, sem criar telas fakes para módulos que dependem de infraestrutura física posterior.”*

- **Onde está aplicada a Arquitetura Orientada a Eventos (EDA)?**
  > *“No pacote `infraestruturaeventos`, onde o `EventBus` conecta produtores e consumidores via Observer/Pub-Sub. O `ControleReator` não conhece a existência de `AlarmeFacade` nem de `AuditoriaSubscriber`, permitindo escalabilidade e isolamento de falhas (RNF-01, RNF-02 e RNF-04).”*
