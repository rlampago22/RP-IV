# 02 — 2FA por celular (RF18) implementado + auditoria do 2FA

**Issue:** #81 (Marcus — auditoria do 2FA + roteiro do Marco 2)
**Spec:** RF18 / UC05, escrita pelo Álvaro (issue #77) em `docs/marco1/01-requisitos-rf-rnf.md` (branch `alvaro`).
**Branch:** `marcus`

## 1. Como as duas partes se encaixam

O Álvaro e o Marcus trabalharam em paralelo no mesmo tema (segundo fator). Não é retrabalho:
cada parte cobre um momento diferente.

| Momento | O que é pedido | Quem especificou | Implementação |
|---|---|---|---|
| **Entrar na Central (login)** | Senha + confirmação no celular **ou** código de 6 dígitos | Álvaro (RF18/UC05, pedido do professor em 04/11) | `SegundoFatorService`, `AutenticacaoController` (este documento) |
| **Ações críticas** (gerar código de ativação, revogar digital, futura contingência RF09/RF10) | Confirmar com a digital (passkey/WebAuthn) | Marcus (RNF08, item 4.2.10 do PDF da APS) | `ConfirmacaoBiometrica`, `BiometriaController` (semana de 26/10) |
| **Auditoria de tudo isso** | Toda tentativa registrada, sucesso e falha, sem dados sensíveis | Issue #81 (RNF03/RNF05) | `AuditoriaAutenticacao` (trilha com hash encadeado) |

## 2. Fluxo do RF18 no código

```text
POST /api/auth/login (e-mail + senha)
  ├─ senha errada  → 401 + registro FALHA
  └─ senha certa   → 202 {segundoFator: true}   (NENHUMA sessão ainda)
        registro SENHA_OK_AGUARDANDO_2FA
        SegundoFatorService.iniciar → CanalCelular.enviar → registro 2FA_SOLICITACAO_ENVIADA

Celular (tela /celular, simulada):  Aprovar | Recusar   → 2FA_APROVADO_NO_CELULAR | 2FA_RECUSADO_NO_CELULAR

Central:
  POST /api/auth/segundo-fator/aprovacao   → 200 + sessão     (2FA_SUCESSO_APROVACAO)  ou 401 (2FA_NEGADO_RECUSADO)
  POST /api/auth/segundo-fator/codigo      → 200 + sessão     (2FA_SUCESSO_CODIGO)     ou 401 (2FA_CODIGO_INVALIDO)
                                             5 erros → 2FA_BLOQUEADO_TENTATIVAS; após 3 min → 2FA_EXPIRADO
```

- O celular é um **adaptador** (`CanalCelular`). No MVP a implementação é o `CelularSimulado`, mostrado
  na página `/celular` do front. Trocar por push/SMS real é escrever outra implementação da interface,
  sem mexer na regra (camadas do backend — arquitetura híbrida, `docs/marco2/01-arquitetura-hibrida.md`).
- A solicitação fica presa à sessão HTTP da tentativa (atributo `2fa.desafio`); depois da confirmação o
  id de sessão é trocado (evita fixação de sessão).
- Configuração: `app.segundo-fator.login` (padrão `true`) e `app.segundo-fator.celular-simulado`
  (padrão `true`; desligar quando houver canal real, porque a tela `/celular` não tem login).

## 3. Critérios de aceite do RF18 → evidência

| Critério (spec do Álvaro) | Evidência |
|---|---|
| Com senha válida, a Central fica pendente até o celular confirmar | `SegundoFatorLoginTest.senhaCertaNaoAbreSessaoSemOSegundoFator` |
| Confirmação no celular ou código válido libera **uma única** sessão | `codigoCertoDoCelularAbreASessao`, `aprovarNoCelularAbreASessaoUmaUnicaVez` |
| Rejeição, código inválido, reutilizado ou expirado não liberam sessão | `recusarNoCelularNegaOAcesso`, `codigoErradoNaoAbreSessaoEBloqueiaDepoisDeCincoErros` |
| Código vinculado à tentativa e sem reuso | atributo de sessão + `Status.CONCLUIDO` remove o desafio |
| Resultado auditável sem código/telefone | `cadaEtapaDo2faVaiParaATrilhaEncadeadaSemOCodigo` |

## 4. Auditoria do 2FA (issue #81)

- Toda gravação em `registro_autenticacao` passa por `AuditoriaAutenticacao.save`, que liga o registro ao
  anterior: `hash = SHA-256(hashAnterior | e-mail | resultado | método | instante)`, igual à ideia do
  `AuditoriaLogs` do núcleo (`mvp/`). Migration `V4__auditoria_encadeada_e_segundo_fator.sql`.
- O registro guarda só e-mail, método (`SENHA`, `CELULAR`, `ACAO_CRITICA`...) e resultado. **Nunca** o código.
- O Administrador vê a trilha na aba **Segurança → Trilha de autenticação** (`GET /api/admin/auditoria/autenticacao`),
  com o selo **CADEIA ÍNTEGRA** ou o id do primeiro registro adulterado.
- Também passaram a ser encadeados os registros que já existiam (login com senha, digital nas ações críticas,
  cadastro/remoção de digital).

## 5. Limites conhecidos

- O celular é simulado (não há envio real de push/SMS). A tela `/celular` é só para demonstração.
- As solicitações pendentes ficam em memória: reiniciar o backend descarta tentativas em andamento (o usuário só entra de novo).
- Registros anteriores à V4 não têm hash; a verificação começa no primeiro registro encadeado.
