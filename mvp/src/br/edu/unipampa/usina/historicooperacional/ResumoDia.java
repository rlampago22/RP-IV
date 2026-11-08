package br.edu.unipampa.usina.historicooperacional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma saída de operação: um dia (no fuso local) sob responsabilidade de um operador.
 *
 * <p>Um dia com troca de turno gera mais de um resumo — um por operador — porque esconder a troca
 * falsearia quem respondia pelo reator em cada intervalo.
 *
 * <p>{@code ciclosComAlarme} conta <b>ciclos</b> em que havia alarme ativo, não alarmes distintos:
 * o histórico guarda quais sensores estavam em alarme por ciclo, não os identificadores dos
 * alarmes, então contar alarmes distintos a partir dele seria invenção. Para a lista de alarmes em
 * si, a fonte é a auditoria.
 *
 * <p>{@code sensoresEnvolvidos} reúne os sensores que apareceram em alarme ou falha na saída —
 * é o que permite ver no primeiro nível <b>onde</b> houve problema, sem abrir o detalhe.
 */
public record ResumoDia(
    LocalDate dia,
    String operador,
    OperadorDeServico.Origem origemOperador,
    Instant primeiroRegistro,
    Instant ultimoRegistro,
    int ciclos,
    int ciclosComAlarme,
    int ciclosEmObservacao,
    int ciclosComFalha,
    List<String> sensoresEnvolvidos,
    String piorStatus
) {
    public ResumoDia {
        sensoresEnvolvidos = sensoresEnvolvidos == null ? List.of() : List.copyOf(sensoresEnvolvidos);
    }
}
