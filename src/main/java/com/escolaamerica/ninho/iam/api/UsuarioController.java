package com.escolaamerica.ninho.iam.api;

import com.escolaamerica.ninho.iam.api.UsuarioDtos.AtualizarUsuarioRequest;
import com.escolaamerica.ninho.iam.api.UsuarioDtos.CriarUsuarioRequest;
import com.escolaamerica.ninho.iam.api.UsuarioDtos.DefinirSenhaRequest;
import com.escolaamerica.ninho.iam.api.UsuarioDtos.UsuarioResumoDto;
import com.escolaamerica.ninho.iam.domain.Usuario;
import com.escolaamerica.ninho.iam.service.UsuarioService;
import com.escolaamerica.ninho.shared.api.ListaLimitada;
import com.escolaamerica.ninho.shared.domain.AutorizacaoService;
import com.escolaamerica.ninho.shared.domain.Papel;
import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro de usuários (SPEC §5.3, §7.1). Todas as rotas exigem {@code RH_ADMIN}. */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarios;
    private final AutorizacaoService autorizacao;

    @GetMapping
    ListaLimitada<UsuarioResumoDto> listar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Papel papel,
                                           @RequestParam(required = false) Boolean ativo,
                                           Authentication authentication) {
        autorizacao.exigirPapel(authentication, Papel.RH_ADMIN);
        ListaLimitada<Usuario> lista = usuarios.listar(new UsuarioService.Filtro(q, papel, ativo));
        return new ListaLimitada<>(lista.itens().stream().map(UsuarioResumoDto::de).toList(), lista.truncado());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioResumoDto criar(@Valid @RequestBody CriarUsuarioRequest pedido, Authentication authentication) {
        UsuarioLogado autor = autorizacao.exigirPapel(authentication, Papel.RH_ADMIN);
        return UsuarioResumoDto.de(usuarios.criar(autor, pedido.nome(), pedido.email(), pedido.papeis(), pedido.senhaInicial()));
    }

    @PutMapping("/{id}")
    UsuarioResumoDto atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarUsuarioRequest pedido,
                               Authentication authentication) {
        UsuarioLogado autor = autorizacao.exigirPapel(authentication, Papel.RH_ADMIN);
        return UsuarioResumoDto.de(usuarios.atualizar(autor, id,
            new UsuarioService.DadosUsuario(pedido.nome(), pedido.email(), pedido.papeis(), pedido.ativo())));
    }

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void definirSenha(@PathVariable UUID id, @Valid @RequestBody DefinirSenhaRequest pedido, Authentication authentication) {
        UsuarioLogado autor = autorizacao.exigirPapel(authentication, Papel.RH_ADMIN);
        usuarios.definirSenha(autor, id, pedido.novaSenha());
    }
}
