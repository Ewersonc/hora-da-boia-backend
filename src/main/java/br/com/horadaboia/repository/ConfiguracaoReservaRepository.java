package br.com.horadaboia.repository;

import br.com.horadaboia.model.ConfiguracaoReserva;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoReservaRepository extends JpaRepository<ConfiguracaoReserva, Short> {
}
