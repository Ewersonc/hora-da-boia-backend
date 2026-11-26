package br.com.horadaboia.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record HorarioRequest(
        @Min(value = 1, message = "Dia da semana inválido.") @Max(value = 7, message = "Dia da semana inválido.") int diaSemana,
        boolean aberto,
        LocalTime abertura,
        LocalTime fechamento) {
}
