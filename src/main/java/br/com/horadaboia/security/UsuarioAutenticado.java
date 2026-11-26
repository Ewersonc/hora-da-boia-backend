package br.com.horadaboia.security;

import br.com.horadaboia.model.Perfil;

/** Usuário identificado pelo token JWT, disponível nos controllers via @AuthenticationPrincipal. */
public record UsuarioAutenticado(Long id, String email, Perfil perfil) {
}
