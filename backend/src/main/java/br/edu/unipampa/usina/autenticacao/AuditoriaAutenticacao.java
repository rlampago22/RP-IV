package br.edu.unipampa.usina.autenticacao;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RNF03/RNF05 — trilha de autenticação append-only e encadeada por hash, no mesmo espírito do
 * {@code AuditoriaLogs} do núcleo (mvp/). Toda gravação de {@link RegistroAutenticacao} passa por aqui,
 * inclusive cada etapa do 2FA por celular (RF18): envio, aprovação, recusa, código certo/errado, expiração.
 * Nunca recebe o código nem o telefone — só o resultado.
 */
@Service
public class AuditoriaAutenticacao {
    private final RegistroAutenticacaoRepository repositorio;

    public AuditoriaAutenticacao(RegistroAutenticacaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public synchronized RegistroAutenticacao save(RegistroAutenticacao registro) {
        String anterior = repositorio.findTopByHashIsNotNullOrderByIdDesc().map(RegistroAutenticacao::getHash).orElse("GENESIS");
        registro.encadear(anterior);
        return repositorio.saveAndFlush(registro);
    }

    /** Recalcula a cadeia inteira. Devolve o id do primeiro registro adulterado, ou {@code null} se íntegra. */
    @Transactional(readOnly = true)
    public Long primeiroRegistroAdulterado() {
        String esperado = "GENESIS";
        for (RegistroAutenticacao r : repositorio.findByHashIsNotNullOrderByIdAsc()) {
            if (!esperado.equals(r.getHashAnterior()) || !r.calcularHash().equals(r.getHash())) return r.getId();
            esperado = r.getHash();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<RegistroAutenticacao> todos() {
        return repositorio.findByHashIsNotNullOrderByIdAsc();
    }
}
