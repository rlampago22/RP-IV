package br.edu.unipampa.usina.controlereator;

/** Strategy de avaliação de limiares — implementações concretas no Marco 2. */
public interface AvaliadorLimiar {
    ResultadoAvaliacao avaliar(MedicaoReator medicao, Limiar limiar);
}
