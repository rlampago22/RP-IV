package br.edu.unipampa.usina.autenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(String email, String senha) { }
    public record UsuarioResponse(String nome, String email, List<String> papeis, boolean biometriaCadastrada) { }

    /** Senha conferida; falta confirmar no celular. {@code opcoes} vai direto para o WebAuthn do navegador. */
    public record EtapaBiometriaResponse(String etapa, JsonNode opcoes) { }
    public record CadastroBiometriaRequest(String apelido, JsonNode credencial) { }
    public record CredencialResponse(Long id, String apelido, Instant criadoEm, Instant ultimoUsoEm) { }
}
