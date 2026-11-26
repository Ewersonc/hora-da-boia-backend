package br.com.horadaboia.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import br.com.horadaboia.dto.MesaRequest;
import br.com.horadaboia.exception.RecursoNaoEncontradoException;
import br.com.horadaboia.exception.RegraNegocioException;
import br.com.horadaboia.model.Mesa;
import br.com.horadaboia.model.StatusReserva;
import br.com.horadaboia.repository.MesaRepository;
import br.com.horadaboia.repository.ReservaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro, edição e desativação de mesas (RF05, RN11). */
@Service
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;

    public MesaService(MesaRepository mesaRepository, ReservaRepository reservaRepository, Clock clock) {
        this.mesaRepository = mesaRepository;
        this.reservaRepository = reservaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Mesa> listar() {
        return mesaRepository.findAllByOrderByIdentificacaoAsc();
    }

    @Transactional
    public Mesa criar(MesaRequest r) {
        String identificacao = r.identificacao().trim();
        if (mesaRepository.existsByIdentificacao(identificacao)) {
            throw new RegraNegocioException("Já existe uma mesa com essa identificação.");
        }
        Mesa mesa = new Mesa();
        mesa.setIdentificacao(identificacao);
        mesa.setCapacidade(r.capacidade());
        mesa.setAtiva(r.ativa() == null || r.ativa());
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa atualizar(Long id, MesaRequest r) {
        Mesa mesa = buscar(id);
        String identificacao = r.identificacao().trim();
        if (mesaRepository.existsByIdentificacaoAndIdNot(identificacao, id)) {
            throw new RegraNegocioException("Já existe uma mesa com essa identificação.");
        }
        boolean desativando = mesa.isAtiva() && r.ativa() != null && !r.ativa();
        if (desativando) {
            exigirSemReservasFuturas(mesa);
        }
        mesa.setIdentificacao(identificacao);
        mesa.setCapacidade(r.capacidade());
        if (r.ativa() != null) {
            mesa.setAtiva(r.ativa());
        }
        return mesa;
    }

    @Transactional
    public Mesa desativar(Long id) {
        Mesa mesa = buscar(id);
        if (mesa.isAtiva()) {
            exigirSemReservasFuturas(mesa);
            mesa.setAtiva(false);
        }
        return mesa;
    }

    private void exigirSemReservasFuturas(Mesa mesa) {
        boolean temReservas = reservaRepository.existsByMesaIdAndStatusAndFimAfter(
                mesa.getId(), StatusReserva.CONFIRMADA, LocalDateTime.now(clock));
        if (temReservas) {
            throw new RegraNegocioException("A mesa tem reservas futuras confirmadas e não pode ser desativada.");
        }
    }

    private Mesa buscar(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mesa não encontrada."));
    }
}
