package br.com.horadaboia.controller;

import java.util.List;

import br.com.horadaboia.dto.HorarioRequest;
import br.com.horadaboia.dto.HorarioResponse;
import br.com.horadaboia.dto.ParametrosReserva;
import br.com.horadaboia.service.ConfiguracaoService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracoes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    @GetMapping("/horarios")
    public List<HorarioResponse> horarios() {
        return configuracaoService.horarios();
    }

    @PutMapping("/horarios")
    public List<HorarioResponse> atualizarHorarios(@Valid @RequestBody List<@Valid HorarioRequest> requisicao) {
        return configuracaoService.atualizarHorarios(requisicao);
    }

    @GetMapping("/reservas")
    public ParametrosReserva parametros() {
        return configuracaoService.parametros();
    }

    @PutMapping("/reservas")
    public ParametrosReserva atualizarParametros(@Valid @RequestBody ParametrosReserva requisicao) {
        return configuracaoService.atualizarParametros(requisicao);
    }
}
