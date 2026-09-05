package br.edu.unipampa.usina.infraestruturaeventos;

public interface IEventPublisher {
    void publicar(EventoDominio evento);
}
