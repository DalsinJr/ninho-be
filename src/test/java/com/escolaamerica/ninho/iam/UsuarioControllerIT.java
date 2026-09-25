package com.escolaamerica.ninho.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolaamerica.ninho.IntegracaoApiBase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class UsuarioControllerIT extends IntegracaoApiBase {

    // --- §7.2 / §15.2: um "permitido" e um "negado" por rota ---

    @Test
    void rhAdminPodeERecrutadorNaoPodeEmTodasAsRotas() throws Exception {
        UUID ritaId = criarUsuario("Rita", "rita@ninho.local", "senha-forte", true, "RH_RECRUTADOR");
        MockHttpSession admin = sessaoDoAdmin();
        MockHttpSession rita = sessaoDe("rita@ninho.local", "senha-forte");

        List<MockHttpServletRequestBuilder> rotas = List.of(
            get("/api/v1/usuarios"),
            comCorpo(post("/api/v1/usuarios"), Map.of("nome", "Novo", "email", "novo@ninho.local",
                "papeis", List.of("GESTOR"), "senhaInicial", "senha-forte")),
            comCorpo(put("/api/v1/usuarios/" + ritaId), Map.of("nome", "Rita", "email", "rita@ninho.local",
                "papeis", List.of("RH_RECRUTADOR"), "ativo", true)),
            comCorpo(put("/api/v1/usuarios/" + ritaId + "/senha"), Map.of("novaSenha", "outra-senha-forte")));

        for (MockHttpServletRequestBuilder rota : rotas) {
            mockMvc.perform(rota.session(rita)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/usuarios").session(admin)).andExpect(status().isOk());
        mockMvc.perform(rotas.get(1).session(admin)).andExpect(status().isCreated());
        mockMvc.perform(rotas.get(2).session(admin)).andExpect(status().isOk());
        mockMvc.perform(rotas.get(3).session(admin)).andExpect(status().isNoContent());
    }

    @Test
    void semSessaoResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
    }

    // --- demo da fatia ---

    @Test
    void adminCriaRecrutadorQueEntraESemPermissaoParaUsuarios() throws Exception {
        MockHttpSession admin = sessaoDoAdmin();

        mockMvc.perform(comCorpo(post("/api/v1/usuarios"), Map.of("nome", "Rita Recrutadora", "email", "Rita@Ninho.Local",
                    "papeis", List.of("RH_RECRUTADOR"), "senhaInicial", "senha-inicial"))
                .session(admin))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("rita@ninho.local"))
            .andExpect(jsonPath("$.papeis[0]").value("RH_RECRUTADOR"))
            .andExpect(jsonPath("$.ativo").value(true));

        MockHttpSession rita = sessaoDe("rita@ninho.local", "senha-inicial");
        mockMvc.perform(get("/api/v1/usuarios").session(rita)).andExpect(status().isForbidden());
    }

    // --- regras da §7.1 ---

    @Test
    void rebaixarOUltimoAdminERecusadoEONadaMuda() throws Exception {
        MockHttpSession admin = sessaoDoAdmin();

        mockMvc.perform(comCorpo(put("/api/v1/usuarios/" + ADMIN_ID), Map.of("nome", "Administrador",
                    "email", ADMIN_EMAIL, "papeis", List.of("DPO"), "ativo", true))
                .session(admin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Este é o último administrador ativo"));

        assertThat(jdbc.queryForList("SELECT papel FROM iam_usuario_papel WHERE usuario_id = ?", String.class, ADMIN_ID))
            .containsExactlyInAnyOrder("RH_ADMIN", "DPO");
    }

    @Test
    void comSegundoAdminOutroAdminPodeSerRebaixado() throws Exception {
        UUID biaId = criarUsuario("Bia", "bia@ninho.local", "senha-forte", true, "RH_ADMIN");
        MockHttpSession admin = sessaoDoAdmin();

        mockMvc.perform(comCorpo(put("/api/v1/usuarios/" + biaId), Map.of("nome", "Bia", "email", "bia@ninho.local",
                    "papeis", List.of("GESTOR"), "ativo", true))
                .session(admin))
            .andExpect(status().isOk());
    }

    @Test
    void comSegundoAdminNaoPodeRemoverOProprioAdmin() throws Exception {
        criarUsuario("Bia", "bia@ninho.local", "senha-forte", true, "RH_ADMIN");
        MockHttpSession admin = sessaoDoAdmin();

        mockMvc.perform(comCorpo(put("/api/v1/usuarios/" + ADMIN_ID), Map.of("nome", "Administrador",
                    "email", ADMIN_EMAIL, "papeis", List.of("DPO"), "ativo", true))
                .session(admin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Você não pode remover seu próprio acesso de administrador"));
    }

    @Test
    void emailDuplicadoComCaixaDiferenteResponde409() throws Exception {
        criarUsuario("Rita", "rita@ninho.local", "senha-forte", true, "RH_RECRUTADOR");

        mockMvc.perform(comCorpo(post("/api/v1/usuarios"), Map.of("nome", "Outra Rita", "email", "RITA@ninho.local",
                    "papeis", List.of("GESTOR"), "senhaInicial", "senha-forte"))
                .session(sessaoDoAdmin()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Já existe um usuário com este e-mail"));
    }

    @Test
    void validacaoApontaOsCampos() throws Exception {
        mockMvc.perform(comCorpo(post("/api/v1/usuarios"), Map.of("nome", "", "email", "x@ninho.local",
                    "papeis", List.of(), "senhaInicial", "curta"))
                .session(sessaoDoAdmin()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields[?(@.field == 'nome')].code").value("OBRIGATORIO"))
            .andExpect(jsonPath("$.fields[?(@.field == 'papeis')].code").value("OBRIGATORIO"))
            .andExpect(jsonPath("$.fields[?(@.field == 'senhaInicial')].code").value("TAMANHO"));
    }

    @Test
    void usuarioInexistenteResponde404() throws Exception {
        mockMvc.perform(comCorpo(put("/api/v1/usuarios/" + UUID.randomUUID() + "/senha"), Map.of("novaSenha", "senha-forte"))
                .session(sessaoDoAdmin()))
            .andExpect(status().isNotFound());
    }

    // --- §5.4: teto ---

    @Test
    void listaCortaEm50EABuscaEncontraOQueFicouDeFora() throws Exception {
        IntStream.rangeClosed(1, 51).forEach(i ->
            criarUsuario(String.format("Usuário %02d", i), "u" + i + "@ninho.local", "senha-forte", true, "GESTOR"));
        MockHttpSession admin = sessaoDoAdmin();

        mockMvc.perform(get("/api/v1/usuarios").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itens.length()").value(50))
            .andExpect(jsonPath("$.truncado").value(true));

        mockMvc.perform(get("/api/v1/usuarios").param("q", "Usuário 51").session(admin))
            .andExpect(jsonPath("$.itens.length()").value(1))
            .andExpect(jsonPath("$.itens[0].email").value("u51@ninho.local"))
            .andExpect(jsonPath("$.truncado").value(false));

        mockMvc.perform(get("/api/v1/usuarios").param("papel", "DPO").session(admin))
            .andExpect(jsonPath("$.itens.length()").value(1))
            .andExpect(jsonPath("$.itens[0].papeis.length()").value(2));
    }

    // --- auditoria ---

    @Test
    void trocaDePapelGravaAuditoriaSemDadoPessoal() throws Exception {
        UUID ritaId = criarUsuario("Rita Souza", "rita@ninho.local", "senha-forte", true, "RH_RECRUTADOR");

        mockMvc.perform(comCorpo(put("/api/v1/usuarios/" + ritaId), Map.of("nome", "Rita Souza", "email", "rita@ninho.local",
                    "papeis", List.of("RH_RECRUTADOR", "GESTOR"), "ativo", true))
                .session(sessaoDoAdmin()))
            .andExpect(status().isOk());

        Map<String, Object> linha = jdbc.queryForMap("SELECT * FROM sys_auditoria WHERE acao = 'iam.usuario.papeis'");
        assertThat(linha.get("usuario_id").toString()).isEqualTo(ADMIN_ID.toString());
        assertThat(linha.get("entidade_id").toString()).isEqualTo(ritaId.toString());
        assertThat((String) linha.get("antes")).isEqualTo("{\"papeis\":[\"RH_RECRUTADOR\"]}");
        assertThat((String) linha.get("depois")).isEqualTo("{\"papeis\":[\"RH_RECRUTADOR\",\"GESTOR\"]}");
        String todaAuditoria = String.join(" ", jdbc.queryForList(
            "SELECT coalesce(antes, '') || coalesce(depois, '') FROM sys_auditoria", String.class));
        assertThat(todaAuditoria).doesNotContain("Rita", "rita@ninho.local");
    }

    private MockHttpServletRequestBuilder comCorpo(MockHttpServletRequestBuilder builder, Map<String, ?> corpo) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
    }
}
