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
import java.util.function.BooleanSupplier;

/**
 * RF07 (credencial simulada) + RF08 — valida acesso a área e persiste {@link RegistroAcesso}.
 * Credenciais de demo (crachá): OPER-01, SUP-01, GUARDA-01.
 */
public final class AcessoFacade {
    public static final String RESULTADO_BLOQUEADO = "BLOQUEADO_CONTINGENCIA";

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
    /** RF10 / #99: com protocolo de contingência ativo, nenhum acesso novo é liberado. */
    private volatile BooleanSupplier contingenciaAtiva = () -> false;

    public AcessoFacade(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public RegistroAcesso solicitarAcesso(String credencial, String area) {
        String cred = credencial == null ? "" : credencial.trim().toUpperCase(Locale.ROOT);
        String areaNorm = area == null ? "" : area.trim().toUpperCase(Locale.ROOT);
        String resultado;
        if (contingenciaAtiva.getAsBoolean()) {
            resultado = RESULTADO_BLOQUEADO; // continua registrado e auditado, só não libera
        } else {
            resultado = autorizar(cred, areaNorm) ? "SUCESSO" : "FALHA";
        }
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

    /**
     * Liga a regra do protocolo de contingência (evacuação/contenção: "bloquear novos acessos").
     * Enquanto o fornecedor disser {@code true}, toda solicitação sai como {@value #RESULTADO_BLOQUEADO}.
     */
    public void bloquearDuranteContingencia(BooleanSupplier contingenciaAtiva) {
        this.contingenciaAtiva = contingenciaAtiva == null ? () -> false : contingenciaAtiva;
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
