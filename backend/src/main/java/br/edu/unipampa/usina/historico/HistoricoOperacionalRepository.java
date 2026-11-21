package br.edu.unipampa.usina.historico;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoOperacionalRepository extends JpaRepository<HistoricoOperacional, Long> {
    List<HistoricoOperacional> findAllByReatorIdOrderByInstanteAsc(String reatorId);

    List<HistoricoOperacional> findAllByOrderByInstanteAsc();
}
