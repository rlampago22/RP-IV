package br.edu.unipampa.usina.contingencia;

import java.io.IOException;
import java.util.List;

/** De onde o núcleo lê os protocolos de contingência (a Central, ou um dublê nos testes). */
@FunctionalInterface
public interface FonteContingencia {
    /** @throws IOException se a Central não respondeu — o núcleo mantém o último estado conhecido */
    List<ProtocoloRemoto> consultar() throws IOException;
}
