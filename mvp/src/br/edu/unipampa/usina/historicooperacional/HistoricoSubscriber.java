package br.edu.unipampa.usina.historicooperacional;

import br.edu.unipampa.usina.infraestruturaeventos.AlarmeEmitido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeReconhecido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeResolvido;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.FalhaSensorDetectada;
import br.edu.unipampa.usina.infraestruturaeventos.IEventSubscriber;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consumidor que deriva o histórico operacional dos eventos do barramento e o persiste linha a
 * linha (RF16/RNF09). Mesmo papel do {@code AuditoriaSubscriber}, com outro destino.
 *
 * <p><b>Quando uma linha é gravada:</b>
 * <ul>
 *   <li>ao fechar um ciclo — os 4 parâmetros RF-1 reportaram desde a última linha. É a cadência
 *       regular enquanto o tempo real está ligado;</li>
 *   <li>ao mudar uma condição (falha, alarme emitido, reconhecido ou resolvido) <b>quando nenhum
 *       ciclo está em andamento</b>. Sem isso, um alarme disparado com o tempo real pausado — que é
 *       o que as rotas de cenário do T05 fazem — não deixaria rastro nenhum no histórico.</li>
 * </ul>
 * Por isso a linha representa <b>o estado conhecido naquele instante</b>, e não estritamente um
 * ciclo de leituras.
 *
 * <p><b>Alarme e observação são estado; falha é evento.</b> Um alarme permanece até ser
 * reconhecido, e uma observação até a próxima medição do sensor — ambos sobrevivem para a linha
 * seguinte. Já a falha, neste simulador, é contraditada pela leitura seguinte do mesmo sensor (o
 * simulador nunca para de ler). Tratá-la como estado fazia a coluna sair sempre vazia: a medição
 * que limparia a falha era a mesma que fechava o ciclo. Então {@code sensoresEmFalha} acumula as
 * <b>falhas detectadas desde a última linha</b> e é zerada a cada gravação.
 */
public final class HistoricoSubscriber implements IEventSubscriber {
    private static final String TEMPERATURA = "TEMPERATURA";
    private static final String PRESSAO = "PRESSAO";
    private static final String RADIACAO = "RADIACAO";
    private static final String FLUXO = "FLUXO_RESFRIAMENTO";
    private static final List<String> TIPOS_RF1 = List.of(TEMPERATURA, PRESSAO, RADIACAO, FLUXO);

    private final RegistroHistorico registro;
    private final OperadorDeServico operador;

    private final Map<String, Double> ultimoValorPorTipo = new HashMap<>();
    private final Map<Long, String> tipoPorSensor = new HashMap<>();
    private final Map<String, Long> sensorPorAlarme = new HashMap<>();
    private final Set<String> tiposDoCicloAtual = new LinkedHashSet<>();
    private final Set<Long> sensoresEmAlarme = new LinkedHashSet<>();
    private final Set<Long> sensoresReconhecidos = new LinkedHashSet<>();
    private final Set<Long> sensoresEmObservacao = new LinkedHashSet<>();
    private final Set<Long> falhasDesdeUltimaLinha = new LinkedHashSet<>();

    public HistoricoSubscriber(EventBus eventBus, RegistroHistorico registro, OperadorDeServico operador) {
        this.registro = registro;
        this.operador = operador;
        eventBus.assinarTodos(this);
    }

    @Override
    public synchronized void onEvento(EventoDominio evento) {
        if (evento instanceof MedicaoRegistrada medicao) {
            tipoPorSensor.put(medicao.sensorId(), medicao.tipoSensor());
            ultimoValorPorTipo.put(medicao.tipoSensor(), medicao.valor());
            tiposDoCicloAtual.add(medicao.tipoSensor());
            sensoresEmObservacao.remove(medicao.sensorId());
            if (cicloCompleto()) {
                gravar(medicao.ocorridoEm());
                tiposDoCicloAtual.clear();
            }
        } else if (evento instanceof ObservacaoRegistrada observacao) {
            tipoPorSensor.put(observacao.sensorId(), observacao.tipoSensor());
            sensoresEmObservacao.add(observacao.sensorId());
        } else if (evento instanceof FalhaSensorDetectada falha) {
            tipoPorSensor.put(falha.sensorId(), falha.tipoSensor());
            falhasDesdeUltimaLinha.add(falha.sensorId());
            gravarTransicao(falha.ocorridoEm());
        } else if (evento instanceof AlarmeEmitido emitido) {
            sensorPorAlarme.put(emitido.alarmeId(), emitido.sensorId());
            sensoresEmAlarme.add(emitido.sensorId());
            gravarTransicao(emitido.ocorridoEm());
        } else if (evento instanceof AlarmeReconhecido reconhecido) {
            Long sensor = sensorPorAlarme.get(reconhecido.alarmeId());
            if (sensor != null) {
                sensoresEmAlarme.remove(sensor);
                sensoresReconhecidos.add(sensor);
            }
            gravarTransicao(reconhecido.ocorridoEm());
        } else if (evento instanceof AlarmeResolvido resolvido) {
            Long sensor = sensorPorAlarme.remove(resolvido.alarmeId());
            if (sensor != null) {
                sensoresEmAlarme.remove(sensor);
                sensoresReconhecidos.remove(sensor);
            }
            gravarTransicao(resolvido.ocorridoEm());
        }
    }

    /**
     * Grava a mudança de condição, desde que os 4 parâmetros já tenham leitura conhecida.
     *
     * <p>Não há guarda de "ciclo em andamento": as rotas de cenário do T05 publicam seeds parciais
     * (o cenário crítico mexe em 2 sensores) com o tempo real pausado, então um ciclo iniciado
     * pode nunca fechar. Esperar pelo fechamento perderia o alarme.
     *
     * <p>Em troca, um alarme durante o tempo real pode gerar duas linhas próximas: uma no instante
     * da transição e outra ao fechar o ciclo. Ambas são verdadeiras — mudam os valores entre uma e
     * outra — e alarme é raro o suficiente para isso não poluir o relatório.
     */
    private void gravarTransicao(Instant instante) {
        if (todosOsValoresConhecidos()) {
            gravar(instante);
        }
    }

    private void gravar(Instant instante) {
        registro.registrar(new SnapshotOperacional(
            instante,
            ultimoValorPorTipo.get(TEMPERATURA),
            ultimoValorPorTipo.get(PRESSAO),
            ultimoValorPorTipo.get(RADIACAO),
            ultimoValorPorTipo.get(FLUXO),
            rotular(sensoresEmAlarme),
            rotular(sensoresReconhecidos),
            rotular(sensoresEmObservacao),
            rotular(falhasDesdeUltimaLinha),
            operador.nome(),
            operador.papeis(),
            operador.origem()
        ));
        falhasDesdeUltimaLinha.clear();
    }

    private boolean cicloCompleto() {
        return tiposDoCicloAtual.containsAll(TIPOS_RF1);
    }

    private boolean todosOsValoresConhecidos() {
        return ultimoValorPorTipo.keySet().containsAll(TIPOS_RF1);
    }

    /** Rótulo {@code id:TIPO} — identifica o sensor sem depender de código definido no frontend. */
    private List<String> rotular(Set<Long> sensores) {
        return sensores.stream()
            .map(id -> id + ":" + tipoPorSensor.getOrDefault(id, "DESCONHECIDO"))
            .toList();
    }
}
