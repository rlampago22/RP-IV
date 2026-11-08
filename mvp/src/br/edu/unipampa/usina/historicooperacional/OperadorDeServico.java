package br.edu.unipampa.usina.historicooperacional;

import java.time.Instant;
import java.util.List;

/**
 * Quem está de serviço no momento em que o histórico é gravado.
 *
 * <p>O {@code mvp/} roda em processo separado da autenticação (Spring Boot em {@code backend/},
 * porta 8081, sessão por cookie server-side) e não tem como validar aquela sessão. Por isso
 * {@link Origem} viaja junto com o nome: o relatório precisa dizer se o operador foi
 * <b>declarado</b> ou <b>verificado</b>, em vez de afirmar autenticação que não houve.
 *
 * <p>Quando a autenticação for integrada, muda apenas quem chama
 * {@link #identificar(String, List, Origem)} — nada no histórico precisa mudar.
 */
public final class OperadorDeServico {
    public enum Origem {
        /** Ninguém se identificou nesta sessão do backend. */
        NAO_IDENTIFICADO,
        /** Nome informado pela interface após login, sem validação no processo do mvp. */
        DECLARADO_PELA_UI,
        /** Reservado: identidade confirmada contra a sessão autenticada. */
        SESSAO_VALIDADA
    }

    private String nome = "";
    private List<String> papeis = List.of();
    private Origem origem = Origem.NAO_IDENTIFICADO;
    private Instant identificadoEm;

    public synchronized void identificar(String nome, List<String> papeis, Origem origem) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O operador de serviço precisa de um nome.");
        }
        if (origem == null || origem == Origem.NAO_IDENTIFICADO) {
            throw new IllegalArgumentException(
                "Informe a origem da identificação (DECLARADO_PELA_UI ou SESSAO_VALIDADA).");
        }
        this.nome = nome.trim();
        this.papeis = papeis == null ? List.of() : List.copyOf(papeis);
        this.origem = origem;
        this.identificadoEm = Instant.now();
    }

    /** Fim de turno ou logout: volta ao estado não identificado. */
    public synchronized void liberar() {
        this.nome = "";
        this.papeis = List.of();
        this.origem = Origem.NAO_IDENTIFICADO;
        this.identificadoEm = null;
    }

    public synchronized String nome() {
        return nome;
    }

    public synchronized List<String> papeis() {
        return papeis;
    }

    public synchronized Origem origem() {
        return origem;
    }

    public synchronized Instant identificadoEm() {
        return identificadoEm;
    }

    public synchronized boolean identificado() {
        return origem != Origem.NAO_IDENTIFICADO;
    }
}
