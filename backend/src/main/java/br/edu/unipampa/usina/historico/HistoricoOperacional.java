package br.edu.unipampa.usina.historico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * RF16/RF17 — uma linha do histórico operacional (um ciclo completo dos 4 sensores RF-1 de um
 * reator). Espelha {@code SnapshotOperacional} do módulo {@code mvp/}: o {@code mvp/} continua
 * dono da lógica de quando fechar um ciclo, aqui só persistimos o resultado.
 *
 * <p>As 4 colunas de sensores guardam rótulos {@code id:TIPO} separados por {@code |} — mesma
 * convenção de {@code SnapshotOperacional.linhaCsv()} no mvp/, pra manter os dois lados
 * compatíveis sem precisar de um tipo de lista no banco.
 */
@Entity
@Table(name = "historico_operacional")
public class HistoricoOperacional {

    public enum OrigemOperador { NAO_IDENTIFICADO, DECLARADO_PELA_UI, SESSAO_VALIDADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reator_id", nullable = false)
    private String reatorId;

    @Column(nullable = false)
    private Instant instante;

    @Column(nullable = false)
    private double temperatura;

    @Column(nullable = false)
    private double pressao;

    @Column(nullable = false)
    private double radiacao;

    @Column(name = "fluxo_resfriamento", nullable = false)
    private double fluxoResfriamento;

    @Column(name = "sensores_em_alarme", nullable = false)
    private String sensoresEmAlarme = "";

    @Column(name = "sensores_reconhecidos", nullable = false)
    private String sensoresReconhecidos = "";

    @Column(name = "sensores_em_observacao", nullable = false)
    private String sensoresEmObservacao = "";

    @Column(name = "sensores_em_falha", nullable = false)
    private String sensoresEmFalha = "";

    @Column(nullable = false)
    private String operador = "";

    @Column(name = "papeis_operador", nullable = false)
    private String papeisOperador = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_operador", nullable = false)
    private OrigemOperador origemOperador;

    protected HistoricoOperacional() { }

    public HistoricoOperacional(
        String reatorId,
        Instant instante,
        double temperatura,
        double pressao,
        double radiacao,
        double fluxoResfriamento,
        String sensoresEmAlarme,
        String sensoresReconhecidos,
        String sensoresEmObservacao,
        String sensoresEmFalha,
        String operador,
        String papeisOperador,
        OrigemOperador origemOperador
    ) {
        this.reatorId = reatorId;
        this.instante = instante;
        this.temperatura = temperatura;
        this.pressao = pressao;
        this.radiacao = radiacao;
        this.fluxoResfriamento = fluxoResfriamento;
        this.sensoresEmAlarme = sensoresEmAlarme == null ? "" : sensoresEmAlarme;
        this.sensoresReconhecidos = sensoresReconhecidos == null ? "" : sensoresReconhecidos;
        this.sensoresEmObservacao = sensoresEmObservacao == null ? "" : sensoresEmObservacao;
        this.sensoresEmFalha = sensoresEmFalha == null ? "" : sensoresEmFalha;
        this.operador = operador == null ? "" : operador;
        this.papeisOperador = papeisOperador == null ? "" : papeisOperador;
        this.origemOperador = origemOperador == null ? OrigemOperador.NAO_IDENTIFICADO : origemOperador;
    }

    public Long getId() { return id; }
    public String getReatorId() { return reatorId; }
    public Instant getInstante() { return instante; }
    public double getTemperatura() { return temperatura; }
    public double getPressao() { return pressao; }
    public double getRadiacao() { return radiacao; }
    public double getFluxoResfriamento() { return fluxoResfriamento; }
    public String getSensoresEmAlarme() { return sensoresEmAlarme; }
    public String getSensoresReconhecidos() { return sensoresReconhecidos; }
    public String getSensoresEmObservacao() { return sensoresEmObservacao; }
    public String getSensoresEmFalha() { return sensoresEmFalha; }
    public String getOperador() { return operador; }
    public String getPapeisOperador() { return papeisOperador; }
    public OrigemOperador getOrigemOperador() { return origemOperador; }

    /** Mesmo formato de {@code SnapshotOperacional.CABECALHO} no mvp/ — RF17 (exportação). */
    public static final String CABECALHO_CSV =
        "reator_id;instante;temperatura;pressao;radiacao;fluxo_resfriamento;"
            + "sensores_em_alarme;sensores_reconhecidos;sensores_em_observacao;sensores_em_falha;"
            + "operador;papeis_operador;origem_operador";

    /** Mesmo formato de {@code SnapshotOperacional.linhaCsv()} no mvp/ — RF17 (exportação). */
    public String linhaCsv() {
        return String.format(
            java.util.Locale.ROOT,
            "%s;%s;%.2f;%.2f;%.2f;%.2f;%s;%s;%s;%s;%s;%s;%s",
            reatorId,
            instante,
            temperatura,
            pressao,
            radiacao,
            fluxoResfriamento,
            sensoresEmAlarme,
            sensoresReconhecidos,
            sensoresEmObservacao,
            sensoresEmFalha,
            operador,
            papeisOperador,
            origemOperador
        );
    }
}
