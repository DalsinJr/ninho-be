package com.escolaamerica.ninho.shared.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RedatorPiiTest {

    private final RedatorPii redator = new RedatorPii(new ObjectMapper());

    @Test
    void campoPessoalNuncaApareceComValor() {
        RedatorPii.Redacao r = redator.redigir(
            Map.of("nome", "Ana Ribeiro", "email", "ana@x.com", "ativo", true),
            Map.of("nome", "Ana R. Souza", "email", "ana@x.com", "ativo", false));

        assertThat(r.antes()).doesNotContain("Ana", "ana@x.com").contains("\"nome\":\"[presente]\"");
        assertThat(r.depois()).doesNotContain("Ana", "ana@x.com")
            .contains("\"nome\":\"[alterado]\"")
            .contains("\"email\":\"[presente]\"")
            .contains("\"ativo\":false");
    }

    @Test
    void camposNaoPessoaisMantemOValor() {
        RedatorPii.Redacao r = redator.redigir(
            Map.of("papeis", List.of("RH_RECRUTADOR")),
            Map.of("papeis", List.of("RH_ADMIN", "RH_RECRUTADOR")));

        assertThat(r.antes()).isEqualTo("{\"papeis\":[\"RH_RECRUTADOR\"]}");
        assertThat(r.depois()).isEqualTo("{\"papeis\":[\"RH_ADMIN\",\"RH_RECRUTADOR\"]}");
    }

    @Test
    void ladoAusenteFicaNulo() {
        RedatorPii.Redacao r = redator.redigir(null, Map.of("email", "rita@ninho.local"));

        assertThat(r.antes()).isNull();
        assertThat(r.depois()).isEqualTo("{\"email\":\"[presente]\"}");
    }
}
