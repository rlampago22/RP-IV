package br.edu.unipampa.usina.autenticacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Entity
@Table(name = "registro_autenticacao")
public class RegistroAutenticacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "email_informado", nullable = false)
    private String emailInformado;

    @Column(nullable = false)
    private String resultado;

    /** Como a pessoa se identificou: {@code SENHA}, {@code CELULAR} (2FA do login, RF18), {@code ACAO_CRITICA}... */
    @Column(nullable = false)
    private String metodo = "SENHA";

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    /** Hash do registro anterior da cadeia (RNF05). {@code null} nos registros antigos, anteriores à V4. */
    @Column(name = "hash_anterior")
    private String hashAnterior;

    @Column(name = "hash")
    private String hash;

    protected RegistroAutenticacao() { }

    public RegistroAutenticacao(Usuario usuario, String emailInformado, String resultado) {
        this.usuario = usuario;
        this.emailInformado = emailInformado;
        this.resultado = resultado;
    }

    public RegistroAutenticacao(Usuario usuario, String emailInformado, String resultado, String metodo) {
        this(usuario, emailInformado, resultado);
        this.metodo = metodo;
    }

    /** Liga este registro ao anterior: hash = SHA-256(hashAnterior | email | resultado | metodo | instante). */
    void encadear(String hashDoAnterior) {
        this.hashAnterior = hashDoAnterior;
        this.hash = calcularHash();
    }

    String calcularHash() {
        String conteudo = String.join("|", String.valueOf(hashAnterior), emailInformado, resultado, metodo,
            String.valueOf(registradoEm.toEpochMilli()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Long getId() { return id; }
    public String getEmailInformado() { return emailInformado; }
    public String getResultado() { return resultado; }
    public String getMetodo() { return metodo; }
    public Instant getRegistradoEm() { return registradoEm; }
    public String getHashAnterior() { return hashAnterior; }
    public String getHash() { return hash; }
}
