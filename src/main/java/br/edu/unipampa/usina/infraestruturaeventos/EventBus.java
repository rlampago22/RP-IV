package br.edu.unipampa.usina.infraestruturaeventos;

/** Barramento in-process (Observer/Pub-Sub). Implementação no Marco 2. */
public class EventBus implements IEventPublisher, IEventSubscriber {
    @Override
    public void publicar(EventoDominio evento) {
        throw new UnsupportedOperationException("Marco 2");
    }

    public void assinar(String tipo, IEventSubscriber subscriber) {
        throw new UnsupportedOperationException("Marco 2");
    }

    @Override
    public void onEvento(EventoDominio evento) {
        throw new UnsupportedOperationException("Marco 2");
    }
}
