package com.escolaamerica.ninho.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ListaLimitadaTest {

    @Test
    void listaVaziaNaoEstaTruncada() {
        ListaLimitada<Integer> lista = ListaLimitada.de(List.of());
        assertThat(lista.itens()).isEmpty();
        assertThat(lista.truncado()).isFalse();
    }

    @Test
    void exatamenteNoTetoNaoEstaTruncada() {
        ListaLimitada<Integer> lista = ListaLimitada.de(numeros(ListaLimitada.TETO));
        assertThat(lista.itens()).hasSize(50);
        assertThat(lista.truncado()).isFalse();
    }

    @Test
    void umAMaisQueOTetoCortaEMarcaTruncado() {
        ListaLimitada<Integer> lista = ListaLimitada.de(numeros(ListaLimitada.LIMITE_CONSULTA));
        assertThat(lista.itens()).hasSize(50).first().isEqualTo(0);
        assertThat(lista.itens()).last().isEqualTo(49);
        assertThat(lista.truncado()).isTrue();
    }

    private static List<Integer> numeros(int quantidade) {
        return IntStream.range(0, quantidade).boxed().toList();
    }
}
