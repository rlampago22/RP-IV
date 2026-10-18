package br.edu.unipampa.usina.historicooperacional;

import java.util.List;

/**
 * Resultado de uma consulta ao histórico.
 *
 * <p>{@code total} é quantas linhas casaram com o filtro; {@code registros} pode trazer menos,
 * se o limite cortou. {@code truncado} existe para a interface poder avisar o operador em vez de
 * deixá-lo achar que viu tudo.
 */
public record ResultadoConsulta(
    List<SnapshotOperacional> registros,
    int total,
    boolean truncado
) {
    public ResultadoConsulta {
        registros = registros == null ? List.of() : List.copyOf(registros);
    }
}
