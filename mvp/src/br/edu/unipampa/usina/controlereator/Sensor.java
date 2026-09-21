package br.edu.unipampa.usina.controlereator;

public record Sensor(
    long id,
    String tipo,
    String unidade,
    double limiteMinimo,
    double limiteMaximo,
    double atencaoMinima,
    double atencaoMaxima
) {
    public Sensor {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("O tipo do sensor é obrigatório.");
        }
        if (limiteMinimo >= limiteMaximo) {
            throw new IllegalArgumentException("A faixa segura do sensor é inválida.");
        }
    }

    public Sensor(long id, String tipo, String unidade, double limiteMinimo, double limiteMaximo) {
        this(
            id,
            tipo,
            unidade,
            limiteMinimo,
            limiteMaximo,
            limiteMinimo + (limiteMaximo - limiteMinimo) * 0.1,
            limiteMaximo - (limiteMaximo - limiteMinimo) * 0.1
        );
    }

    public boolean isNaFaixaObservacao(double valor) {
        // Dentro dos limites normais, mas fora da faixa ideal de atenção
        boolean dentroSeguro = valor >= limiteMinimo && valor <= limiteMaximo;
        boolean foraAtencao = valor < atencaoMinima || valor > atencaoMaxima;
        return dentroSeguro && foraAtencao;
    }
}
