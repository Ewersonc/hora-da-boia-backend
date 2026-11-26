package br.com.horadaboia.model;

/**
 * Ciclo de vida da reserva (RN09): nasce CONFIRMADA e termina em um dos três estados finais.
 */
public enum StatusReserva {
    CONFIRMADA,
    COMPARECEU,
    NAO_COMPARECEU,
    CANCELADA;

    public boolean isFinal() {
        return this != CONFIRMADA;
    }
}
