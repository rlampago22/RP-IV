# Entrega semanal — Bruno Rocha

- **Semana / sprint:** S3 (2026-09-21) + débito (2026-09-08→21)
- **Issue:** [#41](https://github.com/rlampago22/RP-IV/issues/41) (débito Pacotes/EventBus) + [#44](https://github.com/rlampago22/RP-IV/issues/44) (S3 API estado + T01 Overview)
- **Branch:** `bruno`
- **Trilha:** EventBus, API de estado, T01 Overview

## 1. O que foi feito

- **Backend:** novo pacote `apiestado` (`mvp/src/br/edu/unipampa/usina/apiestado/`) com `EstadoAgregador` (consumidor global do EventBus, deriva sensores/status/alarmes/timeline **apenas** dos eventos — sem acoplar a `ReatorFacade`/`AlarmeFacade`) e `ApiEstadoHttpServer` (stub HTTP JDK puro, `GET /api/estado`, `POST /api/tempo-real/{iniciar,pausar}`, `POST /api/alarmes/{id}/{reconhecer,resolver}`). Entry point `ApiEstadoMain` sobe o backend completo (EventBus + 4 sensores + auditoria) e expõe a API em `:8080`.
- **Frontend:** `T01 Overview` (`OverviewPage.jsx`) deixou de ter sensores/contadores/alarmes hardcoded e passou a consumir `estado` via `EstadoContext` (`frontend/src/state/EstadoContext.jsx`, polling 3s) + `frontend/src/api/estado.js` (cliente + contrato mock de fallback). Badge do núcleo e o alarm banner (`main.jsx`) também são dirigidos pelo mesmo estado. Botões "Iniciar/Pausar Tempo Real", "Validar/Reconhecer" e "Normalizar/Encerrar" chamam a API de verdade.
- **Docs:** contrato JSON completo documentado em [`docs/api-estado-contrato.md`](../../api-estado-contrato.md); `docs/marco1/diagramas/pacotes.puml` e `componentes-logicos.puml` atualizados com o pacote `ApiEstado` (evidência de pacotes/componentes do débito #41); `mvp/README.md` e `docs/backend-java.md` linkados ao novo fluxo.
- **Testes:** `TestesApiEstado` (novo, roda `java -cp out br.edu.unipampa.usina.app.TestesApiEstado`) cobre transição ESTAVEL→ATENCAO→CRITICO→ATENCAO→ESTAVEL e o JSON gerado. `2-TESTAR-MVP.bat` (11/11 Must) continua passando sem alteração — sem regressão no fluxo EDA básico.

## 2. Como foi feito

- **EDA:** `EstadoAgregador` é só mais um `IEventSubscriber` (`eventBus.assinarTodos`), igual ao `AuditoriaSubscriber` — reforça que API/Frontend são adaptadores que observam fatos publicados, não o domínio (regra da arquitetura, `docs/marco1/04-arquitetura-eda.md`).
- **Endpoints:** ver contrato completo em `docs/api-estado-contrato.md`. CORS liberado para o Vite local; ações de alarme usam operador/justificativa fixos (documentado — simplificação assumida para não expandir escopo de parsing de JSON de entrada).
- **Componentes React:** `EstadoContext` centraliza fetch + fallback; `OverviewPage`/`Shell` só leem do contexto, sem duplicar lógica de rede.
- **Bug pego em teste manual:** a simulação de "tempo real" com passeio aleatório puro grudou acima do limite de pressão e dobrou ~45 alarmes em 3 min; corrigido com reversão à média (oscila em torno do setpoint operacional, `ApiEstadoMain.proximoValor`). Validado depois por 90s contínuos: só 2 alarmes, ciclo completo reconhecer→resolver→volta a ESTÁVEL confirmado via curl.

## 3. Como será aplicado

- Eventos produzidos: nenhum novo — `ApiEstadoMain` só publica `MedicaoRegistrada` (via `ReatorFacade.receberLeitura`, igual ao `DemoMvp`).
- Eventos consumidos: todos os 6 obrigatórios de `docs/ui/direcao-opcao-a.md` (`MEDICAO_REGISTRADA`, `OBSERVACAO_REGISTRADA`, `FALHA_SENSOR_DETECTADA`, `ALARME_EMITIDO`, `ALARME_RECONHECIDO`, `ALARME_RESOLVIDO`).
- UI: T01 Overview — núcleo/badge, 4 sensores RF-1 com barra, contadores EDA, timeline, caixa de alarmes e as 3 ações, tudo espelhando `/api/estado`.

## 4. O que foi entregue (DoD)

- [x] Código back em `mvp/` (canônico até a migração para `backend/`, ver `docs/backend-java.md`)
- [x] Código front em `frontend/` (T01 Overview completo, Opção A)
- [x] Evidência: testes automatizados (`TestesApiEstado`) + smoke test manual documentado nesta entrega (curl end-to-end: ESTAVEL → CRITICO → reconhecer → resolver → ESTAVEL)
- [ ] PR → `desenvolvimento` (abrir após revisão)

## 5. Bloqueios

- Nenhum bloqueio técnico. Ponto de atenção para a S4 (#49): a API roda em processo único sem persistência entre reinícios (estado em memória) — ok para o MVP, mas vale registrar se o grupo decidir manter o stub até o Marco 1.

## 6. Próxima semana

- #49 (S4, 28/09): polish do T01 (deixar tempo real ligado por padrão na demo, contrato API estável para T02/T03 não quebrarem) + diagramas EDA para a apresentação.
