package br.edu.unipampa.usina.historicooperacional;

import java.time.Instant;
import java.util.Locale;

/**
 * Critérios de consulta do histórico operacional (RF16).
 *
 * <p>Todos os critérios são opcionais e combinam com <b>E</b>. Combinar faixas de parâmetros
 * diferentes só faz sentido porque cada linha guarda os 4 parâmetros do mesmo ciclo — ver a
 * decisão de modelagem em {@link SnapshotOperacional}.
 */
public record FiltroHistorico(
    Instant de,
    Instant ate,
    Faixa temperatura,
    Faixa pressao,
    Faixa radiacao,
    Faixa fluxo,
    String operador
) {
    /** Faixa fechada opcional: {@code null} em qualquer extremo significa sem limite daquele lado. */
    public record Faixa(Double min, Double max) {
        public static final Faixa LIVRE = new Faixa(null, null);

        public Faixa {
            if (min != null && max != null && min > max) {
                throw new FiltroInvalidoException("Faixa invertida: min " + min + " maior que max " + max + ".");
            }
        }

        public boolean aceita(double valor) {
            return (min == null || valor >= min) && (max == null || valor <= max);
        }
    }

    /** Erro de critério informado pelo chamador — vira HTTP 400, não 500. */
    public static final class FiltroInvalidoException extends RuntimeException {
        public FiltroInvalidoException(String mensagem) {
            super(mensagem);
        }
    }

    public static final FiltroHistorico TUDO = new FiltroHistorico(
        null, null, Faixa.LIVRE, Faixa.LIVRE, Faixa.LIVRE, Faixa.LIVRE, null);

    public FiltroHistorico {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new FiltroInvalidoException("O inicio do periodo e posterior ao fim.");
        }
        temperatura = temperatura == null ? Faixa.LIVRE : temperatura;
        pressao = pressao == null ? Faixa.LIVRE : pressao;
        radiacao = radiacao == null ? Faixa.LIVRE : radiacao;
        fluxo = fluxo == null ? Faixa.LIVRE : fluxo;
        operador = operador == null || operador.isBlank() ? null : operador.trim();
    }

    public boolean aceita(SnapshotOperacional snapshot) {
        return dentroDoPeriodo(snapshot.instante())
            && temperatura.aceita(snapshot.temperatura())
            && pressao.aceita(snapshot.pressao())
            && radiacao.aceita(snapshot.radiacao())
            && fluxo.aceita(snapshot.fluxoResfriamento())
            && casaOperador(snapshot.operador());
    }

    private boolean dentroDoPeriodo(Instant instante) {
        return (de == null || !instante.isBefore(de))
            && (ate == null || !instante.isAfter(ate));
    }

    private boolean casaOperador(String nomeNaLinha) {
        if (operador == null) {
            return true;
        }
        return nomeNaLinha.toLowerCase(Locale.ROOT).contains(operador.toLowerCase(Locale.ROOT));
    }
}
