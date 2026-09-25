package com.escolaamerica.ninho.iam.domain;

import com.escolaamerica.ninho.shared.domain.EntidadeAuditavel;
import com.escolaamerica.ninho.shared.domain.Papel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuário do backoffice (SPEC §4.4). Nunca é apagado: é desativado (§7.1). */
@Getter
@Setter
@Entity
@Table(name = "iam_usuario")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultimo_acesso")
    private Instant ultimoAcesso;

    /** LAZY: listas e login usam {@code JOIN FETCH} explícito (SPEC §11.2). */
    @Setter(AccessLevel.NONE)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "iam_usuario_papel", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false)
    private Set<Papel> papeis = new HashSet<>();

    public Usuario(String nome, String email, String senhaHash, Set<Papel> papeis) {
        this.nome = nome;
        setEmail(email);
        this.senhaHash = senhaHash;
        definirPapeis(papeis);
    }

    /** E-mail sempre em minúsculas e sem espaços nas pontas (SPEC §4.2). */
    public void setEmail(String email) {
        this.email = normalizarEmail(email);
    }

    public void definirPapeis(Set<Papel> novos) {
        papeis.clear();
        papeis.addAll(novos);
    }

    public Set<Papel> papeisComoEnumSet() {
        return papeis.isEmpty() ? EnumSet.noneOf(Papel.class) : EnumSet.copyOf(papeis);
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
