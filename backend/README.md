# backend/ — API Java e autenticação (RF07)

API Spring Boot da Central de Supervisão. Entrega autenticação web baseada em sessão e migrations Flyway. O perfil local padrão é **H2** (sem PostgreSQL). Telemetria/alarmes continuam no stub `mvp/apiestado` (porta 8080).

## Papéis implementados

Os códigos são fiéis aos atores formais de [`../docs/marco1/01-requisitos-rf-rnf.md`](../docs/marco1/01-requisitos-rf-rnf.md):

- `OPERADOR_REATOR`
- `SUPERVISAO_CENTRAL`
- `GUARDA_ACESSO` — controle físico do UC04, separado do login da Central
- `ADMINISTRADOR_SISTEMA`

## Executar (demo Must expandido)

Java 21+ e Maven 3.9+.

```powershell
cd backend
$env:APP_BOOTSTRAP_PASSWORD = "troque-esta-senha"
mvn spring-boot:run
```

Perfil padrão: `h2` → `jdbc:h2:file:./data/usina`. Porta **8081**.

Contas bootstrap: `admin@usina.local`, `operador@usina.local`, `supervisao@usina.local`, `guarda@usina.local` — senha = `APP_BOOTSTRAP_PASSWORD` (ou `troque-esta-senha`).

Para PostgreSQL: `$env:SPRING_PROFILES_ACTIVE = "postgres"` e variáveis em [`.env.example`](.env.example).

Endpoints: `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`, `GET /api/central/status`.

Contrato: [`../docs/backend-autenticacao.md`](../docs/backend-autenticacao.md).

**Registro de acesso físico (RF08 / UC04)** vive no domínio `mvp/` (`POST /api/acesso/solicitar`), não nesta API Spring.
