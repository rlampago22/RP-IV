package br.edu.unipampa.usina.historico;

import java.time.ZoneId;
import java.util.List;

/**
 * Serializador JSON manual do histórico operacional — porte de
 * {@code mvp/.../apiestado/HistoricoJson.java}. Contrato idêntico (mesmo documentado em
 * docs/api-estado-contrato.md), pra quem consome (mvp/ em proxy, ou o front direto) não notar
 * diferença nenhuma além da origem do dado agora ser o banco.
 */
final class HistoricoJson {
    private HistoricoJson() { }

    static String escrever(List<HistoricoOperacional> retornados, int total, boolean truncado, FiltroHistorico filtro) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"total\":").append(total).append(',');
        json.append("\"retornados\":").append(retornados.size()).append(',');
        json.append("\"truncado\":").append(truncado).append(',');
        json.append("\"filtro\":").append(filtro(filtro)).append(',');
        json.append("\"registros\":").append(registros(retornados));
        json.append('}');
        return json.toString();
    }

    static String escreverDias(List<ResumoDia> dias, FiltroHistorico filtro, ZoneId fuso) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"fusoHorario\":").append(texto(fuso.getId())).append(',');
        json.append("\"total\":").append(dias.size()).append(',');
        json.append("\"filtro\":").append(filtro(filtro)).append(',');
        json.append("\"dias\":[");
        for (int i = 0; i < dias.size(); i++) {
            ResumoDia d = dias.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"dia\":").append(texto(d.dia().toString())).append(',')
                .append("\"operador\":").append(texto(d.operador())).append(',')
                .append("\"origemOperador\":").append(texto(d.origemOperador().name())).append(',')
                .append("\"primeiroRegistro\":").append(texto(d.primeiroRegistro().toString())).append(',')
                .append("\"ultimoRegistro\":").append(texto(d.ultimoRegistro().toString())).append(',')
                .append("\"ciclos\":").append(d.ciclos()).append(',')
                .append("\"ciclosComAlarme\":").append(d.ciclosComAlarme()).append(',')
                .append("\"ciclosEmObservacao\":").append(d.ciclosEmObservacao()).append(',')
                .append("\"ciclosComFalha\":").append(d.ciclosComFalha()).append(',')
                .append("\"sensoresEnvolvidos\":").append(papeis(d.sensoresEnvolvidos())).append(',')
                .append("\"piorStatus\":").append(texto(d.piorStatus()))
                .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private static String filtro(FiltroHistorico filtro) {
        return "{"
            + "\"de\":" + (filtro.de() == null ? "null" : texto(filtro.de().toString())) + ','
            + "\"ate\":" + (filtro.ate() == null ? "null" : texto(filtro.ate().toString())) + ','
            + "\"temperatura\":" + faixa(filtro.temperatura()) + ','
            + "\"pressao\":" + faixa(filtro.pressao()) + ','
            + "\"radiacao\":" + faixa(filtro.radiacao()) + ','
            + "\"fluxo\":" + faixa(filtro.fluxo()) + ','
            + "\"operador\":" + (filtro.operador() == null ? "null" : texto(filtro.operador())) + ','
            + "\"reatorId\":" + (filtro.reatorId() == null ? "null" : texto(filtro.reatorId()))
            + "}";
    }

    private static String faixa(FiltroHistorico.Faixa faixa) {
        return "{\"min\":" + (faixa.min() == null ? "null" : faixa.min())
            + ",\"max\":" + (faixa.max() == null ? "null" : faixa.max()) + "}";
    }

    private static String registros(List<HistoricoOperacional> registros) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < registros.size(); i++) {
            HistoricoOperacional r = registros.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"reatorId\":").append(texto(r.getReatorId())).append(',')
                .append("\"instante\":").append(texto(r.getInstante().toString())).append(',')
                .append("\"temperatura\":").append(r.getTemperatura()).append(',')
                .append("\"pressao\":").append(r.getPressao()).append(',')
                .append("\"radiacao\":").append(r.getRadiacao()).append(',')
                .append("\"fluxoResfriamento\":").append(r.getFluxoResfriamento()).append(',')
                .append("\"sensoresEmAlarme\":").append(papeis(HistoricoConsulta.separar(r.getSensoresEmAlarme()))).append(',')
                .append("\"sensoresReconhecidos\":").append(papeis(HistoricoConsulta.separar(r.getSensoresReconhecidos()))).append(',')
                .append("\"sensoresEmObservacao\":").append(papeis(HistoricoConsulta.separar(r.getSensoresEmObservacao()))).append(',')
                .append("\"sensoresEmFalha\":").append(papeis(HistoricoConsulta.separar(r.getSensoresEmFalha()))).append(',')
                .append("\"status\":").append(texto(HistoricoConsulta.statusDerivado(r))).append(',')
                .append("\"operador\":").append(texto(r.getOperador())).append(',')
                .append("\"papeisOperador\":").append(papeis(HistoricoConsulta.separar(r.getPapeisOperador()))).append(',')
                .append("\"origemOperador\":").append(texto(r.getOrigemOperador().name()))
                .append('}');
        }
        return json.append(']').toString();
    }

    private static String papeis(List<String> papeis) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < papeis.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(texto(papeis.get(i)));
        }
        return json.append(']').toString();
    }

    private static String texto(String valor) {
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
