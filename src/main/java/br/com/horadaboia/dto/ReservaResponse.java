package br.com.horadaboia.dto;

import java.time.LocalDateTime;

import br.com.horadaboia.model.StatusReserva;

/**
 * Reserva devolvida pela API. Os campos podeAlterar e podeCancelar indicam o que o cliente
 * pode fazer agora, de acordo com o prazo configurado (RN06).
 */
public record ReservaResponse(
        Long id,
        String codigo,
        LocalDateTime inicio,
        LocalDateTime fim,
        int numeroPessoas,
        StatusReserva status,
        String mesa,
        String clienteNome,
        String clienteTelefone,
        boolean podeAlterar,
        boolean podeCancelar) {
}
