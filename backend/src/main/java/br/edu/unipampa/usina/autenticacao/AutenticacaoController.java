package br.edu.unipampa.usina.autenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

/**
 * Login em dois fatores (RNF08). Com a biometria obrigatória, senha certa nunca abre a sessão sozinha:
 * <ul>
 *   <li>tem celular cadastrado → etapa {@code BIOMETRIA}: confirmar no celular ({@code /login/biometria});</li>
 *   <li>não tem → etapa {@code PRIMEIRO_ACESSO}: cadastrar o celular com o código de ativação emitido pelo
 *       Administrador ({@code /primeiro-acesso/**}), como pede o UC02 ("funcionário previamente cadastrado").</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    // Senha conferida, aguardando o segundo fator. Some ao concluir, ao errar o limite ou ao expirar.
    private static final String PENDENTE_EMAIL = "login.pendente.email";
    private static final String PENDENTE_ETAPA = "login.pendente.etapa";
    private static final String PENDENTE_EXPIRA = "login.pendente.expira";
    private static final String PENDENTE_DESAFIO = "login.pendente.desafio";
    private static final String PENDENTE_TENTATIVAS = "login.pendente.tentativas";
    private static final String PENDENTE_CADASTRO = "login.pendente.cadastro";
    private static final String PENDENTE_CODIGO_ID = "login.pendente.codigo";

    private static final String ETAPA_BIOMETRIA = "BIOMETRIA";
    private static final String ETAPA_PRIMEIRO_ACESSO = "PRIMEIRO_ACESSO";
    private static final Duration PRAZO_PRIMEIRO_ACESSO = Duration.ofMinutes(10);
    private static final int MAX_TENTATIVAS_CODIGO = 5;

    private final AuthenticationManager authenticationManager;
    private final AutenticacaoService autenticacao;
    private final BiometriaService biometria;
    private final CodigoAtivacaoService codigos;
    private final UsuarioRepository usuarios;
    private final RegistroAutenticacaoRepository registros;
    private final ObjectMapper json;

    public AutenticacaoController(AuthenticationManager authenticationManager, AutenticacaoService autenticacao,
                                  BiometriaService biometria, CodigoAtivacaoService codigos, UsuarioRepository usuarios,
                                  RegistroAutenticacaoRepository registros, ObjectMapper json) {
        this.authenticationManager = authenticationManager;
        this.autenticacao = autenticacao;
        this.biometria = biometria;
        this.codigos = codigos;
        this.usuarios = usuarios;
        this.registros = registros;
        this.json = json;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDtos.LoginRequest entrada, HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
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

        if (biometria.possuiBiometria(email)) {
            AssertionRequest desafio = biometria.iniciarConfirmacao(email);
            HttpSession sessao = iniciarPendencia(request, email, ETAPA_BIOMETRIA,
                Duration.ofMillis(BiometriaService.TEMPO_LIMITE_MS));
            sessao.setAttribute(PENDENTE_DESAFIO, desafio.toJson());
            JsonNode opcoes = json.readTree(desafio.toCredentialsGetJson()).get("publicKey");
            return ResponseEntity.ok(new AuthDtos.ProximaEtapaResponse(ETAPA_BIOMETRIA, opcoes));
        }
        if (biometria.isObrigatoria()) {
            iniciarPendencia(request, email, ETAPA_PRIMEIRO_ACESSO, PRAZO_PRIMEIRO_ACESSO)
                .setAttribute(PENDENTE_TENTATIVAS, 0);
            return ResponseEntity.ok(new AuthDtos.ProximaEtapaResponse(ETAPA_PRIMEIRO_ACESSO, null));
        }

        // Só com app.webauthn.obrigatoria=false (desenvolvimento sem celular).
        abrirSessao(autenticado, request, response);
        registros.save(new RegistroAutenticacao(usuario, email, "SUCESSO"));
        return ResponseEntity.ok(autenticacao.apresentar(email));
    }

    /** Etapa BIOMETRIA: recebe a assinatura feita pelo celular (resposta do navigator.credentials.get). */
    @PostMapping("/login/biometria")
    public ResponseEntity<?> confirmarBiometria(@RequestBody JsonNode credencial, HttpServletRequest request,
                                                HttpServletResponse response) {
        HttpSession sessao = request.getSession(false);
        Optional<String> pendente = emailPendente(sessao, ETAPA_BIOMETRIA);
        if (pendente.isEmpty()) return semPendencia();
        String email = pendente.get();
        String desafio = (String) sessao.getAttribute(PENDENTE_DESAFIO);
        boolean expirou = expirou(sessao);
        // Uma tentativa por desafio: errou ou expirou, volta para a senha.
        encerrarPendencia(sessao);

        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        if (expirou) {
            registros.save(new RegistroAutenticacao(usuario, email, "FALHA_BIOMETRIA", "SENHA_BIOMETRIA"));
            return naoAutorizado("A confirmação expirou. Entre novamente.");
        }
        try {
            String dono = biometria.concluirConfirmacao(desafio, credencial.toString());
            if (!dono.equalsIgnoreCase(email)) {
                throw new BiometriaService.BiometriaRecusadaException("Passkey de outro usuário.");
            }
        } catch (BiometriaService.BiometriaRecusadaException ex) {
            registros.save(new RegistroAutenticacao(usuario, email, "FALHA_BIOMETRIA", "SENHA_BIOMETRIA"));
            return naoAutorizado("Biometria não confirmada. Entre novamente.");
        }

        abrirSessao(email, request, response);
        registros.save(new RegistroAutenticacao(usuario, email, "SUCESSO", "SENHA_BIOMETRIA"));
        return ResponseEntity.ok(autenticacao.apresentar(email));
    }

    /** Etapa PRIMEIRO_ACESSO, passo 1: confere o código de ativação e devolve o desafio de cadastro do celular. */
    @PostMapping("/primeiro-acesso/opcoes")
    public ResponseEntity<?> opcoesPrimeiroAcesso(@RequestBody AuthDtos.CodigoRequest entrada,
                                                  HttpServletRequest request) throws IOException {
        HttpSession sessao = request.getSession(false);
        Optional<String> pendente = emailPendente(sessao, ETAPA_PRIMEIRO_ACESSO);
        if (pendente.isEmpty()) return semPendencia();
        String email = pendente.get();
        if (expirou(sessao)) {
            encerrarPendencia(sessao);
            return naoAutorizado("O tempo para o primeiro acesso acabou. Entre novamente.");
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();

        Optional<CodigoAtivacao> codigo = codigos.conferir(usuario, entrada.codigo());
        if (codigo.isEmpty()) {
            registros.save(new RegistroAutenticacao(usuario, email, "CODIGO_INVALIDO", "PRIMEIRO_ACESSO"));
            int tentativas = (Integer) sessao.getAttribute(PENDENTE_TENTATIVAS) + 1;
            if (tentativas >= MAX_TENTATIVAS_CODIGO) {
                encerrarPendencia(sessao);
                return naoAutorizado("Muitas tentativas com código errado. Entre novamente com a senha.");
            }
            sessao.setAttribute(PENDENTE_TENTATIVAS, tentativas);
            return ResponseEntity.badRequest().body(Map.of("mensagem",
                "Código de ativação inválido ou expirado. Confira com o Administrador do Sistema."));
        }

        PublicKeyCredentialCreationOptions opcoes = biometria.iniciarCadastro(email);
        sessao.setAttribute(PENDENTE_CADASTRO, opcoes.toJson());
        sessao.setAttribute(PENDENTE_CODIGO_ID, codigo.get().getId());
        return ResponseEntity.ok(json.readTree(opcoes.toCredentialsCreateJson()).get("publicKey"));
    }

    /** Etapa PRIMEIRO_ACESSO, passo 2: grava a biometria, consome o código e abre a sessão. */
    @PostMapping("/primeiro-acesso/concluir")
    public ResponseEntity<?> concluirPrimeiroAcesso(@RequestBody AuthDtos.CadastroBiometriaRequest entrada,
                                                    HttpServletRequest request, HttpServletResponse response) {
        HttpSession sessao = request.getSession(false);
        Optional<String> pendente = emailPendente(sessao, ETAPA_PRIMEIRO_ACESSO);
        if (pendente.isEmpty()) return semPendencia();
        String email = pendente.get();
        String requisicao = (String) sessao.getAttribute(PENDENTE_CADASTRO);
        Long codigoId = (Long) sessao.getAttribute(PENDENTE_CODIGO_ID);
        if (requisicao == null || codigoId == null || entrada.credencial() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Informe o código de ativação antes de cadastrar."));
        }
        if (expirou(sessao)) {
            encerrarPendencia(sessao);
            return naoAutorizado("O tempo para o primeiro acesso acabou. Entre novamente.");
        }
        sessao.removeAttribute(PENDENTE_CADASTRO);

        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        try {
            codigos.ativar(codigoId, email, requisicao, entrada.credencial().toString(), entrada.apelido());
        } catch (BiometriaService.BiometriaRecusadaException ex) {
            registros.save(new RegistroAutenticacao(usuario, email, "FALHA_BIOMETRIA", "PRIMEIRO_ACESSO"));
            return ResponseEntity.badRequest().body(Map.of("mensagem", ex.getMessage()));
        }

        encerrarPendencia(sessao);
        abrirSessao(email, request, response);
        registros.save(new RegistroAutenticacao(usuario, email, "BIOMETRIA_CADASTRADA", "PRIMEIRO_ACESSO"));
        registros.save(new RegistroAutenticacao(usuario, email, "SUCESSO", "PRIMEIRO_ACESSO"));
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

    private static HttpSession iniciarPendencia(HttpServletRequest request, String email, String etapa, Duration prazo) {
        HttpSession sessao = request.getSession(true);
        encerrarPendencia(sessao);
        sessao.setAttribute(PENDENTE_EMAIL, email);
        sessao.setAttribute(PENDENTE_ETAPA, etapa);
        sessao.setAttribute(PENDENTE_EXPIRA, Instant.now().plus(prazo));
        return sessao;
    }

    private static Optional<String> emailPendente(HttpSession sessao, String etapa) {
        if (sessao == null || !etapa.equals(sessao.getAttribute(PENDENTE_ETAPA))) return Optional.empty();
        return Optional.ofNullable((String) sessao.getAttribute(PENDENTE_EMAIL));
    }

    private static boolean expirou(HttpSession sessao) {
        return Instant.now().isAfter((Instant) sessao.getAttribute(PENDENTE_EXPIRA));
    }

    private static void encerrarPendencia(HttpSession sessao) {
        for (String atributo : new String[] {PENDENTE_EMAIL, PENDENTE_ETAPA, PENDENTE_EXPIRA, PENDENTE_DESAFIO,
                PENDENTE_TENTATIVAS, PENDENTE_CADASTRO, PENDENTE_CODIGO_ID}) {
            sessao.removeAttribute(atributo);
        }
    }

    private static ResponseEntity<Map<String, String>> semPendencia() {
        return naoAutorizado("Entre com e-mail e senha antes de confirmar a biometria.");
    }

    private static ResponseEntity<Map<String, String>> naoAutorizado(String mensagem) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensagem", mensagem));
    }

    private void abrirSessao(String email, HttpServletRequest request, HttpServletResponse response) {
        UserDetails detalhes = autenticacao.loadUserByUsername(email);
        abrirSessao(UsernamePasswordAuthenticationToken.authenticated(detalhes, null, detalhes.getAuthorities()),
            request, response);
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
