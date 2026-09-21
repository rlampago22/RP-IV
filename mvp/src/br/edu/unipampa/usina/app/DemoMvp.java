package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.Alarme;
import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import java.nio.file.Path;

/**
 * Demonstração integrada do recorte Must RF01–RF06 e das qualidades verificáveis
 * no MVP local: integridade, tolerância a falhas, auditabilidade e testabilidade.
 */
public final class DemoMvp {
    private DemoMvp() {}

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" MVP USINA NUCLEAR - DEMONSTRACAO INTEGRADA DO DOCUMENTO");
        System.out.println(" Escopo: RF-1 (4 Sensores), RF-2, UC01 (Principal, Alternativo 1, Excecao)");
        System.out.println(" RNF-03/05 (Auditoria SHA-256 Multi-Sessao), RNF-04 (Tolerancia a Falhas)");
        System.out.println("================================================================================");

        EventBus eventBus = new EventBus();
        RegistroAuditoria registro = new RegistroAuditoria(Path.of("dados", "auditoria.log"));
        new AuditoriaSubscriber(eventBus, registro);
        AlarmeFacade alarmes = new AlarmeFacade(
            eventBus,
            new AvaliadorFaixaSegura(),
            new AlarmeFactory()
        );
        ReatorFacade reator = new ReatorFacade(eventBus);

        // 4 Sensores do RF-1
        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 155.0));
        reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.2));
        reator.registrarSensor(new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0));

        System.out.println("\n[PASSO 1] Operacao Normal - Coleta dos 4 parametros do RF-1:");
        reator.receberLeitura(1L, 310.5);
        reator.receberLeitura(2L, 155.0);
        reator.receberLeitura(3L, 2.4);
        reator.receberLeitura(4L, 1100.0);

        System.out.println("\n[PASSO 2] Fluxo Alternativo 1 (UC01) - Observacao preventiva:");
        reator.receberLeitura(1L, 328.0); // Fora da atencao ideal, mas abaixo do limite maximo de 350.0

        System.out.println("\n[PASSO 3] Fluxo de Excecoes (UC01) - Falha de comunicacao com sensor:");
        reator.simularFalhaSensor(2L, "Timeout no barramento Modbus do sensor de pressao");

        System.out.println("\n[PASSO 4] Violacao critica de limites e disparo automatico de alarme (RF-2):");
        reator.receberLeitura(3L, 8.4); // Radiacao critica
        reator.receberLeitura(4L, 420.0); // Resfriamento insuficiente

        System.out.println("\n[PASSO 5] Validacao / Reconhecimento de alertas pelo Operador (UC01):");
        for (Alarme a : alarmes.consultarAlarmes()) {
            if (a.isAtivo()) {
                alarmes.reconhecerAlarme(a.getId(), "Operador de Reator", "Alerta confirmado; procedendo com manobra corretiva.");
            }
        }

        System.out.println("\n[PASSO 6] Normalizacao e encerramento da ocorrencia (UC01):");
        for (Alarme a : alarmes.consultarAlarmes()) {
            if (a.isReconhecido()) {
                alarmes.resolverAlarme(a.getId(), "Engenheiro de Seguranca", "Injecao de emergencia de agua concluida com sucesso.");
            }
        }

        System.out.println("\n============================= RESUMO DA EXECUCAO =============================");
        System.out.println("Medicoes registradas no historico : " + reator.consultarHistorico().size());
        System.out.println("Alarmes processados no ciclo      : " + alarmes.consultarAlarmes().size());
        System.out.println("Eventos auditados na sessao/log   : " + registro.consultar().size());
        System.out.println("Integridade fisica do arquivo     : " + (registro.verificarIntegridadeArquivo() ? "OK (SHA-256 INTEGRA)" : "FALHA"));
        System.out.println("Arquivo append-only persistido    : " + registro.getArquivo());
        System.out.println("================================================================================");
    }
}
