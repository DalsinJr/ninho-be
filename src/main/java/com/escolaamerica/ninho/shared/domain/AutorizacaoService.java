package com.escolaamerica.ninho.shared.domain;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Ponto único de autorização por papel, no padrão do naturexpress (SPEC §7.1): o controller recebe o
 * {@link Authentication} e pede aqui o {@link UsuarioLogado}. Recortes por dado (ex.: vagas do gestor)
 * vão na consulta do serviço, não aqui.
 */
@Service
public class AutorizacaoService {

    static final String SEM_PERMISSAO = "Sem permissão";

    public UsuarioLogado exigirLogado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof UsuarioLogado usuario)) {
            throw new AuthenticationCredentialsNotFoundException("Sessão ausente");
        }
        return usuario;
    }

    public UsuarioLogado exigirPapel(Authentication authentication, Papel... papeis) {
        UsuarioLogado usuario = exigirLogado(authentication);
        if (!usuario.temAlgum(papeis)) {
            throw new AccessDeniedException(SEM_PERMISSAO);
        }
        return usuario;
    }
}
