package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.EncaminhadorEventosSpring;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Gatilho de push para leitura crítica (issue #96): só evento crítico é empurrado para a Central;
 * medição rotineira fica para o polling de {@code /api/estado}. Um servidor HTTP local faz o papel
 * de {@code POST /api/eventos/publicar} do Spring e registra tudo o que chega.
 */
public final class TestesPushCritico {
    /** Tempo para concluir que NADA foi enviado (o envio é assíncrono). */
    private static final long ESPERA_SILENCIO_MS = 600;
    /** Tempo máximo para o push crítico chegar. */
    private static final long ESPERA_PUSH_MS = 3_000;

    private TestesPushCritico() {}

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println(" TESTES DO PUSH DE LEITURA CRITICA (#96)");
        System.out.println("================================================================================");

        classificacao();
        gatilhoPontaAPonta();

        System.out.println("\n[SUCESSO] Testes do push de leitura critica passaram com 100% de exito!");
        // O HttpClient do encaminhador mantém threads vivas; encerra explicitamente.
        System.exit(0);
    }

    private static void classificacao() {
        for (String critico : new String[] {"ALARME_EMITIDO", "FALHA_SENSOR_DETECTADA",
            "PROTOCOLO_CONTINGENCIA_ATIVADO", "PROTOCOLO_CONTINGENCIA_ENCERRADO"}) {
            exigir(EncaminhadorEventosSpring.TIPOS_CRITICOS.contains(critico), critico + " deve ir por push imediato.");
        }
        for (String rotina : new String[] {"MEDICAO_REGISTRADA", "OBSERVACAO_REGISTRADA",
            "ALARME_RECONHECIDO", "ALARME_RESOLVIDO", "ACESSO_REGISTRADO"}) {
            exigir(!EncaminhadorEventosSpring.TIPOS_CRITICOS.contains(rotina), rotina + " deve ficar para o polling.");
        }
    }

    private static void gatilhoPontaAPonta() throws IOException, InterruptedException {
        BlockingQueue<String> recebidos = new LinkedBlockingQueue<>();
        HttpServer central = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        central.createContext("/api/eventos/publicar", troca -> {
            try (InputStream corpo = troca.getRequestBody()) {
                recebidos.add(new String(corpo.readAllBytes(), StandardCharsets.UTF_8));
            }
            troca.sendResponseHeaders(204, -1);
            troca.close();
        });
        central.start();
        try {
            String destino = "http://127.0.0.1:" + central.getAddress().getPort() + "/api/eventos/publicar";

            EventBus eventBus = new EventBus();
            new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
            EstadoAgregador estado = new EstadoAgregador(eventBus);
            new EncaminhadorEventosSpring(eventBus, estado, destino);

            ReatorFacade reator = new ReatorFacade(eventBus, "R-02");
            reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0));
            reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.2));

            // 1. Leituras rotineiras (faixa ideal e faixa de atenção): nada vai por push.
            reator.receberLeitura(1L, 310.0);
            reator.receberLeitura(3L, 2.4);
            reator.receberLeitura(3L, 4.5);
            String inesperado = recebidos.poll(ESPERA_SILENCIO_MS, TimeUnit.MILLISECONDS);
            exigir(inesperado == null, "Leitura normal e de observacao nao devem disparar push (ficam para o polling).");

            // 2. Leitura fora da faixa segura: o alarme sai por push na hora, já com o estado atualizado.
            reator.receberLeitura(3L, 5.3);
            String push = recebidos.poll(ESPERA_PUSH_MS, TimeUnit.MILLISECONDS);
            exigir(push != null, "Leitura fora da faixa segura deve disparar push imediato.");
            exigir(push.startsWith("{\"tipo\":\"ALARME_EMITIDO\""), "O push deve ser do tipo ALARME_EMITIDO.");
            exigir(push.contains("\"reatorId\":\"R-02\"") && push.contains("\"ATIVO\""),
                "O snapshot empurrado ja deve trazer o alarme ativo do R-02.");

            // 3. A medição que gerou o alarme não gera um segundo push: só o alarme é empurrado.
            String extra = recebidos.poll(ESPERA_SILENCIO_MS, TimeUnit.MILLISECONDS);
            exigir(extra == null, "A leitura critica deve gerar 1 push (o alarme), nao 1 por evento do barramento.");
        } finally {
            central.stop(0);
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }
}
