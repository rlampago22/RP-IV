package br.edu.unipampa.usina.autenticacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * RF18 / UC05 (spec do Álvaro em docs/marco1/01-requisitos-rf-rnf.md) — 2FA por celular no login.
 * Depois da senha, cria uma solicitação ligada à tentativa e envia ao celular. A sessão só é aberta
 * quando o celular aprova OU quando o código de 6 dígitos é digitado na Central. Código de uso único,
 * válido por {@link #VALIDADE}, bloqueado após {@link #MAX_TENTATIVAS} erros. Cada etapa vai para a
 * trilha encadeada ({@link AuditoriaAutenticacao}) sem o código e sem o telefone (issue #81, RNF03).
 */
@Service
public class SegundoFatorService {
    public static final Duration VALIDADE = Duration.ofMinutes(3);
    public static final int MAX_TENTATIVAS = 5;
    static final String METODO = "CELULAR";

    public enum Status { PENDENTE, APROVADO, RECUSADO, CONCLUIDO, EXPIRADO, BLOQUEADO }

    public static final class Desafio {
        private final String id = UUID.randomUUID().toString();
        private final Usuario usuario;
        private final Authentication autenticacao;
        private final String codigo;
        private final Instant expiraEm = Instant.now().plus(VALIDADE);
        private volatile Status status = Status.PENDENTE;
        private int tentativas;

        Desafio(Usuario usuario, Authentication autenticacao, String codigo) {
            this.usuario = usuario;
            this.autenticacao = autenticacao;
            this.codigo = codigo;
        }

        public String getId() { return id; }
        public String getCodigo() { return codigo; }
        public Instant getExpiraEm() { return expiraEm; }
        public Status getStatus() { return status; }
        public String getNomeUsuario() { return usuario.getNome(); }

        public String getEmailMascarado() {
            String email = usuario.getEmail();
            int arroba = email.indexOf('@');
            return arroba <= 1 ? email : email.charAt(0) + "***" + email.substring(arroba);
        }
    }

    private final Map<String, Desafio> desafios = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();
    private final CanalCelular celular;
    private final AuditoriaAutenticacao auditoria;

    public SegundoFatorService(CanalCelular celular, AuditoriaAutenticacao auditoria) {
        this.celular = celular;
        this.auditoria = auditoria;
    }

    public Desafio iniciar(Usuario usuario, Authentication autenticacao) {
        Desafio desafio = new Desafio(usuario, autenticacao, "%06d".formatted(aleatorio.nextInt(1_000_000)));
        desafios.put(desafio.id, desafio);
        celular.enviar(desafio);
        registrar(desafio, "2FA_SOLICITACAO_ENVIADA");
        return desafio;
    }

    /** No celular: o usuário aprova ou recusa a solicitação. */
    public synchronized Status responderNoCelular(String id, boolean aprovar) {
        Desafio d = buscarValido(id).orElseThrow(() -> new SegundoFatorException("Solicitação inexistente ou expirada."));
        if (d.status != Status.PENDENTE) throw new SegundoFatorException("Solicitação já respondida.");
        d.status = aprovar ? Status.APROVADO : Status.RECUSADO;
        registrar(d, aprovar ? "2FA_APROVADO_NO_CELULAR" : "2FA_RECUSADO_NO_CELULAR");
        return d.status;
    }

    /** Na Central: conclui o login depois que o celular aprovou. */
    public synchronized Authentication concluirPorAprovacao(String id) {
        Desafio d = buscarValido(id).orElseThrow(() -> new SegundoFatorException("Solicitação expirada. Entre de novo."));
        if (d.status == Status.PENDENTE) throw new SegundoFatorException("Ainda não foi aprovado no celular.");
        if (d.status != Status.APROVADO) {
            registrar(d, "2FA_NEGADO_" + d.status);
            throw new SegundoFatorException("Acesso negado no celular.");
        }
        return concluir(d, "2FA_SUCESSO_APROVACAO");
    }

    /** Na Central: conclui o login digitando o código que chegou no celular. */
    public synchronized Authentication concluirPorCodigo(String id, String codigoInformado) {
        Desafio d = buscarValido(id).orElseThrow(() -> new SegundoFatorException("Código expirado. Entre de novo."));
        if (d.status != Status.PENDENTE && d.status != Status.APROVADO) {
            registrar(d, "2FA_NEGADO_" + d.status);
            throw new SegundoFatorException("Esta solicitação não vale mais. Entre de novo.");
        }
        String informado = codigoInformado == null ? "" : codigoInformado.replaceAll("\\D", "");
        boolean confere = MessageDigest.isEqual(informado.getBytes(StandardCharsets.UTF_8), d.codigo.getBytes(StandardCharsets.UTF_8));
        if (!confere) {
            d.tentativas++;
            if (d.tentativas >= MAX_TENTATIVAS) {
                d.status = Status.BLOQUEADO;
                registrar(d, "2FA_BLOQUEADO_TENTATIVAS");
                throw new SegundoFatorException("Muitas tentativas erradas. Entre de novo.");
            }
            registrar(d, "2FA_CODIGO_INVALIDO");
            throw new SegundoFatorException("Código inválido.");
        }
        return concluir(d, "2FA_SUCESSO_CODIGO");
    }

    private Authentication concluir(Desafio d, String resultado) {
        d.status = Status.CONCLUIDO; // uso único: a mesma aprovação/código não abre outra sessão
        desafios.remove(d.id);
        registrar(d, resultado);
        return d.autenticacao;
    }

    private Optional<Desafio> buscarValido(String id) {
        Desafio d = id == null ? null : desafios.get(id);
        if (d == null) return Optional.empty();
        if (Instant.now().isAfter(d.expiraEm)) {
            desafios.remove(id);
            if (d.status == Status.PENDENTE || d.status == Status.APROVADO) {
                d.status = Status.EXPIRADO;
                registrar(d, "2FA_EXPIRADO");
            }
            return Optional.empty();
        }
        return Optional.of(d);
    }

    private void registrar(Desafio d, String resultado) {
        auditoria.save(new RegistroAutenticacao(d.usuario, d.usuario.getEmail(), resultado, METODO));
    }

    public static class SegundoFatorException extends RuntimeException {
        public SegundoFatorException(String mensagem) { super(mensagem); }
    }
}
