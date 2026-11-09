# Entrega semanal — Marcus Querol

- **Semana / sprint:** Marco 2 (2026-11-09)
- **Issue:** #81 — Auditoria do 2FA + roteiro de demo atualizado
- **Branch:** `marcus` → PR para `desenvolvimento`
- **Trilha:** Segurança / auditoria — RF18, RNF03, RNF05, RNF08

## 1. O que foi feito

- **RF18 (2FA por celular) implementado** a partir da spec do Álvaro (issue #77): depois da senha o login fica
  pendente; a sessão só abre quando o celular aprova a solicitação **ou** quando o código de 6 dígitos é digitado.
  Código de uso único, válido por 3 min, bloqueio após 5 erros.
- **Auditoria do 2FA (issue #81):** cada etapa (envio, aprovação, recusa, código certo/errado, bloqueio,
  expiração) vira registro na trilha de autenticação, encadeada por hash SHA-256, sem o código.
- **Celular simulado** (`/celular` no front) para a demonstração, atrás da interface `CanalCelular`.
- **Tela do Administrador:** aba Segurança mostra a trilha com o selo de cadeia íntegra.
- A digital (passkey) continua como confirmação das **ações críticas** (trabalho de 26/10); RF18 cobre o **login**.
- Docs: `docs/marco2/02-2fa-celular-e-auditoria.md` (como a spec do Álvaro e o código se encaixam) e roteiro do
  Marco 2 no `docs/marcus/GUIA-APRESENTACAO-PROFESSOR.md`.

## 2. Como foi feito

- Backend: `SegundoFatorService`, `CanalCelular`/`CelularSimulado`, `AuditoriaAutenticacao`, endpoints
  `/api/auth/segundo-fator/{codigo,aprovacao}`, `/api/celular-simulado/*` e `/api/admin/auditoria/autenticacao`;
  migration `V4`.
- Frontend: `LoginPage` com a etapa do celular, `CelularSimuladoPage`, `TrilhaAutenticacao` na aba Segurança.
- Arquitetura híbrida: a regra fica no serviço, o canal do celular é adaptador e o controller só traduz HTTP.

## 3. Como será aplicado

- Demo: entrar com senha → tela "Confirme no seu celular" → abrir `/celular` → aprovar (ou digitar o código) →
  entra na Central → aba Segurança mostra a trilha com cada etapa e o selo "CADEIA ÍNTEGRA".

## 4. O que foi entregue (DoD)

- [x] `mvn test`: 23 testes passando (`SegundoFatorLoginTest` com 6 casos novos + `LoginBiometriaTest`)
- [x] `npm run build` sem erro
- [x] Fluxo testado no navegador: senha → celular simulado aprova → sessão aberta → trilha íntegra com 6 registros
- [ ] Revisão do grupo e merge na `desenvolvimento`
- [ ] Integrar com o Observer V1/V2 (PRs #82/#83) quando forem mergeados

## 5. Bloqueios

- O celular é simulado; envio real (push/SMS) exige provedor externo.
- A `marcus` local estava 7 commits à frente do GitHub (trabalho de 26/10 sem push).

## 6. Próxima semana

- Alinhar com o Álvaro a atualização da situação do RF18 de "pendente" para "implementado" no RF-RNF.
- Usar a `ConfirmacaoBiometrica` no protocolo de contingência (RF09/RF10) quando ele existir.
