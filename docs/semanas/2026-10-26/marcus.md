# Entrega semanal — Marcus Querol

- **Semana / sprint:** Marco 2 (2026-10-26)
- **Branch:** `marcus` → PR [#69](https://github.com/rlampago22/RP-IV/pull/69)
- **Trilha:** Segurança — RNF08, login em dois fatores obrigatório com biometria do celular

## 1. O que foi feito

- `marcus` atualizada com a `desenvolvimento` (login web, histórico, visual v2); conflitos resolvidos mantendo as correções da demo do Marco 1 (integridade da auditoria no T04, normalizar sensor ao resolver alarme, pressão sem alarme falso).
- Backend (`backend/`): segundo fator **obrigatório** por **passkey / WebAuthn**. Senha certa nunca abre a sessão sozinha: quem tem celular cadastrado confirma com digital ou rosto; quem não tem passa pelo primeiro acesso com o **código de ativação** emitido pelo Administrador do Sistema.
  - Migrations `V2` (`credencial_biometrica`, coluna `metodo` em `registro_autenticacao`) e `V3` (`codigo_ativacao`, só o hash).
  - Endpoints de login em duas etapas, primeiro acesso, aparelhos do usuário e administração (`/api/admin/usuarios`: gerar código, revogar biometria).
  - Ativação do primeiro Administrador pelo código escrito no log na instalação.
  - Troca do id de sessão no login (evita fixação de sessão).
- Frontend: telas "Confirme no celular" e "Cadastre seu celular" (primeiro acesso) e aba **Segurança** com os aparelhos do usuário e o painel do Administrador.
- Docs: regra, fluxo, endpoints, trilha e roteiro de demonstração em `docs/backend-autenticacao.md`; item incluído no Should da MoSCoW.

## 2. Como foi feito

- Base no PDF da APS: RNF de Segurança ("autenticação multifator", Must), UC02 (funcionário "previamente cadastrado" com biometria), 4.2.7 (Administrador gerencia a autenticação biométrica) e 4.2.10 (dois fatores).
- Primeira versão deixava o 2º fator opcional e o cadastro livre depois do login; revista porque permitia entrar só com a senha e deixava quem tivesse a senha cadastrar o próprio celular.
- Biblioteca `com.yubico:webauthn-server-core` 2.9.0 no backend e `@simplewebauthn/browser` 13.3.0 no front. O servidor guarda só a chave pública; a biometria nunca sai do celular (LGPD: dado sensível).
- Limites: desafio de 2 min com uma tentativa; primeiro acesso de 10 min com até 5 códigos errados; código vale 24 h e uma vez. Tudo registrado em `registro_autenticacao`.

## 3. Como será aplicado

- Demo: log do backend mostra o código do Administrador → admin cadastra o celular → gera código para o Operador → Operador cadastra o celular → novo login pede senha + digital → Administrador revoga (celular perdido) e o Operador volta ao primeiro acesso. Plano B sem Bluetooth: Windows Hello do notebook.

## 4. O que foi entregue (DoD)

- [x] `mvn test`: `LoginBiometriaTest` com 12 casos (senha sozinha não abre sessão, primeiro acesso, código certo/errado/de outro funcionário/substituído, limite de 5 tentativas, assinatura forjada recusada, só o Administrador emite código, revogação, não remover o único celular, política desligada para desenvolvimento)
- [x] Teste ponta a ponta no Chrome com autenticador virtual: os 10 passos do roteiro de demonstração
- [x] Suítes do MVP e `vite build` sem erro após o merge da `desenvolvimento`
- [ ] Teste com celular real (QR code + Bluetooth)
- [ ] Revisão do grupo e merge do PR #69

## 5. Bloqueios

- Muda o login de todo o grupo: para desenvolver sem celular, usar `APP_BIOMETRIA_OBRIGATORIA=false` no `backend/.env`.
- O fluxo do celular (QR code) depende de Bluetooth ligado no PC e internet nos dois aparelhos.
- O WebAuthn confirma que o usuário foi verificado, mas o celular pode aceitar PIN no lugar da digital.

## 6. Próxima semana

- Testar com o celular real e gravar evidência da demo.
- Apresentar a regra ao grupo antes do merge e decidir se a biometria também entra no UC04 (acesso físico às áreas restritas).
