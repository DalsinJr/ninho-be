package com.escolaamerica.ninho.shared.auditoria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Redige dado pessoal antes de gravar na auditoria (SPEC §4.9, §7.3): campo pessoal aparece só pelo
 * NOME, com {@code "[alterado]"} quando mudou ou {@code "[presente]"} quando só consta. Demais campos
 * (ex.: papéis, ativo) mantêm o valor.
 */
@Component
public class RedatorPii {

    public static final String ALTERADO = "[alterado]";
    public static final String PRESENTE = "[presente]";

    /** Nomes de campo tratados como dado pessoal em qualquer entidade. */
    public static final Set<String> CAMPOS_PESSOAIS = Set.of(
        "nome", "nomeCompleto", "nomeSocial", "email", "telefone", "cpf", "cidade", "linkedinUrl",
        "pretensaoSalarial", "disponibilidade", "endereco");

    private final ObjectMapper json;

    public RedatorPii(ObjectMapper json) {
        this.json = json;
    }

    public record Redacao(String antes, String depois) {
    }

    public Redacao redigir(Map<String, ?> antes, Map<String, ?> depois) {
        return new Redacao(serializar(redigirLado(antes, null)), serializar(redigirLado(depois, antes)));
    }

    private static Map<String, Object> redigirLado(Map<String, ?> dados, Map<String, ?> referencia) {
        if (dados == null) {
            return null;
        }
        Map<String, Object> redigido = new LinkedHashMap<>();
        dados.forEach((campo, valor) -> {
            if (!CAMPOS_PESSOAIS.contains(campo)) {
                redigido.put(campo, valor);
            } else if (valor != null) {
                boolean mudou = referencia != null && !Objects.equals(referencia.get(campo), valor);
                redigido.put(campo, mudou ? ALTERADO : PRESENTE);
            }
        });
        return redigido;
    }

    private String serializar(Map<String, Object> dados) {
        if (dados == null) {
            return null;
        }
        try {
            return json.writeValueAsString(dados);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar auditoria", e);
        }
    }
}
