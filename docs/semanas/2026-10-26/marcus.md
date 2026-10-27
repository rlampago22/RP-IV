# Entrega semanal — Marcus Querol

- **Semana / sprint:** Marco 2 (2026-10-26)
- **Branch:** `marcus` → PR [#69](https://github.com/rlampago22/RP-IV/pull/69)
- **Trilha:** Segurança — RNF08, login em dois fatores com biometria do celular

## 1. O que foi feito

- `marcus` atualizada com a `desenvolvimento` (login web, histórico, visual v2); conflitos resolvidos mantendo as correções da demo do Marco 1 (integridade da auditoria no T04, normalizar sensor ao resolver alarme, pressão sem alarme falso).
- Backend (`backend/`): segundo fator por **passkey / WebAuthn**. Quem tem celular cadastrado digita a senha e confirma com digital ou rosto no celular; só então a sessão abre.
  - Migration `V2`: tabela `credencial_biometrica` e coluna `metodo` em `registro_autenticacao` (`SENHA` ou `SENHA_BIOMETRIA`).
  - Endpoints `POST /api/auth/login/biometria` e `/api/auth/biometria/**` (cadastrar, listar, remover).
  - Troca do id de sessão no login (evita fixação de sessão).
- Frontend: etapa "Confirme no celular" na tela de login e aba **Segurança** para cadastrar/remover o celular.
- Docs: contrato em `docs/backend-autenticacao.md`; item incluído no Should da MoSCoW.

## 2. Como foi feito

- Base no PDF da APS: RNF de Segurança ("autenticação multifator"), UC02 (biometria) e 4.2.10 (dois fatores).
- Biblioteca `com.yubico:webauthn-server-core` 2.9.0 no backend e `@simplewebauthn/browser` 13.3.0 no front. O servidor guarda só a chave pública; a biometria nunca sai do celular (LGPD: dado sensível).
- O desafio fica na sessão por 2 minutos e vale uma tentativa; falhas ficam em `registro_autenticacao` como `FALHA_BIOMETRIA`.

## 3. Como será aplicado

- Demo: aba Segurança → "Cadastrar celular" → QR code → digital no celular. Sair → login com senha → "Confirme no celular" → QR code → digital → Central aberta. Plano B sem Bluetooth: Windows Hello do notebook (mesmo código).

## 4. O que foi entregue (DoD)

- [x] `mvn test`: `LoginBiometriaTest` (5 casos: só senha, senha sem biometria não abre sessão, assinatura forjada recusada e desafio consumido, cadastro exige login, opções exigem verificação do usuário)
- [x] Teste ponta a ponta no Chrome com autenticador virtual: cadastro, login em duas etapas, biometria não verificada recusada, último uso registrado, remoção volta ao login só com senha
- [x] Suítes do MVP e `vite build` sem erro após o merge da `desenvolvimento`
- [ ] Teste com celular real (QR code + Bluetooth)
- [ ] Revisão do grupo e merge do PR #69

## 5. Bloqueios

- O fluxo do celular (QR code) depende de Bluetooth ligado no PC e internet nos dois aparelhos.
- O WebAuthn confirma que o usuário foi verificado, mas o celular pode aceitar PIN no lugar da digital.

## 6. Próxima semana

- Testar com o celular real e gravar evidência da demo.
- Alinhar com o grupo se o 2º fator vira obrigatório para algum papel (ex.: Administrador) e se entra no UC04 (acesso físico).
