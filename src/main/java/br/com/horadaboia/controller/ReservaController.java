package br.com.horadaboia.controller;

import java.time.LocalDate;
import java.util.List;

import br.com.horadaboia.dto.ReservaRequest;
import br.com.horadaboia.dto.ReservaResponse;
import br.com.horadaboia.dto.StatusRequest;
import br.com.horadaboia.model.StatusReserva;
import br.com.horadaboia.security.UsuarioAutenticado;
import br.com.horadaboia.service.ReservaService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@AuthenticationPrincipal UsuarioAutenticado autor,
                                 @Valid @RequestBody ReservaRequest requisicao) {
        return reservaService.criarParaCliente(autor, requisicao);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<ReservaResponse> minhas(@AuthenticationPrincipal UsuarioAutenticado autor) {
        return reservaService.minhas(autor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ReservaResponse alterar(@AuthenticationPrincipal UsuarioAutenticado autor, @PathVariable Long id,
                                   @Valid @RequestBody ReservaRequest requisicao) {
        return reservaService.alterar(autor, id, requisicao);
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("isAuthenticated()")
    public ReservaResponse cancelar(@AuthenticationPrincipal UsuarioAutenticado autor, @PathVariable Long id) {
        return reservaService.cancelar(autor, id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public ReservaResponse atualizarStatus(@AuthenticationPrincipal UsuarioAutenticado autor, @PathVariable Long id,
                                           @Valid @RequestBody StatusRequest requisicao) {
        return reservaService.atualizarStatus(autor, id, requisicao.status());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ReservaResponse> historico(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) StatusReserva status) {
        return reservaService.historico(de, ate, status);
    }
}
