package br.edu.unipampa.usina.reatores;

import br.edu.unipampa.usina.autenticacao.AuditoriaAutenticacao;
import br.edu.unipampa.usina.autenticacao.RegistroAutenticacao;
import br.edu.unipampa.usina.autenticacao.Usuario;
import br.edu.unipampa.usina.autenticacao.UsuarioRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue #96 — cadastro de reatores pela tela Reatores (Administrador do Sistema). Grava nas mesmas
 * tabelas que o núcleo lê ({@code nucleo.reator} / {@code nucleo.sensor_reator}, migração V8); o
 * núcleo percebe a mudança em até 2 s ({@code SincronizadorCadastroReatores}) e inclui, ajusta ou
 * retira o reator sem reiniciar.
 *
 * <p>Não há exclusão: alarmes, histórico e auditoria apontam para o reator, então ele só sai de
 * operação ({@code ativo = false}). Cada alteração entra na trilha encadeada (RNF03) com quem fez;
 * a exigência da digital fica no controller.
 */
@Service
public class CadastroReatoresService {
    /** R-01: cenários sem reator e T01/T02 caem nele — não pode sair de operação. */
    public static final String REATOR_PRINCIPAL = "R-01";
    private static final String METODO = "ACAO_CRITICA";

    public record Sensor(long sensorId, String tipo, String unidade, double limiteMinimo, double limiteMaximo,
                         double atencaoMinima, double atencaoMaxima, double setpoint) { }

    public record Reator(String id, String nome, int ordem, boolean ativo, List<Sensor> sensores) { }

    /** Faixa e setpoint de um sensor, como vêm da tela. Tipo e unidade não mudam. */
    public record AjusteSensor(Long sensorId, Double limiteMinimo, Double limiteMaximo,
                               Double atencaoMinima, Double atencaoMaxima, Double setpoint) { }

    /** Os 4 sensores RF-1, com as faixas e o setpoint central da carga inicial (V8). */
    public static final List<Sensor> SENSORES_PADRAO = List.of(
        new Sensor(1, "TEMPERATURA", "Celsius", 0, 350, 20, 325, 310),
        new Sensor(2, "PRESSAO", "bar", 0, 160, 10, 155, 150),
        new Sensor(3, "RADIACAO", "mSv/h", 0, 5, 0.1, 4.2, 2.4),
        new Sensor(4, "FLUXO_RESFRIAMENTO", "m3/h", 500, 1500, 600, 1400, 1100)
    );

    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final AuditoriaAutenticacao auditoria;

    public CadastroReatoresService(JdbcTemplate jdbc, UsuarioRepository usuarios, AuditoriaAutenticacao auditoria) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Reator> listar() {
        Map<String, List<Sensor>> sensores = new LinkedHashMap<>();
        jdbc.query(
            "SELECT reator_id, sensor_id, tipo, unidade, limite_minimo, limite_maximo, atencao_minima, atencao_maxima, setpoint "
                + "FROM nucleo.sensor_reator ORDER BY reator_id, sensor_id",
            linha -> {
                sensores.computeIfAbsent(linha.getString(1), id -> new ArrayList<>()).add(new Sensor(
                    linha.getLong(2), linha.getString(3), linha.getString(4), linha.getDouble(5), linha.getDouble(6),
                    linha.getDouble(7), linha.getDouble(8), linha.getDouble(9)));
            });
        return jdbc.query(
            "SELECT id, nome, ordem, ativo FROM nucleo.reator ORDER BY ordem, id",
            (linha, n) -> new Reator(linha.getString(1), linha.getString(2), linha.getInt(3), linha.getBoolean(4),
                List.copyOf(sensores.getOrDefault(linha.getString(1), List.of()))));
    }

    /**
     * Reator novo, com os 4 sensores RF-1. Id e nome seguem a sequência ({@code R-06}, {@code Reator-06});
     * sem ajustes, entra com as faixas e setpoints padrão.
     */
    @Transactional
    public Reator incluir(List<AjusteSensor> ajustes, String email) {
        Integer ultimo = jdbc.queryForObject(
            "SELECT MAX(CAST(SUBSTRING(id, 3) AS INTEGER)) FROM nucleo.reator WHERE id LIKE 'R-%'", Integer.class);
        int numero = (ultimo == null ? 0 : ultimo) + 1;
        if (numero > 99) throw new IllegalStateException("Limite de 99 reatores atingido.");
        String id = String.format("R-%02d", numero);
        Integer maiorOrdem = jdbc.queryForObject("SELECT MAX(ordem) FROM nucleo.reator", Integer.class);

        List<Sensor> sensores = aplicar(SENSORES_PADRAO, ajustes);
        jdbc.update("INSERT INTO nucleo.reator (id, nome, ordem, ativo) VALUES (?, ?, ?, TRUE)",
            id, String.format("Reator-%02d", numero), (maiorOrdem == null ? 0 : maiorOrdem) + 1);
        for (Sensor s : sensores) {
            jdbc.update("INSERT INTO nucleo.sensor_reator (reator_id, sensor_id, tipo, unidade, limite_minimo, limite_maximo, "
                    + "atencao_minima, atencao_maxima, setpoint) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, s.sensorId(), s.tipo(), s.unidade(), s.limiteMinimo(), s.limiteMaximo(),
                s.atencaoMinima(), s.atencaoMaxima(), s.setpoint());
        }
        registrar(email, "REATOR_INCLUIDO:" + id);
        return buscar(id);
    }

    /** Ajusta faixas e setpoints dos sensores informados; os outros ficam como estão. */
    @Transactional
    public Reator ajustar(String id, List<AjusteSensor> ajustes, String email) {
        Reator atual = buscar(id);
        if (ajustes == null || ajustes.isEmpty()) throw new IllegalArgumentException("Informe ao menos um sensor para ajustar.");
        for (Sensor s : aplicar(atual.sensores(), ajustes)) {
            jdbc.update("UPDATE nucleo.sensor_reator SET limite_minimo = ?, limite_maximo = ?, atencao_minima = ?, "
                    + "atencao_maxima = ?, setpoint = ? WHERE reator_id = ? AND sensor_id = ?",
                s.limiteMinimo(), s.limiteMaximo(), s.atencaoMinima(), s.atencaoMaxima(), s.setpoint(), id, s.sensorId());
        }
        registrar(email, "REATOR_AJUSTADO:" + id);
        return buscar(id);
    }

    @Transactional
    public Reator definirOperacao(String id, boolean ativo, String email) {
        Reator atual = buscar(id);
        if (!ativo && REATOR_PRINCIPAL.equals(id)) {
            throw new IllegalStateException("O R-01 é o reator principal (cenários, T01 e T02 dependem dele) e não pode sair de operação.");
        }
        if (atual.ativo() == ativo) {
            throw new IllegalStateException(id + (ativo ? " já está em operação." : " já está fora de operação."));
        }
        if (ativo && atual.sensores().isEmpty()) {
            throw new IllegalStateException(id + " não tem sensores cadastrados.");
        }
        jdbc.update("UPDATE nucleo.reator SET ativo = ? WHERE id = ?", ativo, id);
        registrar(email, (ativo ? "REATOR_REATIVADO:" : "REATOR_RETIRADO:") + id);
        return buscar(id);
    }

    private Reator buscar(String id) {
        return listar().stream().filter(r -> r.id().equals(id)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("Reator " + id + " não encontrado."));
    }

    /** Aplica os ajustes sobre a base e valida cada sensor resultante. */
    private static List<Sensor> aplicar(List<Sensor> base, List<AjusteSensor> ajustes) {
        Map<Long, Sensor> porId = new LinkedHashMap<>();
        base.forEach(s -> porId.put(s.sensorId(), s));
        if (ajustes != null) {
            for (AjusteSensor ajuste : ajustes) {
                if (ajuste == null || ajuste.sensorId() == null) throw new IllegalArgumentException("Sensor sem id.");
                Sensor atual = porId.get(ajuste.sensorId());
                if (atual == null) throw new IllegalArgumentException("Sensor " + ajuste.sensorId() + " não existe neste reator.");
                porId.put(atual.sensorId(), new Sensor(atual.sensorId(), atual.tipo(), atual.unidade(),
                    valor(ajuste.limiteMinimo(), atual.limiteMinimo()), valor(ajuste.limiteMaximo(), atual.limiteMaximo()),
                    valor(ajuste.atencaoMinima(), atual.atencaoMinima()), valor(ajuste.atencaoMaxima(), atual.atencaoMaxima()),
                    valor(ajuste.setpoint(), atual.setpoint())));
            }
        }
        porId.values().forEach(CadastroReatoresService::validar);
        return List.copyOf(porId.values());
    }

    private static double valor(Double novo, double atual) {
        return novo == null ? atual : novo;
    }

    /**
     * Mesma ordem que o núcleo pressupõe: limite mínimo &lt; atenção mínima &le; setpoint &le; atenção
     * máxima &lt; limite máximo. Setpoint fora da faixa de atenção deixaria o reator sempre amarelo.
     */
    static void validar(Sensor s) {
        double[] valores = {s.limiteMinimo(), s.limiteMaximo(), s.atencaoMinima(), s.atencaoMaxima(), s.setpoint()};
        for (double v : valores) {
            if (!Double.isFinite(v)) throw new IllegalArgumentException(s.tipo() + ": informe todos os valores numéricos.");
        }
        if (!(s.limiteMinimo() < s.atencaoMinima() && s.atencaoMinima() <= s.atencaoMaxima() && s.atencaoMaxima() < s.limiteMaximo())) {
            throw new IllegalArgumentException(s.tipo() + ": a faixa precisa seguir limite mín. < atenção mín. ≤ atenção máx. < limite máx.");
        }
        if (s.setpoint() < s.atencaoMinima() || s.setpoint() > s.atencaoMaxima()) {
            throw new IllegalArgumentException(s.tipo() + ": o setpoint precisa ficar dentro da faixa de atenção (operação normal).");
        }
    }

    private void registrar(String email, String resultado) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
        auditoria.save(new RegistroAutenticacao(usuario, email, resultado, METODO));
    }
}
