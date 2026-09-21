# Entrega semanal — Álvaro Domingues

- **Semana / sprint:** S3 (2026-09-21)
- **Issue:** #43
- **Branch:** `alvaro`
- **Trilha:** aceite Must, feedback APS e UX Opção A

## 1. O que foi feito

- Backend/docs: revisei o checklist Must RF01–RF06 e RNF do núcleo sem antecipar PASS sem
  execução; o JDK continua ausente do `PATH`.
- Frontend/UX: validei o shell React T01–T05 contra a direção visual e registrei critérios,
  divergências e a dependência do fluxo integrado.
- Documentei o percurso do operador e o roteiro de reexecução em
  [`../../ui/walkthrough-s3.md`](../../ui/walkthrough-s3.md).

## 2. Como foi feito

- Comparei `docs/ui/aceite-visual.md`, `docs/ui/direcao-opcao-a.md`, o protótipo Opção A e as
  rotas/componentes em `frontend/src/`.
- Instalei Node.js LTS 24.19.0, executei `npm install` e `npm run build`, e validei a UI em
  `http://127.0.0.1:5173/` nos viewports desktop e 390 × 844.
- Mantive a decisão EDA: T05 precisa publicar cenário e T01/T03/T04 precisam consumir o mesmo
  estado/eventos; não foi criado estado paralelo só para simular o aceite.

## 3. Como será aplicado

- A pessoa apresentadora inicia em T05 e segue T01 → T03 → T04 conforme o walkthrough.
- Ao integrar a API/EventBus, reexecutar os cinco passos do roteiro e anexar screenshots/
  console para promover os itens N/E a PASS ou registrar uma falha.

## 4. O que foi entregue (DoD)

- [x] Checklist Must revisável, preservando evidência obrigatória para PASS.
- [x] Aceite visual Opção A com linha de base S3 marcada e divergências rastreáveis.
- [x] Walkthrough curto T01–T05 entregue.
- [x] Relatório semanal preenchido.
- [x] Build e validação renderizada da UI desktop/mobile executados.
- [ ] Execução Java: bloqueada localmente pela ausência de JDK.
- [ ] PR → `desenvolvimento`.

## 5. Bloqueios

- `java`/`javac` não estão instalados ou não estão no `PATH` desta estação.
- T05, T03 e T04 ainda são telas mockadas sem estado compartilhado; o fluxo crítico é escopo
  da integração de cenários/API em #52.

## 6. Próxima semana

- Após a integração, executar o walkthrough, capturar evidências desktop/mobile e fechar o
  aceite visual final da issue #48.
