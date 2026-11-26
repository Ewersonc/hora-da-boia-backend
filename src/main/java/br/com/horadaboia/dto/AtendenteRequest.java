package br.com.horadaboia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de um atendente. A senha é obrigatória ao criar e opcional ao editar. */
public record AtendenteRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 100) String nome,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 150) String email,
        @Size(max = 20) String telefone,
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.") String senha,
        Boolean ativo) {
}
