package com.escolaamerica.ninho.shared.api;

import java.util.List;

/**
 * Lista com teto (SPEC §5.4): no máximo {@link #TETO} itens e {@code truncado} quando houve corte.
 * A consulta pede {@code TETO + 1} linhas; a linha extra só serve para saber se houve corte.
 */
public record ListaLimitada<T>(List<T> itens, boolean truncado) {

    public static final int TETO = 50;

    /** Quantas linhas a consulta deve pedir ao banco. */
    public static final int LIMITE_CONSULTA = TETO + 1;

    public static <T> ListaLimitada<T> de(List<T> resultadoComUmAMais) {
        boolean truncado = resultadoComUmAMais.size() > TETO;
        List<T> itens = truncado ? List.copyOf(resultadoComUmAMais.subList(0, TETO)) : List.copyOf(resultadoComUmAMais);
        return new ListaLimitada<>(itens, truncado);
    }
}
