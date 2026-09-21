# Diagramas UML do Marcus

Fonte oficial desta trilha: [`Marcus-UML-MVP.asta`](Marcus-UML-MVP.asta).

O projeto contém três diagramas UML nativos do Astah:

1. casos de uso do núcleo Must (UC01, UC02 e UC03);
2. SEQ-UC01 — registrar medição;
3. SEQ-UC02 — emitir alarme e auditar o evento.

As imagens em [`Marcus-UML-MVP/`](Marcus-UML-MVP/) foram exportadas pelo próprio Astah e são as versões usadas nos documentos Markdown e na apresentação.

Os diagramas refletem o código executável atual:

- `ReatorFacade` mantém o histórico de medições em memória;
- `EventBus` entrega eventos de forma síncrona e in-process;
- `AlarmeFacade` avalia limiar, cria/registra o alarme e publica `AlarmeEmitido`;
- `AuditoriaSubscriber` consome os eventos e chama `RegistroAuditoria.registrar`;
- Operador e Supervisão Central aparecem explicitamente como destinatários do alarme.

