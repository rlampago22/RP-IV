package br.edu.unipampa.usina.autenticacao;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tela do "celular" na demonstração do RF18 (página /celular do front). Faz o papel do aplicativo no
 * aparelho do usuário: lista as solicitações recebidas e deixa aprovar ou recusar. Só é ligado com
 * {@code app.segundo-fator.celular-simulado=true} (padrão no ambiente de desenvolvimento).
 */
@RestController
@RequestMapping("/api/celular-simulado")
@ConditionalOnProperty(name = "app.segundo-fator.celular-simulado", havingValue = "true", matchIfMissing = true)
public class CelularSimuladoController {
    public record Notificacao(String id, String usuario, String destino, String codigo, String status, Instant expiraEm) { }

    private final CelularSimulado celular;
    private final SegundoFatorService segundoFator;

    public CelularSimuladoController(CelularSimulado celular, SegundoFatorService segundoFator) {
        this.celular = celular;
        this.segundoFator = segundoFator;
    }

    @GetMapping("/notificacoes")
    public List<Notificacao> notificacoes() {
        return celular.recebidas().stream()
            .map(d -> new Notificacao(d.getId(), d.getNomeUsuario(), d.getEmailMascarado(), d.getCodigo(),
                Instant.now().isAfter(d.getExpiraEm()) && d.getStatus() == SegundoFatorService.Status.PENDENTE
                    ? "EXPIRADO" : d.getStatus().name(),
                d.getExpiraEm()))
            .toList();
    }

    @PostMapping("/{id}/aprovar")
    public Map<String, String> aprovar(@PathVariable String id) {
        return Map.of("status", segundoFator.responderNoCelular(id, true).name());
    }

    @PostMapping("/{id}/recusar")
    public Map<String, String> recusar(@PathVariable String id) {
        return Map.of("status", segundoFator.responderNoCelular(id, false).name());
    }

    @ExceptionHandler(SegundoFatorService.SegundoFatorException.class)
    ResponseEntity<Map<String, String>> invalida(SegundoFatorService.SegundoFatorException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", ex.getMessage()));
    }
}
