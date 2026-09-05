package br.edu.unipampa.usina.infraestruturaeventos;

public interface IEventSubscriber {
    void onEvento(EventoDominio evento);
}
