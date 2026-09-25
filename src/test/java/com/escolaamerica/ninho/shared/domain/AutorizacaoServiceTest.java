package com.escolaamerica.ninho.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AutorizacaoServiceTest {

    private final AutorizacaoService autorizacao = new AutorizacaoService();
    private final UsuarioLogado recrutadora =
        new UsuarioLogado(UUID.randomUUID(), "Rita", "rita@ninho.local", Set.of(Papel.RH_RECRUTADOR));

    @Test
    void semAutenticacaoResponde401() {
        assertThatThrownBy(() -> autorizacao.exigirLogado(null))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void principalQueNaoEUsuarioLogadoResponde401() {
        var anonimo = UsernamePasswordAuthenticationToken.authenticated("anon", null, List.of());
        assertThatThrownBy(() -> autorizacao.exigirLogado(anonimo))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void papelAusenteResponde403() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(recrutadora, null, List.of());
        assertThatThrownBy(() -> autorizacao.exigirPapel(auth, Papel.RH_ADMIN))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void qualquerPapelDaListaBasta() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(recrutadora, null, List.of());
        assertThat(autorizacao.exigirPapel(auth, Papel.RH_ADMIN, Papel.RH_RECRUTADOR)).isEqualTo(recrutadora);
    }
}
