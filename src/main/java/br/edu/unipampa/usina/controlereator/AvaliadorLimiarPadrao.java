package br.edu.unipampa.usina.controlereator;

/** Strategy padrao para limites inferior e superior. */
public class AvaliadorLimiarPadrao implements AvaliadorLimiar {
    @Override
    public ResultadoAvaliacao avaliar(MedicaoReator medicao, Limiar limiar) {
        if (medicao == null || limiar == null) {
            throw new IllegalArgumentException("Medicao e limiar sao obrigatorios");
        }

        double valor = medicao.getValor();
        boolean abaixoDoMinimo = limiar.getValorMin() != null && valor < limiar.getValorMin();
        boolean acimaDoMaximo = limiar.getValorMax() != null && valor > limiar.getValorMax();
        return new ResultadoAvaliacao(abaixoDoMinimo || acimaDoMaximo, medicao);
    }
}