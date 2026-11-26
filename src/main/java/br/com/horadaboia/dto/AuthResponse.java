package br.com.horadaboia.dto;

import br.com.horadaboia.model.Perfil;

public record AuthResponse(String token, Long usuarioId, String nome, Perfil perfil) {
}
