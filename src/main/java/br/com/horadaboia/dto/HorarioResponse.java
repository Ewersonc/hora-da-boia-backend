package br.com.horadaboia.dto;

import java.time.LocalTime;

import br.com.horadaboia.model.HorarioFuncionamento;

public record HorarioResponse(int diaSemana, boolean aberto, LocalTime abertura, LocalTime fechamento) {

    public static HorarioResponse de(HorarioFuncionamento h) {
        return new HorarioResponse(h.getDiaSemana(), h.isAberto(), h.getAbertura(), h.getFechamento());
    }
}
