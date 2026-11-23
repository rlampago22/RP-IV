package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.controlereator.CadastroReatores;
import br.edu.unipampa.usina.controlereator.CadastroReatores.ReatorCadastrado;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.CadastroReatorAtualizado;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Issue #96 — mantém o núcleo igual ao cadastro de reatores do banco, sem reiniciar.
 *
 * <p>A tela Reatores grava pela Central (8081) em {@code nucleo.reator} / {@code nucleo.sensor_reator};
 * este sincronizador relê o cadastro a cada ciclo (mesma ideia do {@code SincronizadorContingencia},
 * que consulta a Central a cada segundo) e aplica só a diferença:
 * <ul>
 *   <li><b>reator novo ou reativado</b>: cria a facade, entra no simulador e publica a leitura inicial;</li>
 *   <li><b>reator alterado</b>: troca os sensores (faixas) na facade e os setpoints no simulador;</li>
 *   <li><b>reator retirado</b>: sai do simulador e das rotas de cenário.</li>
 * </ul>
 * Cada mudança aplicada vira um {@link CadastroReatorAtualizado} no EventBus: a auditoria do núcleo
 * registra e o painel (EstadoAgregador) inclui ou tira o reator — ninguém precisa conhecer o cadastro.
 */
public final class SincronizadorCadastroReatores {
    private final CadastroReatores cadastro;
    private final EventBus eventBus;
    private final SimuladorReatores simulador;
    private final Consumer<Map<String, ReatorFacade>> aoMudarReatores;
    private final Map<String, ReatorCadastrado> aplicados = new LinkedHashMap<>();
    private final Map<String, ReatorFacade> facades = new LinkedHashMap<>();
    private boolean avisouFalha;

    /**
     * @param iniciais        o que o núcleo já carregou na partida (facades + cadastro lido)
     * @param aoMudarReatores recebe as facades em operação sempre que o conjunto muda (rotas de cenário)
     */
    public SincronizadorCadastroReatores(
        CadastroReatores cadastro,
        EventBus eventBus,
        SimuladorReatores simulador,
        List<ReatorCadastrado> iniciais,
        Map<String, ReatorFacade> facadesIniciais,
        Consumer<Map<String, ReatorFacade>> aoMudarReatores
    ) {
        this.cadastro = cadastro;
        this.eventBus = eventBus;
        this.simulador = simulador;
        this.aoMudarReatores = aoMudarReatores;
        iniciais.forEach(reator -> aplicados.put(reator.id(), reator));
        facades.putAll(facadesIniciais);
    }

    public void iniciar(Duration intervalo) {
        ScheduledExecutorService agenda = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "sincronizador-cadastro-reatores");
            thread.setDaemon(true);
            return thread;
        });
        agenda.scheduleWithFixedDelay(this::sincronizarComAviso, intervalo.toMillis(), intervalo.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void sincronizarComAviso() {
        try {
            sincronizar();
            avisouFalha = false;
        } catch (RuntimeException falha) {
            // Banco fora: segue com o cadastro que já está aplicado e tenta de novo no próximo ciclo.
            if (!avisouFalha) {
                avisouFalha = true;
                System.err.println("[CADASTRO] Falha ao reler o cadastro de reatores: " + falha.getMessage());
            }
        }
    }

    /** Relê o cadastro e aplica a diferença. Devolve quantas mudanças aplicou. Público para os testes. */
    public synchronized int sincronizar() {
        Map<String, ReatorCadastrado> lidos = new LinkedHashMap<>();
        cadastro.listarAtivos().forEach(reator -> lidos.put(reator.id(), reator));

        int mudancas = 0;
        for (String id : new ArrayList<>(aplicados.keySet())) {
            if (!lidos.containsKey(id)) {
                retirar(id);
                mudancas++;
            }
        }
        for (ReatorCadastrado lido : lidos.values()) {
            ReatorCadastrado atual = aplicados.get(lido.id());
            if (atual == null) {
                incluir(lido);
                mudancas++;
            } else if (!atual.equals(lido)) {
                alterar(lido);
                mudancas++;
            }
        }
        return mudancas;
    }

    private void incluir(ReatorCadastrado reator) {
        if (reator.setpoints().isEmpty()) {
            return; // reator sem sensores ainda não tem o que simular; entra quando ganhar sensores
        }
        ReatorFacade facade = new ReatorFacade(eventBus, reator.id());
        reator.sensores().forEach(facade::registrarSensor);
        facades.put(reator.id(), facade);
        aplicados.put(reator.id(), reator);
        aoMudarReatores.accept(Map.copyOf(facades));
        // Evento antes da leitura inicial: na linha do tempo o "entrou em operação" vem primeiro.
        publicar(reator.id(), CadastroReatorAtualizado.Acao.INCLUIDO, reator.sensores());
        simulador.adicionar(facade, reator.setpoints());
        System.out.println("[CADASTRO] " + reator.id() + " entrou em operação.");
    }

    private void alterar(ReatorCadastrado reator) {
        if (reator.setpoints().isEmpty()) {
            retirar(reator.id());
            return;
        }
        ReatorFacade facade = facades.get(reator.id());
        facade.substituirSensores(reator.sensores());
        simulador.atualizar(reator.id(), reator.setpoints());
        aplicados.put(reator.id(), reator);
        publicar(reator.id(), CadastroReatorAtualizado.Acao.ALTERADO, reator.sensores());
        System.out.println("[CADASTRO] " + reator.id() + ": faixas/setpoints atualizados.");
    }

    private void retirar(String id) {
        aplicados.remove(id);
        facades.remove(id);
        // Primeiro as rotas de cenário, depois o simulador (que só volta quando não há leitura a
        // caminho), por fim o evento: o painel tira o reator e nada o recoloca.
        aoMudarReatores.accept(Map.copyOf(facades));
        simulador.remover(id);
        publicar(id, CadastroReatorAtualizado.Acao.RETIRADO, List.of());
        System.out.println("[CADASTRO] " + id + " retirado de operação.");
    }

    private void publicar(String id, CadastroReatorAtualizado.Acao acao, List<Sensor> sensores) {
        eventBus.publicar(new CadastroReatorAtualizado(Instant.now(), id, acao, sensores.stream().map(Sensor::id).toList()));
    }
}
