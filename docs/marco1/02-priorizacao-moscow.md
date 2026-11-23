# 02 — Priorização MoSCoW

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Critério: maximizar valor demonstrável da arquitetura EDA + padrões de projeto, com MVP expandido (núcleo + acesso + demo integrada) recuperável a partir do Marco 1.

**Versão:** 2.2 — Must expandido (RF07–RF08 + RF18 + RF19–RF21 + RNF08 + entrega integrada API/UI)

---

## Must (obrigatório no MVP)

| ID | Item | Justificativa |
|----|------|---------------|
| RF01 | Coletar medições de reator | Fluxo produtor de eventos |
| RF02 | Histórico operacional | Persistência dedicada demonstrável |
| RF03 | Avaliar limiares | Base para alarmes |
| RF04 | Emitir alarme (Operador + Supervisão) | Caso de uso crítico + atores secundários explícitos |
| RF05 | Registrar evento de alarme | Entidade `Alarme` com métodos exigidos no feedback APS |
| RF06 | Auditar eventos | RNF05 + consumidor independente no bus |
| RF07 | Autenticar acesso restrito | Identidade na Central (login web) e credencial simulada (UC04) |
| RF08 | `RegistroAcesso` | Persistir tentativa/resultado de acesso; corrige classe APS |
| RF18 | 2FA por celular | Feedback do professor de 04/11/2026: após a senha, confirmar solicitação ou código no celular vinculado antes de criar a sessão |
| RF19 | 5 reatores com simulação independente | Feedback do professor de 13/11/2026: cada reator simula e notifica no seu ritmo, sem sincronia |
| RF20 | Painel único de monitoramento | Mesmo feedback: todos os gráficos numa tela, cada um com o próprio push, gráfico completo (não só a oscilação) |
| RF21 | Indicador verde/amarelo/vermelho por reator | Mesmo feedback: apontar nível crítico (ex.: radiação) em cada reator, alternando entre eles |
| RNF01–RNF05, RNF08–RNF10 | Qualidade do núcleo + segurança de acesso | Sustentam EDA e pacote SegurancaAcesso |

**Entrega integrada Must (já demonstrável / formalizada):**

- API HTTP de estado/cenários (`mvp/apiestado`, `/api/*`)
- UI React Opção A (T01–T05) consumindo o estado agregado
- Ciclo de alarme ATIVO → RECONHECIDO → RESOLVIDO com identidade do operador
- RF18 formalizado para o Marco 2; sua implementação deve incluir a etapa pendente de 2FA antes da sessão autenticada

**Artefatos Must:** RF/RNF, MoSCoW, proposta MVP, pacotes, componentes lógico/físico, UCs/classes/sequências do núcleo + UC04, ER do núcleo.

---

## Should (desejável — próximo marco se o Must expandido estiver estável)

| ID | Item | Justificativa |
|----|------|---------------|
| RF09 | `Emergencia.criarEmergencia` | Feedback APS + domínio de contingência. **Implementado em 16/11** (aba Emergência; `backend/.../emergencias`) |
| — | Abas adaptadas aos 5 reatores (seletor de reator compartilhado, matriz de sensores, resumo da usina, cenário por reator) | Não pedido pelo professor, mas evita que as abas antigas mostrem só o R-01 ao lado do painel de 5 reatores; cenário por reator permite demonstrar o RF21 sem depender de pico aleatório. Histórico por reator fica para outra rodada |
| RF10 | `ProtocoloEmergencia` | Classe faltante crítica no feedback APS. **Implementado em 16/11**: ativar e encerrar pedem a digital (4.2.10) |
| — | Cadastro de reatores em banco (issue #96): reatores, sensores, faixas e setpoints em `nucleo.reator` / `nucleo.sensor_reator`, com tela **Reatores** só para o Administrador do Sistema (incluir, ajustar faixas/setpoints, retirar e devolver à operação). O núcleo aplica a mudança em até 2 s, sem reiniciar | Professor pediu em 16/11 que o cadastro saísse do código para o banco; a tela é acréscimo do grupo para o cadastro ser usável sem SQL. Alterar o cadastro é ação crítica (digital, RNF08) e fica na trilha encadeada (RNF03). Proposta de Bruno, pendente de aprovação do grupo |
| RNF08 (2º fator) | Digital como segundo fator das **ações críticas** (administração de acessos e, quando existir, protocolo de contingência), com cadastro prévio autorizado pelo Administrador (código de ativação). Login continua com senha | PDF APS: RNF Segurança pede "autenticação multifator" (Must), 4.2.10 exige dois fatores na ativação de contingência, UC02 exige funcionário "previamente cadastrado" e 4.2.7 atribui a gestão ao Administrador. Marco 2, trilha Marcus — ver [`../backend-autenticacao.md`](../backend-autenticacao.md#segundo-fator-digital-nas-ações-críticas-rnf08) |

---

## Could (se houver capacidade no Marco 3+)

| ID | Item | Justificativa |
|----|------|---------------|
| — | `HistoricoSubstituicao` | Só se manutenção entrar no escopo |
| — | Polish de persistência DB além de auditoria + auth | Evolução pós-demo |
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
8. (Should) Emergencia + ProtocoloEmergencia
9. (Could, proposta) RF16 consulta → RF17 exportação do histórico (PR #71, pendente de aprovação)
