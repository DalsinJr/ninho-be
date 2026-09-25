package com.escolaamerica.ninho.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MascaraPiiConverterTest {

    @Test
    void mascaraEmail() {
        assertThat(MascaraPiiConverter.mascarar("Falha no login de maria.silva@escola.com.br"))
            .isEqualTo("Falha no login de m***@escola.com.br");
    }

    @Test
    void mascaraTelefonesComESemDdd() {
        assertThat(MascaraPiiConverter.mascarar("tel (27) 99876-5432 e 27998765432 e +55 27 3322-1100"))
            .isEqualTo("tel [telefone] e [telefone] e [telefone]");
    }

    @Test
    void naoMexeEmUuidDataOuTextoComum() {
        String texto = "Usuário 00000000-0000-0000-0000-000000000001 entrou em 2026-09-25T02:28:10 (status 401)";
        assertThat(MascaraPiiConverter.mascarar(texto)).isEqualTo(texto);
    }
}
