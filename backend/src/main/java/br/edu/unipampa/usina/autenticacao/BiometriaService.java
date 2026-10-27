package br.edu.unipampa.usina.autenticacao;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialHint;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RNF08 — segundo fator do login por passkey (WebAuthn). O celular confirma com digital/rosto
 * e assina o desafio; aqui só se confere a assinatura com a chave pública cadastrada.
 * Sem estado: quem chama guarda a requisição (JSON) na sessão entre o início e a conclusão.
 */
@Service
public class BiometriaService {
    static final long TEMPO_LIMITE_MS = 120_000;
    private static final int TAMANHO_USER_HANDLE = 32;

    private final RelyingParty relyingParty;
    private final RepositorioCredenciaisWebAuthn repositorio;
    private final CredencialBiometricaRepository credenciais;
    private final UsuarioRepository usuarios;
    private final boolean preferirCelular;
    private final SecureRandom aleatorio = new SecureRandom();

    public BiometriaService(RepositorioCredenciaisWebAuthn repositorio, CredencialBiometricaRepository credenciais,
                            UsuarioRepository usuarios,
                            @Value("${app.webauthn.rp-id}") String rpId,
                            @Value("${app.webauthn.rp-nome}") String rpNome,
                            @Value("${app.webauthn.origens}") List<String> origens,
                            @Value("${app.webauthn.preferir-celular}") boolean preferirCelular) {
        this.repositorio = repositorio;
        this.credenciais = credenciais;
        this.usuarios = usuarios;
        this.preferirCelular = preferirCelular;
        this.relyingParty = RelyingParty.builder()
            .identity(RelyingPartyIdentity.builder().id(rpId).name(rpNome).build())
            .credentialRepository(repositorio)
            .origins(Set.copyOf(origens))
            .build();
    }

    public PublicKeyCredentialCreationOptions iniciarCadastro(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        ByteArray userHandle = repositorio.getUserHandleForUsername(email).orElseGet(this::novoUserHandle);
        StartRegistrationOptions.StartRegistrationOptionsBuilder opcoes = StartRegistrationOptions.builder()
            .user(UserIdentity.builder().name(usuario.getEmail()).displayName(usuario.getNome()).id(userHandle).build())
            .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.PREFERRED)
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build())
            .timeout(TEMPO_LIMITE_MS);
        if (preferirCelular) opcoes.hints(PublicKeyCredentialHint.HYBRID);
        return relyingParty.startRegistration(opcoes.build());
    }

    @Transactional
    public CredencialBiometrica concluirCadastro(String email, String requisicaoJson, String respostaJson, String apelido) {
        try {
            PublicKeyCredentialCreationOptions requisicao = PublicKeyCredentialCreationOptions.fromJson(requisicaoJson);
            if (!requisicao.getUser().getName().equalsIgnoreCase(email)) {
                throw new BiometriaRecusadaException("O cadastro foi iniciado por outro usuário.");
            }
            RegistrationResult resultado = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                .request(requisicao)
                .response(PublicKeyCredential.parseRegistrationResponseJson(respostaJson))
                .build());
            Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
            return credenciais.save(new CredencialBiometrica(
                usuario,
                resultado.getKeyId().getId().getBase64Url(),
                requisicao.getUser().getId().getBase64Url(),
                resultado.getPublicKeyCose().getBase64Url(),
                resultado.getSignatureCount(),
                apelidoValido(apelido)));
        } catch (RegistrationFailedException | IOException ex) {
            throw new BiometriaRecusadaException("Não foi possível validar a biometria cadastrada.", ex);
        }
    }

    public AssertionRequest iniciarConfirmacao(String email) {
        StartAssertionOptions.StartAssertionOptionsBuilder opcoes = StartAssertionOptions.builder()
            .username(email)
            .userVerification(UserVerificationRequirement.REQUIRED)
            .timeout(TEMPO_LIMITE_MS);
        if (preferirCelular) opcoes.hints(PublicKeyCredentialHint.HYBRID);
        return relyingParty.startAssertion(opcoes.build());
    }

    /** Confere a assinatura do celular e devolve o e-mail do dono da passkey. */
    @Transactional
    public String concluirConfirmacao(String requisicaoJson, String respostaJson) {
        try {
            AssertionResult resultado = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                .request(AssertionRequest.fromJson(requisicaoJson))
                .response(PublicKeyCredential.parseAssertionResponseJson(respostaJson))
                .build());
            if (!resultado.isSuccess() || !resultado.isUserVerified()) {
                throw new BiometriaRecusadaException("Biometria não confirmada.");
            }
            credenciais.findByCredentialId(resultado.getCredential().getCredentialId().getBase64Url())
                .ifPresent(credencial -> credencial.registrarUso(resultado.getSignatureCount()));
            return resultado.getUsername();
        } catch (AssertionFailedException | IOException ex) {
            throw new BiometriaRecusadaException("Biometria não confirmada.", ex);
        }
    }

    public List<CredencialBiometrica> listar(String email) {
        return credenciais.findByUsuarioEmailIgnoreCaseOrderByCriadoEmAsc(email);
    }

    @Transactional
    public boolean remover(String email, Long id) {
        return credenciais.findById(id)
            .filter(credencial -> credencial.getUsuario().getEmail().equalsIgnoreCase(email))
            .map(credencial -> {
                credenciais.delete(credencial);
                return true;
            })
            .orElse(false);
    }

    private ByteArray novoUserHandle() {
        byte[] bytes = new byte[TAMANHO_USER_HANDLE];
        aleatorio.nextBytes(bytes);
        return new ByteArray(bytes);
    }

    private static String apelidoValido(String apelido) {
        String limpo = apelido == null ? "" : apelido.trim();
        if (limpo.isEmpty()) return "Celular";
        return limpo.length() > 80 ? limpo.substring(0, 80) : limpo;
    }

    public static class BiometriaRecusadaException extends RuntimeException {
        public BiometriaRecusadaException(String mensagem) { super(mensagem); }
        public BiometriaRecusadaException(String mensagem, Throwable causa) { super(mensagem, causa); }
    }
}
