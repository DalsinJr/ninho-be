package com.escolaamerica.ninho.iam.api;

import com.escolaamerica.ninho.iam.api.AuthDtos.LoginRequest;
import com.escolaamerica.ninho.iam.api.AuthDtos.TrocaSenhaRequest;
import com.escolaamerica.ninho.iam.api.AuthDtos.UsuarioLogadoDto;
import com.escolaamerica.ninho.iam.service.AutenticacaoService;
import com.escolaamerica.ninho.shared.domain.AutorizacaoService;
import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AutenticacaoService autenticacao;
    private final AutorizacaoService autorizacao;

    @PostMapping("/login")
    UsuarioLogadoDto entrar(@RequestBody LoginRequest login, HttpServletRequest request, HttpServletResponse response) {
        return UsuarioLogadoDto.de(autenticacao.entrar(login.email(), login.senha(), request, response));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sair(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        autorizacao.exigirLogado(authentication);
        autenticacao.sair(request, response, authentication);
    }

    @GetMapping("/me")
    UsuarioLogadoDto eu(Authentication authentication) {
        return UsuarioLogadoDto.de(autorizacao.exigirLogado(authentication));
    }

    @PutMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void trocarSenha(@Valid @RequestBody TrocaSenhaRequest troca, Authentication authentication) {
        UsuarioLogado usuario = autorizacao.exigirLogado(authentication);
        autenticacao.trocarSenha(usuario.id(), troca.senhaAtual(), troca.novaSenha());
    }
}
