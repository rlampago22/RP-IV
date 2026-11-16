package br.edu.unipampa.usina.autenticacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Código de uso único que autoriza o cadastro da biometria de um funcionário. Só o hash é guardado. */
@Entity
@Table(name = "codigo_ativacao")
public class CodigoAtivacao {
    public static final String ATIVO = "ATIVO";
    public static final String USADO = "USADO";
    public static final String SUBSTITUIDO = "SUBSTITUIDO";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @ManyToOne
    @JoinColumn(name = "id_emitido_por")
    private Usuario emitidoPor;

    @Column(nullable = false)
    private String situacao = ATIVO;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected CodigoAtivacao() { }

    public CodigoAtivacao(Usuario usuario, String codigoHash, Usuario emitidoPor, Instant expiraEm) {
        this.usuario = usuario;
        this.codigoHash = codigoHash;
        this.emitidoPor = emitidoPor;
        this.expiraEm = expiraEm;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getCodigoHash() { return codigoHash; }
    public Instant getExpiraEm() { return expiraEm; }

    public boolean valeAgora() {
        return ATIVO.equals(situacao) && Instant.now().isBefore(expiraEm);
    }

    public void marcarUsado() {
        this.situacao = USADO;
        this.usadoEm = Instant.now();
    }

    public void marcarSubstituido() { this.situacao = SUBSTITUIDO; }
}
