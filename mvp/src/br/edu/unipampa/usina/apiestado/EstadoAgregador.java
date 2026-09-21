package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.infraestruturaeventos.AlarmeEmitido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeReconhecido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeResolvido;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.IEventSubscriber;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumidor global (assinarTodos) que deriva o estado exibido no T01 (Opção A) apenas a partir
 * dos fatos publicados no EventBus — não conhece ReatorFacade/AlarmeFacade diretamente, conforme
 * "API / Frontend (web): Adaptadores que observam estado derivado dos eventos"
 * (docs/marco1/04-arquitetura-eda.md).
 */
public final class EstadoAgregador implements IEventSubscriber {
    private static final int LIMITE_TIMELINE = 20;

    private final Map<Long, SensorEstado> sensores = new ConcurrentHashMap<>();
    private final Map<String, AlarmeEstado> alarmes = new LinkedHashMap<>();
    private final Set<Long> sensoresEmAtencao = new HashSet<>();
    private final Deque<EventoEstado> timeline = new ArrayDeque<>();
    private final AtomicInteger totalMedicoes = new AtomicInteger();
    private final AtomicInteger totalAlarmes = new AtomicInteger();
    private final AtomicInteger totalAuditoria = new AtomicInteger();
    private final AtomicBoolean tempoReal = new AtomicBoolean(false);

    public EstadoAgregador(EventBus eventBus) {
        eventBus.assinarTodos(this);
    }

    @Override
    public synchronized void onEvento(EventoDominio evento) {
        totalAuditoria.incrementAndGet();
        registrarTimeline(evento);

        if (evento instanceof MedicaoRegistrada medicao) {
            totalMedicoes.incrementAndGet();
            sensoresEmAtencao.remove(medicao.sensorId());
            sensores.put(medicao.sensorId(), new SensorEstado(
                medicao.sensorId(),
                medicao.tipoSensor(),
                medicao.unidade(),
                medicao.valor(),
                medicao.limiteMinimo(),
                medicao.limiteMaximo(),
                medicao.ocorridoEm()
            ));
        } else if (evento instanceof ObservacaoRegistrada observacao) {
            sensoresEmAtencao.add(observacao.sensorId());
        } else if (evento instanceof AlarmeEmitido emitido) {
            totalAlarmes.incrementAndGet();
            alarmes.put(emitido.alarmeId(), new AlarmeEstado(
                emitido.alarmeId(),
                emitido.sensorId(),
                emitido.severidade(),
                emitido.mensagem(),
                "ATIVO",
                emitido.ocorridoEm(),
                null,
                null
            ));
        } else if (evento instanceof AlarmeReconhecido reconhecido) {
            alarmes.computeIfPresent(
                reconhecido.alarmeId(),
                (id, atual) -> atual.comStatus("RECONHECIDO", reconhecido.operador(), null)
            );
        } else if (evento instanceof AlarmeResolvido resolvido) {
            alarmes.computeIfPresent(
                resolvido.alarmeId(),
                (id, atual) -> atual.comStatus("RESOLVIDO", atual.operador(), resolvido.solucao())
            );
        }
    }

    private void registrarTimeline(EventoDominio evento) {
        timeline.addFirst(new EventoEstado(evento.ocorridoEm(), evento.tipo(), evento.resumo()));
        while (timeline.size() > LIMITE_TIMELINE) {
            timeline.removeLast();
        }
    }

    public void definirTempoReal(boolean ativo) {
        tempoReal.set(ativo);
    }

    public boolean isTempoRealAtivo() {
        return tempoReal.get();
    }

    public synchronized EstadoSnapshot snapshot() {
        return new EstadoSnapshot(
            calcularStatus(),
            tempoReal.get(),
            Instant.now(),
            List.copyOf(sensores.values()),
            new Contadores(totalMedicoes.get(), totalAlarmes.get(), totalAuditoria.get()),
            List.copyOf(alarmes.values()),
            List.copyOf(timeline)
        );
    }

    private StatusReator calcularStatus() {
        boolean critico = alarmes.values().stream().anyMatch(a -> "ATIVO".equals(a.status()));
        if (critico) {
            return StatusReator.CRITICO;
        }
        boolean reconhecidoPendente = alarmes.values().stream().anyMatch(a -> "RECONHECIDO".equals(a.status()));
        boolean atencao = reconhecidoPendente || !sensoresEmAtencao.isEmpty();
        return atencao ? StatusReator.ATENCAO : StatusReator.ESTAVEL;
    }
}
