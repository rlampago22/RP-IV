package br.edu.unipampa.usina.apiestado;

import java.time.Instant;
import java.util.List;

/**
 * Estado de um reator no painel de monitoramento (5 reatores).
 *
 * <p>{@code nivel} é a lâmpada do reator e reflete as <b>leituras atuais</b>:
 * <ul>
 *   <li>{@code CRITICO} — algum sensor está fora do limite seguro agora;</li>
 *   <li>{@code ATENCAO} — algum sensor está na faixa de atenção (gerou observação);</li>
 *   <li>{@code ESTAVEL} — todos dentro da faixa ideal.</li>
 * </ul>
 * É diferente do {@code status} do snapshot, que depende do ciclo do alarme (ATIVO até alguém
 * reconhecer). A lâmpada volta ao verde quando a leitura volta; o alarme continua na lista até ser
 * tratado — por isso {@code alarmesAtivos} vem separado.
 */
public record ReatorEstado(
    String id,
    StatusReator nivel,
    Instant atualizadoEm,
    int alarmesAtivos,
    List<SensorReatorEstado> sensores
) {
    public ReatorEstado {
        sensores = sensores == null ? List.of() : List.copyOf(sensores);
    }
}
