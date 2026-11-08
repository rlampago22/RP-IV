# Entrega semanal: José Guilherme Monteiro

- **Semana / sprint:** 4 (2026-09-28)
- **Issue:** #51
- **Branch:** `jose`
- **Trilha:** Alarmes / limiar / GoF e T03

## 1. O que foi feito

- **Backend:** adicionada a rota de demo `POST /api/demo/anomalia`, que registra leitura crítica de temperatura e dispara o fluxo de alarme existente.
- **Frontend:** T05 consegue disparar a anomalia pela API; T03 reconhece e resolve alarmes por `POST /api/alarmes/{id}/reconhecer` e `/resolver`.
- **Feedback:** falhas da API são exibidas na interface em vez de serem ignoradas.
- **Testes:** criado teste HTTP automatizado cobrindo anomalia, reconhecimento e resolução com o servidor real.
- **Documentação:** roteiro curto da demo e mapa dos padrões GoF adicionados ao roteiro de apresentação.

## 2. Como foi feito

- A rota de demo chama `ReatorFacade.receberLeitura(1L, 372.0)`. A medição é publicada no EventBus, avaliada pela Strategy e gera `AlarmeEmitido` quando viola o limite.
- A tela T05 chama `/api/demo/anomalia`. O `EstadoContext` consulta `/api/estado` e envia as ações de T03 para as rotas de reconhecimento e resolução.
- Cada ação retorna um snapshot atualizado, mantendo o front sincronizado com o estado derivado dos eventos.
- O script padrão de testes do MVP executa o teste HTTP de ciclo de vida depois da suíte Must.

## 3. Como será aplicado

1. T05 dispara anomalia crítica.
2. A API publica `AlarmeEmitido`; o polling atualiza T01 e o banner.
3. T03 chama a rota de reconhecimento e o estado passa para `RECONHECIDO`.
4. T03 chama a rota de resolução; o estado passa para `RESOLVIDO` e a timeline recebe `ALARME_RESOLVIDO`.

Padrões GoF do fluxo Java:

- **Facade:** `mvp/src/br/edu/unipampa/usina/controlereator/ReatorFacade.java`
- **Strategy:** `mvp/src/br/edu/unipampa/usina/alarmes/AvaliadorLimiar.java` e `AvaliadorFaixaSegura.java`
- **Factory:** `mvp/src/br/edu/unipampa/usina/alarmes/AlarmeFactory.java`
- **Observer / Pub-Sub:** `mvp/src/br/edu/unipampa/usina/infraestruturaeventos/EventBus.java` e `mvp/src/br/edu/unipampa/usina/alarmes/AlarmeFacade.java`

## 4. O que foi entregue (DoD)

- [x] T03 ACK/Resolver contra os endpoints reais da API
- [x] Cenário crítico de demo conectado ao backend
- [x] Roteiro curto: anomalia → banner → reconhecer → resolver
- [x] Mapa de padrões GoF com arquivos em `mvp/`
- [x] Teste HTTP do ciclo completo
- [x] Build de produção do frontend
- [x] Evidência por comando e saída dos testes
- [ ] PR da branch `jose` para `desenvolvimento`

## 5. Bloqueios

- Nenhum bloqueio técnico conhecido.
- Commit, push e abertura do PR ficam para execução manual do autor.

## 6. Próxima semana

- Integrar eventuais ajustes solicitados no PR.
- Repetir ensaio do fluxo crítico com T05, T01, T03 e T04.

## Evidência de validação

Comando: `java -Dfile.encoding=UTF-8 -cp <classes> br.edu.unipampa.usina.app.TestesApiAlarmes`

Resultado: ciclo HTTP real passou com HTTP 200 para anomalia, ACK e resolução; snapshot atualizado para `CRITICO`, `ATENCAO` e alarme `RESOLVIDO`; evento `ALARME_RESOLVIDO` presente na timeline.

Ensaio visual: no navegador, T05 disparou a anomalia, o banner apareceu, T03 reconheceu o alarme, habilitou a resolução e terminou com o badge `ESTÁVEL` e ocorrência na lista de encerrados.

Comando: `npm run build` em `frontend/`.

Resultado: build Vite concluído com sucesso.