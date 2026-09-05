# Plano semanal RP4 — entregas por aluno

**Disciplina:** AL0343 — Resolução de Problemas IV  
**Sistema:** Controle de Usina Nuclear (arquitetura EDA mantida)  
**Repositório:** https://github.com/rlampago22/RP-IV

## Premissa

Não entregar o Marco 1 de uma vez. Cada **semana** o grupo sobe algo no GitHub e cada aluno deixa **evidência individual** para a verificação parcial (NOk → TOk).

O material em [`docs/marco1/`](marco1/) e o esqueleto em `src/` são **rascunho base**: o time revisa, corrige (feedback APS) e implementa em fatias até a data oficial do Marco 1.

| Marco | Data |
|-------|------|
| Marco 1 | 05/10/2026 |
| Marco 2 | 16/11/2026 |
| Marco 3 | 16–17/12/2026 |

---

## Papéis fixos (ownership)

| Aluno | Trilha principal | Pasta / pacotes típicos |
|-------|------------------|-------------------------|
| Álvaro Domingues | Requisitos, MoSCoW, aceite MVP, checklist feedback | `docs/marco1/01*`, `02*`, `03*`, checklist |
| Bruno Rocha | Arquitetura EDA, EventBus, pacotes/componentes | `infraestruturaeventos`, diagramas de pacotes/componentes |
| Bernardo Dorneles | ControleReator, medições, PersistenciaReator | `controlereator`, `persistenciareator` |
| José Guilherme Monteiro | Alarmes, limiares (Strategy/Factory), GoF | `alarmes`, doc de padrões |
| Marcus Querol | UCs/sequências, AuditoriaLogs, demo/README, Should (acesso) | UCs, `auditorialogs`, `segurancaacesso` |

---

## Formato padrão de entrega semanal (todos)

1. Branch `semana-NN/<primeiro-nome>` **ou** commits claros no `main` com mensagem identificando o autor
2. Arquivo curto `docs/semanas/YYYY-MM-DD/<nome>.md` (usar o template abaixo)
3. Código ou diagrama na trilha do aluno
4. Atualizar **uma linha** no [`semanas/STATUS.md`](semanas/STATUS.md)

### Template de relatório individual

Copiar para `docs/semanas/YYYY-MM-DD/<seu-nome>.md`:

```markdown
# Relatório semanal — <Nome>

- **Semana:** NN (datas)
- **Trilha:** <papel>
- **Data:** AAAA-MM-DD

## O que entreguei
- …

## Evidências
- Commit(s): …
- Arquivos: …
- Prints / demo (se houver): …

## Bloqueios / dúvidas
- …

## Próxima semana (previsto)
- …
```

---

## Calendário

### Semana 0 — 05 a 07/09 (recuperação imediata)

**Objetivo:** alinhar escopo; **não** fechar Marco 1.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Revisar RF/RNF rascunho; marcar Must vs legado APS; 1 página “escopo congelado parcial” |
| Bruno | Resumo semipresencial Pacotes+Componentes (3 prints cada) + validar diagrama de pacotes rascunho |
| Bernardo | Listar classes do núcleo que vai implementar; confirmar `MedicaoReator.registrarMedicao` |
| José | Listar GoF previstos (Strategy, Factory, Facade) ligados a alarmes |
| Marcus | Reescrever só UC01 fluxo principal (linear, sem `if`) + system boundary |

Pasta: [`semanas/2026-09-07/`](semanas/2026-09-07/)

---

### Semana 1 — 08 a 14/09

**Objetivo:** priorização oficial + 1º código mínimo do bus.

| Aluno | Entrega |
|-------|---------|
| Álvaro | MoSCoW fechado (Must/Should/Could/Won't) revisado em grupo |
| Bruno | `EventBus` funcional in-process (publicar/assinar) + teste manual simples |
| Bernardo | `MedicaoReator` + `ReatorFacade.receberLeitura` salvando em memória |
| José | Interface `AvaliadorLimiar` + 1 implementação Strategy |
| Marcus | Diagrama UC MVP (boundary + include alarme) em PlantUML revisado |

**Semipresencial 12/09:** Álvaro ou Bruno resume **1 padrão arquitetural diferente** dos vídeos (ex.: Duto e Filtro); os outros comentam no PR.

---

### Semana 2 — 15 a 21/09

**Objetivo:** medições disparam eventos; auditoria consome.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Critérios de aceite do Must (checklist testável) |
| Bruno | Eventos tipados `MedicaoRegistrada` / contrato do payload |
| Bernardo | Persistência mínima (arquivo ou H2) de medições |
| José | Ao limiar violado, criar `Alarme` (ainda sem notificação completa) |
| Marcus | `AuditoriaSubscriber` gravando log append-only |

**Demo interna:** 1 `main` que simula 3 medições e mostra log.

---

### Semana 3 — 22 a 28/09

**Objetivo:** alarme completo + correção feedback APS no núcleo.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Checklist feedback APS atualizado (só itens do núcleo) |
| Bruno | Componentes físicos atualizados (o que o JAR realmente empacota) |
| Bernardo | SEQ-UC01 alinhada ao código real |
| José | `Alarme.emitirAlerta` + `registrarEvento` + notificar Operador/Supervisão (console ok) |
| Marcus | SEQ-UC02 + UC02 doc (destinatários explícitos) |

---

### Semana 4 — 29/09 a 05/10 → **MARCO 1**

**Objetivo:** apresentação Marco 1 (docs coerentes + demo curta do fluxo Must parcial).

| Aluno | Entrega |
|-------|---------|
| Álvaro | Slides/seção RF+RNF+MoSCoW+MVP |
| Bruno | Pacotes + componentes L/F + fala da EDA |
| Bernardo | Demo medições + persistência |
| José | Demo alarme por limiar + GoF citados |
| Marcus | UCs/sequências + auditoria na demo |

**Regra:** não inventar módulo novo; mostrar o que já roda + diagrama alinhado.

---

### Semana 5 — 06 a 12/10

**Objetivo:** refatorar com padrões nomeados no código.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Doc “padrões aplicados” (tabela padrão → classe → UC) |
| Bruno | Pub-Sub/Observer documentado no EventBus |
| Bernardo | Repository na persistência |
| José | Factory de alarmes + Strategy de limiar testáveis |
| Marcus | Facade nos controladores; limpeza de sequência |

---

### Semana 6 — 13 a 19/10

**Objetivo:** testes + robustez do núcleo.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Casos de teste de aceite (pass/fail) |
| Bruno | Tratamento de falha: auditoria fora não derruba reator |
| Bernardo | Histórico consultável (RF02) |
| José | Cenário limiar OK vs violado (alternativa A1) |
| Marcus | README “como executar a demo” |

---

### Semana 7 — 20 a 26/10

**Objetivo:** polish Must fechado.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Congelar escopo Must (não aceitar Won't) |
| Bruno | Diagrama de pacotes = pacotes Java reais |
| Bernardo | Seeds/dados de exemplo reator+sensor |
| José | Mensagens de alarme claras + severidade |
| Marcus | Gravar evidências (prints/GIF) da demo |

---

### Semana 8 — 27/10 a 02/11

**Objetivo:** iniciar Should — **RegistroAcesso**.

| Aluno | Entrega |
|-------|---------|
| Álvaro | RF07/RF08 detalhados + prioridade Should |
| Bruno | Evento `AcessoRegistrado` no bus |
| Bernardo | Apoio persistência `registro_acesso` |
| José | Reuso de padrões no módulo acesso (Facade) |
| Marcus | UC04 + `RegistroAcesso.registrar` |

---

### Semana 9 — 03 a 09/11

**Objetivo:** integrar Should + preparação Marco 2.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Roteiro apresentação Marco 2 |
| Bruno | Visão arquitetural “antes/depois” Marco 1→2 |
| Bernardo | Script `run-demo` (ou CI simples) |
| José | Mapa GoF final (mín. 3 padrões) |
| Marcus | Demo acesso restrito sucesso/falha |

---

### Semana 10 — 10 a 16/11 → **MARCO 2**

| Aluno | Papel na apresentação |
|-------|----------------------|
| Álvaro | Escopo e aceite |
| Bruno | Arquitetura e EventBus |
| Bernardo | Persistência e medições |
| José | Alarmes e GoF |
| Marcus | Auditoria, UCs e acesso |

---

### Semana 11 — 17 a 23/11

**Objetivo:** Could mínimo — `Emergencia.criarEmergencia` + `ProtocoloEmergencia`.

| Aluno | Entrega |
|-------|---------|
| Álvaro | Decisão Could vs polish (registrar no STATUS) |
| Bruno | Eventos de emergência no bus |
| Bernardo | Persistência mínima emergência |
| José | State ou Command no protocolo (1 padrão novo) |
| Marcus | UC contingência (fluxo linear + alternativa) |

---

### Semana 12 — 24 a 30/11 (Semana TCC — ritmo reduzido)

| Aluno | Entrega (leve) |
|-------|----------------|
| Todos | 1 commit pequeno + relatório ½ página |
| Álvaro | Atualizar backlog Won't |
| Bruno | Revisar README arquitetura |
| Bernardo | Fix bugs persistência |
| José | Fix bugs alarme |
| Marcus | Organizar `docs/marco3/` rascunho |

---

### Semana 13 — 01 a 07/12

| Aluno | Entrega |
|-------|---------|
| Álvaro | Checklist aceite final completo |
| Bruno | Empacotar execução (script/JAR doc) |
| Bernardo | Dados demo estáveis |
| José | Cenários GoF na apresentação |
| Marcus | Vídeo curto ou roteiro de demo 5 min |

---

### Semana 14 — 08 a 14/12

| Aluno | Entrega |
|-------|---------|
| Todos | Ensaio 10 min; cada um fala da sua trilha |
| Álvaro | Slides finais |
| Bruno | Diagramas finais exportados |
| Bernardo | Backup demo |
| José | Lista padrões com prints de código |
| Marcus | UCs finais PDF |

---

### Semana 15 — 15 a 17/12 → **MARCO 3**

Apresentação final: MVP Must + Should (acesso) + Could mínimo se couber; backlog Won't explícito.

---

## Avaliação individual

Toda semana cada aluno tem:

- Artefato nomeado (código ou doc) na sua trilha
- Relatório em `docs/semanas/...`
- Responsável único (não “todo mundo fez tudo”)

Quadro vivo: [`semanas/STATUS.md`](semanas/STATUS.md)

---

## Uso do material já no GitHub

1. Tratar `docs/marco1/*` e o PDF como **draft**
2. Semanas 0–4: cada um **adota e corrige** a parte da sua trilha
3. Em **05/10** empacotar a versão oficial do Marco 1 a partir do que foi validado semana a semana
