package br.com.horadaboia.controller;

import br.com.horadaboia.dto.AuthResponse;
import br.com.horadaboia.dto.CadastroRequest;
import br.com.horadaboia.dto.LoginRequest;
import br.com.horadaboia.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        return authService.cadastrar(requisicao);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return authService.login(requisicao);
    }
}
