package com.escolaamerica.ninho.iam.service;

import com.escolaamerica.ninho.iam.domain.Usuario;
import com.escolaamerica.ninho.iam.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o usuário para o {@code DaoAuthenticationProvider}. Inativo vira conta desabilitada (§7.1). */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarios.findComPapeisByEmail(Usuario.normalizarEmail(email))
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(usuario.getEmail())
            .password(usuario.getSenhaHash())
            .disabled(!usuario.isAtivo())
            .authorities(usuario.getPapeis().stream().map(p -> new SimpleGrantedAuthority("ROLE_" + p.name())).toList())
            .build();
    }
}
