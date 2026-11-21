package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

/**
 * RF10 — um protocolo de contingência foi ativado na Central (com a digital de quem ativou). Enquanto
 * houver protocolo ativo o núcleo fica em modo de contingência: a simulação em tempo real para e as
 * operações de rotina (cenários, novos acessos) são recusadas.
 */
public record ProtocoloContingenciaAtivado(
    Instant ocorridoEm,
    long emergenciaId,
    String protocolo,
    String nomeProtocolo,
    String ativadoPor
) implements EventoDominio {
    @Override
    public String tipo() {
        return "PROTOCOLO_CONTINGENCIA_ATIVADO";
    }

    @Override
    public String resumo() {
        return "emergencia=" + emergenciaId
            + " protocolo=" + protocolo
            + " nome=\"" + nomeProtocolo + "\""
            + " ativadoPor=\"" + ativadoPor + "\"";
    }
}
