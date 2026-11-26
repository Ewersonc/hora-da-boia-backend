package br.com.horadaboia.repository;

import java.util.List;

import br.com.horadaboia.model.Mesa;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    boolean existsByIdentificacao(String identificacao);

    boolean existsByIdentificacaoAndIdNot(String identificacao, Long id);

    List<Mesa> findAllByOrderByIdentificacaoAsc();

    List<Mesa> findByAtivaTrueAndCapacidadeGreaterThanEqualOrderByCapacidadeAscIdAsc(int capacidade);

    @Query("select max(m.capacidade) from Mesa m where m.ativa = true")
    Integer maiorCapacidade();

    /**
     * Bloqueia (SELECT ... FOR UPDATE) as mesas ativas que comportam o grupo, da menor para a maior.
     * Deve ser chamado dentro de uma transação. A ordem fixa evita deadlock entre reservas simultâneas (RNF06).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.ativa = true and m.capacidade >= :pessoas order by m.capacidade asc, m.id asc")
    List<Mesa> bloquearCandidatas(@Param("pessoas") int pessoas);
}
