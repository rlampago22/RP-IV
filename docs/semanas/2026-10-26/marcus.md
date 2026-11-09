# Entrega semanal — Marcus Querol

- **Semana / sprint:** Marco 2 (2026-10-26)
- **Branch:** `marcus` → PR [#69](https://github.com/rlampago22/RP-IV/pull/69)
- **Trilha:** Segurança — RNF08, digital como segundo fator das ações críticas

## 1. O que foi feito

- `marcus` atualizada com a `desenvolvimento` (login web, histórico, visual v2); conflitos resolvidos mantendo as correções da demo do Marco 1 (integridade da auditoria no T04, normalizar sensor ao resolver alarme, pressão sem alarme falso).
- Backend (`backend/`): digital por **passkey / WebAuthn** como segundo fator das **ações críticas**. O login continua com senha.
  - Ações críticas hoje: gerar código de ativação, revogar digital e remover a própria digital. Ativar protocolo de contingência (RF09/RF10) deve usar o mesmo mecanismo quando for implementado.
  - Cadastro da digital só com **código de ativação** emitido pelo Administrador (uso único, 24 h, só o hash no banco).
  - Primeiro Administrador ativado pelo código escrito no log na instalação.
  - Migrations `V2` (`credencial_biometrica`, coluna `metodo` em `registro_autenticacao`) e `V3` (`codigo_ativacao`).
  - Troca do id de sessão no login (evita fixação de sessão).
- Frontend: aba **Segurança** (minha digital + painel do Administrador) e `executarAcaoCritica`, que pede a digital quando o backend exige.
- Docs: regra, endpoints, trilha e roteiro de demonstração em `docs/backend-autenticacao.md`; item incluído no Should da MoSCoW.

## 2. Como foi feito

- Base no PDF da APS: RNF de Segurança ("autenticação multifator", Must), 4.2.10 ("dois fatores para evitar ativação acidental" da contingência), UC02 (funcionário "previamente cadastrado") e 4.2.7 (gestão pelo Administrador). A biometria do UC02 é a do acesso físico às áreas restritas; por isso o login web não pede digital.
- Duas versões anteriores foram descartadas: digital opcional com cadastro livre (permitia cadastrar o próprio leitor só com a senha) e digital obrigatória em todo login (QR code a cada entrada, mais rígido que o documento e ruim para o grupo).
- Ação crítica sem confirmação recente → resposta 428; o front pede a digital, confirma e repete. A confirmação vale 5 minutos (como o "modo sudo" de bancos e do GitHub).
- Bibliotecas: `com.yubico:webauthn-server-core` 2.9.0 e `@simplewebauthn/browser` 13.3.0. O servidor guarda só a chave pública (LGPD: biometria é dado sensível). Leitor padrão: o do computador (Windows Hello); celular opcional.

## 3. Como será aplicado

- Demo: código do Administrador no terminal → admin cadastra a digital → "Gerar código" pede a digital e entrega o código do Operador → Operador entra só com senha e cadastra a digital → Administrador revoga (leitor perdido).

## 4. O que foi entregue (DoD)

- [x] `mvn test`: `LoginBiometriaTest` com 16 casos (login só com senha, ação crítica pede digital, janela vencida, só Administrador gera código, sem digital cadastrada é orientado, assinatura forjada não libera, código certo/errado/de outro funcionário/substituído, bloqueio após 5 erros, remover digital é ação crítica, política desligada para desenvolvimento)
- [x] Teste ponta a ponta no Chrome com autenticador virtual: os 9 passos do roteiro
- [x] Suítes do MVP e `vite build` sem erro após o merge da `desenvolvimento`
- [ ] Teste com leitor real (Windows Hello e celular)
- [ ] Revisão do grupo e merge do PR #69

## 5. Bloqueios

- O protocolo de contingência (RF09/RF10) ainda não existe na interface; quando existir, precisa chamar a confirmação da digital (ver `docs/backend-autenticacao.md`).
- O WebAuthn confirma que o usuário foi verificado no aparelho, mas o leitor pode aceitar PIN no lugar da digital.

## 6. Próxima semana

- Testar com Windows Hello e com o celular e gravar evidência da demo.
- Combinar com quem fizer RF09/RF10 o uso de `ConfirmacaoBiometrica` na ativação do protocolo.
- Avaliar usar a mesma digital no acesso físico simulado (UC04 do `mvp/`).
