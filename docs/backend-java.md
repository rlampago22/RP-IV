# Backend — Java (EDA)

Código de domínio e EventBus.

## Estado atual

A implementação **executável** do MVP está em [`../mvp/`](../mvp/) (inclui Swing legado de fluxo).  
A pasta [`../backend/`](../backend/) é o **destino** no monorepo (`backend/` + `frontend/`).  
`src/` na raiz é esqueleto legado — ver [`../src/README.md`](../src/README.md).

## Direção

Migrar gradualmente o domínio de `mvp/src/...` → `backend/`, expondo API HTTP para o `frontend/` React (Opção A).

## API de estado (stub, issues #41/#44)

`mvp/src/br/edu/unipampa/usina/apiestado/` expõe `GET/POST http://localhost:8080/api/*` derivado do EventBus (JDK `com.sun.net.httpserver`, sem dependências externas). Contrato completo, endpoints e como rodar: [`api-estado-contrato.md`](api-estado-contrato.md).

## Trilhas (entrega vertical)

| Trilha | Pacotes |
|--------|---------|
| Bernardo | ControleReator, medições |
| Bruno | EventBus |
| José | Alarmes, Strategy |
| Marcus | AuditoriaLogs |
| Álvaro | Aceite / contratos API |
