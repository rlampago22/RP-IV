# Como fica a entrega de segunda (05/10) — TOK = código funcionando

## Sistema que o professor vê

Um único checkout: branch **`desenvolvimento`**.

```bat
mvp\4-EXECUTAR-API-ESTADO.bat
cd frontend && npm install && npm run dev
```

Fluxo ao vivo (valor):

1. Header **ONLINE**
2. T02 → Normal → Observação (atenção)
3. T05 → Crítico (alarme)
4. T03 → reconhecer → resolver
5. T04 → timeline / menção a `mvp/dados/auditoria.log`
6. (opcional) `mvp\2-TESTAR-MVP.bat` → 11/11

Isso cobre RF01–RF06 + EDA + UI Opção A T01–T05. Artefatos (checklist, PDF APS, guia) só **apoiam** esse fluxo — não substituem.

## O que cada um mostra (individual)

| Quem | Issue | Código funcionando |
|------|-------|-------------------|
| Álvaro | #53 | Escopo Must + checklist PASS + abrir UI ONLINE |
| Bruno | #54 | EventBus → EstadoAgregador → T01; tempo real |
| Bernardo | #55 | T02 seeds/histórico = API |
| José | #56 | Anomalia → ACK → Resolver + GoF no código |
| Marcus | #57 | T05 4 cenários + T04 + auditoria |

Slides: individuais (fora do pacote do grupo).

## Branches × Dev

| Branch | Em Dev? | Ação |
|--------|---------|------|
| bernardo / jose / marcus / bruno | Sim (alinhadas) | Usar `desenvolvimento` |
| alvaro (login Spring) | **Não** | Manter na `alvaro` até pós-M1 |

## Pendências leves (não bloqueiam demo)

- #48 Álvaro: confirmar/assumir checklist vigente + falar aceite (docs em `2026-09-28/alvaro.md`)
- #49 Bruno: READMEs alinhados ao HTTP real (este pacote)
