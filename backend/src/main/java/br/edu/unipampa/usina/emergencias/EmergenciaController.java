package br.edu.unipampa.usina.emergencias;

import br.edu.unipampa.usina.autenticacao.ConfirmacaoBiometrica;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
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
 * RF09 (criar emergência) e RF10 (ativar protocolo de contingência). Registrar a emergência é rápido,
 * só com a sessão; ativar o protocolo e encerrar são ações críticas e pedem a digital
 * ({@link ConfirmacaoBiometrica}, 4.2.10 do PDF: dois fatores para evitar ativação acidental).
 */
@RestController
@RequestMapping("/api/emergencias")
@PreAuthorize("hasAnyRole('OPERADOR_REATOR', 'SUPERVISAO_CENTRAL', 'ADMINISTRADOR_SISTEMA')")
public class EmergenciaController {
    public record NovaEmergencia(String tipo, String descricao) { }
    public record AtivacaoProtocolo(String protocolo) { }
    public record ProtocoloResponse(String codigo, String nome, List<String> passos) { }
    public record EmergenciaResponse(Long id, String tipo, String descricao, String status, String criadaPor,
                                     Instant criadaEm, ProtocoloResponse protocolo, String protocoloAtivadoPor,
                                     Instant protocoloAtivadoEm, String encerradaPor, Instant encerradaEm) { }

    private final GestaoEmergencias gestao;
    private final ConfirmacaoBiometrica confirmacao;

    public EmergenciaController(GestaoEmergencias gestao, ConfirmacaoBiometrica confirmacao) {
        this.gestao = gestao;
        this.confirmacao = confirmacao;
    }

    @GetMapping
    public List<EmergenciaResponse> listar() {
        return gestao.listar().stream().map(EmergenciaController::resposta).toList();
    }

    @GetMapping("/protocolos")
    public List<ProtocoloResponse> protocolos() {
        return Arrays.stream(ProtocoloEmergencia.values()).map(EmergenciaController::resposta).toList();
    }

    @PostMapping
    public ResponseEntity<EmergenciaResponse> criar(@RequestBody NovaEmergencia nova, Authentication authentication) {
        if (nova.descricao() == null || nova.descricao().isBlank()) {
            throw new IllegalArgumentException("Descreva a emergência.");
        }
        if (nova.descricao().length() > 400) throw new IllegalArgumentException("Descrição com no máximo 400 caracteres.");
        Emergencia.Tipo tipo = valor(Emergencia.Tipo.class, nova.tipo(), "Tipo de emergência inválido.");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(resposta(gestao.criar(tipo, nova.descricao(), authentication.getName())));
    }

    @PostMapping("/{id}/protocolo")
    public EmergenciaResponse ativarProtocolo(@PathVariable Long id, @RequestBody AtivacaoProtocolo ativacao,
                                              Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request); // ação crítica: digital de quem ativa
        ProtocoloEmergencia protocolo = valor(ProtocoloEmergencia.class, ativacao.protocolo(), "Protocolo inválido.");
        return resposta(gestao.ativarProtocolo(id, protocolo, authentication.getName()));
    }

    @PostMapping("/{id}/encerrar")
    public EmergenciaResponse encerrar(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        confirmacao.exigir(request); // ação crítica: digital de quem encerra
        return resposta(gestao.encerrar(id, authentication.getName()));
    }

    private static <E extends Enum<E>> E valor(Class<E> tipo, String texto, String mensagem) {
        try {
            return Enum.valueOf(tipo, String.valueOf(texto));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private static ProtocoloResponse resposta(ProtocoloEmergencia p) {
        return p == null ? null : new ProtocoloResponse(p.name(), p.getNome(), p.getPassos());
    }

    private static EmergenciaResponse resposta(Emergencia e) {
        return new EmergenciaResponse(e.getId(), e.getTipo().name(), e.getDescricao(), e.getStatus().name(),
            e.getCriadaPor(), e.getCriadaEm(), resposta(e.getProtocolo()), e.getProtocoloAtivadoPor(),
            e.getProtocoloAtivadoEm(), e.getEncerradaPor(), e.getEncerradaEm());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalida(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflito(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> naoEncontrada(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", ex.getMessage()));
    }
}
