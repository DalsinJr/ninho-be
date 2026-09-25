package com.escolaamerica.ninho.iam.repository;

import com.escolaamerica.ninho.iam.domain.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Query("select u from Usuario u left join fetch u.papeis where u.email = :email")
    Optional<Usuario> findComPapeisByEmail(@Param("email") String email);

    @Query("select u from Usuario u left join fetch u.papeis where u.id = :id")
    Optional<Usuario> findComPapeisById(@Param("id") UUID id);

    boolean existsByEmail(String email);
}
