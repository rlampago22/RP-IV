package br.edu.unipampa.usina.emergencias;

import br.edu.unipampa.usina.autenticacao.AuditoriaAutenticacao;
import br.edu.unipampa.usina.autenticacao.RegistroAutenticacao;
import br.edu.unipampa.usina.autenticacao.Usuario;
import br.edu.unipampa.usina.autenticacao.UsuarioRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF09/RF10 — regra da gestão de emergências. Cada passo (criar, ativar protocolo, encerrar) entra na
 * trilha encadeada por hash (RNF03), com quem fez. A exigência da digital fica no controller.
 */
@Service
public class GestaoEmergencias {
    private static final String METODO = "ACAO_CRITICA";

    private final EmergenciaRepository emergencias;
    private final UsuarioRepository usuarios;
    private final AuditoriaAutenticacao auditoria;

    public GestaoEmergencias(EmergenciaRepository emergencias, UsuarioRepository usuarios, AuditoriaAutenticacao auditoria) {
        this.emergencias = emergencias;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Emergencia> listar() {
        return emergencias.findAllByOrderByIdDesc();
    }

    @Transactional
    public Emergencia criar(Emergencia.Tipo tipo, String descricao, String email) {
        Emergencia emergencia = emergencias.save(new Emergencia(tipo, descricao.trim(), email));
        registrar(email, "EMERGENCIA_CRIADA");
        return emergencia;
    }

    @Transactional
    public Emergencia ativarProtocolo(Long id, ProtocoloEmergencia protocolo, String email) {
        Emergencia emergencia = buscar(id);
        emergencia.ativarProtocolo(protocolo, email);
        registrar(email, "PROTOCOLO_CONTINGENCIA_ATIVADO");
        return emergencia;
    }

    @Transactional
    public Emergencia encerrar(Long id, String email) {
        Emergencia emergencia = buscar(id);
        emergencia.encerrar(email);
        registrar(email, "EMERGENCIA_ENCERRADA");
        return emergencia;
    }

    private Emergencia buscar(Long id) {
        return emergencias.findById(id).orElseThrow(() -> new NoSuchElementException("Emergência não encontrada."));
    }

    private void registrar(String email, String resultado) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
        auditoria.save(new RegistroAutenticacao(usuario, email, resultado, METODO));
    }
}
