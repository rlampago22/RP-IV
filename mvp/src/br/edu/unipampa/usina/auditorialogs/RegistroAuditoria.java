package br.edu.unipampa.usina.auditorialogs;

import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.persistencia.ConexaoBanco;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Trilha de auditoria append-only com cadeia SHA-256, persistida na tabela
 * {@code nucleo.auditoria_evento} do banco do sistema (antes: arquivo {@code dados/auditoria.log}).
 *
 * <p>A cadeia não mudou: cada linha guarda o hash da anterior e o próprio hash é
 * {@code SHA-256(sequencia|ocorridoEm|tipo|resumo|hashAnterior)}. Alterar ou apagar uma linha
 * direto no banco quebra a cadeia e {@link #verificarIntegridade()} detecta.
 *
 * <p>Vários escritores ao mesmo tempo (a API e a UI Swing, por exemplo) agora são seguros: cada
 * inserção trava a linha única de {@code nucleo.auditoria_trava} ({@code SELECT ... FOR UPDATE}) antes
 * de ler o fim da cadeia, então os escritores entram um de cada vez, mesmo em processos diferentes.
 * E a {@code sequencia} é chave primária: se algo escapar da trava, o banco recusa a linha duplicada
 * em vez de bifurcar a cadeia. No arquivo isso corrompia a cadeia (incidente de 09/11/2026).
 */
public final class RegistroAuditoria implements AutoCloseable {
    public static final String TABELA = "nucleo.auditoria_evento";
    private static final String TABELA_TRAVA = "nucleo.auditoria_trava";
    /** Log em arquivo usado até a migração para o banco; importado uma vez se a tabela estiver vazia. */
    public static final Path LOG_LEGADO = Path.of("dados", "auditoria.log");

    private static final String GENESIS = "GENESIS";
    private static final int TENTATIVAS_INSERCAO = 10;
    private static final String SQLSTATE_CHAVE_DUPLICADA = "23505";
    private static final String COLUNAS = "sequencia, ocorrido_em, tipo_evento, resumo, hash_anterior, hash";

    private final ConexaoBanco banco;
    private Connection conexao;

    public RegistroAuditoria(ConexaoBanco banco) {
        this.banco = banco;
        garantirTabela();
    }

    /** Banco padrão do sistema, já trazendo o {@code dados/auditoria.log} antigo se for a 1ª vez. */
    public static RegistroAuditoria abrirPadrao() {
        RegistroAuditoria registro = new RegistroAuditoria(ConexaoBanco.padrao());
        int importadas = registro.importarLogLegado(LOG_LEGADO);
        if (importadas > 0) {
            System.out.println("[AUDITORIA] " + importadas + " entradas de " + LOG_LEGADO.toAbsolutePath()
                + " importadas para " + TABELA + ".");
        }
        return registro;
    }

    /**
     * Mesma estrutura da migração {@code backend/.../V6__criar_auditoria_nucleo.sql}. Fica aqui
     * também (idempotente) para o núcleo funcionar com um banco que o backend ainda não migrou e
     * com o H2 em memória dos testes.
     */
    private synchronized void garantirTabela() {
        try (Statement sql = conexao().createStatement()) {
            sql.execute("CREATE SCHEMA IF NOT EXISTS nucleo");
            sql.execute(
                "CREATE TABLE IF NOT EXISTS " + TABELA + " ("
                    + "sequencia BIGINT PRIMARY KEY, "
                    + "ocorrido_em VARCHAR(40) NOT NULL, "
                    + "tipo_evento VARCHAR(80) NOT NULL, "
                    + "resumo VARCHAR(4000) NOT NULL, "
                    + "hash_anterior VARCHAR(64) NOT NULL, "
                    + "hash VARCHAR(64) NOT NULL, "
                    + "registrado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)"
            );
            sql.execute("CREATE TABLE IF NOT EXISTS " + TABELA_TRAVA + " (id INT PRIMARY KEY)");
            try {
                sql.execute("INSERT INTO " + TABELA_TRAVA + " (id) SELECT 1 WHERE NOT EXISTS "
                    + "(SELECT 1 FROM " + TABELA_TRAVA + " WHERE id = 1)");
            } catch (SQLException outroProcessoCriouAntes) {
                if (!SQLSTATE_CHAVE_DUPLICADA.equals(outroProcessoCriouAntes.getSQLState())) {
                    throw outroProcessoCriouAntes;
                }
            }
        } catch (SQLException error) {
            descartarConexao();
            throw new IllegalStateException(
                "Não foi possível preparar a tabela de auditoria em " + banco.descricao() + ": " + error.getMessage(),
                error
            );
        }
    }

    public synchronized EntradaAuditoria registrar(EventoDominio evento) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return inserirProxima(evento);
            } catch (SQLException error) {
                boolean outroEscritorPegouASequencia = SQLSTATE_CHAVE_DUPLICADA.equals(error.getSQLState());
                if (!outroEscritorPegouASequencia) {
                    descartarConexao();
                }
                if (!outroEscritorPegouASequencia || tentativa >= TENTATIVAS_INSERCAO) {
                    throw new IllegalStateException("Não foi possível gravar a auditoria: " + error.getMessage(), error);
                }
            }
        }
    }

    private EntradaAuditoria inserirProxima(EventoDominio evento) throws SQLException {
        Connection c = conexao();
        c.setAutoCommit(false);
        try {
            travarEscrita(c);
            long sequencia = 1L;
            String hashAnterior = GENESIS;
            try (Statement sql = c.createStatement();
                 ResultSet ultima = sql.executeQuery(
                     "SELECT sequencia, hash FROM " + TABELA + " ORDER BY sequencia DESC LIMIT 1")) {
                if (ultima.next()) {
                    sequencia = ultima.getLong(1) + 1L;
                    hashAnterior = ultima.getString(2);
                }
            }
            String hash = calcularHash(sequencia, evento.ocorridoEm(), evento.tipo(), evento.resumo(), hashAnterior);
            EntradaAuditoria entrada = new EntradaAuditoria(
                sequencia, evento.ocorridoEm(), evento.tipo(), evento.resumo(), hashAnterior, hash
            );
            inserir(c, entrada);
            c.commit();
            return entrada;
        } catch (SQLException error) {
            desfazer(c);
            throw error;
        } finally {
            restaurarAutoCommit(c);
        }
    }

    /** Fila única de escrita: segura a linha de trava até o commit/rollback da transação. */
    private static void travarEscrita(Connection c) throws SQLException {
        try (Statement sql = c.createStatement();
             ResultSet trava = sql.executeQuery("SELECT id FROM " + TABELA_TRAVA + " WHERE id = 1 FOR UPDATE")) {
            if (!trava.next()) {
                throw new SQLException("Linha de trava da auditoria ausente em " + TABELA_TRAVA + ".");
            }
        }
    }

    private static void inserir(Connection c, EntradaAuditoria entrada) throws SQLException {
        try (PreparedStatement insert = c.prepareStatement(
            "INSERT INTO " + TABELA + " (" + COLUNAS + ") VALUES (?, ?, ?, ?, ?, ?)")) {
            preencher(insert, entrada);
            insert.executeUpdate();
        }
    }

    private static void preencher(PreparedStatement insert, EntradaAuditoria entrada) throws SQLException {
        insert.setLong(1, entrada.sequencia());
        // Texto ISO exatamente como entra no hash: um TIMESTAMP do banco poderia arredondar os
        // nanossegundos (o Postgres guarda microssegundos) e quebrar o recálculo da cadeia.
        insert.setString(2, entrada.ocorridoEm().toString());
        insert.setString(3, entrada.tipoEvento());
        insert.setString(4, entrada.resumo());
        insert.setString(5, entrada.hashAnterior());
        insert.setString(6, entrada.hash());
    }

    /** Todas as entradas, em ordem de sequência, lidas do banco. */
    public synchronized List<EntradaAuditoria> consultar() {
        try (Statement sql = conexao().createStatement();
             ResultSet linhas = sql.executeQuery("SELECT " + COLUNAS + " FROM " + TABELA + " ORDER BY sequencia")) {
            List<EntradaAuditoria> entradas = new ArrayList<>();
            while (linhas.next()) {
                entradas.add(new EntradaAuditoria(
                    linhas.getLong(1),
                    Instant.parse(linhas.getString(2)),
                    linhas.getString(3),
                    linhas.getString(4),
                    linhas.getString(5),
                    linhas.getString(6)
                ));
            }
            return List.copyOf(entradas);
        } catch (SQLException error) {
            descartarConexao();
            throw new IllegalStateException("Não foi possível consultar a auditoria: " + error.getMessage(), error);
        }
    }

    /**
     * Recalcula a cadeia SHA-256 a partir do que está gravado no banco. Detecta linha alterada,
     * apagada (buraco na sequência) ou inserida fora da cadeia. Se o banco não responder, a
     * integridade não pode ser comprovada e o resultado é {@code false}.
     */
    public synchronized boolean verificarIntegridade() {
        try {
            return cadeiaIntegra(consultar());
        } catch (IllegalStateException bancoIndisponivel) {
            System.err.println("[AUDITORIA] " + bancoIndisponivel.getMessage());
            return false;
        }
    }

    static boolean cadeiaIntegra(List<EntradaAuditoria> entradas) {
        String anterior = GENESIS;
        long sequenciaEsperada = 1L;
        for (EntradaAuditoria entrada : entradas) {
            String esperado = calcularHash(
                entrada.sequencia(), entrada.ocorridoEm(), entrada.tipoEvento(), entrada.resumo(), anterior
            );
            if (entrada.sequencia() != sequenciaEsperada
                || !anterior.equals(entrada.hashAnterior())
                || !esperado.equals(entrada.hash())) {
                return false;
            }
            anterior = entrada.hash();
            sequenciaEsperada++;
        }
        return true;
    }

    /**
     * Traz o log em arquivo antigo para a tabela, com as mesmas sequências e hashes (a cadeia
     * continua verificável desde a 1ª linha). Só roda com a tabela vazia, então é seguro chamar a
     * cada inicialização. Um arquivo com a cadeia quebrada NÃO é importado — ele fica no disco como
     * evidência e a tabela começa do zero, em vez de nascer já marcada como adulterada.
     *
     * @return quantas entradas foram importadas
     */
    public synchronized int importarLogLegado(Path arquivo) {
        if (!Files.isRegularFile(arquivo) || !consultar().isEmpty()) {
            return 0;
        }
        List<EntradaAuditoria> entradas = new ArrayList<>();
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                if (linha.isBlank()) {
                    continue;
                }
                EntradaAuditoria entrada = EntradaAuditoria.deLinhaPersistida(linha.trim());
                if (entrada == null) {
                    System.err.println("[AUDITORIA] " + arquivo + " tem linha ilegível; log antigo não importado.");
                    return 0;
                }
                entradas.add(entrada);
            }
        } catch (IOException error) {
            System.err.println("[AUDITORIA] Não foi possível ler " + arquivo + ": " + error.getMessage());
            return 0;
        }
        if (!cadeiaIntegra(entradas)) {
            System.err.println("[AUDITORIA] A cadeia SHA-256 de " + arquivo.toAbsolutePath()
                + " está quebrada; log antigo mantido no disco e NÃO importado para o banco.");
            return 0;
        }

        Connection c;
        try {
            c = conexao();
            c.setAutoCommit(false);
        } catch (SQLException error) {
            descartarConexao();
            throw new IllegalStateException("Não foi possível importar o log antigo: " + error.getMessage(), error);
        }
        try (PreparedStatement insert = c.prepareStatement(
            "INSERT INTO " + TABELA + " (" + COLUNAS + ") VALUES (?, ?, ?, ?, ?, ?)")) {
            travarEscrita(c);
            try (Statement sql = c.createStatement();
                 ResultSet total = sql.executeQuery("SELECT COUNT(*) FROM " + TABELA)) {
                total.next();
                if (total.getLong(1) > 0) {
                    c.rollback(); // outro processo gravou primeiro; a tabela não está mais vazia
                    return 0;
                }
            }
            for (EntradaAuditoria entrada : entradas) {
                preencher(insert, entrada);
                insert.addBatch();
            }
            insert.executeBatch();
            c.commit();
            return entradas.size();
        } catch (SQLException error) {
            desfazer(c);
            // Chave duplicada = outro processo importou/gravou primeiro; nada a fazer.
            if (SQLSTATE_CHAVE_DUPLICADA.equals(error.getSQLState())
                || (error.getNextException() != null
                    && SQLSTATE_CHAVE_DUPLICADA.equals(error.getNextException().getSQLState()))) {
                return 0;
            }
            throw new IllegalStateException("Não foi possível importar o log antigo: " + error.getMessage(), error);
        } finally {
            restaurarAutoCommit(c);
        }
    }

    /** Grava a trilha num arquivo texto (mesmo formato do log antigo), para abrir/entregar. */
    public synchronized Path exportar(Path destino) throws IOException {
        List<String> linhas = consultar().stream().map(EntradaAuditoria::linhaPersistida).toList();
        Path pasta = destino.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
        return Files.write(destino, linhas, StandardCharsets.UTF_8);
    }

    /** Onde a trilha está gravada, para telas e mensagens. */
    public String descricaoDestino() {
        return TABELA + " em " + banco.descricao();
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

    private static void desfazer(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
            // a transação morre junto com a conexão de qualquer forma.
        }
    }

    private static void restaurarAutoCommit(Connection c) {
        try {
            if (!c.isClosed()) {
                c.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
            // conexão já inutilizada; será reaberta na próxima operação.
        }
    }

    public static String calcularHash(
        long sequencia,
        Instant ocorridoEm,
        String tipo,
        String resumo,
        String hashAnterior
    ) {
        String conteudo = sequencia + "|" + ocorridoEm + "|" + tipo + "|" + resumo + "|" + hashAnterior;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 indisponível.", error);
        }
    }
}
