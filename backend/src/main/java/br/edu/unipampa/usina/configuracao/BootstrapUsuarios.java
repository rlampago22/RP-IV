package br.edu.unipampa.usina.configuracao;

import br.edu.unipampa.usina.autenticacao.Papel;
import br.edu.unipampa.usina.autenticacao.PapelRepository;
import br.edu.unipampa.usina.autenticacao.Usuario;
import br.edu.unipampa.usina.autenticacao.UsuarioRepository;
import java.util.Set;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class BootstrapUsuarios {
    @Bean
    CommandLineRunner criarAdministradorDesenvolvimento(UsuarioRepository usuarios, PapelRepository papeis,
        PasswordEncoder encoder, @Value("${app.bootstrap-password}") String senha,
        @Value("${app.reset-bootstrap-passwords}") boolean redefinirSenhas) {
        return args -> {
            if (usuarios.count() != 0) {
                if (redefinirSenhas) redefinirSenhasLocais(usuarios, encoder, senha);
                return;
            }
            criar(usuarios, papeis, encoder, senha, "Administrador do Sistema", "admin@usina.local", "ADMINISTRADOR_SISTEMA");
            criar(usuarios, papeis, encoder, senha, "Operador de Reator", "operador@usina.local", "OPERADOR_REATOR");
            criar(usuarios, papeis, encoder, senha, "Supervisão Central", "supervisao@usina.local", "SUPERVISAO_CENTRAL");
            criar(usuarios, papeis, encoder, senha, "Guarda de Acesso", "guarda@usina.local", "GUARDA_ACESSO");
        };
    }

    private void criar(UsuarioRepository usuarios, PapelRepository papeis, PasswordEncoder encoder,
                       String senha, String nome, String email, String codigoPapel) {
        Papel papel = papeis.findByCodigo(codigoPapel).orElseThrow();
        usuarios.save(new Usuario(nome, email, encoder.encode(senha), Set.of(papel)));
    }

    private void redefinirSenhasLocais(UsuarioRepository usuarios, PasswordEncoder encoder, String senha) {
        List<String> emails = List.of("admin@usina.local", "operador@usina.local", "supervisao@usina.local", "guarda@usina.local");
        List<Usuario> usuariosLocais = usuarios.findAll().stream()
            .filter(usuario -> emails.contains(usuario.getEmail()))
            .toList();
        usuariosLocais.forEach(usuario -> usuario.definirSenhaHash(encoder.encode(senha)));
        usuarios.saveAll(usuariosLocais);
    }
}
