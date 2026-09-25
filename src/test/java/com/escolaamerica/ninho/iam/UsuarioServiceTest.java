package com.escolaamerica.ninho.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolaamerica.ninho.iam.domain.Usuario;
import com.escolaamerica.ninho.iam.repository.UsuarioRepository;
import com.escolaamerica.ninho.iam.service.UsuarioService;
import com.escolaamerica.ninho.iam.service.UsuarioService.DadosUsuario;
import com.escolaamerica.ninho.shared.auditoria.AuditoriaService;
import com.escolaamerica.ninho.shared.domain.BusinessRuleViolationException;
import com.escolaamerica.ninho.shared.domain.ConflictException;
import com.escolaamerica.ninho.shared.domain.Papel;
import com.escolaamerica.ninho.shared.domain.UsuarioLogado;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepository repositorio;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuditoriaService auditoria;

    UsuarioService servico;

    Usuario admin;
    UsuarioLogado adminLogado;

    @BeforeEach
    void setUp() {
        servico = new UsuarioService(repositorio, passwordEncoder, auditoria);
        admin = usuario("Administrador", "admin@ninho.local", Papel.RH_ADMIN, Papel.DPO);
        adminLogado = new UsuarioLogado(admin.getId(), admin.getNome(), admin.getEmail(), admin.getPapeis());
    }

    @Test
    void naoRebaixaOUltimoAdministradorAtivo() {
        when(repositorio.findComPapeisById(admin.getId())).thenReturn(Optional.of(admin));
        when(repositorio.travarAdministradoresAtivos()).thenReturn(List.of(admin.getId()));

        assertThatThrownBy(() -> servico.atualizar(adminLogado, admin.getId(),
                new DadosUsuario("Administrador", "admin@ninho.local", Set.of(Papel.DPO), true)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Este é o último administrador ativo");
        verify(auditoria, never()).registrar(any(), anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    void naoDesativaOUltimoAdministradorAtivo() {
        Usuario outroAdmin = usuario("Bia", "bia@ninho.local", Papel.RH_ADMIN);
        UsuarioLogado bia = new UsuarioLogado(outroAdmin.getId(), "Bia", "bia@ninho.local", Set.of(Papel.RH_ADMIN));
        when(repositorio.findComPapeisById(admin.getId())).thenReturn(Optional.of(admin));
        when(repositorio.travarAdministradoresAtivos()).thenReturn(List.of(admin.getId()));

        assertThatThrownBy(() -> servico.atualizar(bia, admin.getId(),
                new DadosUsuario("Administrador", "admin@ninho.local", Set.of(Papel.RH_ADMIN), false)))
            .hasMessage("Este é o último administrador ativo");
    }

    @Test
    void comOutroAdministradorNaoRemoveOProprioAdmin() {
        when(repositorio.findComPapeisById(admin.getId())).thenReturn(Optional.of(admin));
        when(repositorio.travarAdministradoresAtivos()).thenReturn(List.of(admin.getId(), UUID.randomUUID()));

        assertThatThrownBy(() -> servico.atualizar(adminLogado, admin.getId(),
                new DadosUsuario("Administrador", "admin@ninho.local", Set.of(Papel.DPO), true)))
            .hasMessage("Você não pode remover seu próprio acesso de administrador");
    }

    @Test
    void naoDesativaOProprioUsuario() {
        Usuario recrutadora = usuario("Rita", "rita@ninho.local", Papel.RH_RECRUTADOR, Papel.RH_ADMIN);
        UsuarioLogado rita = new UsuarioLogado(recrutadora.getId(), "Rita", "rita@ninho.local", recrutadora.getPapeis());
        when(repositorio.findComPapeisById(recrutadora.getId())).thenReturn(Optional.of(recrutadora));
        when(repositorio.travarAdministradoresAtivos()).thenReturn(List.of(admin.getId(), recrutadora.getId()));

        assertThatThrownBy(() -> servico.atualizar(rita, recrutadora.getId(),
                new DadosUsuario("Rita", "rita@ninho.local", recrutadora.getPapeis(), false)))
            .hasMessage("Você não pode desativar o próprio usuário");
    }

    @Test
    void emailDuplicadoNaCriacaoEConflito() {
        when(repositorio.existsByEmail("rita@ninho.local")).thenReturn(true);

        assertThatThrownBy(() -> servico.criar(adminLogado, "Rita", " RITA@ninho.local", Set.of(Papel.RH_RECRUTADOR), "senha-forte"))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Já existe um usuário com este e-mail");
    }

    @Test
    void emailDuplicadoNaEdicaoEConflito() {
        Usuario rita = usuario("Rita", "rita@ninho.local", Papel.RH_RECRUTADOR);
        when(repositorio.findComPapeisById(rita.getId())).thenReturn(Optional.of(rita));
        when(repositorio.existsByEmail("admin@ninho.local")).thenReturn(true);

        assertThatThrownBy(() -> servico.atualizar(adminLogado, rita.getId(),
                new DadosUsuario("Rita", "Admin@Ninho.local", Set.of(Papel.RH_RECRUTADOR), true)))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void trocaDePapelEAuditada() {
        Usuario rita = usuario("Rita", "rita@ninho.local", Papel.RH_RECRUTADOR);
        when(repositorio.findComPapeisById(rita.getId())).thenReturn(Optional.of(rita));

        servico.atualizar(adminLogado, rita.getId(),
            new DadosUsuario("Rita", "rita@ninho.local", Set.of(Papel.RH_RECRUTADOR, Papel.GESTOR), true));

        assertThat(rita.getPapeis()).containsExactlyInAnyOrder(Papel.RH_RECRUTADOR, Papel.GESTOR);
        verify(auditoria).registrar(adminLogado.id(), "iam.usuario.papeis", "iam_usuario", rita.getId(),
            java.util.Map.of("papeis", List.of("RH_RECRUTADOR")),
            java.util.Map.of("papeis", List.of("RH_RECRUTADOR", "GESTOR")), null);
    }

    private static Usuario usuario(String nome, String email, Papel... papeis) {
        Usuario usuario = new Usuario(nome, email, "hash", Set.of(papeis));
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }
}
