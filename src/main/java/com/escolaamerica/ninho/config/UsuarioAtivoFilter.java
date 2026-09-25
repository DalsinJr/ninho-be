package com.escolaamerica.ninho.config;

import com.escolaamerica.ninho.iam.service.AutenticacaoService;
import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Relê o usuário da sessão a cada requisição (SPEC §7.1): se ele foi desativado, a sessão cai com 401;
 * se os papéis mudaram, valem já nesta requisição. Custa uma consulta por requisição autenticada.
 *
 * <p>Não é {@code @Component}: registrado só na cadeia do Spring Security, para não rodar duas vezes.
 */
public class UsuarioAtivoFilter extends OncePerRequestFilter {

    private final AutenticacaoService autenticacao;
    private final SecurityContextRepository securityContextRepository;

    public UsuarioAtivoFilter(AutenticacaoService autenticacao, SecurityContextRepository securityContextRepository) {
        this.autenticacao = autenticacao;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Authentication atual = SecurityContextHolder.getContext().getAuthentication();
        if (atual != null && atual.getPrincipal() instanceof UsuarioLogado daSessao) {
            Optional<UsuarioLogado> vigente = autenticacao.recarregar(daSessao.id());
            if (vigente.isEmpty()) {
                SecurityContextHolder.clearContext();
                HttpSession sessao = request.getSession(false);
                if (sessao != null) {
                    sessao.invalidate();
                }
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            if (!vigente.get().equals(daSessao)) {
                SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(AutenticacaoService.autenticacaoDe(vigente.get()));
                SecurityContextHolder.setContext(contexto);
                securityContextRepository.saveContext(contexto, request, response);
            }
        }
        chain.doFilter(request, response);
    }
}
