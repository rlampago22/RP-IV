package br.edu.unipampa.usina.alarmes;

import br.edu.unipampa.usina.infraestruturaeventos.AlarmeEmitido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeReconhecido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeResolvido;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.IEventSubscriber;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Consome medições e orquestra avaliação, criação, notificação e ciclo de vida do alarme. */
public final class AlarmeFacade implements IEventSubscriber {
    private static final List<String> DESTINATARIOS =
        List.of("Operador de Reator", "Supervisao Central");

    private final EventBus eventBus;
    private final AvaliadorLimiar avaliador;
    private final AlarmeFactory factory;
    private final List<Alarme> alarmes = new ArrayList<>();

    public AlarmeFacade(EventBus eventBus, AvaliadorLimiar avaliador, AlarmeFactory factory) {
        this.eventBus = eventBus;
        this.avaliador = avaliador;
        this.factory = factory;
        eventBus.assinar(MedicaoRegistrada.class, this);
    }

    @Override
    public void onEvento(EventoDominio evento) {
        if (!(evento instanceof MedicaoRegistrada medicao)) {
            return;
        }
        boolean violado = avaliador.foraDaFaixaSegura(medicao);
        processarAvaliacao(medicao, violado);
    }

    public Alarme processarAvaliacao(MedicaoRegistrada medicao, boolean violado) {
        if (!violado) {
            System.out.println("[LIMIAR OK] Medicao dentro da faixa segura.");
            return null;
        }

        Alarme alarme = factory.criar(medicao);
        alarme.emitirAlerta(DESTINATARIOS);
        alarme.registrarEvento();
        alarmes.add(alarme);

        String mensagem = String.format(
            Locale.ROOT,
            "%s fora da faixa segura: %.2f %s",
            medicao.tipoSensor(),
            medicao.valor(),
            medicao.unidade()
        );
        System.out.println("[ALARME CRITICO] " + mensagem);
        System.out.println("[NOTIFICACAO] " + String.join(" e ", DESTINATARIOS));

        eventBus.publicar(new AlarmeEmitido(
            alarme.getCriadoEm(),
            alarme.getId(),
            medicao.sensorId(),
            "CRITICA",
            mensagem,
            alarme.getDestinatarios()
        ));
        return alarme;
    }

    public boolean reconhecerAlarme(String alarmeId, String operador, String justificativa) {
        Optional<Alarme> encontrado = alarmes.stream()
            .filter(a -> a.getId().equalsIgnoreCase(alarmeId) && a.isAtivo())
            .findFirst();
        if (encontrado.isPresent()) {
            Alarme a = encontrado.get();
            a.reconhecer(operador, justificativa);
            System.out.printf(
                Locale.ROOT,
                "[ALARME RECONHECIDO] %s por %s: %s%n",
                alarmeId,
                operador,
                justificativa
            );
            eventBus.publicar(new AlarmeReconhecido(
                Instant.now(),
                alarmeId,
                operador,
                justificativa
            ));
            return true;
        }
        return false;
    }

    public boolean resolverAlarme(String alarmeId, String responsavel, String solucao) {
        Optional<Alarme> encontrado = alarmes.stream()
            .filter(a -> a.getId().equalsIgnoreCase(alarmeId) && !a.isResolvido())
            .findFirst();
        if (encontrado.isPresent()) {
            Alarme a = encontrado.get();
            a.resolver(responsavel, solucao);
            System.out.printf(
                Locale.ROOT,
                "[ALARME RESOLVIDO] %s por %s: %s%n",
                alarmeId,
                responsavel,
                solucao
            );
            eventBus.publicar(new AlarmeResolvido(
                Instant.now(),
                alarmeId,
                responsavel,
                solucao
            ));
            return true;
        }
        return false;
    }

    public List<Alarme> consultarAlarmes() {
        return List.copyOf(alarmes);
    }

    public List<Alarme> consultarAlarmesPendentes() {
        return alarmes.stream()
            .filter(a -> !a.isResolvido())
            .toList();
    }

    public boolean temAlarmePendente() {
        return alarmes.stream().anyMatch(a -> !a.isResolvido());
    }

    public boolean temAlarmeCriticoAtivo() {
        return alarmes.stream().anyMatch(Alarme::isAtivo);
    }
}
