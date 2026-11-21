-- Issue #99 (Marco 2, 16/11): a trilha de auditoria do núcleo (mvp/, RegistroAuditoria) sai do
-- arquivo mvp/dados/auditoria.log e passa a morar no banco. A cadeia SHA-256 continua a mesma:
-- hash = SHA-256(sequencia|ocorrido_em|tipo_evento|resumo|hash_anterior); alterar ou apagar uma linha
-- é detectado em GET :8080/api/auditoria/integridade.
--
-- Schema próprio ("nucleo") porque o núcleo também cria esta tabela sozinho quando sobe antes do
-- backend (CREATE ... IF NOT EXISTS, igual a este arquivo). Se ela ficasse no schema padrão, um banco
-- novo criado pelo núcleo deixaria de estar "vazio" para o Flyway e o backend não migraria.
--
-- ocorrido_em é texto ISO-8601 (Instant.toString), exatamente o que entra no hash: um TIMESTAMP
-- arredondaria os nanossegundos (o Postgres guarda microssegundos) e quebraria o recálculo.
CREATE SCHEMA IF NOT EXISTS nucleo;

CREATE TABLE IF NOT EXISTS nucleo.auditoria_evento (
  sequencia BIGINT PRIMARY KEY,
  ocorrido_em VARCHAR(40) NOT NULL,
  tipo_evento VARCHAR(80) NOT NULL,
  resumo VARCHAR(4000) NOT NULL,
  hash_anterior VARCHAR(64) NOT NULL,
  hash VARCHAR(64) NOT NULL,
  registrado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Linha única travada com SELECT ... FOR UPDATE antes de cada inserção: os escritores (API do
-- núcleo, UI Swing) entram um de cada vez, mesmo em processos diferentes, e a cadeia não bifurca.
CREATE TABLE IF NOT EXISTS nucleo.auditoria_trava (
  id INT PRIMARY KEY
);

INSERT INTO nucleo.auditoria_trava (id)
SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM nucleo.auditoria_trava WHERE id = 1);
