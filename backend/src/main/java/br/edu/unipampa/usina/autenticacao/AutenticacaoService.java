package br.edu.unipampa.usina.autenticacao;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService implements UserDetailsService {
    private final UsuarioRepository usuarios;

    public AutenticacaoService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
            .filter(Usuario::isAtivo)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        List<SimpleGrantedAuthority> authorities = usuario.getPapeis().stream()
            .map(Papel::getCodigo)
            .map(codigo -> new SimpleGrantedAuthority("ROLE_" + codigo))
            .toList();
        return new User(usuario.getEmail(), usuario.getSenhaHash(), authorities);
    }

    public AuthDtos.UsuarioResponse apresentar(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        return new AuthDtos.UsuarioResponse(usuario.getNome(), usuario.getEmail(), usuario.getPapeis().stream()
            .map(Papel::getCodigo).sorted().toList());
    }
}
