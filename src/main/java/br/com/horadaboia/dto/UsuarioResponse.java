package br.com.horadaboia.dto;

import br.com.horadaboia.model.Perfil;
import br.com.horadaboia.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String telefone, Perfil perfil, boolean ativo) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPerfil(), u.isAtivo());
    }
}
