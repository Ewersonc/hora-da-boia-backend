package br.com.horadaboia.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.horadaboia.dto.DisponibilidadeResponse;
import br.com.horadaboia.exception.RegraNegocioException;
import br.com.horadaboia.model.ConfiguracaoReserva;
import br.com.horadaboia.model.HorarioFuncionamento;
import br.com.horadaboia.model.Mesa;
import br.com.horadaboia.model.Reserva;
import br.com.horadaboia.repository.MesaRepository;
import br.com.horadaboia.repository.ReservaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cálculo dos horários livres para uma data e um número de pessoas (RF08, RN02 a RN05). */
@Service
public class DisponibilidadeService {

    /** Intervalo, em minutos, entre os horários de início oferecidos. */
    public static final int PASSO_MINUTOS = 30;

    private final ConfiguracaoService configuracaoService;
    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;

    public DisponibilidadeService(ConfiguracaoService configuracaoService, MesaRepository mesaRepository,
                                  ReservaRepository reservaRepository, Clock clock) {
        this.configuracaoService = configuracaoService;
        this.mesaRepository = mesaRepository;
        this.reservaRepository = reservaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DisponibilidadeResponse consultar(LocalDate data, int pessoas) {
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDate hoje = agora.toLocalDate();

        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(cfg.getAntecedenciaMaxDias()))) {
            throw new RegraNegocioException("A data está fora da janela de reservas, que vai de hoje até "
                    + cfg.getAntecedenciaMaxDias() + " dias à frente.");
        }
        Integer maiorCapacidade = mesaRepository.maiorCapacidade();
        if (maiorCapacidade == null || pessoas > maiorCapacidade) {
            throw new RegraNegocioException("O restaurante não comporta grupos de " + pessoas + " pessoas.");
        }

        Optional<HorarioFuncionamento> horario = configuracaoService.horarioDoDia(data.getDayOfWeek().getValue());
        if (horario.isEmpty() || !horario.get().isAberto()) {
            return new DisponibilidadeResponse(data, pessoas, cfg.getDuracaoMinutos(), List.of());
        }

        List<Mesa> mesas = mesaRepository
                .findByAtivaTrueAndCapacidadeGreaterThanEqualOrderByCapacidadeAscIdAsc(pessoas);
        List<Long> idsMesas = mesas.stream().map(Mesa::getId).toList();
        List<Reserva> ocupadas = idsMesas.isEmpty()
                ? List.of()
                : reservaRepository.confirmadasNasMesas(idsMesas, data.atStartOfDay(), data.plusDays(1).atStartOfDay());

        LocalDateTime primeiroPermitido = agora.plusMinutes(cfg.getAntecedenciaMinMinutos());
        int aberturaMin = horario.get().getAbertura().toSecondOfDay() / 60;
        int fechamentoMin = horario.get().getFechamento().toSecondOfDay() / 60;

        List<LocalTime> livres = new ArrayList<>();
        for (int minuto = aberturaMin; minuto + cfg.getDuracaoMinutos() <= fechamentoMin; minuto += PASSO_MINUTOS) {
            LocalDateTime inicio = data.atTime(minuto / 60, minuto % 60);
            LocalDateTime fim = inicio.plusMinutes(cfg.getDuracaoMinutos());
            if (inicio.isBefore(primeiroPermitido)) {
                continue;
            }
            boolean existeMesaLivre = mesas.stream().anyMatch(mesa -> ocupadas.stream().noneMatch(r ->
                    r.getMesa().getId().equals(mesa.getId()) && r.getInicio().isBefore(fim) && r.getFim().isAfter(inicio)));
            if (existeMesaLivre) {
                livres.add(inicio.toLocalTime());
            }
        }
        return new DisponibilidadeResponse(data, pessoas, cfg.getDuracaoMinutos(), livres);
    }
}
