package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Exporta a trilha de auditoria (tabela {@code nucleo.auditoria_evento}) para um arquivo texto no
 * formato do antigo {@code dados/auditoria.log}, informando se a cadeia SHA-256 está íntegra.
 * Usado pelo atalho {@code 3-ABRIR-LOG-AUDITORIA.bat}.
 */
public final class ExportarAuditoriaMain {
    private ExportarAuditoriaMain() {}

    public static void main(String[] args) throws IOException {
        Path destino = Path.of(args.length > 0 ? args[0] : "dados/auditoria-exportada.txt");
        try (RegistroAuditoria registro = RegistroAuditoria.abrirPadrao()) {
            registro.exportar(destino);
            System.out.println("Trilha de auditoria: " + registro.descricaoDestino());
            System.out.println("Entradas exportadas: " + registro.consultar().size() + " -> " + destino.toAbsolutePath());
            System.out.println("Cadeia SHA-256     : " + (registro.verificarIntegridade() ? "INTEGRA" : "FALHA (adulterada)"));
        }
    }
}
