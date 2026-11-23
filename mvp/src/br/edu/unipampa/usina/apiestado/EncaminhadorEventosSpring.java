package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.IEventSubscriber;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gatilho de push para dado crítico: ponte entre o barramento do núcleo e o canal SSE do Spring.
 *
 * <p>O {@code EventBus} vive neste processo (porta 8080) e o {@code SseEmitter} vive no
 * Spring (porta 8081). Este assinante fecha essa distância: quando é publicado um evento
 * <b>crítico</b> ({@link #TIPOS_CRITICOS}), ele encaminha o tipo e o snapshot para
 * {@code POST /api/eventos/publicar}, que empurra na hora para os navegadores conectados.
 *
 * <p><b>Issue #96 (Marco 2, pedido do professor em 16/11):</b> dado crítico vai por push imediato,
 * dado rotineiro continua por polling. Uma leitura fora da faixa segura vira {@code ALARME_EMITIDO}
 * (o {@code AlarmeFacade} avalia cada {@code MedicaoRegistrada}) e sai daqui sem esperar a próxima
 * consulta do front. Medição normal, observação, reconhecimento/resolução de alarme e acesso NÃO
 * são empurrados: o front busca {@code GET /api/estado} periodicamente.
 *
 * <p>É mais um assinante global, como auditoria e histórico — quem publica o evento não
 * sabe que ele existe.
 *
 * <p>O envio é assíncrono de propósito: {@code EventBus.publicar} chama os assinantes em
 * sequência, na mesma thread, então um POST bloqueante faria cada medição esperar a rede.
 * Se o Spring estiver fora do ar — cenário comum, já que o mvp roda sozinho em boa parte
 * da demo — a falha é silenciosa, com um único aviso no console.
 */
public final class EncaminhadorEventosSpring implements IEventSubscriber {
    private static final String DESTINO_PADRAO = "http://localhost:8081/api/eventos/publicar";

    /**
     * Eventos que não podem esperar o próximo polling: leitura fora da faixa segura (alarme), sensor
     * que parou de responder e protocolo de contingência (o alerta de tela cheia em todas as sessões,
     * #99, depende de chegar na hora).
     */
    public static final Set<String> TIPOS_CRITICOS = Set.of(
        "ALARME_EMITIDO",
        "FALHA_SENSOR_DETECTADA",
        "PROTOCOLO_CONTINGENCIA_ATIVADO",
        "PROTOCOLO_CONTINGENCIA_ENCERRADO"
    );

    private final EstadoAgregador estado;
    private final URI destino;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final AtomicBoolean jaAvisou = new AtomicBoolean(false);

    public EncaminhadorEventosSpring(EventBus eventBus, EstadoAgregador estado) {
        this(eventBus, estado, System.getProperty("usina.push.destino", DESTINO_PADRAO));
    }

    public EncaminhadorEventosSpring(EventBus eventBus, EstadoAgregador estado, String destino) {
        this.estado = estado;
        this.destino = URI.create(destino);
        eventBus.assinarTodos(this);
    }

    /** Se o evento dispara push imediato; o resto chega ao front pelo polling de {@code /api/estado}. */
    public static boolean isCritico(EventoDominio evento) {
        return evento != null && TIPOS_CRITICOS.contains(evento.tipo());
    }

    @Override
    public void onEvento(EventoDominio evento) {
        if (!isCritico(evento)) {
            return;
        }
        String corpo = "{\"tipo\":" + texto(evento.tipo())
            + ",\"estado\":" + EstadoJson.escrever(estado.snapshot()) + "}";

        HttpRequest requisicao = HttpRequest.newBuilder(destino)
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
            .build();

        cliente.sendAsync(requisicao, HttpResponse.BodyHandlers.discarding())
            .exceptionally(falha -> {
                avisarUmaVez();
                return null;
            });
    }

    private void avisarUmaVez() {
        if (jaAvisou.compareAndSet(false, true)) {
            System.out.println("[PUSH] Spring indisponivel em " + destino
                + " — eventos seguem no barramento, so nao sao empurrados para a UI.");
        }
    }

    private static String texto(String valor) {
        return "\"" + (valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }
}
