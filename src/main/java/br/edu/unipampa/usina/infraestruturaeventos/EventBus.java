package br.edu.unipampa.usina.infraestruturaeventos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Barramento in-process baseado em Observer/Pub-Sub. */
public class EventBus implements IEventPublisher, IEventSubscriber {
    private final Map<String, List<IEventSubscriber>> subscribers = new HashMap<>();

    @Override
    public void publicar(EventoDominio evento) {
        if (evento == null) {
            throw new IllegalArgumentException("Evento nao pode ser nulo");
        }

        List<IEventSubscriber> inscritos = subscribers.get(evento.getTipo());
        if (inscritos == null) {
            return;
        }

        for (IEventSubscriber subscriber : List.copyOf(inscritos)) {
            try {
                subscriber.onEvento(evento);
            } catch (RuntimeException ignored) {
                // Falha de um consumidor nao interrompe os demais.
            }
        }
    }

    public void assinar(String tipo, IEventSubscriber subscriber) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Tipo do evento e obrigatorio");
        }
        if (subscriber == null) {
            throw new IllegalArgumentException("Subscriber nao pode ser nulo");
        }

        subscribers.computeIfAbsent(tipo, chave -> new ArrayList<>()).add(subscriber);
    }

    @Override
    public void onEvento(EventoDominio evento) {
        publicar(evento);
    }
}
