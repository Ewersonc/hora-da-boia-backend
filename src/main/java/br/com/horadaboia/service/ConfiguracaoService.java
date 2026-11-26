package br.com.horadaboia.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import br.com.horadaboia.dto.HorarioRequest;
import br.com.horadaboia.dto.HorarioResponse;
import br.com.horadaboia.dto.ParametrosReserva;
import br.com.horadaboia.exception.RegraNegocioException;
import br.com.horadaboia.model.ConfiguracaoReserva;
import br.com.horadaboia.model.HorarioFuncionamento;
import br.com.horadaboia.repository.ConfiguracaoReservaRepository;
import br.com.horadaboia.repository.HorarioFuncionamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Horário de funcionamento (RF06) e parâmetros de reserva (RF07). */
@Service
public class ConfiguracaoService {

    private static final Short ID_CONFIGURACAO = (short) 1;

    private final ConfiguracaoReservaRepository configuracaoRepository;
    private final HorarioFuncionamentoRepository horarioRepository;

    public ConfiguracaoService(ConfiguracaoReservaRepository configuracaoRepository,
                               HorarioFuncionamentoRepository horarioRepository) {
        this.configuracaoRepository = configuracaoRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional(readOnly = true)
    public ConfiguracaoReserva configuracao() {
        return configuracaoRepository.findById(ID_CONFIGURACAO)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuração de reserva não encontrada. Verifique se as migrações do banco foram aplicadas."));
    }

    @Transactional(readOnly = true)
    public Optional<HorarioFuncionamento> horarioDoDia(int diaSemana) {
        return horarioRepository.findByDiaSemana((short) diaSemana);
    }

    @Transactional(readOnly = true)
    public ParametrosReserva parametros() {
        return ParametrosReserva.de(configuracao());
    }

    @Transactional
    public ParametrosReserva atualizarParametros(ParametrosReserva p) {
        ConfiguracaoReserva c = configuracao();
        c.setDuracaoMinutos(p.duracaoMinutos());
        c.setAntecedenciaMinMinutos(p.antecedenciaMinMinutos());
        c.setAntecedenciaMaxDias(p.antecedenciaMaxDias());
        c.setPrazoCancelamentoMinutos(p.prazoCancelamentoMinutos());
        c.setToleranciaAtrasoMinutos(p.toleranciaAtrasoMinutos());
        return ParametrosReserva.de(c);
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> horarios() {
        return horarioRepository.findAllByOrderByDiaSemanaAsc().stream().map(HorarioResponse::de).toList();
    }

    @Transactional
    public List<HorarioResponse> atualizarHorarios(List<HorarioRequest> pedidos) {
        validar(pedidos);
        for (HorarioRequest p : pedidos) {
            HorarioFuncionamento h = horarioRepository.findByDiaSemana((short) p.diaSemana())
                    .orElseGet(HorarioFuncionamento::new);
            h.setDiaSemana((short) p.diaSemana());
            h.setAberto(p.aberto());
            h.setAbertura(p.aberto() ? p.abertura() : null);
            h.setFechamento(p.aberto() ? p.fechamento() : null);
            horarioRepository.save(h);
        }
        return horarios();
    }

    private void validar(List<HorarioRequest> pedidos) {
        if (pedidos == null || pedidos.size() != 7) {
            throw new RegraNegocioException("Informe o horário dos 7 dias da semana.");
        }
        Set<Integer> dias = new HashSet<>();
        for (HorarioRequest p : pedidos) {
            if (p.diaSemana() < 1 || p.diaSemana() > 7 || !dias.add(p.diaSemana())) {
                throw new RegraNegocioException("Os dias da semana devem ser de 1 a 7, sem repetição.");
            }
            if (p.aberto()) {
                if (p.abertura() == null || p.fechamento() == null) {
                    throw new RegraNegocioException("Informe a abertura e o fechamento dos dias abertos.");
                }
                if (!p.abertura().isBefore(p.fechamento())) {
                    throw new RegraNegocioException("O fechamento deve ser depois da abertura.");
                }
            }
        }
    }
}
