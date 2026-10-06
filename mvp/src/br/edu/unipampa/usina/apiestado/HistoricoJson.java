package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.historicooperacional.FiltroHistorico;
import br.edu.unipampa.usina.historicooperacional.ResultadoConsulta;
import br.edu.unipampa.usina.historicooperacional.ResumoDia;
import br.edu.unipampa.usina.historicooperacional.SnapshotOperacional;

import java.time.ZoneId;
import java.util.List;

/**
 * Serializador JSON manual (Zero External Dependencies) da consulta ao histórico (RF16).
 * Contrato documentado em docs/api-estado-contrato.md.
 */
public final class HistoricoJson {
    private HistoricoJson() {}

    public static String escrever(ResultadoConsulta resultado, FiltroHistorico filtro) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"total\":").append(resultado.total()).append(',');
        json.append("\"retornados\":").append(resultado.registros().size()).append(',');
        json.append("\"truncado\":").append(resultado.truncado()).append(',');
        json.append("\"filtro\":").append(filtro(filtro)).append(',');
        json.append("\"registros\":").append(registros(resultado.registros()));
        json.append('}');
        return json.toString();
    }

    /** Lista de saídas de operação (dia local + operador) para o primeiro nível do T06. */
    public static String escreverDias(List<ResumoDia> dias, FiltroHistorico filtro, ZoneId fuso) {
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
            + "\"operador\":" + (filtro.operador() == null ? "null" : texto(filtro.operador()))
            + "}";
    }

    private static String faixa(FiltroHistorico.Faixa faixa) {
        return "{\"min\":" + (faixa.min() == null ? "null" : faixa.min())
            + ",\"max\":" + (faixa.max() == null ? "null" : faixa.max()) + "}";
    }

    private static String registros(List<SnapshotOperacional> registros) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < registros.size(); i++) {
            SnapshotOperacional r = registros.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"instante\":").append(texto(r.instante().toString())).append(',')
                .append("\"temperatura\":").append(r.temperatura()).append(',')
                .append("\"pressao\":").append(r.pressao()).append(',')
                .append("\"radiacao\":").append(r.radiacao()).append(',')
                .append("\"fluxoResfriamento\":").append(r.fluxoResfriamento()).append(',')
                .append("\"sensoresEmAlarme\":").append(papeis(r.sensoresEmAlarme())).append(',')
                .append("\"sensoresReconhecidos\":").append(papeis(r.sensoresReconhecidos())).append(',')
                .append("\"sensoresEmObservacao\":").append(papeis(r.sensoresEmObservacao())).append(',')
                .append("\"sensoresEmFalha\":").append(papeis(r.sensoresEmFalha())).append(',')
                .append("\"status\":").append(texto(r.statusDerivado())).append(',')
                .append("\"operador\":").append(texto(r.operador())).append(',')
                .append("\"papeisOperador\":").append(papeis(r.papeisOperador())).append(',')
                .append("\"origemOperador\":").append(texto(r.origemOperador().name()))
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
