package com.escolaamerica.ninho.iam.service;

import com.escolaamerica.ninho.iam.domain.Usuario;
import com.escolaamerica.ninho.iam.repository.UsuarioRepository;
import com.escolaamerica.ninho.shared.api.ListaLimitada;
import com.escolaamerica.ninho.shared.auditoria.AuditoriaService;
import com.escolaamerica.ninho.shared.domain.BusinessRuleViolationException;
import com.escolaamerica.ninho.shared.domain.ConflictException;
import com.escolaamerica.ninho.shared.domain.Papel;
import com.escolaamerica.ninho.shared.domain.ResourceNotFoundException;
import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import jakarta.persistence.criteria.Join;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de usuários e as regras da SPEC §7.1. Usuários nunca são apagados: são desativados. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    static final String EMAIL_DUPLICADO = "Já existe um usuário com este e-mail";
    static final String ULTIMO_ADMIN = "Este é o último administrador ativo";
    static final String PROPRIO_ADMIN = "Você não pode remover seu próprio acesso de administrador";
    static final String PROPRIA_DESATIVACAO = "Você não pode desativar o próprio usuário";

    private static final String ENTIDADE = "iam_usuario";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public record Filtro(String q, Papel papel, Boolean ativo) {
    }

    public record DadosUsuario(String nome, String email, Set<Papel> papeis, boolean ativo) {
    }

    @Transactional(readOnly = true)
    public ListaLimitada<Usuario> listar(Filtro filtro) {
        List<UUID> ids = usuarios.findBy(especificacao(filtro), consulta -> consulta
                .sortBy(Sort.by("nome").and(Sort.by("id")))
                .limit(ListaLimitada.LIMITE_CONSULTA)
                .all())
            .stream().map(Usuario::getId).toList();
        Map<UUID, Usuario> comPapeis = usuarios.findComPapeisByIdIn(ids).stream()
            .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        return ListaLimitada.de(ids.stream().map(comPapeis::get).toList());
    }

    @Transactional
    public Usuario criar(UsuarioLogado autor, String nome, String email, Set<Papel> papeis, String senhaInicial) {
        String emailNormalizado = Usuario.normalizarEmail(email);
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new ConflictException(EMAIL_DUPLICADO);
        }
        Usuario novo = usuarios.saveAndFlush(
            new Usuario(nome.trim(), emailNormalizado, passwordEncoder.encode(senhaInicial), papeis));
        auditoria.registrar(autor.id(), "iam.usuario.criar", ENTIDADE, novo.getId(), null,
            Map.of("nome", novo.getNome(), "email", novo.getEmail(), "papeis", ordenados(novo.getPapeis()), "ativo", true), null);
        return novo;
    }

    @Transactional
    public Usuario atualizar(UsuarioLogado autor, UUID id, DadosUsuario dados) {
        Usuario usuario = exigir(id);
        Set<Papel> papeisAntes = usuario.papeisComoEnumSet();
        boolean ativoAntes = usuario.isAtivo();
        String nomeAntes = usuario.getNome();
        String emailAntes = usuario.getEmail();
        String emailNovo = Usuario.normalizarEmail(dados.email());

        boolean perdeAdmin = ativoAntes && papeisAntes.contains(Papel.RH_ADMIN)
            && (!dados.ativo() || !dados.papeis().contains(Papel.RH_ADMIN));
        if (perdeAdmin && usuarios.travarAdministradoresAtivos().size() <= 1) {
            throw new BusinessRuleViolationException(ULTIMO_ADMIN);
        }
        if (usuario.getId().equals(autor.id())) {
            if (papeisAntes.contains(Papel.RH_ADMIN) && !dados.papeis().contains(Papel.RH_ADMIN)) {
                throw new BusinessRuleViolationException(PROPRIO_ADMIN);
            }
            if (!dados.ativo()) {
                throw new BusinessRuleViolationException(PROPRIA_DESATIVACAO);
            }
        }
        if (!emailNovo.equals(emailAntes) && usuarios.existsByEmail(emailNovo)) {
            throw new ConflictException(EMAIL_DUPLICADO);
        }

        usuario.setNome(dados.nome().trim());
        usuario.setEmail(emailNovo);
        usuario.definirPapeis(dados.papeis());
        usuario.setAtivo(dados.ativo());
        usuarios.flush();

        if (!papeisAntes.equals(dados.papeis())) {
            auditoria.registrar(autor.id(), "iam.usuario.papeis", ENTIDADE, id,
                Map.of("papeis", ordenados(papeisAntes)), Map.of("papeis", ordenados(dados.papeis())), null);
        }
        if (ativoAntes != dados.ativo()) {
            auditoria.registrar(autor.id(), "iam.usuario.ativo", ENTIDADE, id,
                Map.of("ativo", ativoAntes), Map.of("ativo", dados.ativo()), null);
        }
        if (!nomeAntes.equals(usuario.getNome()) || !emailAntes.equals(emailNovo)) {
            auditoria.registrar(autor.id(), "iam.usuario.dados", ENTIDADE, id,
                Map.of("nome", nomeAntes, "email", emailAntes), Map.of("nome", usuario.getNome(), "email", emailNovo), null);
        }
        return usuario;
    }

    @Transactional
    public void definirSenha(UsuarioLogado autor, UUID id, String novaSenha) {
        Usuario usuario = exigir(id);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        auditoria.registrar(autor.id(), "iam.usuario.senha", ENTIDADE, id, null, null, null);
    }

    private Usuario exigir(UUID id) {
        return usuarios.findComPapeisById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private static List<String> ordenados(Set<Papel> papeis) {
        return papeis.stream().sorted(Comparator.naturalOrder()).map(Enum::name).toList();
    }

    private static Specification<Usuario> especificacao(Filtro filtro) {
        Specification<Usuario> spec = (root, query, cb) -> cb.conjunction();
        if (filtro.q() != null && !filtro.q().isBlank()) {
            String termo = "%" + filtro.q().trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("nome")), termo), cb.like(root.get("email"), termo)));
        }
        if (filtro.papel() != null) {
            spec = spec.and((root, query, cb) -> {
                Join<Usuario, Papel> papel = root.join("papeis");
                return cb.equal(papel, filtro.papel());
            });
        }
        if (filtro.ativo() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ativo"), filtro.ativo()));
        }
        return spec;
    }
}
