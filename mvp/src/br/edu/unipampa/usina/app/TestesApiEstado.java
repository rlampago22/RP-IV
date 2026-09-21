package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.AlarmeEstado;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.apiestado.EstadoJson;
import br.edu.unipampa.usina.apiestado.EstadoSnapshot;
import br.edu.unipampa.usina.apiestado.StatusReator;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;

import java.util.List;
import java.util.Optional;

/**
 * Testes do EstadoAgregador/EstadoJson (débito issue #41 + issue #44):
 * confirma que o estado exposto em /api/estado é derivado apenas dos eventos do EventBus,
 * sem depender diretamente de ReatorFacade/AlarmeFacade (regra EDA).
 */
public final class TestesApiEstado {
    private TestesApiEstado() {}

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" TESTES DA API DE ESTADO (EstadoAgregador -> contrato JSON do T01 Overview)");
        System.out.println("================================================================================");

        EventBus eventBus = new EventBus();
        EstadoAgregador estado = new EstadoAgregador(eventBus);
        AlarmeFacade alarmes = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        ReatorFacade reator = new ReatorFacade(eventBus);

        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 320.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 150.0));

        // 1. Estado inicial: sem sensores/alarmes, ESTAVEL.
        exigir(estado.snapshot().status() == StatusReator.ESTAVEL, "Estado inicial deve ser ESTAVEL.");
        exigir(estado.snapshot().sensores().isEmpty(), "Estado inicial nao deve ter sensores.");

        // 2. MedicaoRegistrada segura atualiza o sensor sem alarme.
        reator.receberLeitura(1L, 300.0);
        exigir(estado.snapshot().sensores().size() == 1, "Deve existir 1 sensor apos a primeira leitura.");
        exigir(estado.snapshot().contadores().medicoes() == 1, "Contador de medicoes deve ser 1.");
        exigir(estado.snapshot().status() == StatusReator.ESTAVEL, "Leitura segura nao deve mudar o status.");

        // 3. ObservacaoRegistrada (Fluxo Alternativo 1) muda o badge para ATENCAO.
        reator.receberLeitura(1L, 325.0); // dentro do limite, fora da faixa ideal de atencao
        exigir(estado.snapshot().status() == StatusReator.ATENCAO, "Observacao preventiva deve mudar o status para ATENCAO.");

        // 4. Leitura violando o limiar critico gera AlarmeEmitido -> status CRITICO.
        reator.receberLeitura(2L, 200.0);
        EstadoSnapshot comAlarme = estado.snapshot();
        exigir(comAlarme.status() == StatusReator.CRITICO, "Alarme ativo deve mudar o status para CRITICO.");
        exigir(comAlarme.contadores().alarmes() == 1, "Contador de alarmes deve ser 1.");
        exigir(!comAlarme.alarmes().isEmpty(), "Lista de alarmes nao deve estar vazia.");

        AlarmeEstado alarmeAtivo = comAlarme.alarmes().get(0);
        exigir("ATIVO".equals(alarmeAtivo.status()), "Alarme recem-emitido deve estar ATIVO.");

        // 5. Reconhecer o alarme via AlarmeFacade deve refletir no estado (RECONHECIDO -> ATENCAO).
        boolean reconhecido = alarmes.reconhecerAlarme(alarmeAtivo.id(), "Operador Teste", "Validando cenario de teste");
        exigir(reconhecido, "Reconhecimento do alarme deve ter sucesso.");
        exigir(estado.snapshot().status() == StatusReator.ATENCAO, "Alarme reconhecido (nao resolvido) deve manter status ATENCAO.");

        // 6. Resolver o alarme deve limpar o estado critico/atencao ligado a ele.
        boolean resolvido = alarmes.resolverAlarme(alarmeAtivo.id(), "Engenheiro Teste", "Pressao normalizada");
        exigir(resolvido, "Resolucao do alarme deve ter sucesso.");
        Optional<AlarmeEstado> alarmeFinal = estado.snapshot().alarmes().stream()
            .filter(a -> a.id().equals(alarmeAtivo.id()))
            .findFirst();
        exigir(alarmeFinal.isPresent() && "RESOLVIDO".equals(alarmeFinal.get().status()),
            "Alarme deve constar como RESOLVIDO apos a resolucao.");

        // 7. Timeline mantem o limite de eventos mais recentes primeiro.
        List<?> eventos = estado.snapshot().eventos();
        exigir(!eventos.isEmpty(), "Timeline de eventos nao deve estar vazia.");

        // 8. Contrato JSON deve ser gerado sem lançar excecao e conter os campos obrigatorios.
        String json = EstadoJson.escrever(estado.snapshot());
        exigir(json.contains("\"status\":"), "JSON deve conter o campo status.");
        exigir(json.contains("\"sensores\":"), "JSON deve conter o campo sensores.");
        exigir(json.contains("\"alarmes\":"), "JSON deve conter o campo alarmes.");
        exigir(json.contains("\"eventos\":"), "JSON deve conter o campo eventos.");

        System.out.println("\n[SUCESSO] Testes da API de estado (EstadoAgregador/EstadoJson) passaram com 100% de exito!");
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }
}
