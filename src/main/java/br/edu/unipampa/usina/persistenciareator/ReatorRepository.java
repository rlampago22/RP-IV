package br.edu.unipampa.usina.persistenciareator;

import br.edu.unipampa.usina.alarmes.Alarme;
import br.edu.unipampa.usina.controlereator.MedicaoReator;
import java.util.ArrayList;
import java.util.List;

public class ReatorRepository {
    private final List<MedicaoReator> medicoes = new ArrayList<>();
    private final List<Alarme> alarmes = new ArrayList<>();

    public void salvarMedicao(MedicaoReator medicao) {
        if (medicao == null) {
            throw new IllegalArgumentException("Medicao nao pode ser nula");
        }
        medicoes.add(medicao);
    }

    public void salvarAlarme(Alarme alarme) {
        if (alarme == null) {
            throw new IllegalArgumentException("Alarme nao pode ser nulo");
        }
        alarmes.add(alarme);
    }

    public Object buscarHistorico(long reatorId) {
        return List.copyOf(medicoes);
    }

    public List<MedicaoReator> consultarMedicoes() {
        return List.copyOf(medicoes);
    }

    public List<Alarme> consultarAlarmes() {
        return List.copyOf(alarmes);
    }
}
