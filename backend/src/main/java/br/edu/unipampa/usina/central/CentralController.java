package br.edu.unipampa.usina.central;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/central")
public class CentralController {
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('OPERADOR_REATOR', 'SUPERVISAO_CENTRAL', 'ADMINISTRADOR_SISTEMA')")
    public Map<String, String> status() {
        return Map.of("status", "ONLINE");
    }
}
