package br.edu.unipampa.usina.persistencia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Onde o núcleo ({@code mvp/}) grava os seus dados: o MESMO banco do {@code backend/}.
 *
 * <p>Por padrão é o H2 em arquivo do backend ({@code backend/data/usina}), aberto com
 * {@code AUTO_SERVER=TRUE}: quem abrir primeiro (a API do núcleo ou o Spring) vira o servidor e o
 * outro processo se conecta a ele, então os dois podem rodar ao mesmo tempo. As tabelas do núcleo
 * ficam no schema {@code nucleo}, criado pela migração {@code V6} do backend (Flyway).
 *
 * <p>Para outro banco (ex.: o Postgres do perfil {@code postgres} do backend), defina
 * {@code USINA_DB_URL}, {@code USINA_DB_USUARIO} e {@code USINA_DB_SENHA} (ou as propriedades
 * {@code -Dusina.db.url}, {@code -Dusina.db.usuario}, {@code -Dusina.db.senha}).
 */
public final class ConexaoBanco {
    private static final String OPCOES_H2 = ";MODE=PostgreSQL;AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE";

    private final String url;
    private final String usuario;
    private final String senha;

    public ConexaoBanco(String url, String usuario, String senha) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL do banco não informada.");
        }
        this.url = url;
        this.usuario = usuario == null ? "" : usuario;
        this.senha = senha == null ? "" : senha;
    }

    /** Banco configurado por variável/propriedade ou, sem configuração, o H2 do backend. */
    public static ConexaoBanco padrao() {
        String url = configuracao("usina.db.url", "USINA_DB_URL");
        if (url != null) {
            return new ConexaoBanco(
                url,
                configuracao("usina.db.usuario", "USINA_DB_USUARIO"),
                configuracao("usina.db.senha", "USINA_DB_SENHA")
            );
        }
        return new ConexaoBanco("jdbc:h2:file:" + caminhoH2(arquivoH2Padrao()) + OPCOES_H2, "sa", "");
    }

    /** Banco H2 só em memória (testes): some quando o processo termina. */
    public static ConexaoBanco emMemoria(String nome) {
        return new ConexaoBanco("jdbc:h2:mem:" + nome + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    public Connection abrir() throws SQLException {
        try {
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException error) {
            if ("08001".equals(error.getSQLState()) && error.getMessage() != null
                && error.getMessage().startsWith("No suitable driver")) {
                throw new SQLException(
                    "Driver JDBC não está no classpath para " + descricao()
                        + ". Rode o núcleo pelos scripts da pasta mvp/ (eles encontram ou baixam o driver H2).",
                    error.getSQLState(),
                    error
                );
            }
            throw error;
        }
    }

    /** URL sem credenciais, para mensagens e telas. */
    public String descricao() {
        int opcoes = url.indexOf(';');
        return opcoes < 0 ? url : url.substring(0, opcoes);
    }

    private static String configuracao(String propriedade, String variavel) {
        String valor = System.getProperty(propriedade);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variavel);
        }
        return valor == null || valor.isBlank() ? null : valor;
    }

    /**
     * Procura a raiz do repositório (pasta com {@code backend/pom.xml}) a partir da pasta atual —
     * os scripts rodam de dentro de {@code mvp/}. Fora do repositório, usa um H2 próprio em
     * {@code dados/}, para o núcleo continuar funcionando sozinho.
     */
    private static Path arquivoH2Padrao() {
        Path pasta = Path.of("").toAbsolutePath().normalize();
        for (int nivel = 0; pasta != null && nivel < 4; nivel++, pasta = pasta.getParent()) {
            if (Files.isRegularFile(pasta.resolve("backend").resolve("pom.xml"))) {
                return pasta.resolve("backend").resolve("data").resolve("usina");
            }
        }
        Path local = Path.of("dados", "usina-nucleo").toAbsolutePath().normalize();
        System.err.println("[BANCO] backend/ não encontrado a partir de " + Path.of("").toAbsolutePath()
            + "; usando o banco local " + local);
        return local;
    }

    private static String caminhoH2(Path arquivo) {
        return arquivo.toString().replace('\\', '/');
    }
}
