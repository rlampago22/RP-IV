package br.edu.unipampa.usina.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RNF08 / 4.2.10 do PDF ("autenticação de dois fatores para evitar ativação acidental"): ações críticas
 * exigem a digital além da sessão aberta com senha. Depois de confirmar, a sessão fica liberada para
 * ações críticas por {@link #JANELA} (como o "modo sudo" de bancos e do GitHub).
 *
 * <p>Uso numa ação crítica: {@code confirmacao.exigir(request);} na primeira linha do endpoint. Sem
 * confirmação recente, responde 428 com {@code exigeBiometria: true} e o front pede a digital.
 */
@Component
public class ConfirmacaoBiometrica {
    public static final Duration JANELA = Duration.ofMinutes(5);
    private static final String CONFIRMADA_ATE = "biometria.confirmada.ate";

    private final BiometriaService biometria;

    public ConfirmacaoBiometrica(BiometriaService biometria) {
        this.biometria = biometria;
    }

    public void exigir(HttpServletRequest request) {
        if (!biometria.isAcoesCriticasProtegidas()) return;
        if (validaAte(request).filter(Instant.now()::isBefore).isEmpty()) {
            throw new ConfirmacaoNecessariaException();
        }
    }

    Instant registrar(HttpServletRequest request) {
        Instant ate = Instant.now().plus(JANELA);
        request.getSession(true).setAttribute(CONFIRMADA_ATE, ate);
        return ate;
    }

    Optional<Instant> validaAte(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        return Optional.ofNullable(sessao == null ? null : (Instant) sessao.getAttribute(CONFIRMADA_ATE));
    }

    public static class ConfirmacaoNecessariaException extends RuntimeException {
        public ConfirmacaoNecessariaException() { super("Ação crítica: confirme com a sua digital."); }
    }

    @RestControllerAdvice
    static class Respostas {
        @ExceptionHandler(ConfirmacaoNecessariaException.class)
        ResponseEntity<Map<String, Object>> confirmacaoNecessaria(ConfirmacaoNecessariaException ex) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .body(Map.of("mensagem", ex.getMessage(), "exigeBiometria", true));
        }
    }
}
