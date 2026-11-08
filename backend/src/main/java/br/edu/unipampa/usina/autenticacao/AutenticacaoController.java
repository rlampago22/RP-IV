package br.edu.unipampa.usina.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    private final AuthenticationManager authenticationManager;
    private final AutenticacaoService autenticacao;
    private final UsuarioRepository usuarios;
    private final RegistroAutenticacaoRepository registros;

    public AutenticacaoController(AuthenticationManager authenticationManager, AutenticacaoService autenticacao,
                                  UsuarioRepository usuarios, RegistroAutenticacaoRepository registros) {
        this.authenticationManager = authenticationManager;
        this.autenticacao = autenticacao;
        this.usuarios = usuarios;
        this.registros = registros;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDtos.LoginRequest entrada, HttpServletRequest request, HttpServletResponse response) {
        String email = entrada.email() == null ? "" : entrada.email().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || entrada.senha() == null || entrada.senha().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("mensagem", "E-mail e senha são obrigatórios."));
        }
        try {
            Authentication autenticado = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, entrada.senha()));
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticado);
            SecurityContextHolder.setContext(contexto);
            HttpSession sessao = request.getSession(true);
            new HttpSessionSecurityContextRepository().saveContext(contexto, request, response);
            Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
            registros.save(new RegistroAutenticacao(usuario, email, "SUCESSO"));
            return ResponseEntity.ok(autenticacao.apresentar(email));
        } catch (BadCredentialsException ex) {
            registros.save(new RegistroAutenticacao(null, email, "FALHA"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("mensagem", "Credenciais inválidas."));
        }
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
}
