package br.edu.unipampa.usina.alarmes;

import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Entidade de alarme com ciclo de vida (ativo, reconhecido, resolvido) e rastreabilidade. */
public final class Alarme {
    public enum Status {
        ATIVO,
        RECONHECIDO,
        RESOLVIDO
    }

    private final String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private final MedicaoRegistrada origem;
    private final Instant criadoEm = Instant.now();
    private List<String> destinatarios = List.of();
    private boolean emitido;
    private boolean registrado;
    private Status status = Status.ATIVO;
    private String operadorReconhecimento;
    private Instant momentoReconhecimento;
    private String justificativaReconhecimento;
    private String responsavelSolucao;
    private String solucao;
    private Instant momentoSolucao;

    public Alarme(MedicaoRegistrada origem) {
        this.origem = origem;
    }

    public void emitirAlerta(List<String> destinatarios) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            throw new IllegalArgumentException("O alarme precisa de destinatários explícitos.");
        }
        this.destinatarios = List.copyOf(destinatarios);
        this.emitido = true;
    }

    public void registrarEvento() {
        if (!emitido) {
            throw new IllegalStateException("O alarme deve ser emitido antes do registro.");
        }
        this.registrado = true;
    }

    public void reconhecer(String operador, String justificativa) {
        if (!emitido) {
            throw new IllegalStateException("O alarme precisa ter sido emitido para ser reconhecido.");
        }
        this.operadorReconhecimento = operador != null ? operador : "Operador";
        this.justificativaReconhecimento = justificativa != null ? justificativa : "Alerta validado";
        this.momentoReconhecimento = Instant.now();
        this.status = Status.RECONHECIDO;
    }

    public void resolver(String responsavel, String solucao) {
        this.responsavelSolucao = responsavel != null ? responsavel : "Operador";
        this.solucao = solucao != null ? solucao : "Parametro normalizado";
        this.momentoSolucao = Instant.now();
        this.status = Status.RESOLVIDO;
    }

    public String getId() {
        return id;
    }

    public MedicaoRegistrada getOrigem() {
        return origem;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<String> getDestinatarios() {
        return destinatarios;
    }

    public boolean isRegistrado() {
        return registrado;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isAtivo() {
        return status == Status.ATIVO;
    }

    public boolean isReconhecido() {
        return status == Status.RECONHECIDO;
    }

    public boolean isResolvido() {
        return status == Status.RESOLVIDO;
    }

    public String getOperadorReconhecimento() {
        return operadorReconhecimento;
    }

    public Instant getMomentoReconhecimento() {
        return momentoReconhecimento;
    }

    public String getSolucao() {
        return solucao;
    }
}
