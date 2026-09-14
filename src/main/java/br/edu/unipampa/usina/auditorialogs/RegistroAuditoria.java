package br.edu.unipampa.usina.auditorialogs;

import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import java.util.ArrayList;
import java.util.List;

public class RegistroAuditoria {
    private final List<EventoDominio> registros = new ArrayList<>();

    public void registrar(EventoDominio evento) {
        if (evento == null) {
            throw new IllegalArgumentException("Evento nao pode ser nulo");
        }
        registros.add(evento);
    }

    public List<EventoDominio> consultarRegistros() {
        return List.copyOf(registros);
    }
}
