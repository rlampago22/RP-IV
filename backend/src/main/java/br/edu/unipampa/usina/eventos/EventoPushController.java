package br.edu.unipampa.usina.eventos;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Canal de push da Central (Observer V2) — server push moderno com SSE.
 *
 * <p>São duas pontas:
 *
 * <ul>
 *   <li>{@code GET /api/eventos} — o navegador abre um {@code EventSource} e a conexão
 *       fica pendurada num {@link SseEmitter}. Exige sessão autenticada, ao contrário da
 *       API do núcleo na porta 8080.
 *   <li>{@code POST /api/eventos/publicar} — ponte interna: o processo do {@code mvp/}
 *       chama esta rota a cada evento do {@code EventBus}, e aqui o evento é espalhado
 *       para todos os emitters abertos.
 * </ul>
 *
 * <p>O Spring não tem referência ao {@code EventBus}, que vive no outro processo — daí a
 * rota de publicação. Ela só aceita chamada de loopback, porque é um canal interno entre
 * os dois processos da mesma máquina, não parte da API pública.
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoPushController {

    /** Sem timeout: o fluxo fica aberto enquanto a aba estiver aberta. */
    private static final long SEM_TIMEOUT = Long.MAX_VALUE;

    private final List<SseEmitter> inscritos = new CopyOnWriteArrayList<>();

    @GetMapping
    public SseEmitter assinar() {
        SseEmitter emissor = new SseEmitter(SEM_TIMEOUT);
        inscritos.add(emissor);
        emissor.onCompletion(() -> inscritos.remove(emissor));
        emissor.onTimeout(() -> inscritos.remove(emissor));
        emissor.onError(erro -> inscritos.remove(emissor));
        return emissor;
    }

    @PostMapping("/publicar")
    public ResponseEntity<Void> publicar(
        @RequestBody Map<String, Object> corpo,
        HttpServletRequest requisicao
    ) {
        if (!veioDeLoopback(requisicao)) {
            return ResponseEntity.status(403).build();
        }

        String tipo = String.valueOf(corpo.getOrDefault("tipo", "snapshot"));
        Object estado = corpo.get("estado");

        for (SseEmitter emissor : inscritos) {
            try {
                emissor.send(SseEmitter.event().name(tipo).data(estado, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException desconectado) {
                // aba fechada: tira da lista; o EventSource do navegador reconecta sozinho
                inscritos.remove(emissor);
            }
        }
        return ResponseEntity.accepted().build();
    }

    public int conectados() {
        return inscritos.size();
    }

    private static boolean veioDeLoopback(HttpServletRequest requisicao) {
        String origem = requisicao.getRemoteAddr();
        return "127.0.0.1".equals(origem) || "0:0:0:0:0:0:0:1".equals(origem) || "::1".equals(origem);
    }
}
