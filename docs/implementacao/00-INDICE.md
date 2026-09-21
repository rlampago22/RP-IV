# Rastreabilidade da implementação — índice

O que já existe no código × o que ainda é idealização.

## Status por camada

| Camada | Onde | Status |
|--------|------|--------|
| EventBus + eventos de domínio | `mvp/src/.../infraestruturaeventos/` | **OK** (Must) |
| Medições RF-1 / ReatorFacade | `mvp/src/.../controlereator/` | **OK** |
| Alarmes + Strategy limiar | `mvp/src/.../alarmes/` | **OK** |
| Auditoria SHA-256 append-only | `mvp/src/.../auditorialogs/` | **OK** |
| Testes Must 11/11 | `mvp/.../TestesMvp.java` + `2-TESTAR-MVP.bat` | **OK** |
| UI Swing (fluxo) | `mvp/.../SistemaMvpUI.java` | **Legado** — não é visual final |
| UI React Opção A | `frontend/` | **Em curso** — shell + páginas T01–T05 mock |
| Protótipo HTML Opção A | `docs/ui/propostas/opcao-a.html` | **OK** (referência visual) |
| API HTTP Java → React | `backend/` | **Pendente** (migração) |
| Esqueleto `src/main/java` | `src/` | **Legado** — não usar na demo |

## Status por requisito (Must)

| ID | Descrição | Evidência |
|----|-----------|-----------|
| RF-1 | Telemetria 4 sensores | MVP + T01/T02 |
| RF-2 | Alarmes por limiar | MVP + T03 |
| Auditoria | Append-only + hash | MVP + T04 |
| UC01 | Fluxos principal/alt/exceção | Roteiro MVP + T05 |
| UI supervisão | Web Opção A | Protótipo A; React parcial |

## Próximos passos (issues)

Ver [`../PLANEJAMENTO_DESENVOLVIMENTO.md`](../PLANEJAMENTO_DESENVOLVIMENTO.md) e [`../semanas/STATUS.md`](../semanas/STATUS.md).
