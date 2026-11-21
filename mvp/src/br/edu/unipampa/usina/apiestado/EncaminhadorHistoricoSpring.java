package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.historicooperacional.SnapshotOperacional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Ponte entre o histórico operacional do núcleo e a persistência em banco do Spring (RF16/RF17).
 *
 * <p>Mesmo padrão de {@link EncaminhadorEventosSpring}: o {@code HistoricoSubscriber} continua
 * gravando cada ciclo fechado no CSV local (nada muda aí) e manda uma cópia assíncrona pra cá,
 * que o Spring persiste em {@code historico_operacional} (Postgres/H2). Reaproveita a mesma
 * linha CSV já serializada por {@link SnapshotOperacional#linhaCsv()} como corpo da requisição —
 * o Spring faz o parse com a mesma convenção (campo a campo, separador {@code ;}), então não
 * existe um segundo formato de serialização pra manter sincronizado.
 *
 * <p>O envio é assíncrono e falha em silêncio se o Spring estiver fora do ar — mesma justificativa
 * de {@link EncaminhadorEventosSpring}: o histórico continua disponível localmente (CSV), só não
 * duplica no banco até o Spring voltar.
 */
public final class EncaminhadorHistoricoSpring implements Consumer<SnapshotOperacional> {
    private static final String DESTINO_PADRAO = "http://localhost:8081/api/historico/registrar";

    private final URI destino;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final AtomicBoolean jaAvisou = new AtomicBoolean(false);

    public EncaminhadorHistoricoSpring() {
        this(System.getProperty("usina.historico.destino", DESTINO_PADRAO));
    }

    public EncaminhadorHistoricoSpring(String destino) {
        this.destino = URI.create(destino);
    }

    @Override
    public void accept(SnapshotOperacional snapshot) {
        String corpo = "{\"linhaCsv\":" + texto(snapshot.linhaCsv()) + "}";

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
            System.out.println("[HISTORICO] Spring indisponivel em " + destino
                + " — historico segue gravado localmente (CSV), so nao duplica no banco.");
        }
    }

    private static String texto(String valor) {
        return "\"" + (valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }
}
