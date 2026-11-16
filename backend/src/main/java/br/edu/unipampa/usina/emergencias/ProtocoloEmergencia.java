package br.edu.unipampa.usina.emergencias;

import java.util.List;

/**
 * RF10 — protocolos de contingência que podem ser acionados a partir de uma emergência aberta.
 * Cada protocolo é um roteiro fixo de passos; ativá-lo é ação crítica (4.2.10: dois fatores).
 */
public enum ProtocoloEmergencia {
    DESLIGAMENTO_EMERGENCIAL("Desligamento emergencial do reator", List.of(
        "Inserir as barras de controle (SCRAM).",
        "Manter a circulação do refrigerante primário.",
        "Confirmar a queda de temperatura do núcleo.")),
    CONTENCAO_VAZAMENTO("Contenção de vazamento", List.of(
        "Isolar o circuito afetado.",
        "Selar a área de contenção.",
        "Acompanhar o nível de radiação até estabilizar.")),
    EVACUACAO_AREA("Evacuação da área", List.of(
        "Acionar o alarme de evacuação.",
        "Liberar as rotas de saída e bloquear novos acessos.",
        "Conferir a contagem de pessoas no ponto de encontro."));

    private final String nome;
    private final List<String> passos;

    ProtocoloEmergencia(String nome, List<String> passos) {
        this.nome = nome;
        this.passos = passos;
    }

    public String getNome() { return nome; }
    public List<String> getPassos() { return passos; }
}
