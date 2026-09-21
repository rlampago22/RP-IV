# UI unificada — aceite visual (Opção A · SCADA escuro)

**Protótipo:** [`propostas/opcao-a.html`](propostas/opcao-a.html) · Spec: [`direcao-opcao-a.md`](direcao-opcao-a.md)  
Refs de fluxo: NUREG-0700 (partitioning) + MVP Marcus (`SistemaMvpUI` / roteiro).

## Chrome idêntico em T01–T05

| Zona | Conteúdo fixo |
|------|----------------|
| TopBar | Reator-01 + badge ESTÁVEL/ATENÇÃO/CRÍTICO + status EventBus |
| Nav/tabs | Visão Geral / Sensores / Alarmes / Auditoria / Demo — ativo com borda cyan |
| AlarmBanner | Mensagem + **Validar / Reconhecer** (âmbar); some se sem alarme ativo |
| Content | único que muda por tela |
| Ações globais (T01) | Tempo Real · Reconhecer · Normalizar/Encerrar |

## Tokens (não misturar)

- bg `#0B0F19` · card `#131C2E` · card2 `#1A263E` · border `#22314F`
- text `#F8FAFC` · muted `#94A3B8`
- ok `#10B981` · cyan `#06B6D4` · warn `#F59E0B` · crit `#EF4444` · purple `#A855F7`

## Aceite

- [ ] Mesmo chrome/tokens nas 5 telas (Opção A)
- [ ] Cor só para status/alarme
- [ ] Núcleo muda ESTÁVEL → ATENÇÃO → CRÍTICO nos cenários
- [ ] 4 sensores RF-1 com valor + barra
- [ ] Timeline de eventos EDA visível
- [ ] ACK + Resolver atualizam estado e auditoria
- [ ] Fluxo demo T05 → T01 → T03 → T04 compreensível em &lt; 5 s

## Registro de validação S3 — 2026-09-21

**Responsável:** Álvaro Domingues · **branch:** `alvaro` · **commit-base:** `51e2ba6`
**Método:** build Vite e teste renderizado em `http://127.0.0.1:5173/` (desktop e 390 × 844),
mais inspeção dos componentes em `frontend/src/`.

| Critério | Estado | Evidência / decisão |
|---|---|---|
| Chrome e tokens Opção A nas rotas T01–T05 | PASS | TopBar, tabs, banner e tokens oficiais visíveis nas rotas React. |
| Cor restrita a status/alarme | PENDENTE | O shell usa as cores previstas, mas T01 colore valores de telemetria normais. Revisar no polish visual (#48). |
| Quatro sensores RF-1 com valor e barra | PASS | T01 apresenta temperatura, pressão, radiação e fluxo, com unidade e barra. |
| Timeline EDA visível | PASS | T01 apresenta `MEDICAO_REGISTRADA` e `ALARME_EMITIDO`. |
| Reconhecer atualiza estado visível | PASS parcial | O botão do banner oculta o alarme e altera o badge para `ESTÁVEL`; ainda não atualiza T03/T04. |
| Cenário altera núcleo, banner e trilha | FAIL (S3) | Ao clicar `Simular Anomalia (Crítico)` em T05, não surgem banner/estado crítico. A própria tela declara a integração para #52. |
| Resolver atualiza estado/auditoria | FAIL (S3) | T03 e T04 não possuem handlers nem dados compartilhados neste scaffold. |
| Fluxo T05 → T01 → T03 → T04 em menos de 5 s | N/E | Depende da integração T05/API/EventBus planejada para #52; não marcar PASS antes de teste integrado. |

**Saúde do ambiente:** `npm run build` passou. A página carregou com conteúdo significativo, sem overlay
de framework e sem erros de console; os dois avisos encontrados são *future flags* do React Router.
O roteiro e os critérios de reexecução estão em [`walkthrough-s3.md`](walkthrough-s3.md). Este registro
é uma linha de base de UX, não substitui o aceite final integrado do MVP Must.
