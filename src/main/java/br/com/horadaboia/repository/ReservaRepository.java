package br.com.horadaboia.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import br.com.horadaboia.model.Reserva;
import br.com.horadaboia.model.StatusReserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Valor passado em ignorarId quando nenhuma reserva deve ser ignorada. */
    long SEM_RESERVA = -1L;

    boolean existsByCodigo(String codigo);

    boolean existsByMesaIdAndStatusAndFimAfter(Long mesaId, StatusReserva status, LocalDateTime momento);

    @Query("""
            select count(r) from Reserva r
            where r.mesa.id = :mesaId
              and r.status = br.com.horadaboia.model.StatusReserva.CONFIRMADA
              and r.inicio < :fim and r.fim > :inicio
              and r.id <> :ignorarId
            """)
    long contarConflitosNaMesa(@Param("mesaId") Long mesaId, @Param("inicio") LocalDateTime inicio,
                               @Param("fim") LocalDateTime fim, @Param("ignorarId") long ignorarId);

    @Query("""
            select count(r) from Reserva r
            where r.usuario.id = :usuarioId
              and r.status = br.com.horadaboia.model.StatusReserva.CONFIRMADA
              and r.inicio < :fim and r.fim > :inicio
              and r.id <> :ignorarId
            """)
    long contarConflitosDoCliente(@Param("usuarioId") Long usuarioId, @Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim, @Param("ignorarId") long ignorarId);

    default boolean existeConflitoNaMesa(Long mesaId, LocalDateTime inicio, LocalDateTime fim, long ignorarId) {
        return contarConflitosNaMesa(mesaId, inicio, fim, ignorarId) > 0;
    }

    default boolean existeConflitoDoCliente(Long usuarioId, LocalDateTime inicio, LocalDateTime fim, long ignorarId) {
        return contarConflitosDoCliente(usuarioId, inicio, fim, ignorarId) > 0;
    }

    @Query("""
            select r from Reserva r join fetch r.mesa
            where r.status = br.com.horadaboia.model.StatusReserva.CONFIRMADA
              and r.mesa.id in :mesaIds
              and r.inicio < :fim and r.fim > :inicio
            """)
    List<Reserva> confirmadasNasMesas(@Param("mesaIds") Collection<Long> mesaIds,
                                      @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("""
            select r from Reserva r join fetch r.mesa join fetch r.usuario
            where r.usuario.id = :usuarioId
            order by r.inicio desc
            """)
    List<Reserva> minhasReservas(@Param("usuarioId") Long usuarioId);

    @Query("""
            select r from Reserva r join fetch r.mesa left join fetch r.usuario
            where r.inicio >= :de and r.inicio < :ate
            order by r.inicio asc, r.id asc
            """)
    List<Reserva> noPeriodo(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);

    @Query("""
            select r from Reserva r join fetch r.mesa left join fetch r.usuario
            where r.inicio >= :de and r.inicio < :ate and r.status = :status
            order by r.inicio asc, r.id asc
            """)
    List<Reserva> noPeriodoComStatus(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate,
                                     @Param("status") StatusReserva status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Reserva r
            set r.status = br.com.horadaboia.model.StatusReserva.NAO_COMPARECEU, r.atualizadoEm = :agora
            where r.status = br.com.horadaboia.model.StatusReserva.CONFIRMADA and r.inicio < :limite
            """)
    int marcarNaoComparecimento(@Param("limite") LocalDateTime limite, @Param("agora") LocalDateTime agora);
}
