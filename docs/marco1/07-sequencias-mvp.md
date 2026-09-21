# 07 — Diagramas de Sequência (núcleo MVP)

**Sistema:** Controle de Usina Nuclear — RP4  
**Equipe:** Álvaro Domingues, Bruno Rocha, Bernardo Dorneles, José Guilherme Monteiro, Marcus Querol

Todas as mensagens referenciam métodos existentes no código do MVP e em [06-classes-projeto-mvp.md](06-classes-projeto-mvp.md).

Fonte UML editável: [Marcus-UML-MVP.asta](../marcus/diagramas/Marcus-UML-MVP.asta). As imagens foram exportadas pelo Astah.

---

## SEQ-UC01 — Registrar Medição (fluxo principal até include de alarme)

![Diagrama UML de sequência UC01](<../marcus/diagramas/Marcus-UML-MVP/02 - SEQ UC01 Registrar Medição.png>)

O desenho acompanha o código atual: o histórico de medições ainda é mantido em memória por `ReatorFacade`; portanto, não foi inventada uma chamada a `ReatorRepository`.

---

## SEQ-UC02 — Emitir Alarme (limiar violado)

![Diagrama UML de sequência UC02](<../marcus/diagramas/Marcus-UML-MVP/03 - SEQ UC02 Emitir Alarme.png>)

As notificações a Operador e Supervisão Central representam a saída de console realizada por `AlarmeFacade`; ainda não existe serviço externo de notificação nem persistência de alarme em banco.

---

## SEQ-UC02-A1 — Limiar não violado (alternativa)

1. `AlarmeFacade.processarAvaliacao` recebe `violado == false`
2. Facade encerra sem `emitirAlerta`
3. Nenhum `AlarmeEmitido` é publicado; a medição original já foi auditada

---

## SEQ-UC04 (Should — rascunho)

`Guarda -> AcessoFacade.solicitarAcesso -> RegistroAcesso.registrar -> EventBus`  
Implementação e desenho detalhado no Marco 3.
