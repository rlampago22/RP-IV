package br.edu.unipampa.usina.apiestado;

import java.time.Instant;

/** Última leitura conhecida de um sensor RF-1, derivada de {@code MedicaoRegistrada}. */
public record SensorEstado(
    long id,
    String tipo,
    String unidade,
    double valor,
    double limiteMinimo,
    double limiteMaximo,
    Instant atualizadoEm
) {}
