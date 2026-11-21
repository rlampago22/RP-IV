package br.edu.unipampa.usina.emergencias;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ponte interna Spring (8081) -> núcleo (8080) do protocolo de contingência (issue #99).
 *
 * <p>A ativação continua aqui, com sessão e digital ({@link EmergenciaController}). O núcleo
 * ({@code mvp/}, sem sessão) consulta esta rota a cada segundo, publica a ativação/encerramento no
 * {@code EventBus} dele e trava as operações de rotina enquanto houver protocolo ativo. É a mão
 * inversa de {@code POST /api/eventos/publicar}.
 *
 * <p>Só leitura e só para o processo da mesma máquina: recusa quem não vem de loopback e quem
 * manda {@code Origin} (todo navegador manda; o cliente HTTP do núcleo não).
 */
@RestController
@RequestMapping("/api/interno/contingencia")
public class ContingenciaInternaController {
    /** Quantas emergências com protocolo entram na resposta (as ativas e as encerradas mais recentes). */
    private static final int LIMITE = 20;

    public record ProtocoloInterno(Long emergenciaId, String status, String protocolo, String nome,
                                   String ativadoPor, Instant ativadoEm, String encerradoPor, Instant encerradoEm) { }

    private final EmergenciaRepository emergencias;

    public ContingenciaInternaController(EmergenciaRepository emergencias) {
        this.emergencias = emergencias;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProtocoloInterno>> protocolos(HttpServletRequest requisicao) {
        if (!veioDoNucleoLocal(requisicao)) {
            return ResponseEntity.status(403).build();
        }
        List<ProtocoloInterno> protocolos = emergencias.findAllByOrderByIdDesc().stream()
            .filter(e -> e.getProtocolo() != null)
            .limit(LIMITE)
            .map(e -> new ProtocoloInterno(e.getId(), e.getStatus().name(), e.getProtocolo().name(),
                e.getProtocolo().getNome(), e.getProtocoloAtivadoPor(), e.getProtocoloAtivadoEm(),
                e.getEncerradaPor(), e.getEncerradaEm()))
            .toList();
        return ResponseEntity.ok(protocolos);
    }

    private static boolean veioDoNucleoLocal(HttpServletRequest requisicao) {
        String origem = requisicao.getRemoteAddr();
        boolean loopback = "127.0.0.1".equals(origem) || "0:0:0:0:0:0:0:1".equals(origem) || "::1".equals(origem);
        return loopback && requisicao.getHeader("Origin") == null;
    }
}
