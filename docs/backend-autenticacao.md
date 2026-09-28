# Autenticação web — primeira fatia vertical

Esta implementação atende à necessidade de login da interface web sem alterar o significado do UC04: autenticação web identifica quem usa a Central; `registro_acesso` continua reservado para a tentativa de entrada física em área restrita.

## Perfis

Os papéis persistidos refletem somente os atores formais de [RF/RNF](marco1/01-requisitos-rf-rnf.md):

| Código | Origem documental | Acesso inicial |
|---|---|---|
| `OPERADOR_REATOR` | Operador de Reator | Central de Supervisão |
| `SUPERVISAO_CENTRAL` | Supervisão Central | Central de Supervisão |
| `GUARDA_ACESSO` | Guarda / Controle de Acesso (UC04) | Sem acesso à Central; futuro módulo UC04 |
| `ADMINISTRADOR_SISTEMA` | Administrador do Sistema (configuração) | Central de Supervisão; futura administração |

Não foi criado o papel de Engenheiro de Segurança: ele aparece no roteiro como responsável de uma ação, mas não consta entre os atores formais do escopo.

## Modelo persistido

`papel` e `usuario` compõem uma relação N:N por `usuario_papel`. Senhas são armazenadas exclusivamente em `senha_hash` com BCrypt. `registro_autenticacao` registra sucesso ou falha de login; é distinto de `registro_acesso`, que permanece no modelo do UC04.

As migrations estão em [`../backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration/). O perfil local padrão usa PostgreSQL; H2 persistente continua disponível pelo perfil `h2` para desenvolvimento isolado. A modelagem e as migrations não dependem do banco escolhido.

## Contrato HTTP inicial

| Método e rota | Uso | Sessão/papel |
|---|---|---|
| `POST /api/auth/login` | Inicia sessão HTTP a partir de e-mail e senha | Público |
| `GET /api/auth/me` | Retorna identidade e papéis da sessão | Autenticado |
| `POST /api/auth/logout` | Encerra a sessão | Autenticado |
| `GET /api/central/status` | Verifica acesso à Central | Operador, Supervisão ou Administrador |

O frontend envia o cookie de sessão com `credentials: 'include'` e não armazena senha ou token no navegador.

## Contas locais e redefinição

Na primeira execução, o bootstrap cria quatro contas locais de demonstração. A senha é lida de `APP_BOOTSTRAP_PASSWORD` somente na criação. Para mudar a senha dessas contas em ambiente local, use `APP_RESET_BOOTSTRAP_PASSWORDS=true` por uma única inicialização e volte a variável para `false`; assim não há redefinição recorrente nem alteração de outros usuários.
