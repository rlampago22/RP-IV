package br.edu.unipampa.usina.contingencia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê {@code GET /api/interno/contingencia} da Central (Spring, porta 8081). É a mão inversa de
 * {@code EncaminhadorEventosSpring}: lá o núcleo empurra eventos para o Spring, aqui ele pergunta
 * ao Spring se algum protocolo foi ativado.
 */
public final class FonteHttpContingencia implements FonteContingencia {
    public static final String ORIGEM_PADRAO = "http://localhost:8081/api/interno/contingencia";

    private static final Pattern OBJETO = Pattern.compile("\\{[^{}]*}");
    private static final Pattern CAMPO =
        Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|(null)|(-?\\d+))");

    private final URI origem;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public FonteHttpContingencia() {
        this(System.getProperty("usina.contingencia.origem", ORIGEM_PADRAO));
    }

    public FonteHttpContingencia(String origem) {
        this.origem = URI.create(origem);
    }

    @Override
    public List<ProtocoloRemoto> consultar() throws IOException {
        HttpRequest requisicao = HttpRequest.newBuilder(origem).timeout(Duration.ofSeconds(2)).GET().build();
        HttpResponse<String> resposta;
        try {
            resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            throw new IOException("consulta interrompida", interrompido);
        }
        if (resposta.statusCode() != 200) {
            throw new IOException("Central respondeu " + resposta.statusCode() + " em " + origem);
        }
        return lerJson(resposta.body());
    }

    public URI getOrigem() {
        return origem;
    }

    /** Lista plana de objetos planos (o formato da rota interna); sem biblioteca de JSON no núcleo. */
    static List<ProtocoloRemoto> lerJson(String json) throws IOException {
        String corpo = json == null ? "" : json.trim();
        if (!corpo.startsWith("[")) {
            throw new IOException("resposta da Central não é uma lista JSON");
        }
        List<ProtocoloRemoto> protocolos = new ArrayList<>();
        Matcher objetos = OBJETO.matcher(corpo);
        while (objetos.find()) {
            Map<String, String> campos = new HashMap<>();
            Matcher campo = CAMPO.matcher(objetos.group());
            while (campo.find()) {
                String valor = campo.group(2) != null ? desescapar(campo.group(2))
                    : campo.group(4); // null -> fica null
                campos.put(campo.group(1), valor);
            }
            String id = campos.get("emergenciaId");
            if (id == null) {
                throw new IOException("protocolo sem emergenciaId na resposta da Central");
            }
            protocolos.add(new ProtocoloRemoto(
                Long.parseLong(id),
                campos.get("status"),
                campos.get("protocolo"),
                campos.get("nome"),
                campos.get("ativadoPor"),
                campos.get("encerradoPor")
            ));
        }
        return protocolos;
    }

    private static String desescapar(String texto) {
        StringBuilder saida = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c != '\\' || i + 1 >= texto.length()) {
                saida.append(c);
                continue;
            }
            char proximo = texto.charAt(++i);
            switch (proximo) {
                case 'n' -> saida.append('\n');
                case 't' -> saida.append('\t');
                case 'r' -> saida.append('\r');
                case 'u' -> {
                    if (i + 4 < texto.length()) {
                        saida.append((char) Integer.parseInt(texto.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                }
                default -> saida.append(proximo); // \" \\ \/
            }
        }
        return saida.toString();
    }
}
