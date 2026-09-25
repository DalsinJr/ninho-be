package com.escolaamerica.ninho.shared.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.escolaamerica.ninho.IntegracaoApiBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

class AuditoriaServiceIT extends IntegracaoApiBase {

    @Autowired
    AuditoriaService auditoria;

    @Autowired
    TransactionTemplate transacao;

    @Test
    void gravaLinhaRedigidaNaTransacaoDeQuemChama() {
        transacao.executeWithoutResult(tx -> auditoria.registrar(ADMIN_ID, "iam.usuario.dados", "iam_usuario", ADMIN_ID,
            Map.of("nome", "Administrador"), Map.of("nome", "Ana Ribeiro"), null));

        Map<String, Object> linha = jdbc.queryForMap("SELECT * FROM sys_auditoria WHERE acao = 'iam.usuario.dados'");
        assertThat(linha.get("usuario_id").toString()).isEqualTo(ADMIN_ID.toString());
        assertThat(linha.get("entidade")).isEqualTo("iam_usuario");
        assertThat((String) linha.get("antes")).isEqualTo("{\"nome\":\"[presente]\"}");
        assertThat((String) linha.get("depois")).isEqualTo("{\"nome\":\"[alterado]\"}");
    }

    @Test
    void foraDeTransacaoERecusada() {
        assertThatThrownBy(() -> auditoria.registrar(ADMIN_ID, "iam.usuario.dados", "iam_usuario", ADMIN_ID, null, null, null))
            .isInstanceOf(IllegalTransactionStateException.class);
    }
}
