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

Marco 2. Base documental (PDF da APS): RNF de **Segurança** pede "autenticação multifator" (Must Have); o **UC02** tem como pré-condição o "funcionário **previamente cadastrado** com dados biométricos"; o item **4.2.7** coloca o **Administrador do Sistema** como responsável pela autenticação biométrica. Implementado com o padrão **WebAuthn / passkey**: o celular confirma com digital ou rosto e assina um desafio; o servidor só confere a assinatura com a chave pública cadastrada. A biometria nunca sai do aparelho (relevante pela LGPD, que trata biometria como dado sensível).

### Regra

- **Ninguém entra na Central só com a senha.** Senha certa leva sempre a uma segunda etapa.
- **O funcionário não se cadastra sozinho.** O cadastro do celular exige um **código de ativação** emitido pelo Administrador do Sistema: uso único, válido por 24 h, guardado só como hash (BCrypt); gerar outro invalida o anterior.
- **Celular perdido:** o Administrador revoga a biometria do funcionário e gera um código novo. O próprio funcionário não pode remover o seu único aparelho (só adicionar um reserva).
- **Primeiro Administrador:** enquanto nenhum Administrador tiver biometria, cada inicialização do backend emite um código para ele e o escreve no log do servidor (`[PRIMEIRO ACESSO] ... Codigo de ativacao de admin@usina.local: XXXX-XXXX`). É a ativação feita na instalação do sistema.

### Fluxo

| Situação após a senha | Resposta de `POST /api/auth/login` | Próximo passo |
|---|---|---|
| Tem celular cadastrado | `{"etapa":"BIOMETRIA","opcoes":{...}}` | Confirmar no celular → `POST /api/auth/login/biometria` |
| Não tem celular | `{"etapa":"PRIMEIRO_ACESSO"}` | Código → `POST /api/auth/primeiro-acesso/opcoes`; cadastro → `POST /api/auth/primeiro-acesso/concluir` |

- Confirmar no celular: o desafio vale 2 minutos e **uma tentativa**; errou ou expirou, volta para a senha (`FALHA_BIOMETRIA`).
- Primeiro acesso: 10 minutos e até **5 códigos errados** (`CODIGO_INVALIDO`); depois volta para a senha. O código só é consumido se a biometria for gravada (mesma transação).
- Sessão aberta → id de sessão trocado (evita fixação de sessão).

### Endpoints

| Método e rota | Uso | Quem |
|---|---|---|
| `POST /api/auth/login/biometria` | Assinatura do celular (resposta do `navigator.credentials.get`) | Etapa BIOMETRIA pendente |
| `POST /api/auth/primeiro-acesso/opcoes` | `{codigo}` → desafio de cadastro do celular | Etapa PRIMEIRO_ACESSO pendente |
| `POST /api/auth/primeiro-acesso/concluir` | `{apelido, credencial}` → grava a biometria e abre a sessão | Etapa PRIMEIRO_ACESSO pendente |
| `GET /api/auth/biometria` | Aparelhos do usuário | Autenticado |
| `POST /api/auth/biometria/cadastro/opcoes` e `/cadastro` | Adicionar aparelho reserva | Autenticado |
| `POST /api/auth/biometria/{id}/remover` | Remover aparelho (nunca o único: 409) | Autenticado |
| `GET /api/admin/usuarios` | Funcionários, aparelhos e código vigente | Administrador |
| `POST /api/admin/usuarios/{id}/codigo-ativacao` | Emite código (o texto só aparece nesta resposta) | Administrador |
| `POST /api/admin/usuarios/{id}/biometria/revogar` | Revoga todos os aparelhos do funcionário | Administrador |

### Trilha (`registro_autenticacao`)

`resultado` × `metodo`: `SUCESSO`/`SENHA_BIOMETRIA` (login normal), `SUCESSO`/`PRIMEIRO_ACESSO`, `BIOMETRIA_CADASTRADA`, `FALHA_BIOMETRIA`, `CODIGO_INVALIDO`, `CODIGO_EMITIDO`/`ADMINISTRADOR` e `BIOMETRIA_REVOGADA`/`ADMINISTRADOR` (nesses dois, `email_informado` é o Administrador que agiu e `id_usuario` é o funcionário).

### Persistência e configuração

- `credencial_biometrica` (migration `V2`): `credential_id`, `user_handle` aleatório, chave pública COSE e contador de assinaturas, em base64url. Verificação pela biblioteca `com.yubico:webauthn-server-core`, exigindo `userVerification = required`.
- `codigo_ativacao` (migration `V3`): hash, quem emitiu (nulo = instalação), validade, situação (`ATIVO`, `USADO`, `SUBSTITUIDO`).
- `application.yml`: `APP_BIOMETRIA_OBRIGATORIA` (padrão `true`; `false` só para desenvolver sem celular — a senha volta a bastar), `APP_WEBAUTHN_RP_ID` (padrão `localhost`, vale para qualquer porta local), `APP_WEBAUTHN_ORIGENS` (padrão = `APP_CORS_ORIGIN`) e `APP_WEBAUTHN_PREFERIR_CELULAR` (padrão `true`: o navegador já abre o QR code).

### Roteiro de demonstração

1. Ligar o backend e copiar do log o código do Administrador (`[PRIMEIRO ACESSO]`).
2. Entrar como `admin@usina.local` → "Cadastre seu celular" → código + digital → Central.
3. Aba **Segurança** → "Gerar código" para o Operador → anotar o código.
4. Sair → entrar como `operador@usina.local` → código + digital → Central.
5. Sair → entrar de novo → "Confirme no celular" → digital → Central.
6. Celular perdido: Administrador → "Revogar biometria" do Operador → o Operador volta ao primeiro acesso.

Plano B sem Bluetooth: na janela do navegador, escolher o Windows Hello do notebook (mesmo fluxo).

### Limites conhecidos

O WebAuthn garante que o usuário foi verificado no aparelho, mas o celular pode aceitar o PIN de desbloqueio no lugar da digital. Fora de `localhost`, a página precisa estar em HTTPS. O código do primeiro Administrador aparece no log do servidor, que portanto deve ter acesso restrito.

Testes: `backend/src/test/.../LoginBiometriaTest.java` (`mvn test`, 12 casos).

## Contas locais e redefinição

Na primeira execução, o bootstrap cria quatro contas locais de demonstração. A senha é lida de `APP_BOOTSTRAP_PASSWORD` somente na criação. Para mudar a senha dessas contas em ambiente local, use `APP_RESET_BOOTSTRAP_PASSWORDS=true` por uma única inicialização e volte a variável para `false`; assim não há redefinição recorrente nem alteração de outros usuários.
