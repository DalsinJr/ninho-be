package com.escolaamerica.ninho.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolaamerica.ninho.IntegracaoApiBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

@ExtendWith(OutputCaptureExtension.class)
class LogSemPiiIT extends IntegracaoApiBase {

    @Test
    void loginComFalhaNaoEscreveOEmailNoLog(CapturedOutput saida) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "segredo.pessoal@ninho.local", "senha", "x"))))
            .andExpect(status().isUnauthorized());

        assertThat(saida.getAll()).doesNotContain("segredo.pessoal@ninho.local");
    }

    @Test
    void layoutDoLogMascaraEmailETelefone(CapturedOutput saida) {
        LoggerFactory.getLogger(LogSemPiiIT.class).info("Contato de teste: ana.ribeiro@escola.com.br, (27) 99876-5432");

        assertThat(saida.getAll())
            .contains("Contato de teste: a***@escola.com.br, [telefone]")
            .doesNotContain("ana.ribeiro@escola.com.br")
            .doesNotContain("99876-5432");
    }
}
