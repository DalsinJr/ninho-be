package com.escolaamerica.ninho.config;

import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Põe no MDC o {@code requestId} e o id do usuário logado (nunca o e-mail), para o layout de log
 * (SPEC §11.3). Registrado na cadeia do Spring Security, depois do {@link UsuarioAtivoFilter}.
 */
public class ContextoRequisicaoFilter extends OncePerRequestFilter {

    static final String REQUEST_ID = "requestId";
    static final String USUARIO = "usuario";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioLogado usuario) {
            MDC.put(USUARIO, usuario.id().toString());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID);
            MDC.remove(USUARIO);
        }
    }
}
