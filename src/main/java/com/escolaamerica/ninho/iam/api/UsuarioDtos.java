package com.escolaamerica.ninho.iam.api;

import com.escolaamerica.ninho.iam.domain.Usuario;
import com.escolaamerica.ninho.shared.domain.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record UsuarioResumoDto(UUID id, String nome, String email, List<Papel> papeis, boolean ativo, Instant ultimoAcesso) {

        public static UsuarioResumoDto de(Usuario usuario) {
            return new UsuarioResumoDto(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPapeis().stream().sorted().toList(), usuario.isAtivo(), usuario.getUltimoAcesso());
        }
    }

    public record CriarUsuarioRequest(
        @NotBlank(message = "Informe o nome.") String nome,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.") String email,
        @NotEmpty(message = "Selecione ao menos um papel.") Set<Papel> papeis,
        @NotBlank(message = "Informe a senha inicial.")
        @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres.") String senhaInicial
    ) {
    }

    public record AtualizarUsuarioRequest(
        @NotBlank(message = "Informe o nome.") String nome,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.") String email,
        @NotEmpty(message = "Selecione ao menos um papel.") Set<Papel> papeis,
        @NotNull(message = "Informe se o usuário está ativo.") Boolean ativo
    ) {
    }

    public record DefinirSenhaRequest(
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres.") String novaSenha
    ) {
    }
}
