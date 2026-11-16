package br.edu.unipampa.usina.configuracao;

import br.edu.unipampa.usina.autenticacao.AuthDtos;
import br.edu.unipampa.usina.autenticacao.BiometriaService;
import br.edu.unipampa.usina.autenticacao.CodigoAtivacaoService;
import br.edu.unipampa.usina.autenticacao.Papel;
import br.edu.unipampa.usina.autenticacao.Usuario;
import br.edu.unipampa.usina.autenticacao.UsuarioRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Quem ativa o primeiro Administrador? A instalação: enquanto nenhum Administrador tiver biometria,
 * cada inicialização emite um código de ativação para eles e o escreve no log do servidor
 * (mesma ideia da senha inicial de instalação de ferramentas como o Jenkins).
 */
@Component
class AtivacaoAdministradorInicial {
    private static final Logger log = LoggerFactory.getLogger(AtivacaoAdministradorInicial.class);
    private static final DateTimeFormatter HORARIO =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    private final UsuarioRepository usuarios;
    private final BiometriaService biometria;
    private final CodigoAtivacaoService codigos;

    AtivacaoAdministradorInicial(UsuarioRepository usuarios, BiometriaService biometria, CodigoAtivacaoService codigos) {
        this.usuarios = usuarios;
        this.biometria = biometria;
        this.codigos = codigos;
    }

    @EventListener(ApplicationReadyEvent.class)
    void emitirCodigoSeNecessario() {
        if (!biometria.isAcoesCriticasProtegidas()) return;
        var administradores = usuarios.findAll().stream()
            .filter(u -> u.isAtivo() && u.getPapeis().stream().map(Papel::getCodigo).anyMatch("ADMINISTRADOR_SISTEMA"::equals))
            .toList();
        if (administradores.stream().anyMatch(u -> biometria.possuiBiometria(u.getEmail()))) return;
        for (Usuario administrador : administradores) {
            AuthDtos.CodigoAtivacaoResponse emitido = codigos.emitir(administrador, null);
            log.warn("[PRIMEIRO ACESSO] Nenhum Administrador com biometria. Codigo de ativacao de {}: {} (vale ate {})",
                emitido.email(), emitido.codigo(), HORARIO.format(emitido.expiraEm()));
        }
    }
}
