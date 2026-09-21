# Relatório semanal — José Guilherme Monteiro

- **Semana:** 2 (entrega 14/09/2026)
- **Trilha:** Alarmes, limiares e padrões GoF
- **Data:** 2026-09-14

## O que entreguei

- `Limiar` imutável com validação dos limites mínimo e máximo
- `AvaliadorLimiarPadrao` para identificar valores fora da faixa segura
- `Alarme` com tipo, status, timestamp e medição de origem
- `AlarmeFactory` para criação de alarmes
- `AlarmeFacade` para notificação, registro e publicação do alarme
- `EventBus` in-process com inscrição por tipo de evento
- `RegistroAuditoria` append-only em memória
- `ReatorRepository` em memória para medições e alarmes
- Integração da avaliação de limiar e emissão de alarme na `ReatorFacade`
- Demo com uma leitura dentro do limite e outra acima do limite de temperatura

## Padrões aplicados

- **Strategy:** `AvaliadorLimiar` permite trocar a estratégia de avaliação
- **Factory:** `AlarmeFactory` centraliza a criação de `Alarme`
- **Facade:** `ReatorFacade` e `AlarmeFacade` coordenam o fluxo entre os módulos
- **Observer/Pub-Sub:** `EventBus` distribui eventos aos consumidores inscritos

## Evidências

- Arquivos:
	- `src/main/java/.../alarmes/Alarme.java`
	- `src/main/java/.../alarmes/AlarmeFactory.java`
	- `src/main/java/.../alarmes/AlarmeFacade.java`
	- `src/main/java/.../controlereator/Limiar.java`
	- `src/main/java/.../controlereator/AvaliadorLimiar.java`
	- `src/main/java/.../controlereator/AvaliadorLimiarPadrao.java`
	- `src/main/java/.../controlereator/ReatorFacade.java`
	- `src/main/java/.../infraestruturaeventos/EventBus.java`
	- `src/main/java/.../auditorialogs/RegistroAuditoria.java`
	- `src/main/java/.../persistenciareator/ReatorRepository.java`
	- `scripts/run-demo-medicoes.bat`
	- `docs/semanas/2026-09-14/jose.md`

## Como rodar a demo

Na raiz do projeto, com um JDK instalado:

```bat
scripts\run-demo-medicoes.bat
```

Ou pelo PowerShell:

```powershell
cmd /c scripts\run-demo-medicoes.bat
```

## Resultado observado

- 3 medições registradas
- A leitura `310.5` permaneceu dentro do limite `280.0–311.0`
- A leitura `312.0` violou o limite máximo
- 1 alarme criado
- Notificações enviadas para Operador e Supervisão Central
- 4 eventos auditados: 3 medições e 1 alarme

Saída principal:

```text
[ALARME] LIMIAR_VIOLADO -> Operador
[ALARME] LIMIAR_VIOLADO -> Supervisao Central
Total de medicoes: 3
Total de alarmes: 1
Total de eventos auditados: 4
```

## Bloqueios / dúvidas

- Nenhum bloqueio após a instalação do JDK.
- A persistência desta primeira fatia é em memória; banco de dados real fica para etapa posterior.

## Próxima semana (previsto)

- Integrar o fluxo de alarmes com os demais módulos do núcleo
- Alinhar os diagramas de sequência ao código executado
- Evoluir a notificação e o tratamento de falhas de entrega