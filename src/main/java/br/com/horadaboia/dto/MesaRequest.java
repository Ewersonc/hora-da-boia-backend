package br.com.horadaboia.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MesaRequest(
        @NotBlank(message = "Informe a identificação da mesa.") @Size(max = 30, message = "A identificação deve ter até 30 caracteres.") String identificacao,
        @Min(value = 1, message = "A capacidade deve ser maior que zero.") int capacidade,
        Boolean ativa) {
}
