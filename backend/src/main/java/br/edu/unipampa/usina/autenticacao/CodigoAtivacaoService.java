package br.edu.unipampa.usina.autenticacao;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC02 — "funcionário previamente cadastrado": o Administrador emite um código de uso único e
 * o funcionário só cadastra a biometria apresentando senha + código. Guarda só o hash (BCrypt).
 */
@Service
public class CodigoAtivacaoService {
    static final Duration VALIDADE = Duration.ofHours(24);
    // Sem 0/O e 1/I/L, para não haver confusão ao ditar o código.
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final CodigoAtivacaoRepository codigos;
    private final BiometriaService biometria;
    private final PasswordEncoder encoder;
    private final SecureRandom aleatorio = new SecureRandom();

    public CodigoAtivacaoService(CodigoAtivacaoRepository codigos, BiometriaService biometria, PasswordEncoder encoder) {
        this.codigos = codigos;
        this.biometria = biometria;
        this.encoder = encoder;
    }

    /** Gera um código novo (o anterior, se houver, deixa de valer). O texto só existe nesta resposta. */
    @Transactional
    public AuthDtos.CodigoAtivacaoResponse emitir(Usuario alvo, Usuario emissor) {
        codigos.findByUsuarioIdAndSituacao(alvo.getId(), CodigoAtivacao.ATIVO).forEach(CodigoAtivacao::marcarSubstituido);
        String codigo = gerar();
        CodigoAtivacao salvo = codigos.save(
            new CodigoAtivacao(alvo, encoder.encode(normalizar(codigo)), emissor, Instant.now().plus(VALIDADE)));
        return new AuthDtos.CodigoAtivacaoResponse(alvo.getEmail(), codigo, salvo.getExpiraEm());
    }

    public Optional<CodigoAtivacao> conferir(Usuario usuario, String digitado) {
        if (digitado == null || digitado.isBlank()) return Optional.empty();
        return vigente(usuario)
            .filter(codigo -> encoder.matches(normalizar(digitado), codigo.getCodigoHash()));
    }

    public Optional<CodigoAtivacao> vigente(Usuario usuario) {
        return codigos.findFirstByUsuarioIdAndSituacaoOrderByCriadoEmDesc(usuario.getId(), CodigoAtivacao.ATIVO)
            .filter(CodigoAtivacao::valeAgora);
    }

    /** Bloqueio após tentativas erradas: o código vigente deixa de valer. */
    @Transactional
    public void invalidarVigente(Usuario usuario) {
        codigos.findByUsuarioIdAndSituacao(usuario.getId(), CodigoAtivacao.ATIVO).forEach(CodigoAtivacao::marcarSubstituido);
    }

    /** Consome o código e grava a biometria na mesma transação: se a biometria falhar, o código continua valendo. */
    @Transactional
    public CredencialBiometrica ativar(Long codigoId, String email, String requisicaoJson, String respostaJson, String apelido) {
        CodigoAtivacao codigo = codigos.findById(codigoId)
            .filter(CodigoAtivacao::valeAgora)
            .orElseThrow(() -> new BiometriaService.BiometriaRecusadaException(
                "O código de ativação expirou. Peça um novo ao Administrador do Sistema."));
        codigo.marcarUsado();
        return biometria.concluirCadastro(email, requisicaoJson, respostaJson, apelido);
    }

    private String gerar() {
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) codigo.append('-');
            codigo.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }

    private static String normalizar(String codigo) {
        return codigo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
