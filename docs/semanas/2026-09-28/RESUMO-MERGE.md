# Resumo da semana 4 — merge em desenvolvimento

- **Período:** 22/09–03/10/2026
- **Data do merge:** 2026-10-03
- **PRs:** [#65](https://github.com/rlampago22/RP-IV/pull/65) bernardo · jose (rebase → `desenvolvimento`) · [#67](https://github.com/rlampago22/RP-IV/pull/67) marcus
- **Escopo:** fechar gaps S4 do MVP web (seeds T02, alarme live T03, T05 → EventBus/API)

## Visão geral

`desenvolvimento` agora tem o fluxo vertical Must para a demo de 05/10: medições previsíveis (seeds), histórico no snapshot, ciclo de alarme via HTTP, e T05 disparando Normal / Observação / Falha / Crítico no mesmo `EstadoContext` da T01–T04. Login Spring da branch `alvaro` **não** entrou (fora do MoSCoW Must).

## Por integrante

| Aluno | Issue | Back | Front | Resultado |
|-------|-------|------|-------|-----------|
| Álvaro | #48 | — | Login Spring em branch (não mergeado) | Pendente: checklist Must PASS com evidência no sistema atual |
| Bruno | #49 | — | — | Pendente: polish T01 + README alinhados |
| Bernardo | #50 | `CenariosMedicao`, `historico[]`, seeds HTTP | T02 seeds + sparkline da API | **Merge #65** |
| José | #51 | `POST /api/demo/anomalia`, `TestesApiAlarmes` | T03 live + T05 parcial | **Código em desenvolvimento** (rebase pós-#65) |
| Marcus | #52 | `POST /api/cenarios/{normal\|observacao\|falha\|critico}` | T05 completo + textos operacionais | **Merge #67** |

## Eventos EDA tocados nesta semana

`MedicaoRegistrada`, `ObservacaoRegistrada`, `FalhaSensorDetectada`, `AlarmeEmitido`, `AlarmeReconhecido`, `AlarmeResolvido` — todos acionáveis pela UI via API.

## Telas T0X pós-merge

| Tela | Status |
|------|--------|
| T01 Overview | `/api/estado` + tempo real + ACK/resolver |
| T02 Sensores | Histórico real + condições Normal/Observação |
| T03 Alarmes | Ciclo live ACK → Resolver |
| T04 Auditoria | Timeline do snapshot |
| T05 Demo | 4 cenários → EventBus |

## Como rodar

```bat
mvp\4-EXECUTAR-API-ESTADO.bat
cd frontend
npm install
npm run dev
```

Fluxo ensaio: T02 Normal → Observação → T05 Crítico → T03 ACK/Resolver → T04.

## Débitos / Marco 1 (05/10)

- [#48](https://github.com/rlampago22/RP-IV/issues/48) Álvaro — checklist PASS (sem slides no pacote grupo)
- [#49](https://github.com/rlampago22/RP-IV/issues/49) Bruno — polish T01/README
- Apresentações individuais #53–#57 — cada um no mesmo commit de `desenvolvimento`
- **Não** mergear auth Spring até pós–Marco 1
