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
| `/api/auth/biometria/**` e `/api/admin/usuarios/**` | Digital como 2º fator das ações críticas (ver abaixo) | Autenticado; administração só Administrador |

O frontend envia o cookie de sessão com `credentials: 'include'` e não armazena senha ou token no navegador.

## Segundo fator: digital nas ações críticas (RNF08)

Marco 2. O que o PDF da APS pede e como foi atendido:

| No documento | O que diz | No sistema |
|---|---|---|
| RNF de **Segurança** (Must) | "autenticação multifator" | Senha (1º fator) + digital (2º fator) |
| **4.2.10** Ativar Protocolos de Contingência | "requer autenticação de **dois fatores** para evitar ativação acidental" | A digital é pedida nas **ações críticas**, não em todo login |
| **UC02** Controle de Acesso / **1.3** | biometria e crachá para **entrar nas áreas restritas**; funcionário "**previamente cadastrado**" | Cadastro da digital só com **código de ativação** do Administrador |
| **4.2.7** | Administrador e Segurança Patrimonial gerenciam a autenticação | Painel do Administrador: gerar código e revogar digital |

A biometria do UC02 é a da **porta** (acesso físico, nosso UC04 no `mvp/`); o login web não exige digital. Padrão usado: **WebAuthn / passkey**. O leitor pode ser o do próprio computador (Windows Hello: encostar o dedo, sem QR code) ou o celular. A digital nunca sai do aparelho: o servidor só confere a assinatura com a chave pública cadastrada (relevante pela LGPD, que trata biometria como dado sensível).

### Regra

- **Login da Central:** e-mail e senha, como antes.
- **Ações críticas** exigem a digital: hoje, gerar código de ativação, revogar digital e remover a própria digital; quando existir, **ativar protocolo de contingência** (RF09/RF10). Confirmada a digital, a sessão fica liberada para ações críticas por **5 minutos**.
- **Cadastro da digital:** só com o código de ativação emitido pelo Administrador (uso único, 24 h, guardado só como hash BCrypt; gerar outro invalida o anterior; 5 códigos errados bloqueiam o código vigente).
- **Leitor perdido:** o Administrador revoga a digital e gera um código novo.
- **Primeiro Administrador:** enquanto nenhum Administrador tiver digital, cada inicialização do backend emite um código para ele e o escreve no log (`[PRIMEIRO ACESSO] ... Codigo de ativacao de admin@usina.local: XXXX-XXXX`). É a ativação na instalação do sistema.

### Como uma ação crítica exige a digital

Backend: chamar `confirmacao.exigir(request)` (bean `ConfirmacaoBiometrica`) na primeira linha do endpoint. Sem confirmação recente, a resposta é **428** com `{"exigeBiometria": true}`.

Frontend: envolver a chamada em `executarAcaoCritica(() => api.minhaAcao())` (`frontend/src/services/acaoCritica.js`). Ao receber 428, ele pede a digital, confirma e repete a ação.

O protocolo de contingência, quando for implementado no backend, deve seguir esse padrão. Se ficar no `mvp/` (API 8080, sem login), ele precisa antes passar a validar a sessão do backend.

### Endpoints

| Método e rota | Uso | Quem |
|---|---|---|
| `GET /api/auth/biometria` | Digitais do usuário | Autenticado |
| `POST /api/auth/biometria/cadastro/opcoes` | `{codigo}` → desafio para o leitor | Autenticado |
| `POST /api/auth/biometria/cadastro` | `{apelido, credencial}` → grava a digital e consome o código | Autenticado |
| `POST /api/auth/biometria/confirmacao/opcoes` | Desafio da confirmação (2 min, uma tentativa); 409 se não tem digital | Autenticado |
| `POST /api/auth/biometria/confirmacao` | Assinatura do leitor → libera ações críticas por 5 min | Autenticado |
| `POST /api/auth/biometria/{id}/remover` | Remove uma digital própria (ação crítica) | Autenticado |
| `GET /api/admin/usuarios` | Funcionários, digital e código vigente | Administrador |
| `POST /api/admin/usuarios/{id}/codigo-ativacao` | Emite código (ação crítica; o texto só aparece nesta resposta) | Administrador |
| `POST /api/admin/usuarios/{id}/biometria/revogar` | Revoga as digitais do funcionário (ação crítica) | Administrador |

### Trilha (`registro_autenticacao`)

`resultado` / `metodo`: `SUCESSO`/`SENHA` (login), `BIOMETRIA_CONFIRMADA`/`ACAO_CRITICA`, `FALHA_BIOMETRIA`, `BIOMETRIA_CADASTRADA`/`CODIGO_ATIVACAO`, `CODIGO_INVALIDO`, `CODIGO_EMITIDO`/`ADMINISTRADOR` e `BIOMETRIA_REVOGADA`/`ADMINISTRADOR` (nesses dois, `email_informado` é o Administrador que agiu e `id_usuario` é o funcionário).

### Persistência e configuração

- `credencial_biometrica` (migration `V2`): `credential_id`, `user_handle` aleatório, chave pública COSE e contador de assinaturas, em base64url. Verificação pela biblioteca `com.yubico:webauthn-server-core`, exigindo `userVerification = required`.
- `codigo_ativacao` (migration `V3`): hash, quem emitiu (nulo = instalação), validade, situação (`ATIVO`, `USADO`, `SUBSTITUIDO`).
- `application.yml`: `APP_BIOMETRIA_ACOES_CRITICAS` (padrão `true`; `false` só para desenvolver sem leitor de digital), `APP_WEBAUTHN_PREFERIR_CELULAR` (padrão `false`: o navegador oferece primeiro o leitor do computador; `true` abre direto o QR code do celular), `APP_WEBAUTHN_RP_ID` (padrão `localhost`) e `APP_WEBAUTHN_ORIGENS` (padrão = `APP_CORS_ORIGIN`).

### Roteiro de demonstração

1. Ligar o backend e copiar do terminal "Login (backend 8081)" o código da linha `[PRIMEIRO ACESSO]`.
2. Entrar como `admin@usina.local` (só senha) → aba **Segurança** → colar o código → **Cadastrar digital** → encostar o dedo.
3. "Gerar código" na linha do Operador → o navegador pede a digital → o código aparece. Gerar outro em seguida não pede de novo (janela de 5 min).
4. Sair → entrar como `operador@usina.local` (só senha) → Segurança → código → **Cadastrar digital**.
5. Leitor perdido: Administrador → "Revogar digital" do Operador (pede a digital se a janela venceu).

### Limites conhecidos

O WebAuthn garante que o usuário foi verificado no aparelho, mas o leitor pode aceitar o PIN no lugar da digital (Windows Hello e celular permitem). Fora de `localhost`, a página precisa estar em HTTPS. O código do primeiro Administrador aparece no log do servidor, que deve ter acesso restrito. Uma aprovação por notificação no celular (como gov.br/Microsoft Authenticator) exigiria um aplicativo móvel e servidor público em HTTPS: fora do escopo do Marco 2.

Testes: `backend/src/test/.../LoginBiometriaTest.java` (`mvn test`, 16 casos).

## Contas locais e redefinição

Na primeira execução, o bootstrap cria quatro contas locais de demonstração. A senha é lida de `APP_BOOTSTRAP_PASSWORD` somente na criação. Para mudar a senha dessas contas em ambiente local, use `APP_RESET_BOOTSTRAP_PASSWORDS=true` por uma única inicialização e volte a variável para `false`; assim não há redefinição recorrente nem alteração de outros usuários.
