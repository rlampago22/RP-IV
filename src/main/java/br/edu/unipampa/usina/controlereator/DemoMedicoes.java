package br.edu.unipampa.usina.controlereator;

/**
 * Demo mínima Semana 2 — registra medições em memória e imprime no console.
 *
 * Compilar/rodar (a partir da raiz do repo):
 *   javac -d out src/main/java/br/edu/unipampa/usina/controlereator/*.java
 *   java -cp out br.edu.unipampa.usina.controlereator.DemoMedicoes
 */
public class DemoMedicoes {

    public static void main(String[] args) {
        ReatorFacade facade = new ReatorFacade();
        facade.registrarSensor(1L, "TEMPERATURA");
        facade.registrarSensor(2L, "PRESSAO");

        facade.receberLeitura(1L, 310.5);
        facade.receberLeitura(2L, 155.0);
        facade.receberLeitura(1L, 312.0);

        System.out.println("=== Demo ControleReator (memoria) ===");
        System.out.println("Total de medicoes: " + facade.quantidadeMedicoes());
        for (MedicaoReator m : facade.consultarHistorico(1L)) {
            System.out.println(
                "- sensor=" + m.getSensor().getId()
                    + " tipo=" + m.getSensor().getTipo()
                    + " valor=" + m.getValor()
                    + " em=" + m.getTimestamp()
            );
        }
    }
}
