package br.edu.unipampa.usina.infraestruturaeventos;

@FunctionalInterface
public interface IEventSubscriber {
    void onEvento(EventoDominio evento);
}
