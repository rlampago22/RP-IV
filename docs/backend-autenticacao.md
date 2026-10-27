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

As migrations estão em [`../backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration/). O perfil local padrão é **H2** (`spring.profiles.default=h2`); PostgreSQL fica no perfil `postgres`. A modelagem e as migrations não dependem do banco escolhido.

RF08 (`RegistroAcesso` / UC04 físico) está implementado no domínio `mvp/` (`POST /api/acesso/solicitar`), separado desta API de login.

## Contrato HTTP inicial

| Método e rota | Uso | Sessão/papel |
|---|---|---|
| `POST /api/auth/login` | Inicia sessão HTTP a partir de e-mail e senha | Público |
| `GET /api/auth/me` | Retorna identidade e papéis da sessão | Autenticado |
| `POST /api/auth/logout` | Encerra a sessão | Autenticado |
| `GET /api/central/status` | Verifica acesso à Central | Operador, Supervisão ou Administrador |
| `POST /api/auth/login/biometria` | 2ª etapa do login: assinatura do celular (ver abaixo) | Público, exige 1ª etapa na mesma sessão |
| `GET /api/auth/biometria` | Lista os aparelhos cadastrados do usuário | Autenticado |
| `POST /api/auth/biometria/cadastro/opcoes` | Gera o desafio para cadastrar um celular | Autenticado |
| `POST /api/auth/biometria/cadastro` | Conclui o cadastro (`{apelido, credencial}`) | Autenticado |
| `POST /api/auth/biometria/{id}/remover` | Remove um aparelho do usuário | Autenticado |

O frontend envia o cookie de sessão com `credentials: 'include'` e não armazena senha ou token no navegador.

## Segundo fator: biometria do celular (RNF08)

Marco 2. Atende ao RNF de Segurança do PDF da APS ("autenticação multifator") com o padrão **WebAuthn / passkey**: o celular confirma com digital ou rosto e assina um desafio; o servidor só confere a assinatura com a chave pública cadastrada. A biometria nunca sai do aparelho (relevante pela LGPD, que trata biometria como dado sensível).

Fluxo de login de quem tem aparelho cadastrado:

1. `POST /api/auth/login` com e-mail e senha corretos **não abre a sessão**: responde `{"etapa":"BIOMETRIA","opcoes":{...}}`, onde `opcoes` vai direto para `navigator.credentials.get` (no front, `startAuthentication` de `@simplewebauthn/browser`).
2. O navegador mostra um QR code; o celular (Bluetooth do PC ligado) confirma com a biometria. O Windows Hello do próprio computador também é aceito.
3. `POST /api/auth/login/biometria` com a resposta do navegador. Assinatura válida → sessão aberta (id de sessão trocado) e `registro_autenticacao` com `metodo = SENHA_BIOMETRIA`. Inválida ou expirada (2 min) → 401 e `FALHA_BIOMETRIA`; o desafio vale uma tentativa só, depois é preciso digitar a senha de novo.

Quem não tem aparelho cadastrado continua entrando só com senha. O cadastro fica na aba **Segurança** da Central.

Persistência: `credencial_biometrica` (migration `V2`) guarda `credential_id`, `user_handle` aleatório, chave pública COSE e contador de assinaturas, tudo em base64url. Verificação feita pela biblioteca `com.yubico:webauthn-server-core`, exigindo `userVerification = required`.

Configuração (`application.yml`): `APP_WEBAUTHN_RP_ID` (padrão `localhost`, vale para qualquer porta local), `APP_WEBAUTHN_ORIGENS` (padrão = `APP_CORS_ORIGIN`) e `APP_WEBAUTHN_PREFERIR_CELULAR` (padrão `true`: o navegador já abre o QR code).

Limites conhecidos: o WebAuthn garante que o usuário foi verificado no aparelho, mas o celular pode aceitar o PIN de desbloqueio no lugar da digital; e fora de `localhost` a página precisa estar em HTTPS.

Teste automatizado: `backend/src/test/.../LoginBiometriaTest.java` (`mvn test`).

## Contas locais e redefinição

Na primeira execução, o bootstrap cria quatro contas locais de demonstração. A senha é lida de `APP_BOOTSTRAP_PASSWORD` somente na criação. Para mudar a senha dessas contas em ambiente local, use `APP_RESET_BOOTSTRAP_PASSWORDS=true` por uma única inicialização e volte a variável para `false`; assim não há redefinição recorrente nem alteração de outros usuários.
