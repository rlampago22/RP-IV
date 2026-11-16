-- RF18 (2FA por celular) + RNF03/RNF05: a trilha de autenticação passa a registrar cada etapa do
-- segundo fator e fica encadeada por hash (SHA-256), como o AuditoriaLogs do núcleo. Alterar ou apagar
-- uma linha quebra a cadeia e é detectado em /api/admin/auditoria/autenticacao.
ALTER TABLE registro_autenticacao ALTER COLUMN resultado SET DATA TYPE VARCHAR(40);
ALTER TABLE registro_autenticacao ALTER COLUMN metodo SET DATA TYPE VARCHAR(40);
ALTER TABLE registro_autenticacao ADD COLUMN hash_anterior VARCHAR(64);
ALTER TABLE registro_autenticacao ADD COLUMN hash VARCHAR(64);
