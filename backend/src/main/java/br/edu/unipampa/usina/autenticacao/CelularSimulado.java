package br.edu.unipampa.usina.autenticacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Celular simulado do RF18: guarda as solicitações "recebidas" para a tela /celular do front mostrar
 * (aprovar, recusar ou ler o código). Só existe para a demonstração; trocar por push/SMS real é trocar
 * esta classe por outra implementação de {@link CanalCelular}.
 */
@Component
public class CelularSimulado implements CanalCelular {
    private final List<SegundoFatorService.Desafio> recebidas = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void enviar(SegundoFatorService.Desafio desafio) {
        recebidas.add(0, desafio);
        if (recebidas.size() > 20) recebidas.remove(recebidas.size() - 1);
    }

    public List<SegundoFatorService.Desafio> recebidas() {
        synchronized (recebidas) {
            return List.copyOf(recebidas);
        }
    }
}
