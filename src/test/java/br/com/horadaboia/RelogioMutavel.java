package br.com.horadaboia;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Relógio de teste cuja hora pode ser alterada, para testar prazos e tolerâncias sem depender da hora real. */
public class RelogioMutavel extends Clock {

    private final ZoneId zona;
    private volatile Instant instante;

    public RelogioMutavel(Instant instanteInicial, ZoneId zona) {
        this.instante = instanteInicial;
        this.zona = zona;
    }

    public void definir(Instant novoInstante) {
        this.instante = novoInstante;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId novaZona) {
        return new RelogioMutavel(instante, novaZona);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
