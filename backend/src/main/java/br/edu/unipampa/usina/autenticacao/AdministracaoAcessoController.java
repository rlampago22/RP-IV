package br.edu.unipampa.usina.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC02 / 4.2.7 do PDF: o Administrador do Sistema gerencia a autenticação biométrica dos funcionários —
 * emite o código de ativação (cadastro prévio) e revoga a biometria quando o celular é perdido.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR_SISTEMA')")
public class AdministracaoAcessoController {
    private final UsuarioRepository usuarios;
    private final BiometriaService biometria;
    private final CodigoAtivacaoService codigos;
    private final RegistroAutenticacaoRepository registros;
    private final ConfirmacaoBiometrica confirmacao;

    public AdministracaoAcessoController(UsuarioRepository usuarios, BiometriaService biometria,
                                         CodigoAtivacaoService codigos, RegistroAutenticacaoRepository registros,
                                         ConfirmacaoBiometrica confirmacao) {
        this.usuarios = usuarios;
        this.biometria = biometria;
        this.codigos = codigos;
        this.registros = registros;
        this.confirmacao = confirmacao;
    }

    @GetMapping
    public List<AuthDtos.UsuarioAdminResponse> listar() {
        return usuarios.findAll().stream()
            .sorted(Comparator.comparing(Usuario::getNome))
            .map(u -> new AuthDtos.UsuarioAdminResponse(
                u.getId(), u.getNome(), u.getEmail(),
                u.getPapeis().stream().map(Papel::getCodigo).sorted().toList(),
                biometria.listar(u.getEmail()).size(),
                codigos.vigente(u).map(CodigoAtivacao::getExpiraEm).orElse(null)))
            .toList();
    }

    @PostMapping("/{id}/codigo-ativacao")
    public ResponseEntity<?> emitirCodigo(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request); // ação crítica: digital do Administrador
        Usuario alvo = usuarios.findById(id).orElse(null);
        if (alvo == null) return naoEncontrado();
        Usuario administrador = usuarios.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        AuthDtos.CodigoAtivacaoResponse emitido = codigos.emitir(alvo, administrador);
        registros.save(new RegistroAutenticacao(alvo, administrador.getEmail(), "CODIGO_EMITIDO", "ADMINISTRADOR"));
        return ResponseEntity.status(HttpStatus.CREATED).body(emitido);
    }

    /** Celular/leitor perdido ou trocado: tira todas as digitais; para cadastrar de novo, o funcionário precisa de um código novo. */
    @PostMapping("/{id}/biometria/revogar")
    public ResponseEntity<?> revogarBiometria(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request); // ação crítica: digital do Administrador
        Usuario alvo = usuarios.findById(id).orElse(null);
        if (alvo == null) return naoEncontrado();
        int removidos = biometria.revogarTodas(alvo.getEmail());
        registros.save(new RegistroAutenticacao(alvo, authentication.getName(), "BIOMETRIA_REVOGADA", "ADMINISTRADOR"));
        return ResponseEntity.ok(Map.of("removidos", removidos, "revogadoEm", Instant.now()));
    }

    private static ResponseEntity<Map<String, String>> naoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", "Usuário não encontrado."));
    }
}
