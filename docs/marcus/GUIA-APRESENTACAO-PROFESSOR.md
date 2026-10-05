# Guia Mestre de Apresentação e Domínio Técnico do MVP
**Aluno:** Marcus Querol (`rlampago22`)  
**Projeto:** Sistema de Controle de Usina Nuclear — RP IV / Análise e Projeto de Software  
**Data:** 28 de setembro de 2026 (Semana 4 / Preparação Marco 1)  
**Documento Base:** `ATUALIZADO -Análise e Projeto de Software Oitava Entrega_ Diagrama de Implantação.docx` (69 págs.)

---

## 1. O que foi feito agora (Resolução das Issues #52 e #57)

### A) Resolução da Issue #52: `Marcus — README demo + T05 Cenários (Opção A)`
* **O Problema que existia:** Na interface web em React (`frontend/`), a aba **T05 (Demo / Cenários)** tinha botões visuais, mas eles apenas alteravam uma variável local (`useState`). Não chamavam o backend Java nem publicavam eventos no `EventBus`. Clicar em "Simular Anomalia" não refletia em nenhuma outra tela.
* **A Solução Técnica implementada:**
  1. **Backend Java ([`ApiEstadoHttpServer.java`](file:///C:/Users/Querol/Documents/ChatGPT/RP%20IV/mvp/src/br/edu/unipampa/usina/apiestado/ApiEstadoHttpServer.java)):** Criamos o endpoint HTTP `POST /api/cenarios/{normal|observacao|falha|critico}`. Ele recebe a chamada, injeta a leitura correspondente diretamente na fachada do reator (`ReatorFacade`), dispara os eventos no barramento `EventBus` e atualiza a auditoria (`auditoria.log`) e o snapshot de estado em tempo real.
  2. **Cliente API Web ([`frontend/src/api/estado.js`](file:///C:/Users/Querol/Documents/ChatGPT/RP%20IV/frontend/src/api/estado.js)):** Adicionada a função exportada `dispararCenario(cenario)`.
  3. **Contexto Global ([`frontend/src/state/EstadoContext.jsx`](file:///C:/Users/Querol/Documents/ChatGPT/RP%20IV/frontend/src/state/EstadoContext.jsx)):** Integrado ao `EstadoProvider`, permitindo que qualquer componente acione cenários e recarregue o estado global.
  4. **Página T05 ([`frontend/src/pages/DemoPage.jsx`](file:///C:/Users/Querol/Documents/ChatGPT/RP%20IV/frontend/src/pages/DemoPage.jsx)):** Botões conectados com feedback visual imediato de sucesso ou erro e indicador de conexão com a API Java.
* **Resultado:** Agora, ao clicar em "Simular Anomalia (Crítico)" no T05 da Web, o reator na tela T01 (Overview) fica vermelho imediatamente, a aba T03 exibe os alarmes gerados e a aba T04 registra os eventos no log, com selos SHA-256 encadeados.

### B) Preparação da Issue #57: `Marcus — UCs/auditoria na demo`
* Seu papel oficial na apresentação do Marco 1 foi blindado: você demonstrará o disparo dos fluxos operacionais (T05) refletindo na auditoria à prova de adulteração (T04) e saberá explicar tecnicamente a correspondência dos diagramas de sequência SEQ-UC01 e SEQ-UC02.

---

## 2. Por que o MVP ESTÁ Alinhado com o Documento Oficial

Se o professor perguntar: *"Por que vocês consideram este MVP aderente ao documento de entrega?"*, responda com estes 5 pilares:

1. **Priorização MoSCoW (Must Have):**
   * O documento oficial (Seção 3) lista vários requisitos como *Must Have* (RF-1 a RF-7, RF-9, RF-13, RF-14 e RF-18, além de RNF-01, 02, 03, 04, 06, 07, 08 e 10). O grupo definiu um **recorte para o MVP do Marco 1**, centrado em **RF-1** (telemetria em tempo real) e **RF-2** (geração automática de alarmes), documentado em `docs/marco1/matriz-pdf-aps-vs-rp4.md`. O MVP implementa esse recorte de forma funcional.
   * Os 4 sensores essenciais do documento estão ativos com unidades e limites exatos:
     * `TEMPERATURA` (0 a 350 °C, setpoint normal 310,5 °C)
     * `PRESSAO` (0 a 160 bar, setpoint normal 155,0 bar)
     * `RADIACAO` (0 a 5 mSv/h, setpoint normal 2,4 mSv/h)
     * `FLUXO_RESFRIAMENTO` (500 a 1500 m³/h, setpoint normal 1100 m³/h)
2. **Cobertura Completa dos Fluxos do Caso de Uso UC01:**
   * **Fluxo Principal:** Coleta contínua, detecção de violação e notificação automática para os atores `Operador de Reator` e `Supervisão Central`.
   * **Fluxo Alternativo 1 (Registro de Observação):** Parâmetros em faixa de atenção emitem `ObservacaoRegistrada` para auditoria preventiva sem soar alarme crítico de pânico.
   * **Fluxo de Exceção (Falha de Comunicação):** Falha no barramento de sensor emite `FalhaSensorDetectada` alertando explicitamente a `Equipe Técnica`.
   * **Ciclo de Vida do Alarme:** Operador reconhece a ocorrência (`AlarmeReconhecido`) e Engenheiro encerra e normaliza (`AlarmeResolvido`).
3. **Arquitetura Orientada a Eventos (EDA - Seção 8 do Documento):**
   * Desacoplamento real via `EventBus`. O módulo `ControleReator` publica eventos sem saber quem consome. Os módulos `Alarmes` e `AuditoriaLogs` assinam os eventos de forma independente.
   * **RNF-04 (Tolerância a Falhas):** Uma falha ou exceção em um consumidor é isolada e nunca derruba o núcleo do reator.
4. **Integridade dos Dados e Auditoria à Prova de Adulteração (RNF-03, RNF-05 e RNF-09):**
   * Arquivo `dados/auditoria.log` em formato append-only, com cada registro ligado ao anterior por um selo SHA-256 (cadeia de hash).
   * O arquivo **não impede** a edição; ele **detecta**: qualquer alteração quebra a cadeia (teste automatizado comprova que alterar 1 caractere gera status `FALHA`), e a tela de Auditoria passa a mostrar INTEGRIDADE COMPROMETIDA. Isso é verificação de integridade, não criptografia.
   * Hoje só a auditoria persiste em arquivo; medições e alarmes ficam em memória (banco de dados previsto para o Marco 2).
5. **Padrões de Projeto GoF Rigorosos:**
   * `Facade` (`ReatorFacade`, `AlarmeFacade`), `Observer/Pub-Sub` (`EventBus`), `Strategy` (`AvaliadorLimiar`), `Factory` (`AlarmeFactory`).

---

## 3. Por que o MVP NÃO Implementa Todo o Documento (E por que isso está certo)

Se o professor perguntar: *"Cadê os outros 12 casos de uso do documento (evacuação, combustíveis, biometria)?"*, responda:

* **Conceito de MVP (Mínimo Produto Viável):** Um MVP acadêmico ou industrial serve para validar a arquitetura e o caminho crítico de maior risco da aplicação (no nosso caso, o controle do reator nuclear e a emissão de alarmes). Tentar construir 13 sistemas distintos em um semestre resultaria em maquetes falsas sem profundidade.
* **Recorte do MVP documentado:** Na matriz `docs/marco1/matriz-pdf-aps-vs-rp4.md` e na auditoria do grupo de 22/09, definimos formalmente o que entra no código do Marco 1:
  * *Fora do recorte:* evacuação (RF-9), rastreabilidade de combustível e rejeitos (RF-6), previsão de falhas (RF-13), controle de acesso biométrico (RF-5) e demais módulos foram mantidos no projeto arquitetural e no modelo conceitual, mas ficaram fora do código executável do Marco 1. Vários deles também são *Must* no PDF, então a justificativa é de **escopo do MVP e prazo**, não de prioridade.
* **A questão do Login / Autenticação:**
  * No documento, o UC02 trata de **acesso físico** às salas da usina (leitores RFID e biometria em catracas e portas blindadas), não de formulário de login web.
  * No software de supervisão, a identificação dos operadores é feita de forma contextual e auditável nas próprias ações (ex.: `Operador de Reator` autentica o reconhecimento, `Engenheiro de Turno` assina a resolução do alarme). Uma tela de login tradicional seria redundante para o Marco 1.
* **Topologia de Implantação:** O Diagrama de Implantação (Seção 12) projeta servidores espelhados com PostgreSQL e firewalls físicos. Para o Marco 1 local, executamos *in-process* com simulação de barramento para manter a reprodutibilidade com zero dependências externas.

---

## 4. Roteiro Prático de Demonstração para a Aula (5 a 8 minutos)

Você tem duas opções excelentes para apresentar: a **Interface Web React** (que acabamos de integrar) ou o **Sistema SCADA Desktop Java Swing** (que você fez no Desktop). O ideal é mostrar a interface principal do grupo e citar a suíte automatizada.

### 🎬 Roteiro Passo a Passo:

#### 1. Abertura Firme (1 minuto)
> *"Boa noite, professor. Na entrega desta semana e no fechamento do Marco 1, fiquei encarregado de validar a consistência entre o documento oficial de 69 páginas e a implementação do MVP. O nosso foco foi o recorte do MVP, parte dos requisitos Must do documento: telemetria dos 4 sensores operacionais em tempo real (RF-1), motor de alarmes com tolerância a falhas (RF-2) e auditoria com selos SHA-256 encadeados, que detecta qualquer adulteração (RNF-03/RNF-05), via arquitetura orientada a eventos."*

#### 2. Comprovação nos Testes Automatizados (1 minuto)
* Abra o terminal e execute:
  ```powershell
  .\mvp\2-TESTAR-MVP.bat
  ```
* Aponte para a tela:
  > *"Antes de olhar a interface, aqui está a evidência da nossa suíte de testes sem dependências externas. São 11 verificações formais cobrindo os critérios de aceite CT-RF01 a CT-RF06 e CT-RNF01 a CT-RNF10, todos passando com 100% de sucesso. Destaco aqui o teste do RNF-04 (isolamento de falhas no EventBus) e o teste do RNF-03, onde o script altera 1 caractere no arquivo de log e o sistema detecta a quebra da cadeia SHA-256."*

#### 3. Demonstração dos Cenários ao Vivo (3 a 4 minutos)
* **Se demonstrar na Web (React):**
  1. Deixe o backend rodando em um terminal: `mvp\4-EXECUTAR-API-ESTADO.bat`
  2. Em outro terminal, suba o frontend: `cd frontend; npm run dev` e abra o navegador.
  3. Vá na aba **T05 (Demo / Cenários)**:
     * Clique em **Operação Normal**: Mostre na aba T01 que o reator está ESTÁVEL e os sensores estão em valores seguros.
     * Volte em T05 e clique em **Simular Observação**: Mostre que o status vai para ATENÇÃO preventiva sem disparar alarme sonoro nem pânico.
     * Clique em **Falha de Sensor**: o status vai para ATENÇÃO. Depois abra **Visão Geral** (ou **Auditoria**): a primeira linha do histórico é o evento `FALHA_SENSOR_DETECTADA` (sensor 2, falha de comunicação, equipe técnica avisada). Falha não abre alarme em **Alarmes**; só leitura fora do limite seguro abre.
     * Volte em **Operação Normal** para limpar a atenção e clique em **Simular Anomalia (Crítico)**: o status vai para CRÍTICO imediatamente, a temperatura salta para 372 °C e o fluxo cai para 420 m³/h.
     * Na aba T03 aparecem **2 alarmes** (temperatura e fluxo, um por sensor). Clique em **Validar / Reconhecer** e depois em **Normalizar / Encerrar** em cada um. Ao encerrar, o sensor volta ao ponto normal e, com os dois encerrados, o reator volta para ESTÁVEL.
     * Vá na aba T04 (Auditoria): o selo **CADEIA ÍNTEGRA** confirma a verificação SHA-256 do arquivo de log, e os passos aparecem com timestamps ISO.
     * Para mostrar o arquivo, abra `mvp\dados\auditoria.log` (ou use `mvp\3-ABRIR-LOG-AUDITORIA.bat`).
     * Cuidado na demo: não ligue o Swing e a API ao mesmo tempo (os dois gravam no mesmo log).
* **Se demonstrar no Swing Desktop:**
  1. Dê dois cliques em `Desktop\MVP-Usina-Nuclear\0-ABRIR-SISTEMA-GRAFICO.vbs`.
  2. Clique em **Iniciar Tempo Real** para ver o reator pulsando.
  3. Clique em **Simular Observação (Alt. 1)** e depois em **Simular Anomalia (Crítico)** (nomes do app Swing).
  4. Clique em **Validar Alerta** e **Normalizar Ocorrência**.
  5. Clique em **Abrir Log** para mostrar o arquivo `auditoria.log`.

#### 4. Fechamento Técnico (1 minuto)
> *"Dessa forma, professor, o Marco 1 entrega um caminho crítico real, auditável e desacoplado por eventos, enquanto os demais módulos (RH, Evacuação, Rejeitos) permanecem mapeados no documento para os próximos marcos."*

---

## 5. Status do Checklist de Aceite Oficial
O arquivo [`docs/mvp/checklist-aceite-must.md`](file:///C:/Users/Querol/Documents/ChatGPT/RP%20IV/docs/mvp/checklist-aceite-must.md) foi preenchido com:
* **13 de 13 critérios aceitos com `[x] PASS`**
* Evidências nominais referenciando os métodos do código e os testes automatizados
* Vínculo formal à execução de 28/09/2026.
