package br.edu.unipampa.usina.auditorialogs;

import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Registro append-only com cadeia criptográfica SHA-256 e persistência multi-sessão.
 * Oferece inclusão e consulta segura, recuperando o histórico em reinicializações
 * e detectando qualquer adulteração física no arquivo de log.
 */
public final class RegistroAuditoria {
    private static final String GENESIS = "GENESIS";

    private final Path arquivo;
    private final List<EntradaAuditoria> entradas = new ArrayList<>();

    public RegistroAuditoria(Path arquivo) {
        this.arquivo = arquivo.toAbsolutePath().normalize();
        carregarArquivoExistente();
    }

    private synchronized void carregarArquivoExistente() {
        if (!Files.exists(arquivo)) {
            return;
        }
        try {
            List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
            for (String linha : linhas) {
                linha = linha.trim();
                if (linha.isEmpty()) continue;
                EntradaAuditoria entrada = parseLinha(linha);
                if (entrada != null) {
                    entradas.add(entrada);
                }
            }
        } catch (IOException error) {
            System.err.println("[AUDITORIA] Aviso ao carregar histórico: " + error.getMessage());
        }
    }

    private EntradaAuditoria parseLinha(String linha) {
        // Formato: seq | ocorridoEm | tipo | resumo | anterior=... | hash=...
        String[] partes = linha.split(" \\| ");
        if (partes.length < 6) return null;
        try {
            long seq = Long.parseLong(partes[0].trim());
            Instant ts = Instant.parse(partes[1].trim());
            String tipo = partes[2].trim();
            String resumo = partes[3].trim();
            String anterior = partes[4].replace("anterior=", "").trim();
            String hash = partes[5].replace("hash=", "").trim();
            return new EntradaAuditoria(seq, ts, tipo, resumo, anterior, hash);
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized EntradaAuditoria registrar(EventoDominio evento) {
        long sequencia = entradas.size() + 1L;
        String hashAnterior = entradas.isEmpty()
            ? GENESIS
            : entradas.get(entradas.size() - 1).hash();
        String hash = calcularHash(
            sequencia,
            evento.ocorridoEm(),
            evento.tipo(),
            evento.resumo(),
            hashAnterior
        );

        EntradaAuditoria entrada = new EntradaAuditoria(
            sequencia,
            evento.ocorridoEm(),
            evento.tipo(),
            evento.resumo(),
            hashAnterior,
            hash
        );
        entradas.add(entrada);
        anexarAoArquivo(entrada);
        return entrada;
    }

    public synchronized List<EntradaAuditoria> consultar() {
        return List.copyOf(entradas);
    }

    public synchronized boolean verificarIntegridade() {
        String anterior = GENESIS;
        for (EntradaAuditoria entrada : entradas) {
            String esperado = calcularHash(
                entrada.sequencia(),
                entrada.ocorridoEm(),
                entrada.tipoEvento(),
                entrada.resumo(),
                anterior
            );
            if (!anterior.equals(entrada.hashAnterior()) || !esperado.equals(entrada.hash())) {
                return false;
            }
            anterior = entrada.hash();
        }
        return true;
    }

    /**
     * Validação física do arquivo em disco:
     * Lê linha por linha e recalcula cada hash SHA-256 encadeado.
     * Detecta imediatamente qualquer tentativa de adulteração (tampering) externa.
     */
    public synchronized boolean verificarIntegridadeArquivo() {
        if (!Files.exists(arquivo)) {
            return true;
        }
        try {
            List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
            String anterior = GENESIS;
            long esperadaSeq = 1L;

            for (String linha : linhas) {
                linha = linha.trim();
                if (linha.isEmpty()) continue;
                EntradaAuditoria entrada = parseLinha(linha);
                if (entrada == null) {
                    return false;
                }
                if (entrada.sequencia() != esperadaSeq) {
                    return false;
                }
                String esperado = calcularHash(
                    entrada.sequencia(),
                    entrada.ocorridoEm(),
                    entrada.tipoEvento(),
                    entrada.resumo(),
                    anterior
                );
                if (!anterior.equals(entrada.hashAnterior()) || !esperado.equals(entrada.hash())) {
                    return false;
                }
                anterior = entrada.hash();
                esperadaSeq++;
            }
            return true;
        } catch (IOException error) {
            return false;
        }
    }

    public Path getArquivo() {
        return arquivo;
    }

    private void anexarAoArquivo(EntradaAuditoria entrada) {
        try {
            Files.createDirectories(arquivo.getParent());
            Files.writeString(
                arquivo,
                entrada.linhaPersistida() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível gravar o log de auditoria.", error);
        }
    }

    public static String calcularHash(
        long sequencia,
        Instant ocorridoEm,
        String tipo,
        String resumo,
        String hashAnterior
    ) {
        String conteudo = sequencia + "|" + ocorridoEm + "|" + tipo + "|" + resumo + "|" + hashAnterior;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 indisponível.", error);
        }
    }
}
