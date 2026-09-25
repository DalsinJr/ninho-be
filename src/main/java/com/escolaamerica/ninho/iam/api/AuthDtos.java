package com.escolaamerica.ninho.iam.api;

import com.escolaamerica.ninho.shared.domain.Papel;
import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** Sem Bean Validation: qualquer falha de login responde o mesmo 401 (§7.1). */
    public record LoginRequest(String email, String senha) {
    }

    public record UsuarioLogadoDto(UUID id, String nome, String email, List<Papel> papeis) {

        static UsuarioLogadoDto de(UsuarioLogado usuario) {
            return new UsuarioLogadoDto(usuario.id(), usuario.nome(), usuario.email(),
                usuario.papeis().stream().sorted().toList());
        }
    }

    public record TrocaSenhaRequest(
        @NotBlank(message = "Informe a senha atual.") String senhaAtual,
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, message = "A nova senha precisa ter ao menos 8 caracteres.") String novaSenha
    ) {
    }
}
