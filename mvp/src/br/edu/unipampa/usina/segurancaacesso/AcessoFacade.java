package br.edu.unipampa.usina.segurancaacesso;

import br.edu.unipampa.usina.infraestruturaeventos.AcessoRegistrado;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RF07 (credencial simulada) + RF08 — valida acesso a área e persiste {@link RegistroAcesso}.
 * Credenciais de demo (crachá): OPER-01, SUP-01, GUARDA-01.
 */
public final class AcessoFacade {
    private static final Map<String, Integer> NIVEL_CREDENCIAL = Map.of(
        "OPER-01", 2,
        "SUP-01", 3,
        "GUARDA-01", 1,
        "ADMIN-01", 3
    );

    private static final Map<String, Integer> NIVEL_AREA = Map.of(
        "SALA-CONTROLE", 2,
        "CORE-REATOR", 3,
        "PERIMETRO", 1
    );

    private final EventBus eventBus;
    private final List<RegistroAcesso> historico = new CopyOnWriteArrayList<>();

    public AcessoFacade(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public RegistroAcesso solicitarAcesso(String credencial, String area) {
        String cred = credencial == null ? "" : credencial.trim().toUpperCase(Locale.ROOT);
        String areaNorm = area == null ? "" : area.trim().toUpperCase(Locale.ROOT);
        boolean autorizado = autorizar(cred, areaNorm);
        String resultado = autorizado ? "SUCESSO" : "FALHA";
        RegistroAcesso registro = new RegistroAcesso(cred, areaNorm, resultado);
        historico.add(registro);
        eventBus.publicar(new AcessoRegistrado(
            registro.getOcorridoEm(),
            registro.getId(),
            registro.getCredencial(),
            registro.getArea(),
            registro.getResultado()
        ));
        return registro;
    }

    public List<RegistroAcesso> listarRegistros() {
        return Collections.unmodifiableList(new ArrayList<>(historico));
    }

    public Set<String> credenciaisDemo() {
        return NIVEL_CREDENCIAL.keySet();
    }

    private boolean autorizar(String credencial, String area) {
        Integer nivelCred = NIVEL_CREDENCIAL.get(credencial);
        Integer nivelArea = NIVEL_AREA.get(area);
        if (nivelCred == null || nivelArea == null) {
            return false;
        }
        return nivelCred >= nivelArea;
    }
}
