package br.edu.unipampa.usina.autenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    // Login com senha conferida, aguardando a confirmação no celular (segundo fator).
    private static final String PENDENTE_EMAIL = "login.pendente.email";
    private static final String PENDENTE_DESAFIO = "login.pendente.desafio";
    private static final String PENDENTE_EXPIRA = "login.pendente.expira";

    private final AuthenticationManager authenticationManager;
    private final AutenticacaoService autenticacao;
    private final BiometriaService biometria;
    private final UsuarioRepository usuarios;
    private final RegistroAutenticacaoRepository registros;
    private final CredencialBiometricaRepository credenciais;
    private final ObjectMapper json;

    public AutenticacaoController(AuthenticationManager authenticationManager, AutenticacaoService autenticacao,
                                  BiometriaService biometria, UsuarioRepository usuarios,
                                  RegistroAutenticacaoRepository registros, CredencialBiometricaRepository credenciais,
                                  ObjectMapper json) {
        this.authenticationManager = authenticationManager;
        this.autenticacao = autenticacao;
        this.biometria = biometria;
        this.usuarios = usuarios;
        this.registros = registros;
        this.credenciais = credenciais;
        this.json = json;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDtos.LoginRequest entrada, HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        String email = entrada.email() == null ? "" : entrada.email().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || entrada.senha() == null || entrada.senha().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("mensagem", "E-mail e senha são obrigatórios."));
        }
        Authentication autenticado;
        try {
            autenticado = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, entrada.senha()));
        } catch (BadCredentialsException ex) {
            registros.save(new RegistroAutenticacao(null, email, "FALHA"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("mensagem", "Credenciais inválidas."));
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();

        if (credenciais.existsByUsuarioEmailIgnoreCase(email)) {
            // Senha certa não basta: a sessão só abre depois da biometria em /login/biometria.
            AssertionRequest desafio = biometria.iniciarConfirmacao(email);
            HttpSession sessao = request.getSession(true);
            sessao.setAttribute(PENDENTE_EMAIL, email);
            sessao.setAttribute(PENDENTE_DESAFIO, desafio.toJson());
            sessao.setAttribute(PENDENTE_EXPIRA, Instant.now().plusMillis(BiometriaService.TEMPO_LIMITE_MS));
            JsonNode opcoes = json.readTree(desafio.toCredentialsGetJson()).get("publicKey");
            return ResponseEntity.ok(new AuthDtos.EtapaBiometriaResponse("BIOMETRIA", opcoes));
        }

        abrirSessao(autenticado, request, response);
        registros.save(new RegistroAutenticacao(usuario, email, "SUCESSO"));
        return ResponseEntity.ok(autenticacao.apresentar(email));
    }

    /** Segunda etapa: recebe a assinatura feita pelo celular (resposta do navigator.credentials.get). */
    @PostMapping("/login/biometria")
    public ResponseEntity<?> confirmarBiometria(@RequestBody JsonNode credencial, HttpServletRequest request,
                                                HttpServletResponse response) {
        HttpSession sessao = request.getSession(false);
        String email = sessao == null ? null : (String) sessao.getAttribute(PENDENTE_EMAIL);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(java.util.Map.of("mensagem", "Entre com e-mail e senha antes de confirmar a biometria."));
        }
        String desafio = (String) sessao.getAttribute(PENDENTE_DESAFIO);
        Instant expira = (Instant) sessao.getAttribute(PENDENTE_EXPIRA);
        // Uma tentativa por desafio: errou ou expirou, volta para a senha.
        sessao.removeAttribute(PENDENTE_EMAIL);
        sessao.removeAttribute(PENDENTE_DESAFIO);
        sessao.removeAttribute(PENDENTE_EXPIRA);

        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        if (Instant.now().isAfter(expira)) {
            registros.save(new RegistroAutenticacao(usuario, email, "FALHA_BIOMETRIA", "SENHA_BIOMETRIA"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(java.util.Map.of("mensagem", "A confirmação expirou. Entre novamente."));
        }
        try {
            String dono = biometria.concluirConfirmacao(desafio, credencial.toString());
            if (!dono.equalsIgnoreCase(email)) {
                throw new BiometriaService.BiometriaRecusadaException("Passkey de outro usuário.");
            }
        } catch (BiometriaService.BiometriaRecusadaException ex) {
            registros.save(new RegistroAutenticacao(usuario, email, "FALHA_BIOMETRIA", "SENHA_BIOMETRIA"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(java.util.Map.of("mensagem", "Biometria não confirmada. Entre novamente."));
        }

        UserDetails detalhes = autenticacao.loadUserByUsername(email);
        abrirSessao(UsernamePasswordAuthenticationToken.authenticated(detalhes, null, detalhes.getAuthorities()),
            request, response);
        registros.save(new RegistroAutenticacao(usuario, email, "SUCESSO", "SENHA_BIOMETRIA"));
        return ResponseEntity.ok(autenticacao.apresentar(email));
    }

    @GetMapping("/me")
    public AuthDtos.UsuarioResponse eu(Authentication authentication) {
        return autenticacao.apresentar(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) sessao.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private static void abrirSessao(Authentication autenticado, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticado);
        SecurityContextHolder.setContext(contexto);
        request.getSession(true);
        request.changeSessionId(); // evita fixação de sessão: o id anterior ao login deixa de valer
        new HttpSessionSecurityContextRepository().saveContext(contexto, request, response);
    }
}
