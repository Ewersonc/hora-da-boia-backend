package br.com.horadaboia.controller;

import br.com.horadaboia.dto.ReservaBalcaoRequest;
import br.com.horadaboia.dto.ReservaResponse;
import br.com.horadaboia.security.UsuarioAutenticado;
import br.com.horadaboia.service.ReservaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reservas registradas pela equipe para clientes sem cadastro (RF10). */
@RestController
@RequestMapping("/api/atendimento")
@PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
public class AtendimentoController {

    private final ReservaService reservaService;

    public AtendimentoController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criarNoBalcao(@AuthenticationPrincipal UsuarioAutenticado autor,
                                         @Valid @RequestBody ReservaBalcaoRequest requisicao) {
        return reservaService.criarNoBalcao(autor, requisicao);
    }
}
