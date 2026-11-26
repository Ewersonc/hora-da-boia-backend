package br.com.horadaboia.dto;

import br.com.horadaboia.model.ConfiguracaoReserva;

import jakarta.validation.constraints.Min;

/** Parâmetros das regras de reserva, usados na leitura e na atualização (RF07). */
public record ParametrosReserva(
        @Min(value = 1, message = "A duração deve ser maior que zero.") int duracaoMinutos,
        @Min(value = 0, message = "A antecedência mínima não pode ser negativa.") int antecedenciaMinMinutos,
        @Min(value = 1, message = "A antecedência máxima deve ser de pelo menos 1 dia.") int antecedenciaMaxDias,
        @Min(value = 0, message = "O prazo de cancelamento não pode ser negativo.") int prazoCancelamentoMinutos,
        @Min(value = 0, message = "A tolerância de atraso não pode ser negativa.") int toleranciaAtrasoMinutos) {

    public static ParametrosReserva de(ConfiguracaoReserva c) {
        return new ParametrosReserva(c.getDuracaoMinutos(), c.getAntecedenciaMinMinutos(), c.getAntecedenciaMaxDias(),
                c.getPrazoCancelamentoMinutos(), c.getToleranciaAtrasoMinutos());
    }
}
