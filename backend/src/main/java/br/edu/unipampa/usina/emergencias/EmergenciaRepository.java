package br.edu.unipampa.usina.emergencias;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergenciaRepository extends JpaRepository<Emergencia, Long> {
    List<Emergencia> findAllByOrderByIdDesc();
}
