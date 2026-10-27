package br.edu.unipampa.usina.autenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro e gestão das passkeys do usuário logado (RNF08 — segundo fator por biometria). */
@RestController
@RequestMapping("/api/auth/biometria")
public class BiometriaController {
    private static final String CADASTRO_PENDENTE = "biometria.cadastro.pendente";

    private final BiometriaService biometria;
    private final UsuarioRepository usuarios;
    private final RegistroAutenticacaoRepository registros;
    private final ObjectMapper json;

    public BiometriaController(BiometriaService biometria, UsuarioRepository usuarios,
                               RegistroAutenticacaoRepository registros, ObjectMapper json) {
        this.biometria = biometria;
        this.usuarios = usuarios;
        this.registros = registros;
        this.json = json;
    }

    @GetMapping
    public List<AuthDtos.CredencialResponse> listar(Authentication authentication) {
        return biometria.listar(authentication.getName()).stream()
            .map(c -> new AuthDtos.CredencialResponse(c.getId(), c.getApelido(), c.getCriadoEm(), c.getUltimoUsoEm()))
            .toList();
    }

    @PostMapping("/cadastro/opcoes")
    public JsonNode iniciarCadastro(Authentication authentication, HttpServletRequest request) throws IOException {
        PublicKeyCredentialCreationOptions opcoes = biometria.iniciarCadastro(authentication.getName());
        request.getSession(true).setAttribute(CADASTRO_PENDENTE, opcoes.toJson());
        return json.readTree(opcoes.toCredentialsCreateJson()).get("publicKey");
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> concluirCadastro(@RequestBody AuthDtos.CadastroBiometriaRequest entrada,
                                              Authentication authentication, HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        String pendente = sessao == null ? null : (String) sessao.getAttribute(CADASTRO_PENDENTE);
        if (pendente == null || entrada.credencial() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Inicie o cadastro da biometria novamente."));
        }
        sessao.removeAttribute(CADASTRO_PENDENTE);
        String email = authentication.getName();
        try {
            CredencialBiometrica credencial =
                biometria.concluirCadastro(email, pendente, entrada.credencial().toString(), entrada.apelido());
            registros.save(new RegistroAutenticacao(credencial.getUsuario(), email, "BIOMETRIA_CADASTRADA"));
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthDtos.CredencialResponse(
                credencial.getId(), credencial.getApelido(), credencial.getCriadoEm(), credencial.getUltimoUsoEm()));
        } catch (BiometriaService.BiometriaRecusadaException ex) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/remover")
    public ResponseEntity<?> remover(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        try {
            if (!biometria.remover(email, id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", "Biometria não encontrada."));
            }
        } catch (BiometriaService.BiometriaRecusadaException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", ex.getMessage()));
        }
        registros.save(new RegistroAutenticacao(usuarios.findByEmailIgnoreCase(email).orElseThrow(), email, "BIOMETRIA_REMOVIDA"));
        return ResponseEntity.noContent().build();
    }
}
