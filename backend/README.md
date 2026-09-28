# backend/ — API Java e autenticação

API Spring Boot da Central de Supervisão. A implementação inicial entrega autenticação web baseada em sessão e migrations Flyway. O perfil local padrão usa PostgreSQL; H2 permanece como alternativa para desenvolvimento isolado.

## Papéis implementados

Os códigos são fiéis aos atores formais de [`../docs/marco1/01-requisitos-rf-rnf.md`](../docs/marco1/01-requisitos-rf-rnf.md):

- `OPERADOR_REATOR`
- `SUPERVISAO_CENTRAL`
- `GUARDA_ACESSO` — controle físico do UC04, separado do login web
- `ADMINISTRADOR_SISTEMA`

## Executar

É necessário Java 21+, Maven 3.9+ e o PostgreSQL local em execução.

```powershell
cd backend
$env:POSTGRES_PASSWORD = "senha-local-do-postgres"
$env:APP_BOOTSTRAP_PASSWORD = "defina-uma-senha-local-segura"
mvn spring-boot:run
```

O perfil padrão aponta para `jdbc:postgresql://localhost:5432/usina`, com usuário configurável por `POSTGRES_USER` (padrão `postgres`). Consulte [`.env.example`](.env.example) para as variáveis. A senha não é guardada no repositório.

Na primeira execução, Flyway cria as tabelas `usuario`, `papel`, `usuario_papel` e `registro_autenticacao`. O bootstrap de desenvolvimento cria uma conta para cada perfil: `admin@usina.local`, `operador@usina.local`, `supervisao@usina.local` e `guarda@usina.local`. Todas usam a senha definida em `APP_BOOTSTRAP_PASSWORD`; se a variável não for definida, é usada apenas para desenvolvimento a senha temporária `troque-esta-senha`.

Para usar H2 sem PostgreSQL, defina `$env:SPRING_PROFILES_ACTIVE = "h2"` antes de iniciar.

### Redefinir as contas locais

As contas de bootstrap são criadas apenas na primeira execução. Se mudar `APP_BOOTSTRAP_PASSWORD` depois disso, defina temporariamente `APP_RESET_BOOTSTRAP_PASSWORDS=true` no `.env`, reinicie a API uma vez e volte a variável para `false`. Esse reset altera somente `admin@usina.local`, `operador@usina.local`, `supervisao@usina.local` e `guarda@usina.local`.

Endpoints iniciais: `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` e `GET /api/central/status`. O último é protegido no backend para Operador, Supervisão e Administrador; Guarda recebe acesso negado por ser ator exclusivo do UC04.
