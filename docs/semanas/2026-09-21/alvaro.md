# Entrega semanal — Álvaro Domingues

- **Semana / sprint:** S3 (2026-09-21)
- **Issue:** #43
- **Branch:** `alvaro`
- **Trilha:** aceite Must, feedback APS e UX Opção A

## 1. O que foi feito

- Backend/docs: executei a suíte integrada do checklist Must com Oracle JDK 27; as 11
  verificações RF01–RF06/RNF passaram com 100% de êxito.
- Frontend/UX: validei o shell React T01–T05 contra a direção visual e registrei critérios,
  divergências e a dependência do fluxo integrado.
- Documentei o percurso do operador e o roteiro de reexecução em
  [`../../ui/walkthrough-s3.md`](../../ui/walkthrough-s3.md).

## 2. Como foi feito

- Comparei `docs/ui/aceite-visual.md`, `docs/ui/direcao-opcao-a.md`, o protótipo Opção A e as
  rotas/componentes em `frontend/src/`.
- Instalei Node.js LTS 24.19.0, executei `npm install` e `npm run build`, e validei a UI em
  `http://127.0.0.1:5173/` nos viewports desktop e 390 × 844.
- Modelei um estado compartilhado de demonstração entre T05, T01, T03 e T04. Ele reproduz o
  ciclo de eventos da UI sem substituir a futura publicação real via EventBus/API.

## 3. Como será aplicado

- A pessoa apresentadora inicia em T05 e segue T01 → T03 → T04 conforme o walkthrough.
- Ao integrar a API/EventBus, substituir a fonte de estado mockada e reexecutar os cinco passos
  do roteiro contra os eventos reais.

## 4. O que foi entregue (DoD)

- [x] Checklist Must revisável, preservando evidência obrigatória para PASS.
- [x] Aceite visual Opção A com linha de base S3 marcada e divergências rastreáveis.
- [x] Walkthrough curto T01–T05 entregue.
- [x] Relatório semanal preenchido.
- [x] Build e validação renderizada do ciclo T05 → T01 → T03 → T04 executados.
- [x] Execução Java da suíte Must: 11/11 verificações passaram.
- [ ] PR → `desenvolvimento`.

## 5. Bloqueios

- O JDK 27 está instalado; terminais abertos antes da instalação podem exigir reinicialização
  para enxergar `java`/`javac` no `PATH`.
- A UI usa estado compartilhado de demonstração; a ligação com o EventBus/API permanece na #52.

## 6. Próxima semana

- Após a integração, executar o walkthrough, capturar evidências desktop/mobile e fechar o
  aceite visual final da issue #48.
