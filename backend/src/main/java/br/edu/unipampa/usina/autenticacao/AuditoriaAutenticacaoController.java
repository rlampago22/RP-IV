package br.edu.unipampa.usina.autenticacao;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RNF03/RNF05 — o Administrador consulta a trilha de autenticação (inclui o 2FA do RF18) e confere se a
 * cadeia de hash está íntegra. Mostra só resultado, método e e-mail: nunca código nem telefone.
 */
@RestController
@RequestMapping("/api/admin/auditoria/autenticacao")
@PreAuthorize("hasRole('ADMINISTRADOR_SISTEMA')")
public class AuditoriaAutenticacaoController {
    public record Linha(Long id, Instant registradoEm, String email, String metodo, String resultado, String hash) { }

    private final AuditoriaAutenticacao auditoria;

    public AuditoriaAutenticacaoController(AuditoriaAutenticacao auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public Map<String, Object> consultar() {
        List<Linha> linhas = auditoria.todos().stream()
            .map(r -> new Linha(r.getId(), r.getRegistradoEm(), r.getEmailInformado(), r.getMetodo(), r.getResultado(),
                r.getHash().substring(0, 12)))
            .toList();
        Long adulterado = auditoria.primeiroRegistroAdulterado();
        return Map.of(
            "integra", adulterado == null,
            "primeiroAdulterado", adulterado == null ? "" : adulterado,
            "total", linhas.size(),
            "registros", linhas.subList(Math.max(0, linhas.size() - 50), linhas.size()));
    }
}
