package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.IEventSubscriber;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ponte entre o barramento do núcleo e o canal de push do Spring.
 *
 * <p>O {@code EventBus} vive neste processo (porta 8080) e o {@code SseEmitter} vive no
 * Spring (porta 8081). Este assinante fecha essa distância: a cada evento publicado, ele
 * encaminha o tipo e o snapshot para {@code POST /api/eventos/publicar}, que então empurra
 * para os navegadores conectados.
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

    @Override
    public void onEvento(EventoDominio evento) {
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
