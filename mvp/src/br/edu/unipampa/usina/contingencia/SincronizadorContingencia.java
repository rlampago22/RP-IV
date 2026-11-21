package br.edu.unipampa.usina.contingencia;

import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.ProtocoloContingenciaAtivado;
import br.edu.unipampa.usina.infraestruturaeventos.ProtocoloContingenciaEncerrado;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Liga o protocolo de contingência da Central (backend, com digital) ao {@code EventBus} do núcleo
 * (issue #99). A cada consulta compara o que está ativo lá com o que o núcleo já sabe e publica
 * {@link ProtocoloContingenciaAtivado} / {@link ProtocoloContingenciaEncerrado} — daí em diante é
 * EDA puro: a auditoria grava, o painel mostra, o push leva ao navegador e o
 * {@code EstadoAgregador} põe o núcleo em modo de contingência, sem nenhum deles saber de onde veio.
 *
 * <p>Se a Central sair do ar o núcleo NÃO destrava: mantém o último estado conhecido até conseguir
 * consultar de novo (falha segura). O primeiro contato depois de subir publica a ativação dos
 * protocolos que já estavam em vigor.
 */
public final class SincronizadorContingencia implements AutoCloseable {
    private static final String NAO_INFORMADO = "nao informado";

    private final EventBus eventBus;
    private final FonteContingencia fonte;
    private final Map<Long, ProtocoloRemoto> ativos = new LinkedHashMap<>();
    private ScheduledExecutorService agendador;
    private boolean avisouIndisponivel;

    public SincronizadorContingencia(EventBus eventBus, FonteContingencia fonte) {
        this.eventBus = eventBus;
        this.fonte = fonte;
    }

    /**
     * Uma rodada de sincronização.
     *
     * @return {@code false} se a Central não respondeu (estado anterior mantido)
     */
    public synchronized boolean sincronizar() {
        List<ProtocoloRemoto> remotos;
        try {
            remotos = fonte.consultar();
        } catch (IOException | RuntimeException falha) {
            if (!avisouIndisponivel) {
                avisouIndisponivel = true;
                System.out.println("[CONTINGENCIA] Central indisponível (" + falha.getMessage()
                    + "); mantendo o último estado conhecido: " + ativos.size() + " protocolo(s) ativo(s).");
            }
            return false;
        }
        if (avisouIndisponivel) {
            avisouIndisponivel = false;
            System.out.println("[CONTINGENCIA] Central respondendo de novo.");
        }

        Map<Long, ProtocoloRemoto> porId = new HashMap<>();
        remotos.forEach(remoto -> porId.put(remoto.emergenciaId(), remoto));

        for (ProtocoloRemoto remoto : remotos) {
            if (remoto.ativo() && !ativos.containsKey(remoto.emergenciaId())) {
                ativos.put(remoto.emergenciaId(), remoto);
                eventBus.publicar(new ProtocoloContingenciaAtivado(
                    Instant.now(),
                    remoto.emergenciaId(),
                    remoto.protocolo(),
                    valorOu(remoto.nome(), remoto.protocolo()),
                    valorOu(remoto.ativadoPor(), NAO_INFORMADO)
                ));
            }
        }
        for (ProtocoloRemoto conhecido : List.copyOf(ativos.values())) {
            ProtocoloRemoto atual = porId.get(conhecido.emergenciaId());
            if (atual == null || !atual.ativo()) {
                ativos.remove(conhecido.emergenciaId());
                eventBus.publicar(new ProtocoloContingenciaEncerrado(
                    Instant.now(),
                    conhecido.emergenciaId(),
                    conhecido.protocolo(),
                    valorOu(atual == null ? null : atual.encerradoPor(), NAO_INFORMADO)
                ));
            }
        }
        return true;
    }

    /** Consulta em segundo plano a cada {@code intervalo} (thread daemon: não segura o processo). */
    public synchronized void iniciar(Duration intervalo) {
        if (agendador != null) {
            return;
        }
        agendador = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "sincronizador-contingencia");
            thread.setDaemon(true);
            return thread;
        });
        agendador.scheduleWithFixedDelay(this::sincronizar, 0, intervalo.toMillis(), TimeUnit.MILLISECONDS);
    }

    public synchronized List<ProtocoloRemoto> ativosConhecidos() {
        return List.copyOf(ativos.values());
    }

    @Override
    public synchronized void close() {
        if (agendador != null) {
            agendador.shutdownNow();
            agendador = null;
        }
    }

    private static String valorOu(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
