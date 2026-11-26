package br.com.horadaboia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 100, message = "O nome deve ter até 100 caracteres.") String nome,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 150) String email,
        @NotBlank(message = "Informe o telefone.") @Size(max = 20, message = "O telefone deve ter até 20 caracteres.") String telefone,
        @NotBlank(message = "Informe a senha.") @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.") String senha) {
}
