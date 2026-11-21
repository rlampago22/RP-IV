package br.edu.unipampa.usina.historico;

import java.time.Instant;
import java.util.Locale;

/**
 * Critérios de consulta do histórico operacional (RF16) — porte de
 * {@code mvp/.../historicooperacional/FiltroHistorico.java} pro backend, já que a consulta
 * passa a rodar aqui, em cima de {@link HistoricoOperacional} em vez de {@code SnapshotOperacional}.
 * Mesmas regras: todos os critérios são opcionais e combinam com <b>E</b>.
 */
public record FiltroHistorico(
    Instant de,
    Instant ate,
    Faixa temperatura,
    Faixa pressao,
    Faixa radiacao,
    Faixa fluxo,
    String operador,
    String reatorId
) {
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

    public static final class FiltroInvalidoException extends RuntimeException {
        public FiltroInvalidoException(String mensagem) {
            super(mensagem);
        }
    }

    public static final FiltroHistorico TUDO = new FiltroHistorico(
        null, null, Faixa.LIVRE, Faixa.LIVRE, Faixa.LIVRE, Faixa.LIVRE, null, null);

    public FiltroHistorico {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new FiltroInvalidoException("O inicio do periodo e posterior ao fim.");
        }
        temperatura = temperatura == null ? Faixa.LIVRE : temperatura;
        pressao = pressao == null ? Faixa.LIVRE : pressao;
        radiacao = radiacao == null ? Faixa.LIVRE : radiacao;
        fluxo = fluxo == null ? Faixa.LIVRE : fluxo;
        operador = operador == null || operador.isBlank() ? null : operador.trim();
        reatorId = reatorId == null || reatorId.isBlank() ? null : reatorId.trim();
    }

    public boolean aceita(HistoricoOperacional linha) {
        return dentroDoPeriodo(linha.getInstante())
            && temperatura.aceita(linha.getTemperatura())
            && pressao.aceita(linha.getPressao())
            && radiacao.aceita(linha.getRadiacao())
            && fluxo.aceita(linha.getFluxoResfriamento())
            && casaOperador(linha.getOperador())
            && casaReator(linha.getReatorId());
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

    private boolean casaReator(String reatorDaLinha) {
        return reatorId == null || reatorId.equals(reatorDaLinha);
    }
}
