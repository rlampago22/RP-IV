package br.edu.unipampa.usina.alarmes;

import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;

/** Factory responsável pela criação consistente da entidade Alarme. */
public final class AlarmeFactory {
    public Alarme criar(MedicaoRegistrada medicao) {
        return new Alarme(medicao);
    }
}
