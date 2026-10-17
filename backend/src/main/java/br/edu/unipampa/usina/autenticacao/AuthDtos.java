package br.edu.unipampa.usina.autenticacao;

import java.util.List;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(String email, String senha) { }
    public record UsuarioResponse(String nome, String email, List<String> papeis) { }
}
