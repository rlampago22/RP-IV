package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.segurancaacesso.AcessoFacade;
import br.edu.unipampa.usina.segurancaacesso.RegistroAcesso;

/**
 * Smoke RF07/RF08 — credencial simulada + RegistroAcesso via EventBus.
 */
public final class TestesAcesso {
    private TestesAcesso() {}

    public static void main(String[] args) {
        EventBus bus = new EventBus();
        AcessoFacade acesso = new AcessoFacade(bus);

        RegistroAcesso ok = acesso.solicitarAcesso("OPER-01", "SALA-CONTROLE");
        exigir("SUCESSO".equals(ok.getResultado()), "OPER-01 deve acessar SALA-CONTROLE");

        RegistroAcesso falha = acesso.solicitarAcesso("GUARDA-01", "CORE-REATOR");
        exigir("FALHA".equals(falha.getResultado()), "GUARDA-01 nao deve acessar CORE-REATOR");

        exigir(acesso.listarRegistros().size() == 2, "Devem existir 2 registros de acesso");
        System.out.println("[SUCESSO] RF08 RegistroAcesso: SUCESSO + FALHA.");
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }
}
