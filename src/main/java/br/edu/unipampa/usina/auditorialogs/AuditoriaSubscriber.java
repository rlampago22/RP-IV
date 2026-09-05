package br.edu.unipampa.usina.auditorialogs;

import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.IEventSubscriber;

public class AuditoriaSubscriber implements IEventSubscriber {
    private final RegistroAuditoria registro = new RegistroAuditoria();

    @Override
    public void onEvento(EventoDominio evento) {
        registro.registrar(evento);
    }
}
