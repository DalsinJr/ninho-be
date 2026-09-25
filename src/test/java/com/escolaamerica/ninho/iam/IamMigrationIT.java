package com.escolaamerica.ninho.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.escolaamerica.ninho.IntegracaoApiBase;
import com.escolaamerica.ninho.iam.domain.Usuario;
import com.escolaamerica.ninho.iam.repository.UsuarioRepository;
import com.escolaamerica.ninho.shared.domain.Papel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

class IamMigrationIT extends IntegracaoApiBase {

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void seedTemOAdminLocalComRhAdminEDpo() {
        Usuario admin = usuarios.findComPapeisByEmail(ADMIN_EMAIL).orElseThrow();

        assertThat(admin.getId()).isEqualTo(ADMIN_ID);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.getPapeis()).containsExactlyInAnyOrder(Papel.RH_ADMIN, Papel.DPO);
        assertThat(passwordEncoder.matches(ADMIN_SENHA, admin.getSenhaHash())).isTrue();
    }

    @Test
    void emailMaiusculoERecusadoPeloBanco() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO iam_usuario (nome, email, senha_hash) VALUES ('X', 'Maiuscula@ninho.local', 'h')"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailDuplicadoERecusadoPeloBanco() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO iam_usuario (nome, email, senha_hash) VALUES ('X', ?, 'h')", ADMIN_EMAIL))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void papelForaDaListaERecusadoPeloBanco() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO iam_usuario_papel (usuario_id, papel) VALUES (?, 'SUPERUSUARIO')", ADMIN_ID))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sysAuditoriaExiste() {
        List<String> colunas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'sys_auditoria'", String.class);
        assertThat(colunas).contains("usuario_id", "acao", "entidade", "entidade_id", "antes", "depois", "justificativa");
    }

    @Test
    @Transactional
    void entidadeNormalizaEmailESalvaPapeis() {
        Usuario novo = usuarios.saveAndFlush(new Usuario("Rita", "  Rita@Ninho.Local ", "hash", java.util.Set.of(Papel.RH_RECRUTADOR)));

        Usuario lido = usuarios.findComPapeisById(novo.getId()).orElseThrow();
        assertThat(lido.getEmail()).isEqualTo("rita@ninho.local");
        assertThat(lido.getPapeis()).containsExactly(Papel.RH_RECRUTADOR);
        assertThat(lido.getCriadoEm()).isNotNull();
    }
}
