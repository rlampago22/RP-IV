package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.ApiEstadoHttpServer;
import br.edu.unipampa.usina.apiestado.EncaminhadorEventosSpring;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.segurancaacesso.AcessoFacade;
import br.edu.unipampa.usina.historicooperacional.HistoricoSubscriber;
import br.edu.unipampa.usina.historicooperacional.OperadorDeServico;
import br.edu.unipampa.usina.historicooperacional.RegistroHistorico;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sobe o backend EDA (EventBus + facades) e expõe {@code /api/estado} para o frontend React
 * (T01 Overview, Opção A). Contrato: docs/api-estado-contrato.md.
 */
public final class ApiEstadoMain {
    /** Padrão 8080; {@code -Dusina.api.porta=...} sobe outra instância sem disputar a porta. */
    private static final int PORTA = Integer.getInteger("usina.api.porta", 8080);
    /** Reatores da usina. O primeiro é o principal: cenários do T05, T01, T02 e histórico. */
    private static final List<String> REATORES = List.of("R-01", "R-02", "R-03", "R-04", "R-05");

    private ApiEstadoMain() {}

    public static void main(String[] args) throws IOException {
        EventBus eventBus = new EventBus();
        RegistroAuditoria registro = new RegistroAuditoria(Path.of("dados", "auditoria.log"));
        new AuditoriaSubscriber(eventBus, registro);

        RegistroHistorico historico = new RegistroHistorico(Path.of("dados", "historico-operacional.csv"));
        OperadorDeServico operadorDeServico = new OperadorDeServico();
        new HistoricoSubscriber(eventBus, historico, operadorDeServico);

        AlarmeFacade alarmeFacade = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        AcessoFacade acesso = new AcessoFacade(eventBus);
        EstadoAgregador estado = new EstadoAgregador(eventBus);

        // Depois do EstadoAgregador de propósito: o EventBus chama os assinantes globais na
        // ordem de inscrição, então o snapshot encaminhado já inclui o evento que o disparou.
        new EncaminhadorEventosSpring(eventBus, estado);

        // Cada reator tem a sua facade (e o seu repositório de medições), todas no mesmo
        // barramento — alarmes, auditoria e o painel observam os 5 sem saber quantos são.
        List<ReatorFacade> reatores = new ArrayList<>();
        for (String reatorId : REATORES) {
            ReatorFacade reator = new ReatorFacade(eventBus, reatorId);
            registrarSensoresRf1(reator);
            reatores.add(reator);
        }
        ReatorFacade principal = reatores.get(0);

        // Leitura inicial de todos: o painel já abre com os 5 reatores e o T01 com os 4 parâmetros RF-1.
        SimuladorReatores simulador = new SimuladorReatores(reatores, estado::isTempoRealAtivo);
        simulador.semear();

        ApiEstadoHttpServer servidor =
            new ApiEstadoHttpServer(estado, alarmeFacade, principal, acesso, historico, operadorDeServico, PORTA);
        Map<String, ReatorFacade> porId = new LinkedHashMap<>();
        reatores.forEach(reator -> porId.put(reator.getReatorId(), reator));
        servidor.registrarReatores(porId, reatorId -> simulador.segurar(reatorId, SimuladorReatores.SEGURAR_CENARIO_MS));
        servidor.iniciar();

        // Só publica enquanto o tempo real estiver ligado (POST /api/tempo-real/iniciar).
        simulador.iniciar();

        System.out.println("[API ESTADO] " + REATORES.size() + " reatores simulados: " + String.join(", ", REATORES));
        System.out.println("[API ESTADO] Pressione CTRL+C para encerrar.");
    }

    /** Os 4 parâmetros RF-1, com as mesmas faixas em todos os reatores. */
    private static void registrarSensoresRf1(ReatorFacade reator) {
        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 155.0));
        reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.2));
        reator.registrarSensor(new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0));
    }
}
