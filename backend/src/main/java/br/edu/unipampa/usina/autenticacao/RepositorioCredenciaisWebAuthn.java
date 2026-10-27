package br.edu.unipampa.usina.autenticacao;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Adapta {@link CredencialBiometricaRepository} ao contrato da biblioteca WebAuthn (Yubico). */
@Component
class RepositorioCredenciaisWebAuthn implements CredentialRepository {
    private final CredencialBiometricaRepository credenciais;

    RepositorioCredenciaisWebAuthn(CredencialBiometricaRepository credenciais) {
        this.credenciais = credenciais;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String email) {
        return credenciais.findByUsuarioEmailIgnoreCaseOrderByCriadoEmAsc(email).stream()
            .map(c -> PublicKeyCredentialDescriptor.builder().id(bytes(c.getCredentialId())).build())
            .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String email) {
        return credenciais.findByUsuarioEmailIgnoreCaseOrderByCriadoEmAsc(email).stream()
            .findFirst()
            .map(c -> bytes(c.getUserHandle()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return credenciais.findFirstByUserHandle(userHandle.getBase64Url())
            .map(c -> c.getUsuario().getEmail());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credenciais.findByCredentialId(credentialId.getBase64Url())
            .filter(c -> c.getUserHandle().equals(userHandle.getBase64Url()))
            .map(RepositorioCredenciaisWebAuthn::registrada);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credenciais.findByCredentialId(credentialId.getBase64Url())
            .map(RepositorioCredenciaisWebAuthn::registrada)
            .map(Set::of)
            .orElse(Set.of());
    }

    private static RegisteredCredential registrada(CredencialBiometrica c) {
        return RegisteredCredential.builder()
            .credentialId(bytes(c.getCredentialId()))
            .userHandle(bytes(c.getUserHandle()))
            .publicKeyCose(bytes(c.getChavePublicaCose()))
            .signatureCount(c.getContadorAssinatura())
            .build();
    }

    static ByteArray bytes(String base64Url) {
        try {
            return ByteArray.fromBase64Url(base64Url);
        } catch (Base64UrlException e) {
            throw new IllegalStateException("Identificador WebAuthn corrompido no banco.", e);
        }
    }
}
