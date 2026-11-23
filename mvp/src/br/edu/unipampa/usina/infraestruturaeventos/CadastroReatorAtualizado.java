package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.List;

/**
 * Issue #96 — o cadastro de um reator mudou no banco e o núcleo já aplicou a mudança: o reator
 * entrou em operação, saiu dela ou teve faixas/setpoints ajustados. Quem alterou fica na trilha da
 * Central (8081); aqui o fato entra na auditoria do núcleo e na linha do tempo do painel.
 *
 * @param sensores ids dos sensores do reator depois da mudança (vazio quando foi retirado)
 */
public record CadastroReatorAtualizado(
    Instant ocorridoEm,
    String reatorId,
    Acao acao,
    List<Long> sensores
) implements EventoDominio {
    public enum Acao { INCLUIDO, ALTERADO, RETIRADO }

    public CadastroReatorAtualizado {
        sensores = List.copyOf(sensores);
    }

    @Override
    public String tipo() {
        return "CADASTRO_REATOR_ATUALIZADO";
    }

    @Override
    public String resumo() {
        return "reator=" + reatorId + " acao=" + acao + " sensores=" + sensores;
    }
}
