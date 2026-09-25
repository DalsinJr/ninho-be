package com.escolaamerica.ninho.iam.service;

import com.escolaamerica.ninho.iam.domain.Usuario;
import com.escolaamerica.ninho.iam.repository.UsuarioRepository;
import com.escolaamerica.ninho.shared.domain.BusinessRuleViolationException;
import com.escolaamerica.ninho.shared.domain.ResourceNotFoundException;
import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login simplificado com sessão do Spring Security (SPEC D12, §7.1). */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    /** Mesma mensagem para e-mail inexistente, senha errada e usuário inativo (§7.1, §7.3). */
    public static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";
    static final String SENHA_ATUAL_INCORRETA = "Senha atual incorreta";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public UsuarioLogado entrar(String email, String senha, HttpServletRequest request, HttpServletResponse response) {
        String emailNormalizado = Usuario.normalizarEmail(email);
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(emailNormalizado, senha == null ? "" : senha));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }
        Usuario usuario = usuarios.findComPapeisByEmail(emailNormalizado)
            .orElseThrow(() -> new BadCredentialsException(CREDENCIAIS_INVALIDAS));
        usuario.setUltimoAcesso(Instant.now(clock));
        UsuarioLogado logado = paraUsuarioLogado(usuario);

        if (request.getSession(false) != null) {
            request.changeSessionId(); // fixação de sessão
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacaoDe(logado));
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);
        return logado;
    }

    public void sair(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    @Transactional
    public void trocarSenha(UUID usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarios.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (senhaAtual == null || !passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new BusinessRuleViolationException(SENHA_ATUAL_INCORRETA);
        }
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
    }

    /** Relê o usuário da sessão: vazio se ele foi desativado ou não existe mais (§7.1). */
    @Transactional(readOnly = true)
    public Optional<UsuarioLogado> recarregar(UUID usuarioId) {
        return usuarios.findComPapeisById(usuarioId)
            .filter(Usuario::isAtivo)
            .map(AutenticacaoService::paraUsuarioLogado);
    }

    public static Authentication autenticacaoDe(UsuarioLogado logado) {
        return UsernamePasswordAuthenticationToken.authenticated(logado, null,
            logado.papeis().stream().map(p -> new SimpleGrantedAuthority("ROLE_" + p.name())).toList());
    }

    private static UsuarioLogado paraUsuarioLogado(Usuario usuario) {
        return new UsuarioLogado(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapeis());
    }
}
