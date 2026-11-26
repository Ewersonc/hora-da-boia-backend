package br.com.horadaboia.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReservaRequest(
        @NotNull(message = "Informe a data.") LocalDate data,
        @NotNull(message = "Informe o horário.") LocalTime hora,
        @Min(value = 1, message = "Informe o número de pessoas.") int pessoas) {
}
