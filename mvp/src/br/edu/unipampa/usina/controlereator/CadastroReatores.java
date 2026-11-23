package br.edu.unipampa.usina.controlereator;

import br.edu.unipampa.usina.persistencia.ConexaoBanco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cadastro dos reatores da usina e dos seus sensores (issue #96), nas tabelas
 * {@code nucleo.reator} e {@code nucleo.sensor_reator} do banco do sistema.
 *
 * <p>Substitui o que antes estava fixo no código: a lista {@code R-01..R-05} do
 * {@code ApiEstadoMain}, os 4 sensores RF-1 e os pontos de operação do {@code SimuladorReatores}.
 * Só a geração do valor do sensor continua simulada.
 *
 * <p>Mesmo padrão de {@code RegistroAuditoria} e {@code RegistroHistorico}: JDBC direto via
 * {@link ConexaoBanco}, tabelas criadas de forma idempotente (igual à migração
 * {@code backend/.../V8__criar_reator_nucleo.sql}) para o núcleo funcionar com um banco que o backend
 * ainda não migrou e com o H2 em memória dos testes. Se o cadastro estiver vazio, recebe a carga
 * inicial com os mesmos valores da V8.
 */
public final class CadastroReatores implements AutoCloseable {
    public static final String TABELA_REATOR = "nucleo.reator";
    public static final String TABELA_SENSOR = "nucleo.sensor_reator";

    private final ConexaoBanco banco;
    private Connection conexao;

    public CadastroReatores(ConexaoBanco banco) {
        this.banco = banco;
        garantirTabelas();
        semearSeVazio();
    }

    /** Cadastro no banco padrão do sistema (o mesmo do backend). */
    public static CadastroReatores abrirPadrao() {
        return new CadastroReatores(ConexaoBanco.padrao());
    }

    /** Um reator cadastrado, com os sensores e o ponto de operação de cada um (por id de sensor). */
    public record ReatorCadastrado(String id, String nome, int ordem, List<Sensor> sensores, Map<Long, Double> setpoints) {
        public ReatorCadastrado {
            sensores = List.copyOf(sensores);
            setpoints = Collections.unmodifiableMap(new LinkedHashMap<>(setpoints));
        }
    }

    /** Reatores ativos, na ordem cadastrada (o primeiro é o principal), cada um com os seus sensores. */
    public synchronized List<ReatorCadastrado> listarAtivos() {
        try {
            Connection c = conexao();
            List<String[]> reatores = new ArrayList<>();
            try (Statement sql = c.createStatement();
                 ResultSet linhas = sql.executeQuery(
                     "SELECT id, nome, ordem FROM " + TABELA_REATOR + " WHERE ativo = TRUE ORDER BY ordem, id")) {
                while (linhas.next()) {
                    reatores.add(new String[] {linhas.getString(1), linhas.getString(2), Integer.toString(linhas.getInt(3))});
                }
            }

            List<ReatorCadastrado> resultado = new ArrayList<>();
            try (PreparedStatement consulta = c.prepareStatement(
                "SELECT sensor_id, tipo, unidade, limite_minimo, limite_maximo, atencao_minima, atencao_maxima, setpoint "
                    + "FROM " + TABELA_SENSOR + " WHERE reator_id = ? ORDER BY sensor_id")) {
                for (String[] reator : reatores) {
                    List<Sensor> sensores = new ArrayList<>();
                    Map<Long, Double> setpoints = new LinkedHashMap<>();
                    consulta.setString(1, reator[0]);
                    try (ResultSet linhas = consulta.executeQuery()) {
                        while (linhas.next()) {
                            Sensor sensor = new Sensor(
                                linhas.getLong(1),
                                linhas.getString(2),
                                linhas.getString(3),
                                linhas.getDouble(4),
                                linhas.getDouble(5),
                                linhas.getDouble(6),
                                linhas.getDouble(7)
                            );
                            sensores.add(sensor);
                            setpoints.put(sensor.id(), linhas.getDouble(8));
                        }
                    }
                    resultado.add(new ReatorCadastrado(reator[0], reator[1], Integer.parseInt(reator[2]), sensores, setpoints));
                }
            }
            return resultado;
        } catch (SQLException error) {
            descartarConexao();
            throw new IllegalStateException(
                "Não foi possível ler o cadastro de reatores em " + banco.descricao() + ": " + error.getMessage(),
                error
            );
        }
    }

    /** Mesma estrutura da migração V8 do backend. */
    private synchronized void garantirTabelas() {
        try (Statement sql = conexao().createStatement()) {
            sql.execute("CREATE SCHEMA IF NOT EXISTS nucleo");
            sql.execute(
                "CREATE TABLE IF NOT EXISTS " + TABELA_REATOR + " ("
                    + "id VARCHAR(10) PRIMARY KEY, "
                    + "nome VARCHAR(60) NOT NULL, "
                    + "ordem INTEGER NOT NULL, "
                    + "ativo BOOLEAN NOT NULL DEFAULT TRUE)"
            );
            sql.execute(
                "CREATE TABLE IF NOT EXISTS " + TABELA_SENSOR + " ("
                    + "reator_id VARCHAR(10) NOT NULL REFERENCES " + TABELA_REATOR + " (id), "
                    + "sensor_id BIGINT NOT NULL, "
                    + "tipo VARCHAR(40) NOT NULL, "
                    + "unidade VARCHAR(20) NOT NULL, "
                    + "limite_minimo DOUBLE PRECISION NOT NULL, "
                    + "limite_maximo DOUBLE PRECISION NOT NULL, "
                    + "atencao_minima DOUBLE PRECISION NOT NULL, "
                    + "atencao_maxima DOUBLE PRECISION NOT NULL, "
                    + "setpoint DOUBLE PRECISION NOT NULL, "
                    + "PRIMARY KEY (reator_id, sensor_id))"
            );
        } catch (SQLException error) {
            descartarConexao();
            throw new IllegalStateException(
                "Não foi possível preparar o cadastro de reatores em " + banco.descricao() + ": " + error.getMessage(),
                error
            );
        }
    }

    /**
     * Carga inicial (mesmos valores da V8): 5 reatores, 4 sensores RF-1 com as mesmas faixas em todos,
     * setpoint deslocado um pouco por reator. Só roda com o cadastro vazio — reator desativado ou
     * alterado no banco não volta ao padrão.
     */
    private synchronized void semearSeVazio() {
        try {
            Connection c = conexao();
            try (Statement sql = c.createStatement();
                 ResultSet total = sql.executeQuery("SELECT COUNT(*) FROM " + TABELA_REATOR)) {
                total.next();
                if (total.getLong(1) > 0) {
                    return;
                }
            }
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement reator = c.prepareStatement(
                     "INSERT INTO " + TABELA_REATOR + " (id, nome, ordem, ativo) VALUES (?, ?, ?, TRUE)");
                 PreparedStatement sensor = c.prepareStatement(
                     "INSERT INTO " + TABELA_SENSOR + " (reator_id, sensor_id, tipo, unidade, limite_minimo, "
                         + "limite_maximo, atencao_minima, atencao_maxima, setpoint) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (int ordem = 1; ordem <= 5; ordem++) {
                    String id = String.format("R-%02d", ordem);
                    reator.setString(1, id);
                    reator.setString(2, String.format("Reator-%02d", ordem));
                    reator.setInt(3, ordem);
                    reator.executeUpdate();

                    int deslocamento = ordem - 3;
                    inserirSensor(sensor, id, new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0),
                        310.0 + deslocamento * 1.5);
                    inserirSensor(sensor, id, new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 155.0),
                        150.0 + deslocamento * 0.5);
                    inserirSensor(sensor, id, new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.2),
                        2.4 + deslocamento * 0.075);
                    inserirSensor(sensor, id, new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0),
                        1100.0 + deslocamento * 10.0);
                }
                c.commit();
            } catch (SQLException error) {
                c.rollback();
                throw error;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        } catch (SQLException error) {
            descartarConexao();
            throw new IllegalStateException(
                "Não foi possível gravar a carga inicial de reatores em " + banco.descricao() + ": " + error.getMessage(),
                error
            );
        }
    }

    private static void inserirSensor(PreparedStatement insert, String reatorId, Sensor sensor, double setpoint)
        throws SQLException {
        insert.setString(1, reatorId);
        insert.setLong(2, sensor.id());
        insert.setString(3, sensor.tipo());
        insert.setString(4, sensor.unidade());
        insert.setDouble(5, sensor.limiteMinimo());
        insert.setDouble(6, sensor.limiteMaximo());
        insert.setDouble(7, sensor.atencaoMinima());
        insert.setDouble(8, sensor.atencaoMaxima());
        // Arredonda o ruído de ponto flutuante (2.4 - 0.075 = 2.3249999999999997): o banco guarda
        // o mesmo 2.325 que a V8 grava pelo SQL, venha a carga de qual lado vier.
        insert.setDouble(9, Math.round(setpoint * 1_000_000d) / 1_000_000d);
        insert.executeUpdate();
    }

    @Override
    public synchronized void close() {
        descartarConexao();
    }

    private Connection conexao() throws SQLException {
        if (conexao == null || conexao.isClosed()) {
            conexao = banco.abrir();
        }
        return conexao;
    }

    /** Fecha uma conexão que deu erro; a próxima operação abre outra (ex.: banco reiniciado). */
    private void descartarConexao() {
        if (conexao == null) {
            return;
        }
        try {
            conexao.close();
        } catch (SQLException ignored) {
            // melhor esforço: a conexão já estava com problema.
        }
        conexao = null;
    }
}
