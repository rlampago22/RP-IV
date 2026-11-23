-- Issue #96 (Marco 2, 16/11): o cadastro dos reatores sai do código e passa a morar no banco.
-- Antes: List.of("R-01".."R-05") em ApiEstadoMain, os 4 sensores RF-1 em registrarSensoresRf1()
-- e os pontos de operação calculados dentro do SimuladorReatores. Só a GERAÇÃO do valor do sensor
-- continua simulada; o que é cada reator, quais sensores ele tem, as faixas e o setpoint vêm daqui.
--
-- Mesmo schema "nucleo" e mesma convenção de V6/V7: o núcleo (mvp/, CadastroReatores) também cria
-- estas tabelas sozinho (CREATE ... IF NOT EXISTS) quando sobe antes do backend. Por isso a carga
-- inicial abaixo é "insere se não existir" linha a linha — se o núcleo já semeou, nada é duplicado
-- e a migração não falha por chave repetida. (WHERE NOT EXISTS em vez de ON CONFLICT para rodar
-- igual no Postgres e no H2.)
CREATE SCHEMA IF NOT EXISTS nucleo;

CREATE TABLE IF NOT EXISTS nucleo.reator (
  id VARCHAR(10) PRIMARY KEY,
  nome VARCHAR(60) NOT NULL,
  ordem INTEGER NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Um sensor pertence a um reator; o id do sensor (1 a 4) se repete entre reatores, por isso a
-- chave é o par. setpoint = ponto de operação em torno do qual o simulador gera as leituras.
CREATE TABLE IF NOT EXISTS nucleo.sensor_reator (
  reator_id VARCHAR(10) NOT NULL REFERENCES nucleo.reator (id),
  sensor_id BIGINT NOT NULL,
  tipo VARCHAR(40) NOT NULL,
  unidade VARCHAR(20) NOT NULL,
  limite_minimo DOUBLE PRECISION NOT NULL,
  limite_maximo DOUBLE PRECISION NOT NULL,
  atencao_minima DOUBLE PRECISION NOT NULL,
  atencao_maxima DOUBLE PRECISION NOT NULL,
  setpoint DOUBLE PRECISION NOT NULL,
  PRIMARY KEY (reator_id, sensor_id)
);

-- Carga inicial: exatamente os valores que estavam no código, para o comportamento não mudar.
-- Faixas iguais nos 5 reatores (RF-1); o setpoint varia um pouco por reator para os gráficos não
-- saírem idênticos (antes: deslocamento (índice - 2) * 0,5 no SimuladorReatores).
INSERT INTO nucleo.reator (id, nome, ordem, ativo)
  SELECT 'R-01', 'Reator-01', 1, TRUE WHERE NOT EXISTS (SELECT 1 FROM nucleo.reator WHERE id = 'R-01');
INSERT INTO nucleo.reator (id, nome, ordem, ativo)
  SELECT 'R-02', 'Reator-02', 2, TRUE WHERE NOT EXISTS (SELECT 1 FROM nucleo.reator WHERE id = 'R-02');
INSERT INTO nucleo.reator (id, nome, ordem, ativo)
  SELECT 'R-03', 'Reator-03', 3, TRUE WHERE NOT EXISTS (SELECT 1 FROM nucleo.reator WHERE id = 'R-03');
INSERT INTO nucleo.reator (id, nome, ordem, ativo)
  SELECT 'R-04', 'Reator-04', 4, TRUE WHERE NOT EXISTS (SELECT 1 FROM nucleo.reator WHERE id = 'R-04');
INSERT INTO nucleo.reator (id, nome, ordem, ativo)
  SELECT 'R-05', 'Reator-05', 5, TRUE WHERE NOT EXISTS (SELECT 1 FROM nucleo.reator WHERE id = 'R-05');

INSERT INTO nucleo.sensor_reator
  (reator_id, sensor_id, tipo, unidade, limite_minimo, limite_maximo, atencao_minima, atencao_maxima, setpoint)
  SELECT r.id, s.sensor_id, s.tipo, s.unidade, s.limite_minimo, s.limite_maximo, s.atencao_minima, s.atencao_maxima,
         s.setpoint_base + (r.ordem - 3) * s.passo_setpoint
  FROM nucleo.reator r
  CROSS JOIN (
    SELECT 1 AS sensor_id, 'TEMPERATURA' AS tipo, 'Celsius' AS unidade, 0.0 AS limite_minimo, 350.0 AS limite_maximo,
           20.0 AS atencao_minima, 325.0 AS atencao_maxima, 310.0 AS setpoint_base, 1.5 AS passo_setpoint
    UNION ALL SELECT 2, 'PRESSAO', 'bar', 0.0, 160.0, 10.0, 155.0, 150.0, 0.5
    UNION ALL SELECT 3, 'RADIACAO', 'mSv/h', 0.0, 5.0, 0.1, 4.2, 2.4, 0.075
    UNION ALL SELECT 4, 'FLUXO_RESFRIAMENTO', 'm3/h', 500.0, 1500.0, 600.0, 1400.0, 1100.0, 10.0
  ) s
  WHERE r.id IN ('R-01', 'R-02', 'R-03', 'R-04', 'R-05')
    AND NOT EXISTS (
      SELECT 1 FROM nucleo.sensor_reator x WHERE x.reator_id = r.id AND x.sensor_id = s.sensor_id
    );
