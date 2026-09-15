package br.edu.unipampa.usina.alarmes;

import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;

/** Strategy usada para decidir se uma medição viola a faixa segura. */
@FunctionalInterface
public interface AvaliadorLimiar {
    boolean foraDaFaixaSegura(MedicaoRegistrada medicao);
}
