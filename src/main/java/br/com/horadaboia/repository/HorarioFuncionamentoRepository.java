package br.com.horadaboia.repository;

import java.util.List;
import java.util.Optional;

import br.com.horadaboia.model.HorarioFuncionamento;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Long> {

    Optional<HorarioFuncionamento> findByDiaSemana(short diaSemana);

    List<HorarioFuncionamento> findAllByOrderByDiaSemanaAsc();
}
