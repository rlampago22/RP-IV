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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumidor global (assinarTodos) que deriva o estado exibido na UI apenas a partir dos fatos
 * publicados no EventBus — não conhece ReatorFacade/AlarmeFacade diretamente, conforme
 * "API / Frontend (web): Adaptadores que observam estado derivado dos eventos"
 * (docs/marco1/04-arquitetura-eda.md).
 *
 * <p>Guarda o estado <b>separado por reator</b>: os ids de sensor (1 a 4) se repetem em todos os
 * reatores, então a chave é sempre o par reator + sensor. Os campos antigos do snapshot
 * ({@code status}, {@code sensores}) continuam descrevendo o reator principal.
 */
public final class EstadoAgregador implements IEventSubscriber {
    private static final int LIMITE_TIMELINE = 20;
    /** Amostras do histórico curto do T02 (sparkline) — contrato original. */
    private static final int LIMITE_HISTORICO = 6;
    /** Amostras por sensor no painel de monitoramento: ~5 min com o intervalo médio da simulação. */
    static final int LIMITE_SERIE = 60;

    private final Map<String, EstadoDoReator> reatores = new TreeMap<>();
    private final Map<String, AlarmeEstado> alarmes = new LinkedHashMap<>();
    private final Deque<EventoEstado> timeline = new ArrayDeque<>();
    private final AtomicInteger totalMedicoes = new AtomicInteger();
    private final AtomicInteger totalAlarmes = new AtomicInteger();
    private final AtomicInteger totalAuditoria = new AtomicInteger();
    private final AtomicBoolean tempoReal = new AtomicBoolean(false);

    /** Estado mutável de um reator — só existe dentro do agregador, sob o lock dele. */
    private static final class EstadoDoReator {
        final Map<Long, MedicaoRegistrada> ultimaPorSensor = new TreeMap<>();
        final Map<Long, Deque<Amostra>> seriePorSensor = new TreeMap<>();
        final Set<Long> sensoresEmAtencao = new HashSet<>();
        Instant atualizadoEm;
    }

    public EstadoAgregador(EventBus eventBus) {
        eventBus.assinarTodos(this);
    }

    @Override
    public synchronized void onEvento(EventoDominio evento) {
        totalAuditoria.incrementAndGet();
        registrarTimeline(evento);

        if (evento instanceof MedicaoRegistrada medicao) {
            totalMedicoes.incrementAndGet();
            EstadoDoReator reator = reator(medicao.reatorId());
            reator.sensoresEmAtencao.remove(medicao.sensorId());
            reator.ultimaPorSensor.put(medicao.sensorId(), medicao);
            Deque<Amostra> serie = reator.seriePorSensor.computeIfAbsent(medicao.sensorId(), id -> new ArrayDeque<>());
            serie.addLast(new Amostra(medicao.ocorridoEm(), medicao.valor()));
            while (serie.size() > LIMITE_SERIE) {
                serie.removeFirst();
            }
            reator.atualizadoEm = medicao.ocorridoEm();
        } else if (evento instanceof ObservacaoRegistrada observacao) {
            reator(observacao.reatorId()).sensoresEmAtencao.add(observacao.sensorId());
        } else if (evento instanceof AlarmeEmitido emitido) {
            totalAlarmes.incrementAndGet();
            alarmes.put(emitido.alarmeId(), new AlarmeEstado(
                emitido.alarmeId(),
                emitido.reatorId(),
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

    private EstadoDoReator reator(String reatorId) {
        return reatores.computeIfAbsent(EventoDominio.reatorOuPrincipal(reatorId), id -> new EstadoDoReator());
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
            calcularStatusPrincipal(),
            tempoReal.get(),
            Instant.now(),
            sensoresDoPrincipal(),
            new Contadores(totalMedicoes.get(), totalAlarmes.get(), totalAuditoria.get()),
            List.copyOf(alarmes.values()),
            List.copyOf(timeline),
            estadoDosReatores()
        );
    }

    /** Sensores do reator principal no formato original (histórico curto de 6 valores). */
    private List<SensorEstado> sensoresDoPrincipal() {
        EstadoDoReator principal = reatores.get(EventoDominio.REATOR_PRINCIPAL);
        if (principal == null) {
            return List.of();
        }
        List<SensorEstado> sensores = new ArrayList<>();
        for (MedicaoRegistrada medicao : principal.ultimaPorSensor.values()) {
            List<Double> valores = principal.seriePorSensor.get(medicao.sensorId()).stream()
                .map(Amostra::valor)
                .toList();
            List<Double> historico = valores.subList(Math.max(0, valores.size() - LIMITE_HISTORICO), valores.size());
            sensores.add(new SensorEstado(
                medicao.sensorId(),
                medicao.tipoSensor(),
                medicao.unidade(),
                medicao.valor(),
                medicao.limiteMinimo(),
                medicao.limiteMaximo(),
                medicao.ocorridoEm(),
                historico
            ));
        }
        return List.copyOf(sensores);
    }

    private List<ReatorEstado> estadoDosReatores() {
        List<ReatorEstado> lista = new ArrayList<>();
        for (Map.Entry<String, EstadoDoReator> entrada : reatores.entrySet()) {
            String reatorId = entrada.getKey();
            EstadoDoReator reator = entrada.getValue();
            List<SensorReatorEstado> sensores = new ArrayList<>();
            for (MedicaoRegistrada medicao : reator.ultimaPorSensor.values()) {
                sensores.add(new SensorReatorEstado(
                    medicao.sensorId(),
                    medicao.tipoSensor(),
                    medicao.unidade(),
                    medicao.valor(),
                    medicao.limiteMinimo(),
                    medicao.limiteMaximo(),
                    reator.sensoresEmAtencao.contains(medicao.sensorId()),
                    medicao.ocorridoEm(),
                    List.copyOf(reator.seriePorSensor.get(medicao.sensorId()))
                ));
            }
            int ativos = (int) alarmes.values().stream()
                .filter(a -> reatorId.equals(a.reatorId()) && "ATIVO".equals(a.status()))
                .count();
            lista.add(new ReatorEstado(reatorId, calcularNivel(sensores), reator.atualizadoEm, ativos, sensores));
        }
        return List.copyOf(lista);
    }

    /** Lâmpada do reator: o pior nível entre as leituras atuais dos seus sensores. */
    static StatusReator calcularNivel(List<SensorReatorEstado> sensores) {
        if (sensores.stream().anyMatch(SensorReatorEstado::foraDoLimite)) {
            return StatusReator.CRITICO;
        }
        if (sensores.stream().anyMatch(SensorReatorEstado::emAtencao)) {
            return StatusReator.ATENCAO;
        }
        return StatusReator.ESTAVEL;
    }

    /** Badge do T01: regra original, aplicada ao reator principal. */
    private StatusReator calcularStatusPrincipal() {
        List<AlarmeEstado> doPrincipal = alarmes.values().stream()
            .filter(a -> EventoDominio.REATOR_PRINCIPAL.equals(a.reatorId()))
            .toList();
        if (doPrincipal.stream().anyMatch(a -> "ATIVO".equals(a.status()))) {
            return StatusReator.CRITICO;
        }
        boolean reconhecidoPendente = doPrincipal.stream().anyMatch(a -> "RECONHECIDO".equals(a.status()));
        EstadoDoReator principal = reatores.get(EventoDominio.REATOR_PRINCIPAL);
        boolean observacaoPendente = principal != null && !principal.sensoresEmAtencao.isEmpty();
        return reconhecidoPendente || observacaoPendente ? StatusReator.ATENCAO : StatusReator.ESTAVEL;
    }
}
