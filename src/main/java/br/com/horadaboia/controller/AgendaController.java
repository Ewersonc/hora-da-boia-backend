package br.com.horadaboia.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import br.com.horadaboia.dto.ReservaResponse;
import br.com.horadaboia.service.ReservaService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agenda diária da equipe (RF15). Sem data, devolve as reservas de hoje. */
@RestController
@RequestMapping("/api/agenda")
@PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
public class AgendaController {

    private final ReservaService reservaService;
    private final Clock clock;

    public AgendaController(ReservaService reservaService, Clock clock) {
        this.reservaService = reservaService;
        this.clock = clock;
    }

    @GetMapping
    public List<ReservaResponse> agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return reservaService.agenda(data != null ? data : LocalDate.now(clock));
    }
}
