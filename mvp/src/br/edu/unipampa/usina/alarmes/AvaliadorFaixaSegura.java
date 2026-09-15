package br.edu.unipampa.usina.alarmes;

import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;

public final class AvaliadorFaixaSegura implements AvaliadorLimiar {
    @Override
    public boolean foraDaFaixaSegura(MedicaoRegistrada medicao) {
        return medicao.valor() < medicao.limiteMinimo()
            || medicao.valor() > medicao.limiteMaximo();
    }
}
