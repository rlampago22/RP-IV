# Semana 2 — José Guilherme

## Objetivo

Implementar a primeira fatia de código do núcleo Must: avaliação de limiares, emissão de alarmes, publicação de eventos e auditoria.

## Entregas

- `EventBus` in-process com inscrição por tipo de evento.
- `RegistroAuditoria` append-only em memória.
- `Limiar` imutável com validação de limites.
- `AvaliadorLimiarPadrao` como Strategy de avaliação.
- `AlarmeFactory` para criação de alarmes.
- `AlarmeFacade` para notificação, persistência e evento `AlarmeEmitido`.
- `ReatorRepository` em memória para medições e alarmes.
- Integração do fluxo na `ReatorFacade`.
- Demo com leitura normal e leitura que viola o limite de temperatura.

## Padrões aplicados

- **Strategy:** `AvaliadorLimiar` permite trocar a regra de avaliação.
- **Factory:** `AlarmeFactory` centraliza a criação de `Alarme`.
- **Facade:** `ReatorFacade` e `AlarmeFacade` coordenam os módulos.
- **Observer/Pub-Sub:** `EventBus` distribui eventos aos consumidores.

## Critérios demonstrados

- Medições são persistidas e publicadas como `MedicaoRegistrada`.
- Limite respeitado não cria alarme.
- Limite violado cria e registra alarme para Operador e Supervisão Central.
- Eventos de medição e alarme são recebidos pelo auditor.

## Validação

- A checagem de erros do editor não encontrou erros nos arquivos alterados.
- A execução da demo ficou pendente porque o ambiente atual não possui `java`/`javac` no `PATH`.
- O script `scripts\run-demo-medicoes.bat` foi atualizado para compilar todos os módulos Java necessários.