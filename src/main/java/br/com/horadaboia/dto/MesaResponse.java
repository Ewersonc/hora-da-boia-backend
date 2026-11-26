package br.com.horadaboia.dto;

import br.com.horadaboia.model.Mesa;

public record MesaResponse(Long id, String identificacao, int capacidade, boolean ativa) {

    public static MesaResponse de(Mesa m) {
        return new MesaResponse(m.getId(), m.getIdentificacao(), m.getCapacidade(), m.isAtiva());
    }
}
