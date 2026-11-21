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

    /** Um ciclo por reator — os ids de sensor (1 a 4) se repetem em todos os 5 reatores, então o
     * ciclo de um não pode compartilhar estado com o de outro (ver {@link EstadoCiclo}). */
    private final Map<String, EstadoCiclo> estadoPorReator = new HashMap<>();
    /** alarmeId -> (sensorId, reatorId): reconhecimento/resolução só carregam o alarmeId. */
    private final Map<String, Long> sensorPorAlarme = new HashMap<>();
    private final Map<String, String> reatorPorAlarme = new HashMap<>();

    public HistoricoSubscriber(EventBus eventBus, RegistroHistorico registro, OperadorDeServico operador) {
        this.registro = registro;
        this.operador = operador;
        eventBus.assinarTodos(this);
    }

    @Override
    public synchronized void onEvento(EventoDominio evento) {
        if (evento instanceof MedicaoRegistrada medicao) {
            EstadoCiclo estado = estadoDe(medicao.reatorId());
            estado.tipoPorSensor.put(medicao.sensorId(), medicao.tipoSensor());
            estado.ultimoValorPorTipo.put(medicao.tipoSensor(), medicao.valor());
            estado.tiposDoCicloAtual.add(medicao.tipoSensor());
            estado.sensoresEmObservacao.remove(medicao.sensorId());
            if (cicloCompleto(estado)) {
                gravar(medicao.reatorId(), estado, medicao.ocorridoEm());
                estado.tiposDoCicloAtual.clear();
            }
        } else if (evento instanceof ObservacaoRegistrada observacao) {
            EstadoCiclo estado = estadoDe(observacao.reatorId());
            estado.tipoPorSensor.put(observacao.sensorId(), observacao.tipoSensor());
            estado.sensoresEmObservacao.add(observacao.sensorId());
        } else if (evento instanceof FalhaSensorDetectada falha) {
            EstadoCiclo estado = estadoDe(falha.reatorId());
            estado.tipoPorSensor.put(falha.sensorId(), falha.tipoSensor());
            estado.falhasDesdeUltimaLinha.add(falha.sensorId());
            gravarTransicao(falha.reatorId(), estado, falha.ocorridoEm());
        } else if (evento instanceof AlarmeEmitido emitido) {
            sensorPorAlarme.put(emitido.alarmeId(), emitido.sensorId());
            reatorPorAlarme.put(emitido.alarmeId(), emitido.reatorId());
            EstadoCiclo estado = estadoDe(emitido.reatorId());
            estado.sensoresEmAlarme.add(emitido.sensorId());
            gravarTransicao(emitido.reatorId(), estado, emitido.ocorridoEm());
        } else if (evento instanceof AlarmeReconhecido reconhecido) {
            Long sensor = sensorPorAlarme.get(reconhecido.alarmeId());
            String reatorId = reatorPorAlarme.get(reconhecido.alarmeId());
            if (sensor == null || reatorId == null) {
                return; // alarme desconhecido: nada a atualizar
            }
            EstadoCiclo estado = estadoDe(reatorId);
            estado.sensoresEmAlarme.remove(sensor);
            estado.sensoresReconhecidos.add(sensor);
            gravarTransicao(reatorId, estado, reconhecido.ocorridoEm());
        } else if (evento instanceof AlarmeResolvido resolvido) {
            Long sensor = sensorPorAlarme.remove(resolvido.alarmeId());
            String reatorId = reatorPorAlarme.remove(resolvido.alarmeId());
            if (sensor == null || reatorId == null) {
                return;
            }
            EstadoCiclo estado = estadoDe(reatorId);
            estado.sensoresEmAlarme.remove(sensor);
            estado.sensoresReconhecidos.remove(sensor);
            gravarTransicao(reatorId, estado, resolvido.ocorridoEm());
        }
    }

    private EstadoCiclo estadoDe(String reatorId) {
        return estadoPorReator.computeIfAbsent(reatorId, id -> new EstadoCiclo());
    }

    /**
     * Grava a mudança de condição, desde que os 4 parâmetros do reator já tenham leitura conhecida.
     *
     * <p>Não há guarda de "ciclo em andamento": as rotas de cenário do T05 publicam seeds parciais
     * (o cenário crítico mexe em 2 sensores) com o tempo real pausado, então um ciclo iniciado
     * pode nunca fechar. Esperar pelo fechamento perderia o alarme.
     *
     * <p>Em troca, um alarme durante o tempo real pode gerar duas linhas próximas: uma no instante
     * da transição e outra ao fechar o ciclo. Ambas são verdadeiras — mudam os valores entre uma e
     * outra — e alarme é raro o suficiente para isso não poluir o relatório.
     */
    private void gravarTransicao(String reatorId, EstadoCiclo estado, Instant instante) {
        if (todosOsValoresConhecidos(estado)) {
            gravar(reatorId, estado, instante);
        }
    }

    private void gravar(String reatorId, EstadoCiclo estado, Instant instante) {
        SnapshotOperacional snapshot = new SnapshotOperacional(
            reatorId,
            instante,
            estado.ultimoValorPorTipo.get(TEMPERATURA),
            estado.ultimoValorPorTipo.get(PRESSAO),
            estado.ultimoValorPorTipo.get(RADIACAO),
            estado.ultimoValorPorTipo.get(FLUXO),
            rotular(estado, estado.sensoresEmAlarme),
            rotular(estado, estado.sensoresReconhecidos),
            rotular(estado, estado.sensoresEmObservacao),
            rotular(estado, estado.falhasDesdeUltimaLinha),
            operador.nome(),
            operador.papeis(),
            operador.origem()
        );
        registro.registrar(snapshot);
        estado.falhasDesdeUltimaLinha.clear();
    }

    private boolean cicloCompleto(EstadoCiclo estado) {
        return estado.tiposDoCicloAtual.containsAll(TIPOS_RF1);
    }

    private boolean todosOsValoresConhecidos(EstadoCiclo estado) {
        return estado.ultimoValorPorTipo.keySet().containsAll(TIPOS_RF1);
    }

    /** Rótulo {@code id:TIPO} — identifica o sensor sem depender de código definido no frontend. */
    private List<String> rotular(EstadoCiclo estado, Set<Long> sensores) {
        return sensores.stream()
            .map(id -> id + ":" + estado.tipoPorSensor.getOrDefault(id, "DESCONHECIDO"))
            .toList();
    }

    /** Ciclo de leituras em andamento de 1 reator — ver nota de campo acima. */
    private static final class EstadoCiclo {
        final Map<String, Double> ultimoValorPorTipo = new HashMap<>();
        final Map<Long, String> tipoPorSensor = new HashMap<>();
        final Set<String> tiposDoCicloAtual = new LinkedHashSet<>();
        final Set<Long> sensoresEmAlarme = new LinkedHashSet<>();
        final Set<Long> sensoresReconhecidos = new LinkedHashSet<>();
        final Set<Long> sensoresEmObservacao = new LinkedHashSet<>();
        final Set<Long> falhasDesdeUltimaLinha = new LinkedHashSet<>();
    }
}
