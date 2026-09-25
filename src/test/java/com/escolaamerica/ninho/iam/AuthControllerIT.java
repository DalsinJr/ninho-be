package com.escolaamerica.ninho.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolaamerica.ninho.IntegracaoApiBase;
import com.escolaamerica.ninho.iam.service.AutenticacaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AuthControllerIT extends IntegracaoApiBase {

    // --- smoke 12 (SPEC §12.2) ---

    @Test
    void senhaErradaEEmailInexistenteDevolvemAMesmaResposta() throws Exception {
        JsonNode senhaErrada = respostaDeLogin(ADMIN_EMAIL, "senha-errada");
        JsonNode emailInexistente = respostaDeLogin("ninguem@ninho.local", "qualquer-coisa");

        assertThat(senhaErrada.get("status").asInt()).isEqualTo(401);
        assertThat(senhaErrada.get("message").asText()).isEqualTo(AutenticacaoService.CREDENCIAIS_INVALIDAS);
        assertThat(semTimestamp(senhaErrada)).isEqualTo(semTimestamp(emailInexistente));
    }

    @Test
    void usuarioInativoNaoEntraERecebeAMesmaResposta() throws Exception {
        criarUsuario("Inativo", "inativo@ninho.local", "senha-forte", false, "RH_RECRUTADOR");

        JsonNode inativo = respostaDeLogin("inativo@ninho.local", "senha-forte");
        JsonNode senhaErrada = respostaDeLogin(ADMIN_EMAIL, "senha-errada");

        assertThat(semTimestamp(inativo)).isEqualTo(semTimestamp(senhaErrada));
    }

    // --- sessão ---

    @Test
    void loginAbreSessaoEMeDevolveOUsuario() throws Exception {
        MockHttpSession sessao = sessaoDoAdmin();

        mockMvc.perform(get("/api/v1/auth/me").session(sessao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ADMIN_ID.toString()))
            .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
            .andExpect(jsonPath("$.papeis[0]").value("RH_ADMIN"))
            .andExpect(jsonPath("$.papeis[1]").value("DPO"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT ultimo_acesso IS NOT NULL FROM iam_usuario WHERE id = ?",
            Boolean.class, ADMIN_ID)).isTrue();
    }

    @Test
    void emailComMaiusculasEEspacosTambemEntra() throws Exception {
        sessaoDe("  Admin@Ninho.Local ", ADMIN_SENHA);
    }

    @Test
    void semSessaoMeResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidaASessao() throws Exception {
        MockHttpSession sessao = sessaoDoAdmin();

        mockMvc.perform(post("/api/v1/auth/logout").session(sessao)).andExpect(status().isNoContent());

        assertThat(sessao.isInvalid()).isTrue();
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesativadoComSessaoAbertaCaiNaProximaRequisicao() throws Exception {
        UUID id = criarUsuario("Rita", "rita@ninho.local", "senha-forte", true, "RH_RECRUTADOR");
        MockHttpSession sessao = sessaoDe("rita@ninho.local", "senha-forte");

        jdbc.update("UPDATE iam_usuario SET ativo = false WHERE id = ?", id);

        mockMvc.perform(get("/api/v1/auth/me").session(sessao)).andExpect(status().isUnauthorized());
        assertThat(sessao.isInvalid()).isTrue();
    }

    @Test
    void trocaDePapelValeNaRequisicaoSeguinte() throws Exception {
        UUID id = criarUsuario("Rita", "rita@ninho.local", "senha-forte", true, "RH_RECRUTADOR");
        MockHttpSession sessao = sessaoDe("rita@ninho.local", "senha-forte");

        jdbc.update("INSERT INTO iam_usuario_papel (usuario_id, papel) VALUES (?, 'GESTOR')", id);

        mockMvc.perform(get("/api/v1/auth/me").session(sessao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.papeis.length()").value(2));
    }

    // --- troca de senha ---

    @Test
    void novaSenhaCurtaResponde400ComOCampo() throws Exception {
        MockHttpSession sessao = sessaoDoAdmin();

        mockMvc.perform(trocaDeSenha(sessao, ADMIN_SENHA, "curta"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields[0].field").value("novaSenha"))
            .andExpect(jsonPath("$.fields[0].code").value("TAMANHO"));
    }

    @Test
    void senhaAtualErradaResponde400() throws Exception {
        MockHttpSession sessao = sessaoDoAdmin();

        mockMvc.perform(trocaDeSenha(sessao, "nao-e-esta", "nova-senha-forte"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Senha atual incorreta"));
    }

    @Test
    void trocaDeSenhaPermiteEntrarComANova() throws Exception {
        MockHttpSession sessao = sessaoDoAdmin();

        mockMvc.perform(trocaDeSenha(sessao, ADMIN_SENHA, "nova-senha-forte")).andExpect(status().isNoContent());

        sessaoDe(ADMIN_EMAIL, "nova-senha-forte");
        assertThat(respostaDeLogin(ADMIN_EMAIL, ADMIN_SENHA).get("status").asInt()).isEqualTo(401);
    }

    private JsonNode respostaDeLogin(String email, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "senha", senha))))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo);
    }

    private static JsonNode semTimestamp(JsonNode resposta) {
        ObjectNode copia = resposta.deepCopy();
        copia.remove("timestamp");
        return copia;
    }

    private org.springframework.test.web.servlet.RequestBuilder trocaDeSenha(MockHttpSession sessao, String atual, String nova)
        throws Exception {
        return put("/api/v1/auth/me/senha").session(sessao)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("senhaAtual", atual, "novaSenha", nova)));
    }
}
