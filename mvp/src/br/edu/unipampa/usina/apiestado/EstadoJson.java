package br.edu.unipampa.usina.apiestado;

import java.util.List;

/**
 * Serializador JSON manual (Zero External Dependencies) do contrato de estado.
 * Contrato documentado em docs/api-estado-contrato.md.
 */
public final class EstadoJson {
    private EstadoJson() {}

    public static String escrever(EstadoSnapshot estado) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"status\":").append(str(estado.status().name())).append(',');
        json.append("\"tempoReal\":").append(estado.tempoReal()).append(',');
        json.append("\"atualizadoEm\":").append(str(estado.atualizadoEm().toString())).append(',');
        json.append("\"sensores\":").append(sensores(estado.sensores())).append(',');
        json.append("\"contadores\":").append(contadores(estado.contadores())).append(',');
        json.append("\"alarmes\":").append(alarmes(estado.alarmes())).append(',');
        json.append("\"eventos\":").append(eventos(estado.eventos())).append(',');
        json.append("\"reatores\":").append(reatores(estado.reatores())).append(',');
        json.append("\"contingencia\":").append(contingencia(estado.contingencias()));
        json.append('}');
        return json.toString();
    }

    /** RF10 / #99: {@code {"ativa":true,"protocolos":[{...}]}} — o front trava os botões de rotina. */
    private static String contingencia(List<ContingenciaEstado> contingencias) {
        StringBuilder json = new StringBuilder("{\"ativa\":").append(!contingencias.isEmpty()).append(",\"protocolos\":[");
        for (int i = 0; i < contingencias.size(); i++) {
            ContingenciaEstado c = contingencias.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"emergenciaId\":").append(c.emergenciaId())
                .append(",\"protocolo\":").append(str(c.protocolo()))
                .append(",\"nome\":").append(str(c.nome()))
                .append(",\"ativadoPor\":").append(str(c.ativadoPor()))
                .append(",\"ativadoEm\":").append(str(c.ativadoEm().toString()))
                .append('}');
        }
        return json.append("]}").toString();
    }

    /**
     * Painel de monitoramento. A série vai como pares {@code [epochMillis, valor]} em vez de
     * objetos: são 5 reatores × 4 sensores × até 60 pontos em cada push, e o par corta o tamanho
     * do payload pela metade sem perder o instante de cada ponto.
     */
    private static String reatores(List<ReatorEstado> reatores) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < reatores.size(); i++) {
            ReatorEstado reator = reatores.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"id\":").append(str(reator.id())).append(',')
                .append("\"nivel\":").append(str(reator.nivel().name())).append(',')
                .append("\"atualizadoEm\":").append(reator.atualizadoEm() == null ? "null" : str(reator.atualizadoEm().toString())).append(',')
                .append("\"alarmesAtivos\":").append(reator.alarmesAtivos()).append(',')
                .append("\"sensores\":[");
            for (int j = 0; j < reator.sensores().size(); j++) {
                SensorReatorEstado sensor = reator.sensores().get(j);
                if (j > 0) {
                    json.append(',');
                }
                json.append('{')
                    .append("\"id\":").append(sensor.id()).append(',')
                    .append("\"tipo\":").append(str(sensor.tipo())).append(',')
                    .append("\"unidade\":").append(str(sensor.unidade())).append(',')
                    .append("\"valor\":").append(sensor.valor()).append(',')
                    .append("\"limiteMinimo\":").append(sensor.limiteMinimo()).append(',')
                    .append("\"limiteMaximo\":").append(sensor.limiteMaximo()).append(',')
                    .append("\"emAtencao\":").append(sensor.emAtencao()).append(',')
                    .append("\"atualizadoEm\":").append(str(sensor.atualizadoEm().toString())).append(',')
                    .append("\"serie\":").append(serie(sensor.serie()))
                    .append('}');
            }
            json.append("]}");
        }
        return json.append(']').toString();
    }

    private static String serie(List<Amostra> serie) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < serie.size(); i++) {
            Amostra amostra = serie.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('[').append(amostra.instante().toEpochMilli()).append(',').append(amostra.valor()).append(']');
        }
        return json.append(']').toString();
    }

    private static String sensores(List<SensorEstado> sensores) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < sensores.size(); i++) {
            SensorEstado sensor = sensores.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"id\":").append(sensor.id()).append(',')
                .append("\"tipo\":").append(str(sensor.tipo())).append(',')
                .append("\"unidade\":").append(str(sensor.unidade())).append(',')
                .append("\"valor\":").append(sensor.valor()).append(',')
                .append("\"limiteMinimo\":").append(sensor.limiteMinimo()).append(',')
                .append("\"limiteMaximo\":").append(sensor.limiteMaximo()).append(',')
                .append("\"atualizadoEm\":").append(str(sensor.atualizadoEm().toString())).append(',')
                .append("\"historico\":").append(doubles(sensor.historico()))
                .append('}');
        }
        return json.append(']').toString();
    }

    private static String doubles(List<Double> valores) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(valores.get(i));
        }
        return json.append(']').toString();
    }

    private static String contadores(Contadores contadores) {
        return "{\"medicoes\":" + contadores.medicoes()
            + ",\"alarmes\":" + contadores.alarmes()
            + ",\"auditoria\":" + contadores.auditoria() + "}";
    }

    private static String alarmes(List<AlarmeEstado> alarmes) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < alarmes.size(); i++) {
            AlarmeEstado alarme = alarmes.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"id\":").append(str(alarme.id())).append(',')
                .append("\"reatorId\":").append(str(alarme.reatorId())).append(',')
                .append("\"sensorId\":").append(alarme.sensorId()).append(',')
                .append("\"severidade\":").append(str(alarme.severidade())).append(',')
                .append("\"mensagem\":").append(str(alarme.mensagem())).append(',')
                .append("\"status\":").append(str(alarme.status())).append(',')
                .append("\"criadoEm\":").append(str(alarme.criadoEm().toString())).append(',')
                .append("\"operador\":").append(nullavel(alarme.operador())).append(',')
                .append("\"solucao\":").append(nullavel(alarme.solucao())).append(',')
                .append("\"resolvidoEm\":").append(alarme.resolvidoEm() == null
                    ? "null" : str(alarme.resolvidoEm().toString())).append(',')
                .append("\"resolvidoPor\":").append(nullavel(alarme.resolvidoPor()))
                .append('}');
        }
        return json.append(']').toString();
    }

    private static String eventos(List<EventoEstado> eventos) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < eventos.size(); i++) {
            EventoEstado evento = eventos.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"ocorridoEm\":").append(str(evento.ocorridoEm().toString())).append(',')
                .append("\"tipo\":").append(str(evento.tipo())).append(',')
                .append("\"resumo\":").append(str(evento.resumo()))
                .append('}');
        }
        return json.append(']').toString();
    }

    private static String nullavel(String valor) {
        return valor == null ? "null" : str(valor);
    }

    private static String str(String valor) {
        StringBuilder json = new StringBuilder(valor.length() + 2);
        json.append('"');
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        return json.append('"').toString();
    }
}
