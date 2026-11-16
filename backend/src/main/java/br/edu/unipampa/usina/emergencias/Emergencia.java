package br.edu.unipampa.usina.emergencias;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** RF09 — emergência registrada por um usuário da Central. Ciclo: ABERTA → PROTOCOLO_ATIVO → ENCERRADA. */
@Entity
@Table(name = "emergencia")
public class Emergencia {
    public enum Status { ABERTA, PROTOCOLO_ATIVO, ENCERRADA }

    public enum Tipo { SUPERAQUECIMENTO, VAZAMENTO_RADIOATIVO, FALHA_REFRIGERACAO, OUTRA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ABERTA;

    @Column(name = "criada_por", nullable = false)
    private String criadaPor;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm = Instant.now();

    @Enumerated(EnumType.STRING)
    private ProtocoloEmergencia protocolo;

    @Column(name = "protocolo_ativado_por")
    private String protocoloAtivadoPor;

    @Column(name = "protocolo_ativado_em")
    private Instant protocoloAtivadoEm;

    @Column(name = "encerrada_por")
    private String encerradaPor;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    protected Emergencia() { }

    public Emergencia(Tipo tipo, String descricao, String criadaPor) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.criadaPor = criadaPor;
    }

    /** RF10: só uma emergência aberta, ainda sem protocolo, pode ter um protocolo ativado. */
    void ativarProtocolo(ProtocoloEmergencia protocolo, String por) {
        if (status != Status.ABERTA) {
            throw new IllegalStateException(status == Status.ENCERRADA
                ? "A emergência já foi encerrada."
                : "Esta emergência já tem um protocolo ativo.");
        }
        this.protocolo = protocolo;
        this.protocoloAtivadoPor = por;
        this.protocoloAtivadoEm = Instant.now();
        this.status = Status.PROTOCOLO_ATIVO;
    }

    void encerrar(String por) {
        if (status == Status.ENCERRADA) throw new IllegalStateException("A emergência já foi encerrada.");
        this.encerradaPor = por;
        this.encerradaEm = Instant.now();
        this.status = Status.ENCERRADA;
    }

    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public Status getStatus() { return status; }
    public String getCriadaPor() { return criadaPor; }
    public Instant getCriadaEm() { return criadaEm; }
    public ProtocoloEmergencia getProtocolo() { return protocolo; }
    public String getProtocoloAtivadoPor() { return protocoloAtivadoPor; }
    public Instant getProtocoloAtivadoEm() { return protocoloAtivadoEm; }
    public String getEncerradaPor() { return encerradaPor; }
    public Instant getEncerradaEm() { return encerradaEm; }
}
