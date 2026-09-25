package com.escolaamerica.ninho.shared.domain;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/** Principal da sessão (SPEC §7.1): quem decide, com os papéis vigentes. Nunca vai para log. */
public record UsuarioLogado(UUID id, String nome, String email, Set<Papel> papeis) implements Serializable {

    public UsuarioLogado {
        papeis = Set.copyOf(papeis);
    }

    public boolean temAlgum(Papel... exigidos) {
        for (Papel papel : exigidos) {
            if (papeis.contains(papel)) {
                return true;
            }
        }
        return false;
    }
}
