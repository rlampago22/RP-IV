package br.edu.unipampa.usina.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login da Central. 1º fator: e-mail e senha. 2º fator (RF18, spec do Álvaro): solicitação no celular
 * vinculado — o usuário aprova no celular ou digita o código recebido. A sessão só existe depois dos
 * dois fatores. A digital continua como confirmação das ações críticas (4.2.10, {@link ConfirmacaoBiometrica}).
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    static final String DESAFIO_NA_SESSAO = "2fa.desafio";

    private final AuthenticationManager authenticationManager;
    private final AutenticacaoService autenticacao;
    private final UsuarioRepository usuarios;
    private final AuditoriaAutenticacao registros;
    private final SegundoFatorService segundoFator;
    private final boolean segundoFatorNoLogin;

    public AutenticacaoController(AuthenticationManager authenticationManager, AutenticacaoService autenticacao,
                                  UsuarioRepository usuarios, AuditoriaAutenticacao registros,
                                  SegundoFatorService segundoFator,
                                  @Value("${app.segundo-fator.login:true}") boolean segundoFatorNoLogin) {
        this.authenticationManager = authenticationManager;
        this.autenticacao = autenticacao;
        this.usuarios = usuarios;
        this.registros = registros;
        this.segundoFator = segundoFator;
        this.segundoFatorNoLogin = segundoFatorNoLogin;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDtos.LoginRequest entrada, HttpServletRequest request, HttpServletResponse response) {
        String email = entrada.email() == null ? "" : entrada.email().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || entrada.senha() == null || entrada.senha().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "E-mail e senha são obrigatórios."));
        }
        Authentication autenticado;
        try {
            autenticado = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, entrada.senha()));
        } catch (BadCredentialsException ex) {
            registros.save(new RegistroAutenticacao(null, email, "FALHA"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensagem", "Credenciais inválidas."));
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();

        if (!segundoFatorNoLogin) {
            registros.save(new RegistroAutenticacao(usuario, email, "SUCESSO"));
            abrirSessao(autenticado, request, response);
            return ResponseEntity.ok(autenticacao.apresentar(email));
        }

        // RF18: senha certa ainda NÃO abre sessão; fica pendente até o celular confirmar.
        registros.save(new RegistroAutenticacao(usuario, email, "SENHA_OK_AGUARDANDO_2FA"));
        SegundoFatorService.Desafio desafio = segundoFator.iniciar(usuario, autenticado);
        request.getSession(true).setAttribute(DESAFIO_NA_SESSAO, desafio.getId());
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("segundoFator", true);
        corpo.put("mensagem", "Enviamos uma solicitação ao seu celular. Aprove lá ou digite o código recebido.");
        corpo.put("destino", desafio.getEmailMascarado());
        corpo.put("expiraEm", desafio.getExpiraEm());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(corpo);
    }

    /** RF18 — conclui com o código de 6 dígitos que chegou no celular. */
    @PostMapping("/segundo-fator/codigo")
    public AuthDtos.UsuarioResponse concluirComCodigo(@RequestBody AuthDtos.CodigoRequest entrada,
                                                      HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticado = segundoFator.concluirPorCodigo(desafioDaSessao(request), entrada.codigo());
        return finalizar(autenticado, request, response);
    }

    /** RF18 — conclui depois de o usuário aprovar a solicitação no celular. */
    @PostMapping("/segundo-fator/aprovacao")
    public AuthDtos.UsuarioResponse concluirComAprovacao(HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticado = segundoFator.concluirPorAprovacao(desafioDaSessao(request));
        return finalizar(autenticado, request, response);
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

    @ExceptionHandler(SegundoFatorService.SegundoFatorException.class)
    ResponseEntity<Map<String, Object>> segundoFatorRecusado(SegundoFatorService.SegundoFatorException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensagem", ex.getMessage()));
    }

    private AuthDtos.UsuarioResponse finalizar(Authentication autenticado, HttpServletRequest request, HttpServletResponse response) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) sessao.removeAttribute(DESAFIO_NA_SESSAO);
        abrirSessao(autenticado, request, response);
        return autenticacao.apresentar(autenticado.getName());
    }

    private static String desafioDaSessao(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        return sessao == null ? null : (String) sessao.getAttribute(DESAFIO_NA_SESSAO);
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
