package br.com.horadaboia.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservaBalcaoRequest(
        @NotBlank(message = "Informe o nome do cliente.") @Size(max = 100) String nome,
        @NotBlank(message = "Informe o telefone do cliente.") @Size(max = 20) String telefone,
        @NotNull(message = "Informe a data.") LocalDate data,
        @NotNull(message = "Informe o horário.") LocalTime hora,
        @Min(value = 1, message = "Informe o número de pessoas.") int pessoas) {
}
