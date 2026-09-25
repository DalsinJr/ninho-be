package com.escolaamerica.ninho.iam.repository;

import com.escolaamerica.ninho.iam.domain.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

    @Query("select u from Usuario u left join fetch u.papeis where u.email = :email")
    Optional<Usuario> findComPapeisByEmail(@Param("email") String email);

    @Query("select u from Usuario u left join fetch u.papeis where u.id = :id")
    Optional<Usuario> findComPapeisById(@Param("id") UUID id);

    /** Segunda etapa da lista: carrega os papéis dos ids já cortados no teto, numa consulta só. */
    @Query("select distinct u from Usuario u left join fetch u.papeis where u.id in :ids")
    List<Usuario> findComPapeisByIdIn(@Param("ids") Collection<UUID> ids);

    boolean existsByEmail(String email);

    /**
     * Trava os administradores ativos até o fim da transação: a regra do último {@code RH_ADMIN}
     * (§7.1) só vale sob concorrência se contagem e escrita forem atômicas.
     */
    @Query(value = """
        SELECT u.id FROM iam_usuario u
          JOIN iam_usuario_papel p ON p.usuario_id = u.id
         WHERE p.papel = 'RH_ADMIN' AND u.ativo
         FOR UPDATE OF u
        """, nativeQuery = true)
    List<UUID> travarAdministradoresAtivos();
}
