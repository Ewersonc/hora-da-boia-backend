package br.com.horadaboia.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadeResponse(LocalDate data, int pessoas, int duracaoMinutos, List<LocalTime> horarios) {
}
