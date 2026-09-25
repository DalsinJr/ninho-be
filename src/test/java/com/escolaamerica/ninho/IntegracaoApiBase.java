package com.escolaamerica.ninho;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes de integração: contexto completo, Postgres e MinIO por Testcontainers.
 * Sem Docker, os testes ficam desabilitados — condicionados, nunca removidos (SPEC §12.1).
 */
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegracaoApiBase {

    /** Administrador semeado pela V2 (SPEC §7.1). */
    protected static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    protected static final String ADMIN_EMAIL = "admin@ninho.local";
    protected static final String ADMIN_SENHA = "ninho123";
    private static final String ADMIN_HASH = "$2a$10$YEoeN6Re/J6lmZE8maK0ouDwKxQPISMzkkr0OspC8HxfVD5jkBxM.";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Volta o banco ao estado das migrations: só o admin semeado, ativo, com a senha e os papéis de origem. */
    @AfterEach
    void restaurarBase() {
        jdbc.update("DELETE FROM sys_auditoria");
        jdbc.update("DELETE FROM iam_usuario_papel WHERE usuario_id <> ?", ADMIN_ID);
        jdbc.update("DELETE FROM iam_usuario WHERE id <> ?", ADMIN_ID);
        jdbc.update("UPDATE iam_usuario SET nome = 'Administrador', email = ?, senha_hash = ?, ativo = true, ultimo_acesso = NULL WHERE id = ?",
            ADMIN_EMAIL, ADMIN_HASH, ADMIN_ID);
        jdbc.update("DELETE FROM iam_usuario_papel WHERE usuario_id = ?", ADMIN_ID);
        jdbc.update("INSERT INTO iam_usuario_papel (usuario_id, papel) VALUES (?, 'RH_ADMIN'), (?, 'DPO')", ADMIN_ID, ADMIN_ID);
    }
}
