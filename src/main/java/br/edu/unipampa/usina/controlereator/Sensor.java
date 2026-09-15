package br.edu.unipampa.usina.controlereator;

public class Sensor {
    private final long id;
    private final String tipo;
    private final String status;
    private final String unidade;
    private final double limiteMinimo;
    private final double limiteMaximo;
    private final double atencaoMinima;
    private final double atencaoMaxima;

    public Sensor() {
        this(0L, "DESCONHECIDO", "OPERACIONAL");
    }

    public Sensor(long id, String tipo, String status) {
        this(
            id,
            tipo,
            definirUnidade(tipo),
            definirMin(tipo),
            definirMax(tipo),
            definirAtencaoMin(tipo),
            definirAtencaoMax(tipo),
            status
        );
    }

    public Sensor(long id, String tipo, String unidade, double limiteMinimo, double limiteMaximo) {
        this(
            id,
            tipo,
            unidade,
            limiteMinimo,
            limiteMaximo,
            limiteMinimo + (limiteMaximo - limiteMinimo) * 0.1,
            limiteMaximo - (limiteMaximo - limiteMinimo) * 0.1,
            "OPERACIONAL"
        );
    }

    public Sensor(
        long id,
        String tipo,
        String unidade,
        double limiteMinimo,
        double limiteMaximo,
        double atencaoMinima,
        double atencaoMaxima
    ) {
        this(id, tipo, unidade, limiteMinimo, limiteMaximo, atencaoMinima, atencaoMaxima, "OPERACIONAL");
    }

    public Sensor(
        long id,
        String tipo,
        String unidade,
        double limiteMinimo,
        double limiteMaximo,
        double atencaoMinima,
        double atencaoMaxima,
        String status
    ) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("O tipo do sensor é obrigatório.");
        }
        if (limiteMinimo >= limiteMaximo) {
            throw new IllegalArgumentException("A faixa segura do sensor é inválida.");
        }
        this.id = id;
        this.tipo = tipo;
        this.unidade = (unidade != null && !unidade.isBlank()) ? unidade : "un";
        this.limiteMinimo = limiteMinimo;
        this.limiteMaximo = limiteMaximo;
        this.atencaoMinima = atencaoMinima;
        this.atencaoMaxima = atencaoMaxima;
        this.status = (status != null && !status.isBlank()) ? status : "OPERACIONAL";
    }

    private static String definirUnidade(String tipo) {
        if (tipo == null) return "un";
        return switch (tipo.toUpperCase()) {
            case "TEMPERATURA" -> "Celsius";
            case "PRESSAO" -> "bar";
            case "RADIACAO" -> "mSv/h";
            case "FLUXO_RESFRIAMENTO" -> "m3/h";
            default -> "un";
        };
    }

    private static double definirMin(String tipo) {
        if ("FLUXO_RESFRIAMENTO".equalsIgnoreCase(tipo)) return 500.0;
        return 0.0;
    }

    private static double definirMax(String tipo) {
        if (tipo == null) return 100.0;
        return switch (tipo.toUpperCase()) {
            case "TEMPERATURA" -> 350.0;
            case "PRESSAO" -> 160.0;
            case "RADIACAO" -> 5.0;
            case "FLUXO_RESFRIAMENTO" -> 1500.0;
            default -> 100.0;
        };
    }

    private static double definirAtencaoMin(String tipo) {
        if ("FLUXO_RESFRIAMENTO".equalsIgnoreCase(tipo)) return 600.0;
        if ("TEMPERATURA".equalsIgnoreCase(tipo)) return 20.0;
        if ("PRESSAO".equalsIgnoreCase(tipo)) return 10.0;
        if ("RADIACAO".equalsIgnoreCase(tipo)) return 0.1;
        return 10.0;
    }

    private static double definirAtencaoMax(String tipo) {
        if ("FLUXO_RESFRIAMENTO".equalsIgnoreCase(tipo)) return 1400.0;
        if ("TEMPERATURA".equalsIgnoreCase(tipo)) return 320.0;
        if ("PRESSAO".equalsIgnoreCase(tipo)) return 150.0;
        if ("RADIACAO".equalsIgnoreCase(tipo)) return 4.0;
        return 90.0;
    }

    public boolean isNaFaixaObservacao(double valor) {
        boolean dentroSeguro = valor >= limiteMinimo && valor <= limiteMaximo;
        boolean foraAtencao = valor < atencaoMinima || valor > atencaoMaxima;
        return dentroSeguro && foraAtencao;
    }

    // Acessores estilo JavaBeans
    public long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public String getUnidade() { return unidade; }
    public double getLimiteMinimo() { return limiteMinimo; }
    public double getLimiteMaximo() { return limiteMaximo; }
    public double getAtencaoMinima() { return atencaoMinima; }
    public double getAtencaoMaxima() { return atencaoMaxima; }
    public String obterLocalizacao() { return "Vaso de Pressão R-01"; }

    // Acessores estilo Record
    public long id() { return id; }
    public String tipo() { return tipo; }
    public String status() { return status; }
    public String unidade() { return unidade; }
    public double limiteMinimo() { return limiteMinimo; }
    public double limiteMaximo() { return limiteMaximo; }
    public double atencaoMinima() { return atencaoMinima; }
    public double atencaoMaxima() { return atencaoMaxima; }
}
