package br.edu.unipampa.usina.auditorialogs;

import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.IEventSubscriber;

/** Consumidor global que registra todos os fatos relevantes sem alterar os produtores. */
public final class AuditoriaSubscriber implements IEventSubscriber {
    private final RegistroAuditoria registro;

    public AuditoriaSubscriber(EventBus eventBus, RegistroAuditoria registro) {
        this.registro = registro;
        eventBus.assinarTodos(this);
    }

    @Override
    public void onEvento(EventoDominio evento) {
        EntradaAuditoria entrada = registro.registrar(evento);
        System.out.println(
            "[AUDITORIA] #" + entrada.sequencia()
                + " " + entrada.tipoEvento()
                + " hash=" + entrada.hashCurto()
        );
    }
}
