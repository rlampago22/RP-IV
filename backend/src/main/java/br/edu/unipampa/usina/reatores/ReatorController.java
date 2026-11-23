package br.edu.unipampa.usina.reatores;

import br.edu.unipampa.usina.autenticacao.ConfirmacaoBiometrica;
import br.edu.unipampa.usina.reatores.CadastroReatoresService.AjusteSensor;
import br.edu.unipampa.usina.reatores.CadastroReatoresService.Reator;
import br.edu.unipampa.usina.reatores.CadastroReatoresService.Sensor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issue #96 — tela Reatores. Só o Administrador do Sistema; incluir, ajustar, retirar e devolver à
 * operação mudam a usina e são ações críticas (digital, RNF08). Tudo por POST porque o CORS da Central
 * só libera GET/POST.
 */
@RestController
@RequestMapping("/api/reatores")
@PreAuthorize("hasRole('ADMINISTRADOR_SISTEMA')")
public class ReatorController {
    public record Ajustes(List<AjusteSensor> sensores) { }

    private final CadastroReatoresService cadastro;
    private final ConfirmacaoBiometrica confirmacao;

    public ReatorController(CadastroReatoresService cadastro, ConfirmacaoBiometrica confirmacao) {
        this.cadastro = cadastro;
        this.confirmacao = confirmacao;
    }

    @GetMapping
    public List<Reator> listar() {
        return cadastro.listar();
    }

    /** Faixas e setpoints com que um reator novo entra, para a tela já abrir o formulário preenchido. */
    @GetMapping("/padrao")
    public List<Sensor> padrao() {
        return CadastroReatoresService.SENSORES_PADRAO;
    }

    @PostMapping
    public ResponseEntity<Reator> incluir(@RequestBody(required = false) Ajustes ajustes,
                                          Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request); // ação crítica: digital do Administrador
        Reator novo = cadastro.incluir(ajustes == null ? null : ajustes.sensores(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    @PostMapping("/{id}")
    public Reator ajustar(@PathVariable String id, @RequestBody Ajustes ajustes,
                          Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request);
        return cadastro.ajustar(id, ajustes == null ? null : ajustes.sensores(), authentication.getName());
    }

    @PostMapping("/{id}/retirar")
    public Reator retirar(@PathVariable String id, Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request);
        return cadastro.definirOperacao(id, false, authentication.getName());
    }

    @PostMapping("/{id}/reativar")
    public Reator reativar(@PathVariable String id, Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request);
        return cadastro.definirOperacao(id, true, authentication.getName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflito(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> naoEncontrado(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", ex.getMessage()));
    }
}
