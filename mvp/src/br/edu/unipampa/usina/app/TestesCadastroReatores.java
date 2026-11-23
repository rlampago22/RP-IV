package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.controlereator.CadastroReatores;
import br.edu.unipampa.usina.controlereator.CadastroReatores.ReatorCadastrado;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.CadastroReatorAtualizado;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.persistencia.ConexaoBanco;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Testes do cadastro de reatores em banco (issue #96): carga inicial com os valores que antes eram
 * fixos no código, idempotência, reator desativado e o simulador usando os setpoints do banco.
 */
public final class TestesCadastroReatores {
    private static final double TOLERANCIA = 1e-9;

    private TestesCadastroReatores() {}

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println(" TESTES DO CADASTRO DE REATORES EM BANCO (#96)");
        System.out.println("================================================================================");

        cargaInicial(ConexaoBanco.emMemoria("testes-cadastro-carga-" + System.nanoTime()));
        cadastroAlteradoNoBanco(ConexaoBanco.emMemoria("testes-cadastro-alterado-" + System.nanoTime()));
        simuladorUsaSetpointsDoBanco(ConexaoBanco.emMemoria("testes-cadastro-simulador-" + System.nanoTime()));
        cadastroAplicadoSemReiniciar(ConexaoBanco.emMemoria("testes-cadastro-vivo-" + System.nanoTime()));
        picoSegueAFaixaCadastrada();

        System.out.println("\n[SUCESSO] Testes do cadastro de reatores passaram com 100% de exito!");
    }

    /** Banco vazio: recebe os 5 reatores com exatamente os valores que estavam no código. */
    private static void cargaInicial(ConexaoBanco banco) {
        List<ReatorCadastrado> reatores;
        try (CadastroReatores cadastro = new CadastroReatores(banco)) {
            reatores = cadastro.listarAtivos();
        }
        exigir(reatores.size() == 5, "Banco vazio deve receber a carga inicial com 5 reatores.");
        exigir(reatores.stream().map(ReatorCadastrado::id).toList().equals(List.of("R-01", "R-02", "R-03", "R-04", "R-05")),
            "Reatores devem vir na ordem do cadastro, R-01 (principal) primeiro.");
        exigir("Reator-03".equals(reatores.get(2).nome()), "Nome do R-03 deve ser Reator-03 (mesmo rotulo do front).");

        for (ReatorCadastrado reator : reatores) {
            exigirSilencioso(reator.sensores().size() == 4, reator.id() + " deve ter os 4 sensores RF-1.");
            Sensor radiacao = sensor(reator, 3L);
            exigirSilencioso("RADIACAO".equals(radiacao.tipo()) && radiacao.limiteMaximo() == 5.0
                    && radiacao.atencaoMaxima() == 4.2,
                reator.id() + ": faixa da radiacao deve ser a mesma de antes (limite 5,0, atencao 4,2).");
        }
        exigir(true, "Os 5 reatores tem os 4 sensores RF-1 com as faixas de antes.");

        // Setpoints antigos do SimuladorReatores: deslocamento (indice - 2) * 0,5.
        exigir(perto(reatores.get(0).setpoints().get(1L), 307.0), "Setpoint de temperatura do R-01 deve ser 307,0.");
        exigir(perto(reatores.get(2).setpoints().get(3L), 2.4), "Setpoint de radiacao do R-03 deve ser 2,4.");
        exigir(perto(reatores.get(4).setpoints().get(4L), 1120.0), "Setpoint de fluxo do R-05 deve ser 1120,0.");
        exigir(perto(reatores.get(1).setpoints().get(2L), 149.5), "Setpoint de pressao do R-02 deve ser 149,5.");
        exigir(reatores.get(1).setpoints().get(3L) == 2.325,
            "Setpoint de radiacao do R-02 deve ser exatamente 2,325 (sem ruido de ponto flutuante, igual a V8).");

        // Abrir de novo no mesmo banco não duplica nada.
        try (CadastroReatores outraVez = new CadastroReatores(banco)) {
            exigir(outraVez.listarAtivos().size() == 5, "Reabrir o cadastro nao deve duplicar a carga inicial.");
        }
        exigir(contar(banco, "nucleo.sensor_reator") == 20, "Devem existir 20 sensores (5 x 4), sem duplicatas.");
    }

    /** O que vale é o banco: reator desativado some, setpoint alterado é respeitado, nada volta ao padrão. */
    private static void cadastroAlteradoNoBanco(ConexaoBanco banco) {
        new CadastroReatores(banco).close();
        executar(banco, "UPDATE nucleo.reator SET ativo = FALSE WHERE id = 'R-04'");
        executar(banco, "UPDATE nucleo.sensor_reator SET setpoint = 1000.0 WHERE reator_id = 'R-02' AND sensor_id = 4");

        try (CadastroReatores cadastro = new CadastroReatores(banco)) {
            List<ReatorCadastrado> reatores = cadastro.listarAtivos();
            exigir(reatores.size() == 4 && reatores.stream().noneMatch(r -> r.id().equals("R-04")),
                "Reator desativado no banco nao deve ser carregado (e a carga inicial nao o recria).");
            exigir(perto(reatores.get(1).setpoints().get(4L), 1000.0),
                "Setpoint alterado no banco deve ser o usado (R-02, fluxo 1000,0).");
        }

        executar(banco, "INSERT INTO nucleo.reator (id, nome, ordem, ativo) VALUES ('R-06', 'Reator-06', 6, TRUE)");
        executar(banco, "INSERT INTO nucleo.sensor_reator VALUES ('R-06', 1, 'TEMPERATURA', 'Celsius', 0, 350, 20, 325, 300)");
        try (CadastroReatores cadastro = new CadastroReatores(banco)) {
            List<ReatorCadastrado> reatores = cadastro.listarAtivos();
            ReatorCadastrado novo = reatores.get(reatores.size() - 1);
            exigir("R-06".equals(novo.id()) && novo.sensores().size() == 1,
                "Reator novo cadastrado direto no banco deve aparecer, com so os sensores dele.");
        }
    }

    /** Leitura inicial do simulador sai exatamente nos setpoints do banco, reator por reator. */
    private static void simuladorUsaSetpointsDoBanco(ConexaoBanco banco) {
        new CadastroReatores(banco).close();
        executar(banco, "UPDATE nucleo.sensor_reator SET setpoint = 222.0 WHERE reator_id = 'R-03' AND sensor_id = 1");

        List<ReatorCadastrado> cadastrados;
        try (CadastroReatores cadastro = new CadastroReatores(banco)) {
            cadastrados = cadastro.listarAtivos();
        }

        EventBus eventBus = new EventBus();
        List<MedicaoRegistrada> medicoes = new ArrayList<>();
        eventBus.assinarTodos(evento -> {
            if (evento instanceof MedicaoRegistrada medicao) {
                medicoes.add(medicao);
            }
        });

        List<ReatorFacade> facades = new ArrayList<>();
        Map<String, Map<Long, Double>> setpoints = new LinkedHashMap<>();
        for (ReatorCadastrado cadastrado : cadastrados) {
            ReatorFacade facade = new ReatorFacade(eventBus, cadastrado.id());
            cadastrado.sensores().forEach(facade::registrarSensor);
            facades.add(facade);
            setpoints.put(cadastrado.id(), cadastrado.setpoints());
        }

        SimuladorReatores simulador = new SimuladorReatores(facades, setpoints, () -> false);
        simulador.semear();
        simulador.parar();

        exigir(medicoes.size() == 20, "Leitura inicial deve publicar 20 medicoes (5 reatores x 4 sensores).");
        MedicaoRegistrada temperaturaR03 = medicoes.stream()
            .filter(m -> m.reatorId().equals("R-03") && m.sensorId() == 1L)
            .findFirst()
            .orElseThrow();
        exigir(perto(temperaturaR03.valor(), 222.0), "Simulador deve partir do setpoint do banco (R-03, temperatura 222,0).");

        boolean recusou = false;
        try {
            new SimuladorReatores(facades, Map.of(), () -> false);
        } catch (IllegalArgumentException esperado) {
            recusou = true;
        }
        exigir(recusou, "Simulador deve recusar reator sem setpoints cadastrados.");
    }

    /**
     * Tela Reatores com o núcleo no ar: o sincronizador relê o banco e inclui, ajusta e retira
     * reatores sem reiniciar — simulador, rotas de cenário, painel e auditoria acompanham.
     */
    private static void cadastroAplicadoSemReiniciar(ConexaoBanco banco) {
        EventBus eventBus = new EventBus();
        EstadoAgregador estado = new EstadoAgregador(eventBus);
        List<CadastroReatorAtualizado> eventos = new ArrayList<>();
        List<MedicaoRegistrada> medicoes = new ArrayList<>();
        eventBus.assinarTodos(evento -> {
            if (evento instanceof CadastroReatorAtualizado cadastroAtualizado) {
                eventos.add(cadastroAtualizado);
            } else if (evento instanceof MedicaoRegistrada medicao) {
                medicoes.add(medicao);
            }
        });

        CadastroReatores cadastro = new CadastroReatores(banco);
        List<ReatorCadastrado> iniciais = cadastro.listarAtivos();
        Map<String, ReatorFacade> facades = new LinkedHashMap<>();
        Map<String, Map<Long, Double>> setpoints = new LinkedHashMap<>();
        for (ReatorCadastrado cadastrado : iniciais) {
            ReatorFacade facade = new ReatorFacade(eventBus, cadastrado.id());
            cadastrado.sensores().forEach(facade::registrarSensor);
            facades.put(cadastrado.id(), facade);
            setpoints.put(cadastrado.id(), cadastrado.setpoints());
        }
        SimuladorReatores simulador = new SimuladorReatores(List.copyOf(facades.values()), setpoints, () -> false);
        simulador.semear();
        List<Map<String, ReatorFacade>> rotas = new ArrayList<>();
        SincronizadorCadastroReatores sincronizador =
            new SincronizadorCadastroReatores(cadastro, eventBus, simulador, iniciais, facades, rotas::add);

        exigir(sincronizador.sincronizar() == 0 && eventos.isEmpty(), "Cadastro sem mudanca: sincronizar nao faz nada.");

        // Inclusão (o que a tela faz ao cadastrar o R-06).
        executar(banco, "INSERT INTO nucleo.reator (id, nome, ordem, ativo) VALUES ('R-06', 'Reator-06', 6, TRUE)");
        executar(banco, "INSERT INTO nucleo.sensor_reator VALUES ('R-06', 1, 'TEMPERATURA', 'Celsius', 0, 350, 20, 325, 300)");
        executar(banco, "INSERT INTO nucleo.sensor_reator VALUES ('R-06', 3, 'RADIACAO', 'mSv/h', 0, 5, 0.1, 4.2, 2.0)");
        medicoes.clear();
        exigir(sincronizador.sincronizar() == 1, "Reator novo no banco: uma mudanca aplicada.");
        exigir(simulador.reatoresSimulados().contains("R-06"), "R-06 deve entrar no simulador sem reiniciar o nucleo.");
        exigir(medicoes.size() == 2 && medicoes.stream().allMatch(m -> m.reatorId().equals("R-06")),
            "R-06 deve publicar a leitura inicial dos 2 sensores dele na hora.");
        exigir(ultimo(rotas).containsKey("R-06"), "R-06 deve entrar nas rotas de cenario (?reator=R-06).");
        exigir(eventos.get(0).acao() == CadastroReatorAtualizado.Acao.INCLUIDO && eventos.get(0).reatorId().equals("R-06"),
            "Inclusao deve publicar CADASTRO_REATOR_ATUALIZADO (INCLUIDO) no EventBus (auditoria).");
        exigir(estado.snapshot().reatores().stream().anyMatch(r -> r.id().equals("R-06")),
            "Painel (/api/estado) deve mostrar o R-06.");

        // Ajuste de faixa e setpoint.
        executar(banco, "UPDATE nucleo.sensor_reator SET limite_maximo = 340, setpoint = 280 WHERE reator_id = 'R-06' AND sensor_id = 1");
        exigir(sincronizador.sincronizar() == 1, "Faixa/setpoint alterados: uma mudanca aplicada.");
        exigir(ultimo(rotas).get("R-06").obterSensor(1L).limiteMaximo() == 340.0,
            "Facade do R-06 deve usar o limite novo (340) a partir da proxima leitura.");
        exigir(eventos.get(1).acao() == CadastroReatorAtualizado.Acao.ALTERADO, "Ajuste deve publicar ALTERADO.");

        // Retirada.
        executar(banco, "UPDATE nucleo.reator SET ativo = FALSE WHERE id = 'R-02'");
        exigir(sincronizador.sincronizar() == 1, "Reator desativado: uma mudanca aplicada.");
        exigir(!simulador.reatoresSimulados().contains("R-02"), "R-02 retirado deve sair do simulador.");
        exigir(!ultimo(rotas).containsKey("R-02"), "R-02 retirado deve sair das rotas de cenario.");
        exigir(estado.snapshot().reatores().stream().noneMatch(r -> r.id().equals("R-02")),
            "R-02 retirado deve sair do painel.");
        exigir(eventos.get(2).acao() == CadastroReatorAtualizado.Acao.RETIRADO, "Retirada deve publicar RETIRADO.");

        // Devolvido à operação.
        executar(banco, "UPDATE nucleo.reator SET ativo = TRUE WHERE id = 'R-02'");
        exigir(sincronizador.sincronizar() == 1 && simulador.reatoresSimulados().contains("R-02")
                && estado.snapshot().reatores().stream().anyMatch(r -> r.id().equals("R-02")),
            "R-02 reativado deve voltar ao simulador e ao painel.");

        simulador.parar();
        cadastro.close();
    }

    /** Pico de radiação calculado da faixa cadastrada: o vermelho passa do limite só no topo. */
    private static void picoSegueAFaixaCadastrada() {
        java.util.Random aleatorio = new java.util.Random(42);
        for (int i = 0; i < 200; i++) {
            // Faixa diferente da padrão, como se o Administrador tivesse mudado: atenção 8, limite 10.
            List<Double> critico = SimuladorReatores.formaDoPico(4.0, 8.0, 10.0, true, aleatorio);
            exigirSilencioso(critico.get(2) > 10.0 && critico.stream().filter(v -> v > 10.0).count() == 1,
                "Pico critico deve passar do limite cadastrado (10) em um unico ponto: " + critico);
            List<Double> atencao = SimuladorReatores.formaDoPico(4.0, 8.0, 10.0, false, aleatorio);
            exigirSilencioso(atencao.get(2) > 8.0 && atencao.stream().allMatch(v -> v <= 10.0),
                "Pico de atencao deve ficar entre a atencao (8) e o limite (10): " + atencao);
        }
        exigir(true, "Pico de radiacao segue a faixa cadastrada (200 picos de cada tipo com atencao 8 / limite 10).");

        java.util.Random fixo = new java.util.Random(7);
        double topoPadrao = SimuladorReatores.formaDoPico(2.4, 4.2, 5.0, true, fixo).get(2);
        exigir(topoPadrao >= 5.15 && topoPadrao <= 5.5, "Com a faixa padrao, o topo vermelho continua entre 5,15 e 5,5.");
    }

    private static <T> T ultimo(List<T> lista) {
        return lista.get(lista.size() - 1);
    }

    private static Sensor sensor(ReatorCadastrado reator, long id) {
        return reator.sensores().stream().filter(s -> s.id() == id).findFirst().orElseThrow();
    }

    private static boolean perto(Double valor, double esperado) {
        return valor != null && Math.abs(valor - esperado) < TOLERANCIA;
    }

    private static void executar(ConexaoBanco banco, String comando) {
        try (Connection c = banco.abrir(); Statement sql = c.createStatement()) {
            sql.execute(comando);
        } catch (SQLException error) {
            throw new AssertionError("Falha ao preparar o banco: " + error.getMessage(), error);
        }
    }

    private static long contar(ConexaoBanco banco, String tabela) {
        try (Connection c = banco.abrir(); Statement sql = c.createStatement();
             var linhas = sql.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            linhas.next();
            return linhas.getLong(1);
        } catch (SQLException error) {
            throw new AssertionError("Falha ao contar " + tabela + ": " + error.getMessage(), error);
        }
    }

    private static void exigirSilencioso(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        exigirSilencioso(condicao, mensagem);
        System.out.println("[OK] " + mensagem);
    }
}
