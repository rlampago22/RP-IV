# 02 — Priorização MoSCoW

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Critério: maximizar valor demonstrável da arquitetura EDA + padrões de projeto, com MVP expandido (núcleo + acesso + demo integrada) recuperável a partir do Marco 1.

**Versão:** 2.0 — Must expandido (RF07–RF08 + RNF08 + entrega integrada API/UI)

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
| RNF01–RNF05, RNF08–RNF10 | Qualidade do núcleo + segurança de acesso | Sustentam EDA e pacote SegurancaAcesso |

**Entrega integrada Must (já demonstrável / formalizada):**

- API HTTP de estado/cenários (`mvp/apiestado`, `/api/*`)
- UI React Opção A (T01–T05) consumindo o estado agregado
- Ciclo de alarme ATIVO → RECONHECIDO → RESOLVIDO com identidade do operador

**Artefatos Must:** RF/RNF, MoSCoW, proposta MVP, pacotes, componentes lógico/físico, UCs/classes/sequências do núcleo + UC04, ER do núcleo.

---

## Should (desejável — próximo marco se o Must expandido estiver estável)

| ID | Item | Justificativa |
|----|------|---------------|
| RF09 | `Emergencia.criarEmergencia` | Feedback APS + domínio de contingência |
| RF10 | `ProtocoloEmergencia` | Classe faltante crítica no feedback APS |

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
7. (Should) Emergencia + ProtocoloEmergencia
8. (Could, proposta) RF16 consulta → RF17 exportação do histórico (PR #71, pendente de aprovação)
