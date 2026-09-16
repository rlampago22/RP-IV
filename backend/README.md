# backend/ — API Java (destino)

Pasta reservada no padrão monorepo (**backend/** + **frontend/**), como SafePlace/Sentinela.

## Estado atual

| Fonte | Papel |
|-------|--------|
| [`../mvp/`](../mvp/) | **Canônico** — domínio EDA executável + testes Must + Swing legado |
| [`../src/`](../src/) | Esqueleto Java antigo (não usar na demo) |
| **esta pasta** | Destino da migração + API HTTP para o React |

## Direção

1. Mover/consolidar pacotes de `mvp/src/...` para `backend/src/...` (Maven/Gradle quando o grupo fechar).
2. Expor estado derivado do EventBus (JSON/HTTP) consumido por `frontend/`.
3. Manter regra EDA: domínio sem acoplar UI.

Detalhes: [`../docs/backend-java.md`](../docs/backend-java.md)

## Por enquanto

Não há servidor HTTP aqui. Para rodar o núcleo:

```bat
cd ..\mvp
2-TESTAR-MVP.bat
```
