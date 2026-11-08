package br.edu.unipampa.usina.historicooperacional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistência append-only do histórico operacional em CSV, recuperável entre sessões (RNF09).
 *
 * <p>Sem cadeia de hash: detecção de adulteração é responsabilidade do {@code RegistroAuditoria}
 * (RNF05). Aqui o objetivo é dado operacional consultável, não trilha inviolável.
 */
public final class RegistroHistorico {
    public static final int LIMITE_PADRAO = 500;
    public static final int LIMITE_MAXIMO = 5000;

    private final Path arquivo;
    private final List<SnapshotOperacional> snapshots = new ArrayList<>();

    public RegistroHistorico(Path arquivo) {
        this.arquivo = arquivo.toAbsolutePath().normalize();
        carregarArquivoExistente();
    }

    private synchronized void carregarArquivoExistente() {
        if (!Files.exists(arquivo)) {
            return;
        }
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                SnapshotOperacional snapshot = SnapshotOperacional.deLinhaCsv(linha);
                if (snapshot != null) {
                    snapshots.add(snapshot);
                }
            }
        } catch (IOException error) {
            System.err.println("[HISTORICO] Aviso ao carregar historico: " + error.getMessage());
        }
    }

    public synchronized void registrar(SnapshotOperacional snapshot) {
        snapshots.add(snapshot);
        anexarAoArquivo(snapshot);
    }

    public synchronized List<SnapshotOperacional> consultar() {
        return List.copyOf(snapshots);
    }

    /**
     * Consulta filtrada (RF16). O filtro roda sobre a lista já carregada em memória — adequado à
     * ordem de grandeza do MVP; um volume muito maior pediria índice ou banco.
     */
    public synchronized ResultadoConsulta consultar(FiltroHistorico filtro, int limite) {
        int teto = Math.min(limite <= 0 ? LIMITE_PADRAO : limite, LIMITE_MAXIMO);
        List<SnapshotOperacional> casaram = snapshots.stream()
            .filter(filtro::aceita)
            .toList();
        boolean truncado = casaram.size() > teto;
        return new ResultadoConsulta(
            truncado ? casaram.subList(0, teto) : casaram,
            casaram.size(),
            truncado
        );
    }

    public synchronized int tamanho() {
        return snapshots.size();
    }

    /**
     * RF16 — agrupa o histórico em saídas de operação: um resumo por (dia local + operador).
     *
     * <p>O dia é calculado no fuso informado, não em UTC: uma leitura às 23h30 UTC de 05/10 é
     * 20h30 de 05/10 em São Paulo, e agrupar por UTC jogaria registros para o dia errado no
     * relatório de operação.
     *
     * <p>Ordena do mais recente para o mais antigo — quem consulta costuma procurar o que
     * aconteceu há pouco.
     */
    public synchronized List<ResumoDia> agruparPorDia(FiltroHistorico filtro, ZoneId fuso) {
        Map<String, List<SnapshotOperacional>> porDiaEOperador = new LinkedHashMap<>();
        for (SnapshotOperacional snapshot : snapshots) {
            if (!filtro.aceita(snapshot)) {
                continue;
            }
            LocalDate dia = snapshot.instante().atZone(fuso).toLocalDate();
            porDiaEOperador
                .computeIfAbsent(dia + "\u0000" + snapshot.operador(), chave -> new ArrayList<>())
                .add(snapshot);
        }

        List<ResumoDia> resumos = new ArrayList<>();
        for (List<SnapshotOperacional> grupo : porDiaEOperador.values()) {
            resumos.add(resumir(grupo, fuso));
        }
        resumos.sort(
            Comparator.comparing(ResumoDia::dia).thenComparing(ResumoDia::primeiroRegistro).reversed());
        return List.copyOf(resumos);
    }

    private static ResumoDia resumir(List<SnapshotOperacional> grupo, ZoneId fuso) {
        SnapshotOperacional primeiro = grupo.get(0);
        SnapshotOperacional ultimo = grupo.get(grupo.size() - 1);
        int comAlarme = 0;
        int emObservacao = 0;
        int comFalha = 0;
        Set<String> envolvidos = new LinkedHashSet<>();
        String pior = "ESTAVEL";
        for (SnapshotOperacional snapshot : grupo) {
            if (!snapshot.sensoresEmAlarme().isEmpty()) {
                comAlarme++;
                envolvidos.addAll(snapshot.sensoresEmAlarme());
            }
            if (!snapshot.sensoresEmObservacao().isEmpty()) {
                emObservacao++;
            }
            if (snapshot.temFalhaDeSensor()) {
                comFalha++;
                envolvidos.addAll(snapshot.sensoresEmFalha());
            }
            pior = piorEntre(pior, snapshot.statusDerivado());
        }
        return new ResumoDia(
            primeiro.instante().atZone(fuso).toLocalDate(),
            primeiro.operador(),
            primeiro.origemOperador(),
            primeiro.instante(),
            ultimo.instante(),
            grupo.size(),
            comAlarme,
            emObservacao,
            comFalha,
            List.copyOf(envolvidos),
            pior
        );
    }

    private static String piorEntre(String atual, String candidato) {
        return severidade(candidato) > severidade(atual) ? candidato : atual;
    }

    private static int severidade(String status) {
        return switch (status) {
            case "CRITICO" -> 2;
            case "ATENCAO" -> 1;
            default -> 0;
        };
    }

    /**
     * RF17 — conjunto completo que casa com o filtro, <b>sem limite</b>.
     *
     * <p>A consulta da tela limita para não travar o navegador; a exportação não pode limitar,
     * porque um arquivo cortado em silêncio seria pior que nenhum arquivo.
     */
    public synchronized List<SnapshotOperacional> exportar(FiltroHistorico filtro) {
        return snapshots.stream().filter(filtro::aceita).toList();
    }

    public Path getArquivo() {
        return arquivo;
    }

    private void anexarAoArquivo(SnapshotOperacional snapshot) {
        try {
            Files.createDirectories(arquivo.getParent());
            boolean novo = !Files.exists(arquivo);
            String conteudo = novo
                ? SnapshotOperacional.CABECALHO + System.lineSeparator()
                    + snapshot.linhaCsv() + System.lineSeparator()
                : snapshot.linhaCsv() + System.lineSeparator();
            Files.writeString(
                arquivo,
                conteudo,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível gravar o histórico operacional.", error);
        }
    }
}
