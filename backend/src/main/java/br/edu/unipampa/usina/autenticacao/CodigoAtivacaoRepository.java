package br.edu.unipampa.usina.autenticacao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoAtivacaoRepository extends JpaRepository<CodigoAtivacao, Long> {
    List<CodigoAtivacao> findByUsuarioIdAndSituacao(Long usuarioId, String situacao);
    Optional<CodigoAtivacao> findFirstByUsuarioIdAndSituacaoOrderByCriadoEmDesc(Long usuarioId, String situacao);
}
