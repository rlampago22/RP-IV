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

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm = Instant.now();

    protected RegistroAutenticacao() { }

    public RegistroAutenticacao(Usuario usuario, String emailInformado, String resultado) {
        this.usuario = usuario;
        this.emailInformado = emailInformado;
        this.resultado = resultado;
    }
}
