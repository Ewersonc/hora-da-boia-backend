package br.com.horadaboia.controller;

import java.util.List;

import br.com.horadaboia.dto.AtendenteRequest;
import br.com.horadaboia.dto.UsuarioResponse;
import br.com.horadaboia.service.UsuarioService;

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
@RequestMapping("/api/atendentes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AtendenteController {

    private final UsuarioService usuarioService;

    public AtendenteController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listarAtendentes().stream().map(UsuarioResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody AtendenteRequest requisicao) {
        return UsuarioResponse.de(usuarioService.criarAtendente(requisicao));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtendenteRequest requisicao) {
        return UsuarioResponse.de(usuarioService.atualizarAtendente(id, requisicao));
    }

    @PatchMapping("/{id}/desativacao")
    public UsuarioResponse desativar(@PathVariable Long id) {
        return UsuarioResponse.de(usuarioService.desativarAtendente(id));
    }
}
