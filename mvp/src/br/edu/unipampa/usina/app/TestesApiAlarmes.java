package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.ApiEstadoHttpServer;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestesApiAlarmes {
    private static final Pattern ID_ALARME = Pattern.compile("\\\"alarmes\\\":\\[\\{\\\"id\\\":\\\"([^\\\"]+)\\\"");

    private TestesApiAlarmes() {}

    public static void main(String[] args) throws Exception {
        EventBus eventBus = new EventBus();
        EstadoAgregador estado = new EstadoAgregador(eventBus);
        AlarmeFacade alarmes = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        ReatorFacade reator = new ReatorFacade(eventBus);
        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0));

        int porta = obterPortaLivre();
        ApiEstadoHttpServer servidor = new ApiEstadoHttpServer(estado, alarmes, reator, porta);
        servidor.iniciar();

        try {
            HttpClient cliente = HttpClient.newHttpClient();
            String base = "http://localhost:" + porta;

            HttpResponse<String> disparo = post(cliente, base + "/api/demo/anomalia");
            exigir(disparo.statusCode() == 200, "Cenario critico deve responder HTTP 200.");
            exigir(disparo.body().contains("\"status\":\"CRITICO\""),
                "Cenario critico deve retornar status CRITICO.");

            Matcher matcher = ID_ALARME.matcher(disparo.body());
            exigir(matcher.find(), "Resposta deve conter o ID do alarme emitido.");
            String id = matcher.group(1);

            HttpResponse<String> reconhecimento = post(
                cliente,
                base + "/api/alarmes/" + id + "/reconhecer"
            );
            exigir(reconhecimento.statusCode() == 200, "ACK deve responder HTTP 200.");
            exigir(reconhecimento.body().contains("\"status\":\"ATENCAO\""),
                "ACK deve atualizar o snapshot para ATENCAO.");
            exigir(reconhecimento.body().contains("\"operador\":\"Operador de Reator (UI Web)\""),
                "ACK deve registrar o operador no estado.");

            HttpResponse<String> resolucao = post(
                cliente,
                base + "/api/alarmes/" + id + "/resolver"
            );
            exigir(resolucao.statusCode() == 200, "Resolver deve responder HTTP 200.");
            exigir(resolucao.body().contains("\"status\":\"RESOLVIDO\""),
                "Resolver deve atualizar o alarme para RESOLVIDO.");
            exigir(resolucao.body().contains("\"tipo\":\"ALARME_RESOLVIDO\""),
                "Resolver deve publicar ALARME_RESOLVIDO na timeline.");

            System.out.println("[SUCESSO] Ciclo HTTP de alarme: anomalia -> ACK -> resolver.");
        } finally {
            servidor.parar();
        }
    }

    private static HttpResponse<String> post(HttpClient cliente, String endereco) throws IOException, InterruptedException {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(endereco))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private static int obterPortaLivre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }
}