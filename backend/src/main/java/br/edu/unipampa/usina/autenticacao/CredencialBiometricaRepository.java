package br.edu.unipampa.usina.autenticacao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredencialBiometricaRepository extends JpaRepository<CredencialBiometrica, Long> {
    List<CredencialBiometrica> findByUsuarioEmailIgnoreCaseOrderByCriadoEmAsc(String email);
    boolean existsByUsuarioEmailIgnoreCase(String email);
    Optional<CredencialBiometrica> findByCredentialId(String credentialId);
    Optional<CredencialBiometrica> findFirstByUserHandle(String userHandle);
}
