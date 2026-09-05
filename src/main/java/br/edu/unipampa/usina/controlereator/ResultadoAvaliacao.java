package br.edu.unipampa.usina.controlereator;

public class ResultadoAvaliacao {
    private final boolean violado;
    private final MedicaoReator medicao;

    public ResultadoAvaliacao(boolean violado, MedicaoReator medicao) {
        this.violado = violado;
        this.medicao = medicao;
    }

    public boolean isViolado() { return violado; }
    public MedicaoReator getMedicao() { return medicao; }
}
