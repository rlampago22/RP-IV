# Backend — Java (EDA)

Código de domínio e EventBus.

## Estado atual

A implementação **executável** do MVP está em [`../mvp/`](../mvp/) (inclui Swing legado de fluxo).  
A pasta [`../backend/`](../backend/) é o **destino** no monorepo (padrão SafePlace/Sentinela).  
`src/` na raiz é esqueleto legado — ver [`../src/README.md`](../src/README.md).

## Direção

Migrar gradualmente o domínio de `mvp/src/...` → `backend/`, expondo API HTTP para o `frontend/` React (Opção A).

## Trilhas (entrega vertical)

| Trilha | Pacotes |
|--------|---------|
| Bernardo | ControleReator, medições |
| Bruno | EventBus |
| José | Alarmes, Strategy |
| Marcus | AuditoriaLogs |
| Álvaro | Aceite / contratos API |
