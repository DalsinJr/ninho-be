package com.escolaamerica.ninho.shared.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolaamerica.ninho.shared.domain.BusinessRuleViolationException;
import com.escolaamerica.ninho.shared.domain.ConflictException;
import com.escolaamerica.ninho.shared.domain.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T14:03:00Z"), ZoneOffset.UTC);
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
            .setControllerAdvice(new ApiExceptionHandler(clock))
            .build();
    }

    @Test
    void regraDeNegocioResponde400ComAMensagemDoServico() throws Exception {
        mockMvc.perform(get("/teste/regra"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Este é o último administrador ativo"))
            .andExpect(jsonPath("$.path").value("/teste/regra"))
            .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    void validacaoResponde400ComFieldsECodigosDaSpec() throws Exception {
        mockMvc.perform(post("/teste/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "nome": "", "email": "nao-e-email", "senha": "123" }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.length()").value(3))
            .andExpect(jsonPath("$.fields[?(@.field == 'nome')].code").value("OBRIGATORIO"))
            .andExpect(jsonPath("$.fields[?(@.field == 'email')].code").value("FORMATO"))
            .andExpect(jsonPath("$.fields[?(@.field == 'senha')].code").value("TAMANHO"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void corpoIlegivelResponde400() throws Exception {
        mockMvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{ quebrado"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(ApiExceptionHandler.MENSAGEM_REQUISICAO_INVALIDA));
    }

    @Test
    void conflitoDoServicoResponde409() throws Exception {
        mockMvc.perform(get("/teste/conflito"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Já existe um usuário com este e-mail"));
    }

    @Test
    void lockOtimistaResponde409() throws Exception {
        mockMvc.perform(get("/teste/versao"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(ApiExceptionHandler.MENSAGEM_CONFLITO_VERSAO));
    }

    @Test
    void integridadeResponde409SemVazarOBanco() throws Exception {
        mockMvc.perform(get("/teste/integridade"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(ApiExceptionHandler.MENSAGEM_CONFLITO_DADOS))
            .andExpect(jsonPath("$.message", not(containsString("uk_iam_usuario_email"))));
    }

    @Test
    void naoEncontradoResponde404() throws Exception {
        mockMvc.perform(get("/teste/ausente")).andExpect(status().isNotFound());
    }

    @Test
    void acessoNegadoResponde403() throws Exception {
        mockMvc.perform(get("/teste/negado")).andExpect(status().isForbidden());
    }

    @Test
    void inesperadaResponde500Generico() throws Exception {
        mockMvc.perform(get("/teste/inesperada"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Erro inesperado"));
    }

    record Cadastro(@NotBlank String nome, @Email String email, @Size(min = 8) String senha) {
    }

    @RestController
    static class ControllerDeTeste {

        @GetMapping("/teste/regra")
        void regra() {
            throw new BusinessRuleViolationException("Este é o último administrador ativo");
        }

        @PostMapping("/teste/validacao")
        void validacao(@Valid @RequestBody Cadastro cadastro) {
        }

        @GetMapping("/teste/conflito")
        void conflito() {
            throw new ConflictException("Já existe um usuário com este e-mail");
        }

        @GetMapping("/teste/versao")
        void versao() {
            throw new ObjectOptimisticLockingFailureException(Object.class, "id");
        }

        @GetMapping("/teste/integridade")
        void integridade() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_iam_usuario_email\"",
                new SQLException("uk_iam_usuario_email"));
        }

        @GetMapping("/teste/ausente")
        void ausente() {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }

        @GetMapping("/teste/negado")
        void negado() {
            throw new AccessDeniedException("Sem permissão");
        }

        @GetMapping("/teste/inesperada")
        void inesperada() {
            throw new IllegalStateException("boom");
        }
    }
}
