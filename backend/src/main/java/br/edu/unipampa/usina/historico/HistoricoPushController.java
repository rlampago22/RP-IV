package br.edu.unipampa.usina.historico;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ponte interna RF16/RF17 — o processo do {@code mvp/} chama esta rota a cada linha de histórico
 * operacional fechada, pra persistir em {@code historico_operacional} (Postgres/H2). Mesmo
 * desenho de {@code EventoPushController}: só aceita chamada de loopback, porque é um canal
 * interno entre os dois processos da mesma máquina, não parte da API pública.
 *
 * <p>O corpo carrega a mesma linha já serializada por {@code SnapshotOperacional.linhaCsv()} no
 * mvp/ (campo {@code linhaCsv}, separador {@code ;}) — evita manter dois formatos de serialização
 * sincronizados entre os dois módulos.
 */
@RestController
@RequestMapping("/api/historico")
public class HistoricoPushController {
    private static final int COLUNAS = 13;

    private final HistoricoOperacionalRepository repositorio;

    public HistoricoPushController(HistoricoOperacionalRepository repositorio) {
        this.repositorio = repositorio;
    }

    @PostMapping("/registrar")
    public ResponseEntity<Void> registrar(
        @RequestBody Map<String, Object> corpo,
        HttpServletRequest requisicao
    ) {
        if (!veioDeLoopback(requisicao)) {
            return ResponseEntity.status(403).build();
        }

        String linhaCsv = String.valueOf(corpo.get("linhaCsv"));
        HistoricoOperacional entidade = deLinhaCsv(linhaCsv);
        if (entidade == null) {
            return ResponseEntity.badRequest().build();
        }
        repositorio.save(entidade);
        return ResponseEntity.accepted().build();
    }

    /** Mesma convenção de campos de {@code SnapshotOperacional.deCampos()} no mvp/. */
    private static HistoricoOperacional deLinhaCsv(String linha) {
        if (linha == null) {
            return null;
        }
        String[] campos = linha.trim().split(";", -1);
        if (campos.length < COLUNAS) {
            return null;
        }
        try {
            return new HistoricoOperacional(
                campos[0].trim(),
                Instant.parse(campos[1].trim()),
                Double.parseDouble(campos[2].trim()),
                Double.parseDouble(campos[3].trim()),
                Double.parseDouble(campos[4].trim()),
                Double.parseDouble(campos[5].trim()),
                campos[6].trim(),
                campos[7].trim(),
                campos[8].trim(),
                campos[9].trim(),
                campos[10].trim(),
                campos[11].trim(),
                HistoricoOperacional.OrigemOperador.valueOf(campos[12].trim())
            );
        } catch (RuntimeException malformada) {
            return null;
        }
    }

    private static boolean veioDeLoopback(HttpServletRequest requisicao) {
        String origem = requisicao.getRemoteAddr();
        return "127.0.0.1".equals(origem) || "0:0:0:0:0:0:0:1".equals(origem) || "::1".equals(origem);
    }
}
