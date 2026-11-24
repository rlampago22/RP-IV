# 02 — Priorização MoSCoW

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Critério: maximizar valor demonstrável da arquitetura EDA + padrões de projeto, com MVP expandido (núcleo + acesso + demo integrada) recuperável a partir do Marco 1.

**Versão:** 2.3 — Must expandido (RF07–RF08 + RF18 + RF19–RF21 + RNF08 + entrega integrada API/UI) + **persistência em banco** (feedback do professor de 16/11)

> **O que mudou na 2.3 (23/11/2026, Álvaro):** em 16/11 o professor pediu para sair da simulação em memória — reator, sensores/histórico e auditoria passam a ser gravados em banco; só a *geração* do valor do sensor continua simulada. Também pediu que dado crítico chegue por push imediato e dado rotineiro siga por polling. Isso muda o critério de aceite de várias RFs Must: "persistir" agora significa **gravar em tabela do banco** (H2 local / PostgreSQL pelo perfil `postgres`), e não em arquivo ou lista em memória. A tabela [Critérios de aceite de persistência](#critérios-de-aceite-de-persistência-em-banco-feedback-de-1611) abaixo diz o que já cumpre e o que falta.

---

## Must (obrigatório no MVP)

| ID | Item | Justificativa |
|----|------|---------------|
| RF01 | Coletar medições de reator | Fluxo produtor de eventos. Desde 16/11: o valor continua simulado, mas a leitura é gravada no banco |
| RF02 | Histórico operacional | Persistência dedicada demonstrável — em banco (`nucleo.historico_operacional`) desde 16/11 |
| RF03 | Avaliar limiares | Base para alarmes. Faixas lidas do cadastro em banco (RF19) |
| RF04 | Emitir alarme (Operador + Supervisão) | Caso de uso crítico + atores secundários explícitos. Alarme é dado crítico: push imediato |
| RF05 | Registrar evento de alarme | Entidade `Alarme` com métodos exigidos no feedback APS. Desde 16/11: o registro do alarme deve ir para o banco |
| RF06 | Auditar eventos | RNF05 + consumidor independente no bus — em banco (`nucleo.auditoria_evento`) com cadeia SHA-256 |
| RF07 | Autenticar acesso restrito | Identidade na Central (login web) e credencial simulada (UC04) |
| RF08 | `RegistroAcesso` | Persistir tentativa/resultado de acesso em banco; corrige classe APS |
| RF18 | 2FA por celular | Feedback do professor de 04/11/2026: após a senha, confirmar solicitação ou código no celular vinculado antes de criar a sessão |
| RF19 | 5 reatores com simulação independente | Feedback do professor de 13/11/2026: cada reator simula e notifica no seu ritmo, sem sincronia. Feedback de 16/11: reatores, sensores e faixas vêm do **cadastro em banco** (`nucleo.reator` / `nucleo.sensor_reator`), não de lista no código |
| RF20 | Painel único de monitoramento | Mesmo feedback: todos os gráficos numa tela, cada um com o próprio push, gráfico completo (não só a oscilação). Feedback de 16/11: **dado crítico por push imediato, dado rotineiro por polling** |
| RF21 | Indicador verde/amarelo/vermelho por reator | Mesmo feedback: apontar nível crítico (ex.: radiação) em cada reator, alternando entre eles |
| RNF01–RNF05, RNF08–RNF10 | Qualidade do núcleo + segurança de acesso | Sustentam EDA e pacote SegurancaAcesso. **RNF09** passa a exigir banco para os dados do núcleo (antes bastava auditoria em arquivo + schema de auth) |

**Entrega integrada Must (já demonstrável / formalizada):**

- API HTTP de estado/cenários (`mvp/apiestado`, `/api/*`)
- UI React Opção A (T01–T05) consumindo o estado agregado
- Ciclo de alarme ATIVO → RECONHECIDO → RESOLVIDO com identidade do operador
- RF18 formalizado para o Marco 2; sua implementação deve incluir a etapa pendente de 2FA antes da sessão autenticada
- Banco único para Central e núcleo: H2 em `backend/data/usina` (`AUTO_SERVER`) ou PostgreSQL por `USINA_DB_URL`; migrations Flyway no `backend/`, tabelas do núcleo no schema `nucleo`, acessadas pelo `mvp/` via JDBC (`persistencia/ConexaoBanco`)

**Artefatos Must:** RF/RNF, MoSCoW, proposta MVP, pacotes, componentes lógico/físico, UCs/classes/sequências do núcleo + UC04, ER do núcleo (agora com as tabelas do schema `nucleo`).

### Critérios de aceite de persistência em banco (feedback de 16/11)

Regra geral: um item Must que "persiste" só passa se o dado **sobrevive a reiniciar o núcleo e a API** e pode ser consultado no banco. Lista ou arquivo em memória não conta mais. O banco de testes é o mesmo código em H2 em memória (`ConexaoBanco.emMemoria(...)`).

| Item | O que precisa estar no banco | Onde está hoje | Situação em 23/11 |
|------|------------------------------|----------------|-------------------|
| RF01 / RF02 | Leituras dos sensores consolidadas por ciclo, com `reator_id`, consultáveis por reator e por data | `nucleo.historico_operacional` (V7, issue #97, Bernardo); CSV antigo importado uma vez | **Cumpre** |
| RF03 / RF19 | Reatores, sensores, faixas e setpoints; mudança aplicada sem reiniciar o núcleo | `nucleo.reator` / `nucleo.sensor_reator` (V8, issue #96, Bruno) | **Cumpre** |
| RF04 / RF20 | Dado crítico (alarme, contingência, cadastro alterado) por push imediato; rotina por polling de 2 s | Push SSE pela Central disparado pelo evento no barramento (não é `CREATE TRIGGER` de banco: o mesmo código roda em H2 e Postgres) — ver `docs/api-estado-contrato.md` | **Cumpre** |
| RF05 | Cada alarme (tipo, severidade, instante, sensor/reator de origem) e seu ciclo ATIVO → RECONHECIDO → RESOLVIDO com quem reconheceu/resolveu | Só em memória (`AlarmeFacade`, lista); o banco guarda apenas o evento na auditoria | **Pendente** — falta tabela própria de alarme (ex.: `nucleo.alarme`) |
| RF06 / RNF03 / RNF05 | Trilha de auditoria append-only com cadeia SHA-256 verificável, sem corromper com dois processos gravando | `nucleo.auditoria_evento` (V6, issue #99, Marcus); `GET /api/auditoria/integridade` | **Cumpre** |
| RF07 / RF18 / RNF08 | Usuários, papéis, tentativas de login, 2º fator e credencial biométrica | Tabelas do `backend/` (V1–V4) | **Cumpre** |
| RF08 | Cada `RegistroAcesso` da UC04 (credencial, área, resultado, instante) | Só em memória (`AcessoFacade`, lista); o banco guarda apenas o evento `AcessoRegistrado` na auditoria | **Pendente** — falta tabela própria (ex.: `nucleo.registro_acesso`) |
| RF09 / RF10 (Should) | Emergências e protocolo de contingência com quem ativou/encerrou | `emergencia` (V5); estado lido pelo núcleo via `/api/interno/contingencia` | **Cumpre** |
| RNF09 | Reiniciar o processo e recuperar os registros acima sem perda nem duplicação | Coberto para auditoria (`TestesMvp`), histórico e cadastro; não para alarmes e `RegistroAcesso` | **Parcial** — fecha junto com RF05 e RF08 |

Os dois pendentes (RF05 e RF08) são proposta de Álvaro para fechar o Must sob a regra nova, pendente de aprovação do grupo; se o grupo preferir, podem ficar como evidência só na auditoria em banco, mas aí o critério do RF05/RF08 tem de ser reescrito aqui.

---

## Should (desejável — próximo marco se o Must expandido estiver estável)

| ID | Item | Justificativa |
|----|------|---------------|
| RF09 | `Emergencia.criarEmergencia` | Feedback APS + domínio de contingência. **Implementado em 16/11** (aba Emergência; `backend/.../emergencias`, tabela `emergencia`) |
| — | Abas adaptadas aos 5 reatores (seletor de reator compartilhado, matriz de sensores, resumo da usina, cenário por reator) | Não pedido pelo professor, mas evita que as abas antigas mostrem só o R-01 ao lado do painel de 5 reatores; cenário por reator permite demonstrar o RF21 sem depender de pico aleatório. Histórico por reator fica para outra rodada |
| RF10 | `ProtocoloEmergencia` | Classe faltante crítica no feedback APS. **Implementado em 16/11**: ativar e encerrar pedem a digital (4.2.10). **Ligado ao EventBus em 21/11** (issue #99): ativar trava o núcleo (modo de contingência), com evento auditado e push para a tela |
| — | Tela **Reatores** do Administrador do Sistema (issue #96): incluir, ajustar faixas/setpoints, retirar e devolver à operação, sem SQL. O núcleo aplica a mudança em até 2 s, sem reiniciar | O cadastro em banco em si subiu para Must (RF19, pedido do professor em 16/11); a tela é acréscimo do grupo. Alterar o cadastro é ação crítica (digital, RNF08) e fica na trilha encadeada (RNF03). Proposta de Bruno, pendente de aprovação do grupo |
| RNF08 (2º fator) | Digital como segundo fator das **ações críticas** (administração de acessos e, quando existir, protocolo de contingência), com cadastro prévio autorizado pelo Administrador (código de ativação). Login continua com senha | PDF APS: RNF Segurança pede "autenticação multifator" (Must), 4.2.10 exige dois fatores na ativação de contingência, UC02 exige funcionário "previamente cadastrado" e 4.2.7 atribui a gestão ao Administrador. Marco 2, trilha Marcus — ver [`../backend-autenticacao.md`](../backend-autenticacao.md#segundo-fator-digital-nas-ações-críticas-rnf08) |

---

## Could (se houver capacidade no Marco 3+)

| ID | Item | Justificativa |
|----|------|---------------|
| — | `HistoricoSubstituicao` | Só se manutenção entrar no escopo |
| — | Banco além do núcleo: réplica/backup automatizado, PostgreSQL como padrão da demo | Evolução pós-demo; o Must já exige banco para os dados do núcleo (ver critérios de persistência) |
| RF16 | Consultar histórico operacional (filtros por data e faixa) | Realiza a cláusula "para consulta" do RF02, nunca entregue — implementado por Bruno (PR #71), proposta pendente de aprovação do grupo para promover a Should |
| RF17 | Exportar histórico consultado em CSV | Completa a consulta; CSV mantém zero dependência externa — mesma proposta do PR #71 |

---

## Won't (fora do MVP da disciplina)

| ID | Item | Motivo |
|----|------|--------|
| RF11 | Evacuação completa | Escopo grande, pouco retorno para padrões |
| RF12 | RH / capacitação / treinamento | Feedback APS extenso; não demonstra EDA do núcleo |
| RF13 | Conformidade regulatória ampla | Relatórios secundários |
| RF14 | Predição por IA | Fora do foco GoF/arquitetura |
| RF15 | Rastreio completo de material | Pode ser fase futura pós-disciplina |
| — | Microserviços reais / cluster | EDA in-process + stub HTTP bastam no MVP acadêmico |

---

## Ordem de implementação sugerida

1. Event bus + publicação de `MedicaoRegistrada`
2. Persistência de medição + histórico operacional
3. Avaliação de limiar (Strategy) + emissão/registro de alarme
4. Consumidor de auditoria
5. API de estado + UI T01–T05 + ciclo ACK/resolver
6. Autenticação (RF07) + `RegistroAcesso` (RF08) + RNF08
7. 2FA por celular (RF18): solicitação → confirmação/código → criação da sessão + auditoria da tentativa
7.1. Monitoramento de 5 reatores (RF19–RF21): reator de origem nos eventos → estado por reator → simulação independente → painel com lâmpada por reator
7.2. Persistência em banco (feedback de 16/11): auditoria (#99) → histórico (#97) → cadastro de reatores (#96) → push crítico / polling rotineiro → **pendente:** alarmes (RF05) e `RegistroAcesso` (RF08) em tabela própria
8. (Should) Emergencia + ProtocoloEmergencia (ligado ao EventBus na #99)
9. (Could, proposta) RF16 consulta → RF17 exportação do histórico (PR #71, pendente de aprovação)
