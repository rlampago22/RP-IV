# 02 — Priorização MoSCoW

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Critério: maximizar valor demonstrável da arquitetura EDA + padrões de projeto até o fim da disciplina, com entrega recuperável no Marco 1.

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
| RNF01–RNF05, RNF09–RNF10 | Subconjunto de qualidade do núcleo | Sustentam escolha EDA |

**Artefatos Must do Marco 1:** RF/RNF, MoSCoW, proposta MVP, pacotes, componentes lógico/físico, UCs/classes/sequências do núcleo, ER do núcleo.

---

## Should (desejável — Marco 2/3 se o núcleo estiver estável)

| ID | Item | Justificativa |
|----|------|---------------|
| RF07 | Autenticar acesso restrito | Amplia domínio sem mudar arquitetura |
| RF08 | `RegistroAcesso` | Corrige classe faltante do feedback APS |
| RNF08 | Segurança granular | Natural ao pacote SegurancaAcesso |

---

## Could (se houver capacidade no Marco 3)

| ID | Item | Justificativa |
|----|------|---------------|
| RF09 | `Emergencia.criarEmergencia` | Feedback APS + domínio |
| RF10 | `ProtocoloEmergencia` | Classe faltante crítica no feedback |
| — | `HistoricoSubstituicao` | Só se manutenção entrar no escopo |

---

## Won't (fora do MVP da disciplina)

| ID | Item | Motivo |
|----|------|--------|
| RF11 | Evacuação completa | Escopo grande, pouco retorno para padrões |
| RF12 | RH / capacitação / treinamento | Feedback APS extenso; não demonstra EDA do núcleo |
| RF13 | Conformidade regulatória ampla | Relatórios secundários |
| RF14 | Predição por IA | Fora do foco GoF/arquitetura |
| RF15 | Rastreio completo de material | Pode ser fase futura pós-disciplina |
| — | Microserviços reais / cluster | EDA in-process basta no MVP acadêmico |

---

## Ordem de implementação sugerida

1. Event bus + publicação de `MedicaoRegistrada`
2. Persistência de medição + `MedicaoReator.registrarMedicao`
3. Avaliação de limiar (Strategy) + `Alarme.emitirAlerta` / `registrarEvento`
4. Consumidor de auditoria
5. (Should) RegistroAcesso
6. (Could) Emergencia + ProtocoloEmergencia
