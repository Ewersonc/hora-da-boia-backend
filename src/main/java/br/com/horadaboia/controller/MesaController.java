package br.com.horadaboia.controller;

import java.util.List;

import br.com.horadaboia.dto.MesaRequest;
import br.com.horadaboia.dto.MesaResponse;
import br.com.horadaboia.service.MesaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mesas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    public List<MesaResponse> listar() {
        return mesaService.listar().stream().map(MesaResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MesaResponse criar(@Valid @RequestBody MesaRequest requisicao) {
        return MesaResponse.de(mesaService.criar(requisicao));
    }

    @PutMapping("/{id}")
    public MesaResponse atualizar(@PathVariable Long id, @Valid @RequestBody MesaRequest requisicao) {
        return MesaResponse.de(mesaService.atualizar(id, requisicao));
    }

    @PatchMapping("/{id}/desativacao")
    public MesaResponse desativar(@PathVariable Long id) {
        return MesaResponse.de(mesaService.desativar(id));
    }
}
