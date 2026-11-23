package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.ApiEstadoHttpServer;
import br.edu.unipampa.usina.apiestado.EncaminhadorEventosSpring;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import br.edu.unipampa.usina.contingencia.FonteHttpContingencia;
import br.edu.unipampa.usina.contingencia.SincronizadorContingencia;
import br.edu.unipampa.usina.controlereator.CadastroReatores;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.segurancaacesso.AcessoFacade;
import br.edu.unipampa.usina.historicooperacional.HistoricoSubscriber;
import br.edu.unipampa.usina.historicooperacional.OperadorDeServico;
import br.edu.unipampa.usina.historicooperacional.RegistroHistorico;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;

import java.io.IOException;
import java.time.Duration;
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
    /** De quanto em quanto tempo o núcleo relê o cadastro de reatores (#96). */
    private static final Duration INTERVALO_CADASTRO = Duration.ofSeconds(2);

    private ApiEstadoMain() {}

    public static void main(String[] args) throws IOException {
        EventBus eventBus = new EventBus();
        RegistroAuditoria registro = RegistroAuditoria.abrirPadrao();
        new AuditoriaSubscriber(eventBus, registro);

        RegistroHistorico historico = RegistroHistorico.abrirPadrao();
        OperadorDeServico operadorDeServico = new OperadorDeServico();
        new HistoricoSubscriber(eventBus, historico, operadorDeServico);

        AlarmeFacade alarmeFacade = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        AcessoFacade acesso = new AcessoFacade(eventBus);
        EstadoAgregador estado = new EstadoAgregador(eventBus);

        // Depois do EstadoAgregador de propósito: o EventBus chama os assinantes globais na
        // ordem de inscrição, então o snapshot encaminhado já inclui o evento que o disparou.
        new EncaminhadorEventosSpring(eventBus, estado);

        // #96: quais reatores existem, os sensores de cada um, as faixas e os setpoints vêm do
        // cadastro no banco (nucleo.reator / nucleo.sensor_reator), não mais do código.
        // A conexão fica aberta: o sincronizador relê o cadastro durante toda a execução.
        CadastroReatores cadastro = CadastroReatores.abrirPadrao();
        List<CadastroReatores.ReatorCadastrado> cadastrados = cadastro.listarAtivos();
        if (cadastrados.isEmpty()) {
            throw new IllegalStateException("Nenhum reator ativo em " + CadastroReatores.TABELA_REATOR + ".");
        }

        // Cada reator tem a sua facade (e o seu repositório de medições), todas no mesmo
        // barramento — alarmes, auditoria e o painel observam todos sem saber quantos são.
        List<ReatorFacade> reatores = new ArrayList<>();
        Map<String, Map<Long, Double>> setpoints = new LinkedHashMap<>();
        for (CadastroReatores.ReatorCadastrado cadastrado : cadastrados) {
            ReatorFacade reator = new ReatorFacade(eventBus, cadastrado.id());
            cadastrado.sensores().forEach(reator::registrarSensor);
            reatores.add(reator);
            setpoints.put(cadastrado.id(), cadastrado.setpoints());
        }
        // O primeiro na ordem do cadastro é o principal: cenários do T05, T01, T02 e histórico.
        ReatorFacade principal = reatores.get(0);

        // Leitura inicial de todos: o painel já abre com todos os reatores e o T01 com os parâmetros RF-1.
        SimuladorReatores simulador = new SimuladorReatores(reatores, setpoints, estado::isTempoRealAtivo);
        simulador.semear();

        ApiEstadoHttpServer servidor =
            new ApiEstadoHttpServer(estado, alarmeFacade, principal, acesso, historico, operadorDeServico, registro, PORTA);
        Map<String, ReatorFacade> porId = new LinkedHashMap<>();
        reatores.forEach(reator -> porId.put(reator.getReatorId(), reator));
        servidor.registrarReatores(porId, reatorId -> simulador.segurar(reatorId, SimuladorReatores.SEGURAR_CENARIO_MS));
        servidor.iniciar();

        // Só publica enquanto o tempo real estiver ligado (POST /api/tempo-real/iniciar).
        simulador.iniciar();

        // #96: reator incluído, ajustado ou retirado na tela Reatores (Central, 8081) entra aqui em
        // até INTERVALO_CADASTRO, sem reiniciar o núcleo.
        new SincronizadorCadastroReatores(cadastro, eventBus, simulador, cadastrados, porId, servidor::atualizarReatores)
            .iniciar(INTERVALO_CADASTRO);

        // RF10 / #99: o protocolo de contingência é ativado na Central (8081, com digital). Aqui o
        // núcleo consulta a Central a cada segundo e publica a ativação/encerramento no EventBus;
        // o EstadoAgregador pausa a simulação e as rotas de rotina e novos acessos ficam travados.
        acesso.bloquearDuranteContingencia(estado::isContingenciaAtiva);
        FonteHttpContingencia central = new FonteHttpContingencia();
        new SincronizadorContingencia(eventBus, central).iniciar(Duration.ofSeconds(1));
        System.out.println("[CONTINGENCIA] Acompanhando protocolos em " + central.getOrigem());

        System.out.println("[API ESTADO] " + reatores.size() + " reatores (cadastro em " + CadastroReatores.TABELA_REATOR
            + "): " + String.join(", ", porId.keySet()));
        System.out.println("[API ESTADO] Pressione CTRL+C para encerrar.");
    }
}
