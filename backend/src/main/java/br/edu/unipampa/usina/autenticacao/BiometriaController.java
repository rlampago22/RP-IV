package br.edu.unipampa.usina.autenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Digital do usuário logado (RNF08): cadastro com o código de ativação do Administrador (UC02,
 * "previamente cadastrado") e confirmação antes das ações críticas ({@link ConfirmacaoBiometrica}).
 */
@RestController
@RequestMapping("/api/auth/biometria")
public class BiometriaController {
    private static final String CADASTRO_REQUISICAO = "biometria.cadastro.requisicao";
    private static final String CADASTRO_CODIGO_ID = "biometria.cadastro.codigo";
    private static final String CADASTRO_TENTATIVAS = "biometria.cadastro.tentativas";
    private static final String CONFIRMACAO_DESAFIO = "biometria.confirmacao.desafio";
    private static final String CONFIRMACAO_EXPIRA = "biometria.confirmacao.expira";
    private static final int MAX_TENTATIVAS_CODIGO = 5;

    private final BiometriaService biometria;
    private final CodigoAtivacaoService codigos;
    private final ConfirmacaoBiometrica confirmacao;
    private final UsuarioRepository usuarios;
    private final AuditoriaAutenticacao registros;
    private final ObjectMapper json;

    public BiometriaController(BiometriaService biometria, CodigoAtivacaoService codigos, ConfirmacaoBiometrica confirmacao,
                               UsuarioRepository usuarios, AuditoriaAutenticacao registros, ObjectMapper json) {
        this.biometria = biometria;
        this.codigos = codigos;
        this.confirmacao = confirmacao;
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

    /** Cadastro, passo 1: confere o código de ativação e devolve o desafio para o leitor de digital. */
    @PostMapping("/cadastro/opcoes")
    public ResponseEntity<?> iniciarCadastro(@RequestBody AuthDtos.CodigoRequest entrada, Authentication authentication,
                                             HttpServletRequest request) throws IOException {
        String email = authentication.getName();
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        HttpSession sessao = request.getSession(true);

        Optional<CodigoAtivacao> codigo = codigos.conferir(usuario, entrada.codigo());
        if (codigo.isEmpty()) {
            registros.save(new RegistroAutenticacao(usuario, email, "CODIGO_INVALIDO", "CODIGO_ATIVACAO"));
            Integer anteriores = (Integer) sessao.getAttribute(CADASTRO_TENTATIVAS);
            int tentativas = (anteriores == null ? 0 : anteriores) + 1;
            if (tentativas >= MAX_TENTATIVAS_CODIGO) {
                // Proteção contra adivinhação: o código vigente deixa de valer, só o Administrador emite outro.
                codigos.invalidarVigente(usuario);
                sessao.removeAttribute(CADASTRO_TENTATIVAS);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("mensagem",
                    "Muitas tentativas com código errado: o código foi bloqueado. Peça um novo ao Administrador do Sistema."));
            }
            sessao.setAttribute(CADASTRO_TENTATIVAS, tentativas);
            return ResponseEntity.badRequest().body(Map.of("mensagem",
                "Código de ativação inválido ou expirado. Confira com o Administrador do Sistema."));
        }

        PublicKeyCredentialCreationOptions opcoes = biometria.iniciarCadastro(email);
        sessao.removeAttribute(CADASTRO_TENTATIVAS);
        sessao.setAttribute(CADASTRO_REQUISICAO, opcoes.toJson());
        sessao.setAttribute(CADASTRO_CODIGO_ID, codigo.get().getId());
        return ResponseEntity.ok(json.readTree(opcoes.toCredentialsCreateJson()).get("publicKey"));
    }

    /** Cadastro, passo 2: grava a digital e consome o código na mesma transação. */
    @PostMapping("/cadastro")
    public ResponseEntity<?> concluirCadastro(@RequestBody AuthDtos.CadastroBiometriaRequest entrada,
                                              Authentication authentication, HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        String requisicao = sessao == null ? null : (String) sessao.getAttribute(CADASTRO_REQUISICAO);
        Long codigoId = sessao == null ? null : (Long) sessao.getAttribute(CADASTRO_CODIGO_ID);
        if (requisicao == null || codigoId == null || entrada.credencial() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Informe o código de ativação antes de cadastrar."));
        }
        sessao.removeAttribute(CADASTRO_REQUISICAO);
        sessao.removeAttribute(CADASTRO_CODIGO_ID);
        String email = authentication.getName();
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        try {
            CredencialBiometrica credencial =
                codigos.ativar(codigoId, email, requisicao, entrada.credencial().toString(), entrada.apelido());
            registros.save(new RegistroAutenticacao(usuario, email, "BIOMETRIA_CADASTRADA", "CODIGO_ATIVACAO"));
            // O cadastro já exigiu a digital (userVerification = required): vale como confirmação recente.
            confirmacao.registrar(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthDtos.CredencialResponse(
                credencial.getId(), credencial.getApelido(), credencial.getCriadoEm(), credencial.getUltimoUsoEm()));
        } catch (BiometriaService.BiometriaRecusadaException ex) {
            registros.save(new RegistroAutenticacao(usuario, email, "FALHA_BIOMETRIA", "CODIGO_ATIVACAO"));
            return ResponseEntity.badRequest().body(Map.of("mensagem", ex.getMessage()));
        }
    }

    /** Confirmação antes de uma ação crítica, passo 1: desafio para o leitor de digital (2 min, uma tentativa). */
    @PostMapping("/confirmacao/opcoes")
    public ResponseEntity<?> iniciarConfirmacao(Authentication authentication, HttpServletRequest request) throws IOException {
        String email = authentication.getName();
        if (!biometria.possuiBiometria(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem",
                "Você ainda não tem digital cadastrada. Peça o código de ativação ao Administrador e cadastre na aba Segurança."));
        }
        AssertionRequest desafio = biometria.iniciarConfirmacao(email);
        HttpSession sessao = request.getSession(true);
        sessao.setAttribute(CONFIRMACAO_DESAFIO, desafio.toJson());
        sessao.setAttribute(CONFIRMACAO_EXPIRA, Instant.now().plusMillis(BiometriaService.TEMPO_LIMITE_MS));
        return ResponseEntity.ok(json.readTree(desafio.toCredentialsGetJson()).get("publicKey"));
    }

    /** Confirmação, passo 2: confere a assinatura; se válida, libera ações críticas por alguns minutos. */
    @PostMapping("/confirmacao")
    public ResponseEntity<?> concluirConfirmacao(@RequestBody JsonNode credencial, Authentication authentication,
                                                 HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        String desafio = sessao == null ? null : (String) sessao.getAttribute(CONFIRMACAO_DESAFIO);
        Instant expira = sessao == null ? null : (Instant) sessao.getAttribute(CONFIRMACAO_EXPIRA);
        if (desafio == null) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Peça a confirmação da digital novamente."));
        }
        sessao.removeAttribute(CONFIRMACAO_DESAFIO);
        sessao.removeAttribute(CONFIRMACAO_EXPIRA);
        String email = authentication.getName();
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        try {
            if (Instant.now().isAfter(expira)) {
                throw new BiometriaService.BiometriaRecusadaException("A confirmação expirou. Tente de novo.");
            }
            String dono = biometria.concluirConfirmacao(desafio, credencial.toString());
            if (!dono.equalsIgnoreCase(email)) {
                throw new BiometriaService.BiometriaRecusadaException("Digital de outro usuário.");
            }
        } catch (BiometriaService.BiometriaRecusadaException ex) {
            registros.save(new RegistroAutenticacao(usuario, email, "FALHA_BIOMETRIA", "ACAO_CRITICA"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensagem", "Digital não confirmada. Tente de novo."));
        }
        Instant validaAte = confirmacao.registrar(request);
        registros.save(new RegistroAutenticacao(usuario, email, "BIOMETRIA_CONFIRMADA", "ACAO_CRITICA"));
        return ResponseEntity.ok(Map.of("validaAte", validaAte));
    }

    @PostMapping("/{id}/remover")
    public ResponseEntity<?> remover(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request);
        String email = authentication.getName();
        if (!biometria.remover(email, id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", "Digital não encontrada."));
        }
        registros.save(new RegistroAutenticacao(usuarios.findByEmailIgnoreCase(email).orElseThrow(), email, "BIOMETRIA_REMOVIDA"));
        return ResponseEntity.noContent().build();
    }
}
