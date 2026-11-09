package br.edu.unipampa.usina.autenticacao;

/**
 * RF18 — adaptador para o celular do usuário, isolado da regra de negócio (camadas do backend,
 * docs/marco2/01-arquitetura-hibrida.md). Em produção seria push/SMS de um provedor; no MVP é o
 * {@link CelularSimulado}.
 */
public interface CanalCelular {
    void enviar(SegundoFatorService.Desafio desafio);
}
