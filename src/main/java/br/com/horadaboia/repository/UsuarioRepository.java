package br.com.horadaboia.repository;

import java.util.List;
import java.util.Optional;

import br.com.horadaboia.model.Perfil;
import br.com.horadaboia.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    List<Usuario> findByPerfilOrderByNomeAsc(Perfil perfil);
}
