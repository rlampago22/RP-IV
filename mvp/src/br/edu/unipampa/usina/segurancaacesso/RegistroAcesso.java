package br.edu.unipampa.usina.segurancaacesso;

import java.time.Instant;
import java.util.UUID;

/**
 * RF08 / UC04 — registro de tentativa de acesso a área restrita.
 */
public final class RegistroAcesso {
    private final String id;
    private final String credencial;
    private final String area;
    private final String resultado;
    private final Instant ocorridoEm;

    public RegistroAcesso(String credencial, String area, String resultado) {
        this.id = UUID.randomUUID().toString();
        this.credencial = credencial != null ? credencial : "";
        this.area = area != null ? area : "";
        this.resultado = resultado != null ? resultado : "FALHA";
        this.ocorridoEm = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCredencial() {
        return credencial;
    }

    public String getArea() {
        return area;
    }

    public String getResultado() {
        return resultado;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    /** Compatível com o esqueleto legado — já persistido no construtor via facade. */
    public void registrar(String credencialIgnorada, String areaIgnorada, String resultadoIgnorado) {
        // Instantâneo: o registro é imutável após a criação pela facade.
    }
}
