package br.com.horadaboia.model;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Horário de funcionamento de um dia da semana (1 é segunda-feira e 7 é domingo). */
@Entity
@Table(name = "horario_funcionamento")
public class HorarioFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dia_semana", nullable = false)
    private short diaSemana;

    @Column(nullable = false)
    private boolean aberto = true;

    private LocalTime abertura;

    private LocalTime fechamento;

    public Long getId() { return id; }
    public short getDiaSemana() { return diaSemana; }
    public void setDiaSemana(short diaSemana) { this.diaSemana = diaSemana; }
    public boolean isAberto() { return aberto; }
    public void setAberto(boolean aberto) { this.aberto = aberto; }
    public LocalTime getAbertura() { return abertura; }
    public void setAbertura(LocalTime abertura) { this.abertura = abertura; }
    public LocalTime getFechamento() { return fechamento; }
    public void setFechamento(LocalTime fechamento) { this.fechamento = fechamento; }
}
