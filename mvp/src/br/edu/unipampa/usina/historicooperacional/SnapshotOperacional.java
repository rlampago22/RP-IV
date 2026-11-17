package br.edu.unipampa.usina.historicooperacional;

import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
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
 *
 * <p>Carrega o {@code reatorId} desde 16/11 — até então o histórico operacional só cobria o
 * reator principal (R-01), porque os 4 ids de sensor (1 a 4) se repetem em todos os 5 reatores e
 * sem essa coluna não dava pra saber de qual reator cada linha era. Linhas antigas no CSV, escritas
 * antes dessa coluna existir, têm 1 campo a menos: {@link #deLinhaCsv(String)} detecta isso e
 * assume {@code R-01}, que é o que elas de fato eram.
 */
public record SnapshotOperacional(
    String reatorId,
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
        "reator_id;instante;temperatura;pressao;radiacao;fluxo_resfriamento;"
            + "sensores_em_alarme;sensores_reconhecidos;sensores_em_observacao;sensores_em_falha;"
            + "operador;papeis_operador;origem_operador";

    private static final String SEPARADOR = ";";
    private static final String SEPARADOR_LISTA = "|";
    private static final int COLUNAS = 13;
    private static final int COLUNAS_SEM_REATOR_LEGADO = 12;

    public SnapshotOperacional {
        reatorId = EventoDominio.reatorOuPrincipal(reatorId);
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
            "%s;%s;%.2f;%.2f;%.2f;%.2f;%s;%s;%s;%s;%s;%s;%s",
            reatorId,
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
        if (linha == null || linha.isBlank() || linha.startsWith("instante") || linha.startsWith("reator_id")) {
            return null;
        }
        String[] campos = linha.trim().split(SEPARADOR, -1);
        if (campos.length >= COLUNAS) {
            return deCampos(campos);
        }
        if (campos.length >= COLUNAS_SEM_REATOR_LEGADO) {
            // Linha gravada antes de 16/11, sem a coluna reator_id: era sempre do R-01.
            return deCamposLegado(campos);
        }
        return null;
    }

    private static SnapshotOperacional deCampos(String[] campos) {
        try {
            return new SnapshotOperacional(
                campos[0].trim(),
                Instant.parse(campos[1].trim()),
                Double.parseDouble(campos[2].trim()),
                Double.parseDouble(campos[3].trim()),
                Double.parseDouble(campos[4].trim()),
                Double.parseDouble(campos[5].trim()),
                separar(campos[6]),
                separar(campos[7]),
                separar(campos[8]),
                separar(campos[9]),
                campos[10].trim(),
                separar(campos[11]),
                OperadorDeServico.Origem.valueOf(campos[12].trim())
            );
        } catch (RuntimeException malformada) {
            return null;
        }
    }

    private static SnapshotOperacional deCamposLegado(String[] campos) {
        try {
            return new SnapshotOperacional(
                EventoDominio.REATOR_PRINCIPAL,
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
