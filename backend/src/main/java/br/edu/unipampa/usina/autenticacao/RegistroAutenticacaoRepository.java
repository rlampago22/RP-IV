package br.edu.unipampa.usina.autenticacao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAutenticacaoRepository extends JpaRepository<RegistroAutenticacao, Long> {
    Optional<RegistroAutenticacao> findTopByHashIsNotNullOrderByIdDesc();
    List<RegistroAutenticacao> findByHashIsNotNullOrderByIdAsc();
}
