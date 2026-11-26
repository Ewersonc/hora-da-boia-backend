package br.com.horadaboia.dto;

import br.com.horadaboia.model.StatusReserva;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "Informe o status.") StatusReserva status) {
}
