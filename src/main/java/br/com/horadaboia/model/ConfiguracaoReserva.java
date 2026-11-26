package br.com.horadaboia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Parâmetros das regras de reserva. A tabela tem uma única linha, com id igual a 1. */
@Entity
@Table(name = "configuracao_reserva")
public class ConfiguracaoReserva {

    @Id
    private Short id;

    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    @Column(name = "antecedencia_min_minutos", nullable = false)
    private int antecedenciaMinMinutos;

    @Column(name = "antecedencia_max_dias", nullable = false)
    private int antecedenciaMaxDias;

    @Column(name = "prazo_cancelamento_minutos", nullable = false)
    private int prazoCancelamentoMinutos;

    @Column(name = "tolerancia_atraso_minutos", nullable = false)
    private int toleranciaAtrasoMinutos;

    public Short getId() { return id; }
    public int getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(int valor) { this.duracaoMinutos = valor; }
    public int getAntecedenciaMinMinutos() { return antecedenciaMinMinutos; }
    public void setAntecedenciaMinMinutos(int valor) { this.antecedenciaMinMinutos = valor; }
    public int getAntecedenciaMaxDias() { return antecedenciaMaxDias; }
    public void setAntecedenciaMaxDias(int valor) { this.antecedenciaMaxDias = valor; }
    public int getPrazoCancelamentoMinutos() { return prazoCancelamentoMinutos; }
    public void setPrazoCancelamentoMinutos(int valor) { this.prazoCancelamentoMinutos = valor; }
    public int getToleranciaAtrasoMinutos() { return toleranciaAtrasoMinutos; }
    public void setToleranciaAtrasoMinutos(int valor) { this.toleranciaAtrasoMinutos = valor; }
}
