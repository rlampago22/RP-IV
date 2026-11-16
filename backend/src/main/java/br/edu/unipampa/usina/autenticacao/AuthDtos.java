package br.edu.unipampa.usina.autenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(String email, String senha) { }
    public record UsuarioResponse(String nome, String email, List<String> papeis, boolean biometriaCadastrada) { }

    public record CodigoRequest(String codigo) { }
    public record CadastroBiometriaRequest(String apelido, JsonNode credencial) { }
    public record CredencialResponse(Long id, String apelido, Instant criadoEm, Instant ultimoUsoEm) { }
    public record CodigoAtivacaoResponse(String email, String codigo, Instant expiraEm) { }
    public record UsuarioAdminResponse(Long id, String nome, String email, List<String> papeis, int aparelhos,
                                       Instant codigoValidoAte) { }
}
