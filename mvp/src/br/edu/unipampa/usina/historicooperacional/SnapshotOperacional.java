package br.edu.unipampa.usina.historicooperacional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Uma linha do histórico operacional: os 4 parâmetros RF-1 do mesmo ciclo, quais sensores estavam
 * em cada condição, e quem estava de serviço.
 *
 * <p>As condições são listas de sensores no formato {@code id:TIPO} (ex.: {@code 2:PRESSAO}), e não
 * contadores: para o relatório de operação, saber <b>qual</b> sensor alarmou é mais importante que
 * saber quantos alarmes havia.
 *
 * <p>O status <b>não</b> é armazenado — é derivado em {@link #statusDerivado()} pela mesma regra do
 * {@code EstadoAgregador}. Note que falha de sensor não entra nessa regra, porque o agregador
 * também não a considera; falha é um eixo próprio, na coluna {@code sensores_em_falha}.
 *
 * <p><b>Alarme e observação são estado no instante da linha; falha é evento do intervalo.</b>
 * {@code sensoresEmAlarme} e {@code sensoresEmObservacao} dizem como as coisas estavam quando a
 * linha foi escrita. {@code sensoresEmFalha} lista as falhas <b>detectadas desde a linha
 * anterior</b> — ver a justificativa em {@code HistoricoSubscriber}.
 *
 * <p>A coluna de valor de um sensor que falhou mantém a <b>última leitura conhecida</b>: o valor
 * não é apagado, e a lista de falha avisa que ele pode estar velho.
 *
 * <p>Separador {@code ;} para o Excel dividir as colunas; decimais com ponto ({@code Locale.ROOT})
 * porque a consulta filtra numericamente. Listas internas usam {@code |}.
 */
public record SnapshotOperacional(
    Instant instante,
    double temperatura,
    double pressao,
    double radiacao,
    double fluxoResfriamento,
    List<String> sensoresEmAlarme,
    List<String> sensoresReconhecidos,
    List<String> sensoresEmObservacao,
    List<String> sensoresEmFalha,
    String operador,
    List<String> papeisOperador,
    OperadorDeServico.Origem origemOperador
) {
    public static final String CABECALHO =
        "instante;temperatura;pressao;radiacao;fluxo_resfriamento;"
            + "sensores_em_alarme;sensores_reconhecidos;sensores_em_observacao;sensores_em_falha;"
            + "operador;papeis_operador;origem_operador";

    private static final String SEPARADOR = ";";
    private static final String SEPARADOR_LISTA = "|";
    private static final int COLUNAS = 12;

    public SnapshotOperacional {
        sensoresEmAlarme = copia(sensoresEmAlarme);
        sensoresReconhecidos = copia(sensoresReconhecidos);
        sensoresEmObservacao = copia(sensoresEmObservacao);
        sensoresEmFalha = copia(sensoresEmFalha);
        papeisOperador = copia(papeisOperador);
        operador = operador == null ? "" : operador;
        origemOperador = origemOperador == null
            ? OperadorDeServico.Origem.NAO_IDENTIFICADO
            : origemOperador;
    }

    private static List<String> copia(List<String> lista) {
        return lista == null ? List.of() : List.copyOf(lista);
    }

    public String linhaCsv() {
        return String.format(
            Locale.ROOT,
            "%s;%.2f;%.2f;%.2f;%.2f;%s;%s;%s;%s;%s;%s;%s",
            instante,
            temperatura,
            pressao,
            radiacao,
            fluxoResfriamento,
            juntar(sensoresEmAlarme),
            juntar(sensoresReconhecidos),
            juntar(sensoresEmObservacao),
            juntar(sensoresEmFalha),
            operador,
            juntar(papeisOperador),
            origemOperador
        );
    }

    private static String juntar(List<String> lista) {
        return String.join(SEPARADOR_LISTA, lista);
    }

    /** Devolve {@code null} para linha de cabeçalho, vazia ou malformada. */
    public static SnapshotOperacional deLinhaCsv(String linha) {
        if (linha == null || linha.isBlank() || linha.startsWith("instante")) {
            return null;
        }
        String[] campos = linha.trim().split(SEPARADOR, -1);
        if (campos.length < COLUNAS) {
            return null;
        }
        try {
            return new SnapshotOperacional(
                Instant.parse(campos[0].trim()),
                Double.parseDouble(campos[1].trim()),
                Double.parseDouble(campos[2].trim()),
                Double.parseDouble(campos[3].trim()),
                Double.parseDouble(campos[4].trim()),
                separar(campos[5]),
                separar(campos[6]),
                separar(campos[7]),
                separar(campos[8]),
                campos[9].trim(),
                separar(campos[10]),
                OperadorDeServico.Origem.valueOf(campos[11].trim())
            );
        } catch (RuntimeException malformada) {
            return null;
        }
    }

    private static List<String> separar(String campo) {
        String limpo = campo == null ? "" : campo.trim();
        if (limpo.isEmpty()) {
            return List.of();
        }
        return List.of(limpo.split("\\" + SEPARADOR_LISTA));
    }

    /** Mesma regra de {@code EstadoAgregador.calcularStatus()}. */
    public String statusDerivado() {
        if (!sensoresEmAlarme.isEmpty()) {
            return "CRITICO";
        }
        if (!sensoresReconhecidos.isEmpty() || !sensoresEmObservacao.isEmpty()) {
            return "ATENCAO";
        }
        return "ESTAVEL";
    }

    public boolean temFalhaDeSensor() {
        return !sensoresEmFalha.isEmpty();
    }
}
