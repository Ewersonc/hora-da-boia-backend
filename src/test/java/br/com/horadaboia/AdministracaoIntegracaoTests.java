package br.com.horadaboia;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** US03 a US06: atendentes, mesas, horário de funcionamento e parâmetros de reserva. */
class AdministracaoIntegracaoTests extends TesteIntegracaoBase {

    @Test
    @DisplayName("TC-029: mesa com identificação repetida retorna 409")
    void mesaDuplicada() throws Exception {
        String admin = tokenAdmin();
        String corpo = json(Map.of("identificacao", "DUP-" + UUID.randomUUID().toString().substring(0, 6), "capacidade", 4));
        mvc.perform(post("/api/mesas").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
        mvc.perform(post("/api/mesas").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-030: mesa com capacidade zero retorna 400")
    void mesaComCapacidadeInvalida() throws Exception {
        String corpo = json(Map.of("identificacao", "Mesa inválida", "capacidade", 0));
        mvc.perform(post("/api/mesas").header("Authorization", bearer(tokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-031: mesa com reserva futura confirmada não pode ser desativada")
    void mesaComReservaFuturaNaoDesativa() throws Exception {
        long mesaId = criarMesa(4);
        criarReserva(cadastrarCliente(), AMANHA, "19:00", 2).andExpect(status().isCreated());

        mvc.perform(patch("/api/mesas/" + mesaId + "/desativacao").header("Authorization", bearer(tokenAdmin())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("reservas futuras")));
    }

    @Test
    @DisplayName("TC-032: mesa desativada deixa de entrar na disponibilidade")
    void mesaDesativadaNaoEntraNaDisponibilidade() throws Exception {
        long mesaId = criarMesa(4);
        mvc.perform(patch("/api/mesas/" + mesaId + "/desativacao").header("Authorization", bearer(tokenAdmin())))
                .andExpect(status().isOk());

        consultar(null, "/api/disponibilidade?data=" + AMANHA + "&pessoas=2")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-033: dia marcado como fechado não oferece horários")
    void diaFechadoNaoOfereceHorarios() throws Exception {
        criarMesa(4);
        // 17/11/2026 é terça-feira (dia 2)
        List<Map<String, Object>> horarios = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            if (dia == 2) {
                horarios.add(Map.of("diaSemana", dia, "aberto", false));
            } else {
                horarios.add(Map.of("diaSemana", dia, "aberto", true, "abertura", "18:00", "fechamento", "23:00"));
            }
        }
        mvc.perform(put("/api/configuracoes/horarios").header("Authorization", bearer(tokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(json(horarios))).andExpect(status().isOk());

        consultar(null, "/api/disponibilidade?data=" + AMANHA + "&pessoas=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios", hasSize(0)));
    }

    @Test
    @DisplayName("TC-034: fechamento antes da abertura é recusado")
    void fechamentoAntesDaAbertura() throws Exception {
        List<Map<String, Object>> horarios = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            horarios.add(Map.of("diaSemana", dia, "aberto", true, "abertura", "20:00", "fechamento", "18:00"));
        }
        mvc.perform(put("/api/configuracoes/horarios").header("Authorization", bearer(tokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(json(horarios))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-035: novos parâmetros valem para novas consultas e valores inválidos são recusados")
    void parametrosDeReserva() throws Exception {
        criarMesa(4);
        String admin = tokenAdmin();
        String novos = json(Map.of("duracaoMinutos", 60, "antecedenciaMinMinutos", 60, "antecedenciaMaxDias", 30,
                "prazoCancelamentoMinutos", 120, "toleranciaAtrasoMinutos", 15));
        mvc.perform(put("/api/configuracoes/reservas").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(novos)).andExpect(status().isOk());

        // abre 18:00 e fecha 23:00 com duração de 60 minutos: inícios de 18:00 a 22:00, de 30 em 30 minutos
        consultar(null, "/api/disponibilidade?data=" + AMANHA + "&pessoas=2")
                .andExpect(jsonPath("$.horarios", hasSize(9)));

        String invalido = json(Map.of("duracaoMinutos", 0, "antecedenciaMinMinutos", 60, "antecedenciaMaxDias", 30,
                "prazoCancelamentoMinutos", 120, "toleranciaAtrasoMinutos", 15));
        mvc.perform(put("/api/configuracoes/reservas").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(invalido)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-036: atendente criado consegue entrar e, desativado, não consegue mais")
    void atendenteCriadoEDesativado() throws Exception {
        String admin = tokenAdmin();
        String email = "atendente-" + UUID.randomUUID() + "@teste.local";
        String corpo = json(Map.of("nome", "Atendente", "email", email, "senha", "senha12345"));
        String resposta = mvc.perform(post("/api/atendentes").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ATENDENTE"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(resposta).get("id").asLong();

        login(email, "senha12345");

        mvc.perform(patch("/api/atendentes/" + id + "/desativacao").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "senha", "senha12345")))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-037: atendente não gerencia mesas nem atendentes")
    void atendenteNaoGerenciaMesasNemAtendentes() throws Exception {
        String atendente = criarAtendenteEEntrar();
        consultar(atendente, "/api/mesas").andExpect(status().isForbidden());
        consultar(atendente, "/api/atendentes").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-038: administrador consulta o histórico por período e status")
    void historicoDoAdministrador() throws Exception {
        criarMesa(4);
        criarReserva(cadastrarCliente(), AMANHA, "19:00", 2).andExpect(status().isCreated());

        consultar(tokenAdmin(), "/api/reservas?de=" + AMANHA + "&ate=" + AMANHA + "&status=CONFIRMADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        consultar(tokenAdmin(), "/api/reservas?de=" + AMANHA + "&ate=" + AMANHA + "&status=CANCELADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
