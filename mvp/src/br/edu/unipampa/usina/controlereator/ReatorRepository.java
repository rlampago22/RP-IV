package br.edu.unipampa.usina.controlereator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Persistência em memória das medições (papel de ReatorRepository no SEQ-UC01).
 * Mantém histórico curto consultável por sensor e por limite recente.
 */
public final class ReatorRepository {
    public static final int CAPACIDADE_PADRAO = 200;

    private final int capacidadeMaxima;
    private final List<MedicaoReator> medicoes = new ArrayList<>();

    public ReatorRepository() {
        this(CAPACIDADE_PADRAO);
    }

    public ReatorRepository(int capacidadeMaxima) {
        if (capacidadeMaximoInvalido(capacidadeMaxima)) {
            throw new IllegalArgumentException("Capacidade do histórico deve ser >= 1.");
        }
        this.capacidadeMaxima = capacidadeMaxima;
    }

    private static boolean capacidadeMaximoInvalido(int capacidadeMaxima) {
        return capacidadeMaxima < 1;
    }

    /** SEQ-UC01: RF → ReatorRepository.salvarMedicao(medicao) */
    public void salvarMedicao(MedicaoReator medicao) {
        Objects.requireNonNull(medicao, "medicao");
        medicoes.add(medicao);
        while (medicoes.size() > capacidadeMaxima) {
            medicoes.remove(0);
        }
    }

    public List<MedicaoReator> listarTodas() {
        return List.copyOf(medicoes);
    }

    public List<MedicaoReator> listarPorSensor(long sensorId) {
        return List.copyOf(
            medicoes.stream()
                .filter(m -> m.getSensor() != null && m.getSensor().id() == sensorId)
                .collect(Collectors.toList())
        );
    }

    /** Últimas N medições (mais antigas → mais recentes). */
    public List<MedicaoReator> listarRecentes(int limite) {
        if (limite < 1) {
            return List.of();
        }
        int from = Math.max(0, medicoes.size() - limite);
        return List.copyOf(medicoes.subList(from, medicoes.size()));
    }

    public int tamanho() {
        return medicoes.size();
    }

    public int capacidadeMaxima() {
        return capacidadeMaxima;
    }
}
