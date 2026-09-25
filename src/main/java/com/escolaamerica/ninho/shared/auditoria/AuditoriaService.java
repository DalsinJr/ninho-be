package com.escolaamerica.ninho.shared.auditoria;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava a trilha em {@code sys_auditoria} (SPEC §4.9, §7.3), sempre dentro da transação de quem chama:
 * auditoria fora dela registraria algo que pode não ter acontecido. Ação no padrão
 * {@code "<feature>.<entidade>.<verbo>"}. Nunca grava valor de dado pessoal ({@link RedatorPii}).
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final JdbcTemplate jdbc;
    private final RedatorPii redator;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(UUID usuarioId, String acao, String entidade, UUID entidadeId,
                          Map<String, ?> antes, Map<String, ?> depois, String justificativa) {
        RedatorPii.Redacao redacao = redator.redigir(antes, depois);
        jdbc.update("""
                INSERT INTO sys_auditoria (usuario_id, acao, entidade, entidade_id, antes, depois, justificativa)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            usuarioId, acao, entidade, entidadeId, redacao.antes(), redacao.depois(), justificativa);
    }
}
