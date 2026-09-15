package br.edu.unipampa.usina.infraestruturaeventos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Barramento in-process do MVP.
 * Implementa Observer/Pub-Sub e desacopla produtores de consumidores com isolamento de falhas.
 */
public final class EventBus {
    private final List<IEventSubscriber> assinantesGlobais = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends EventoDominio>, List<IEventSubscriber>> assinantesPorTipo =
        new ConcurrentHashMap<>();

    public void assinarTodos(IEventSubscriber subscriber) {
        assinantesGlobais.add(subscriber);
    }

    public void assinar(
        Class<? extends EventoDominio> tipoEvento,
        IEventSubscriber subscriber
    ) {
        assinantesPorTipo
            .computeIfAbsent(tipoEvento, ignored -> new CopyOnWriteArrayList<>())
            .add(subscriber);
    }

    public void publicar(EventoDominio evento) {
        for (IEventSubscriber subscriber : assinantesGlobais) {
            try {
                subscriber.onEvento(evento);
            } catch (Throwable error) {
                System.err.println("[EVENTBUS ISOLAMENTO FALHA] Erro em assinante global: " + error.getMessage());
            }
        }
        for (IEventSubscriber subscriber : assinantesPorTipo.getOrDefault(evento.getClass(), List.of())) {
            try {
                subscriber.onEvento(evento);
            } catch (Throwable error) {
                System.err.println("[EVENTBUS ISOLAMENTO FALHA] Erro em assinante por tipo: " + error.getMessage());
            }
        }
    }
}
