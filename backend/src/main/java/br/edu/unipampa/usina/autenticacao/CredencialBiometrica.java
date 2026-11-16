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

/**
 * Passkey (WebAuthn) cadastrada por um usuário. Guarda só a chave pública e o contador de
 * assinaturas; o dado biométrico fica no celular. Identificadores binários em base64url.
 */
@Entity
@Table(name = "credencial_biometrica")
public class CredencialBiometrica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "credential_id", nullable = false, unique = true)
    private String credentialId;

    @Column(name = "user_handle", nullable = false)
    private String userHandle;

    @Column(name = "chave_publica_cose", nullable = false)
    private String chavePublicaCose;

    @Column(name = "contador_assinatura", nullable = false)
    private long contadorAssinatura;

    @Column(nullable = false)
    private String apelido;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "ultimo_uso_em")
    private Instant ultimoUsoEm;

    protected CredencialBiometrica() { }

    public CredencialBiometrica(Usuario usuario, String credentialId, String userHandle,
                                String chavePublicaCose, long contadorAssinatura, String apelido) {
        this.usuario = usuario;
        this.credentialId = credentialId;
        this.userHandle = userHandle;
        this.chavePublicaCose = chavePublicaCose;
        this.contadorAssinatura = contadorAssinatura;
        this.apelido = apelido;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getCredentialId() { return credentialId; }
    public String getUserHandle() { return userHandle; }
    public String getChavePublicaCose() { return chavePublicaCose; }
    public long getContadorAssinatura() { return contadorAssinatura; }
    public String getApelido() { return apelido; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getUltimoUsoEm() { return ultimoUsoEm; }

    public void registrarUso(long novoContador) {
        this.contadorAssinatura = novoContador;
        this.ultimoUsoEm = Instant.now();
    }
}
