package br.edu.unipampa.usina.alarmes;

import br.edu.unipampa.usina.controlereator.ResultadoAvaliacao;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.persistenciareator.ReatorRepository;
import java.time.Instant;
import java.util.List;

public class AlarmeFacade {
    private final AlarmeFactory factory;
    private final ReatorRepository repository;
    private final EventBus eventBus;

    public AlarmeFacade() {
        this(new AlarmeFactory(), new ReatorRepository(), new EventBus());
    }

    public AlarmeFacade(AlarmeFactory factory, ReatorRepository repository, EventBus eventBus) {
        this.factory = factory;
        this.repository = repository;
        this.eventBus = eventBus;
    }

    public void processarAvaliacao(ResultadoAvaliacao resultado) {
        if (resultado == null || !resultado.isViolado()) {
            return;
        }

        Alarme alarme = factory.criar(resultado.getMedicao(), "LIMIAR_VIOLADO");
        alarme.emitirAlerta(List.of("Operador", "Supervisao Central"));
        alarme.registrarEvento();
        repository.salvarAlarme(alarme);
        eventBus.publicar(new EventoDominio("AlarmeEmitido", Instant.now(), alarme));
    }

    public ReatorRepository getRepository() { return repository; }
    public EventBus getEventBus() { return eventBus; }
}
