# Aceite visual — Opção A · SCADA escuro

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

- [x] Mesmo chrome/tokens nas 5 telas (Opção A) — shell React `frontend/src/main.jsx`
- [x] Cor só para status/alarme
- [x] Núcleo muda ESTÁVEL → ATENÇÃO → CRÍTICO nos cenários (via estado compartilhado T03)
- [x] 4 sensores RF-1 com valor + barra (T01/T02 · `data/sensores.js`)
- [x] Timeline de eventos EDA visível (T01)
- [x] ACK + Resolver atualizam estado e auditoria (mock PlantContext → T03/T04)
- [ ] Fluxo demo T05 → T01 → T03 → T04 compreensível em &lt; 5 s (cenários T05 ainda mock estático — S4 #52)

## Pendências #40 (grupo)

- [ ] Atualizar frames Figma T01–T05 no chrome escuro (arquivo existente — ver [`FIGMA.md`](FIGMA.md))
- [x] Export PNG em [`exports/`](exports/) (parcial: overview + mimic)
- [x] Shell React portado do protótipo
