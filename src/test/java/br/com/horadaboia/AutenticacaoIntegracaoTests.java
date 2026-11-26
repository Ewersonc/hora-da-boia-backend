package br.com.horadaboia;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** US01 (cadastro) e US02 (login e perfis). */
class AutenticacaoIntegracaoTests extends TesteIntegracaoBase {

    @Test
    @DisplayName("TC-001: cadastro válido cria a conta de cliente e permite login")
    void cadastroValidoPermiteLogin() throws Exception {
        String email = "novo-cliente@teste.local";
        cadastrarCliente(email);

        String corpo = json(Map.of("email", email, "senha", "senha12345"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));
    }

    @Test
    @DisplayName("TC-002: e-mail já cadastrado retorna 409")
    void emailDuplicadoRetorna409() throws Exception {
        String email = "duplicado@teste.local";
        cadastrarCliente(email);

        String corpo = json(Map.of("nome", "Outro", "email", email, "telefone", "83999990000", "senha", "senha12345"));
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("já utilizado")));
    }

    @Test
    @DisplayName("TC-003: senha curta e telefone em branco retornam 400 com os campos inválidos")
    void cadastroInvalidoRetorna400() throws Exception {
        String corpo = json(Map.of("nome", "Fulano", "email", "fulano@teste.local", "telefone", "", "senha", "123"));
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists())
                .andExpect(jsonPath("$.campos.telefone").exists());
    }

    @Test
    @DisplayName("TC-004: senha errada retorna 401 com mensagem genérica")
    void senhaErradaRetorna401() throws Exception {
        String corpo = json(Map.of("email", EMAIL_ADMIN, "senha", "senha-errada"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("TC-005: rota protegida sem token retorna 401")
    void rotaProtegidaSemTokenRetorna401() throws Exception {
        consultar(null, "/api/reservas/minhas").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-006: cliente não acessa áreas de administrador nem da agenda")
    void clienteNaoAcessaAreasDaEquipe() throws Exception {
        String token = cadastrarCliente();
        consultar(token, "/api/mesas").andExpect(status().isForbidden());
        consultar(token, "/api/atendentes").andExpect(status().isForbidden());
        consultar(token, "/api/agenda").andExpect(status().isForbidden());
    }
}
