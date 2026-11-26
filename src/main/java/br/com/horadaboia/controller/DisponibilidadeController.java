package br.com.horadaboia.controller;

import java.time.LocalDate;

import br.com.horadaboia.dto.DisponibilidadeResponse;
import br.com.horadaboia.exception.RequisicaoInvalidaException;
import br.com.horadaboia.service.DisponibilidadeService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/disponibilidade")
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidadeService;

    public DisponibilidadeController(DisponibilidadeService disponibilidadeService) {
        this.disponibilidadeService = disponibilidadeService;
    }

    @GetMapping
    public DisponibilidadeResponse consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam int pessoas) {
        if (pessoas < 1) {
            throw new RequisicaoInvalidaException("Informe o número de pessoas.");
        }
        return disponibilidadeService.consultar(data, pessoas);
    }
}
