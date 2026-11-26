package br.com.horadaboia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base dos testes de integração: sobe a aplicação com um MySQL real em contêiner (precisa do Docker),
 * usa um relógio controlável e devolve o banco a um estado conhecido antes de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ConfiguracaoDeTeste.class)
public abstract class TesteIntegracaoBase {

    /** Um único contêiner compartilhado por todas as classes de teste. */
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void propriedadesDoBanco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registro.add("spring.datasource.username", MYSQL::getUsername);
        registro.add("spring.datasource.password", MYSQL::getPassword);
    }

    protected static final LocalDate HOJE = LocalDate.of(2026, 11, 16);
    protected static final LocalDate AMANHA = HOJE.plusDays(1);
    protected static final String EMAIL_ADMIN = "admin@teste.local";
    protected static final String SENHA_ADMIN = "senha-admin-teste";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelogioMutavel relogio;

    @BeforeEach
    void restaurarEstadoInicial() {
        relogio.definir(ConfiguracaoDeTeste.INICIO);
        jdbc.update("DELETE FROM reserva");
        jdbc.update("UPDATE mesa SET ativa = FALSE");
        jdbc.update("UPDATE configuracao_reserva SET duracao_minutos = 120, antecedencia_min_minutos = 60, "
                + "antecedencia_max_dias = 30, prazo_cancelamento_minutos = 120, tolerancia_atraso_minutos = 15 WHERE id = 1");
        jdbc.update("UPDATE horario_funcionamento SET aberto = TRUE, abertura = '18:00:00', fechamento = '23:00:00'");
    }

    // ------------------------------------------------------------------ ajudantes

    protected String json(Object objeto) throws Exception {
        return mapper.writeValueAsString(objeto);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String cadastrarCliente() throws Exception {
        return cadastrarCliente("cliente-" + UUID.randomUUID() + "@teste.local");
    }

    protected String cadastrarCliente(String email) throws Exception {
        String corpo = json(Map.of("nome", "Cliente Teste", "email", email, "telefone", "83999990000", "senha", "senha12345"));
        String resposta = mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("token").asText();
    }

    protected String login(String email, String senha) throws Exception {
        String corpo = json(Map.of("email", email, "senha", senha));
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("token").asText();
    }

    protected String tokenAdmin() throws Exception {
        return login(EMAIL_ADMIN, SENHA_ADMIN);
    }

    protected String criarAtendenteEEntrar() throws Exception {
        String email = "atendente-" + UUID.randomUUID() + "@teste.local";
        String corpo = json(Map.of("nome", "Atendente Teste", "email", email, "senha", "senha12345"));
        mvc.perform(post("/api/atendentes").header("Authorization", bearer(tokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
        return login(email, "senha12345");
    }

    protected long criarMesa(int capacidade) throws Exception {
        String corpo = json(Map.of("identificacao", "T-" + UUID.randomUUID().toString().substring(0, 8), "capacidade", capacidade));
        String resposta = mvc.perform(post("/api/mesas").header("Authorization", bearer(tokenAdmin()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("id").asLong();
    }

    protected ResultActions criarReserva(String token, LocalDate data, String hora, int pessoas) throws Exception {
        String corpo = json(Map.of("data", data.toString(), "hora", hora, "pessoas", pessoas));
        return mvc.perform(post("/api/reservas").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    protected ResultActions alterarReserva(String token, long id, LocalDate data, String hora, int pessoas) throws Exception {
        String corpo = json(Map.of("data", data.toString(), "hora", hora, "pessoas", pessoas));
        return mvc.perform(put("/api/reservas/" + id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    protected ResultActions criarReservaNoBalcao(String token, LocalDate data, String hora, int pessoas) throws Exception {
        String corpo = json(Map.of("nome", "Cliente de Balcão", "telefone", "83988880000",
                "data", data.toString(), "hora", hora, "pessoas", pessoas));
        return mvc.perform(post("/api/atendimento/reservas").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    protected ResultActions cancelar(String token, long id) throws Exception {
        return mvc.perform(post("/api/reservas/" + id + "/cancelamento").header("Authorization", bearer(token)));
    }

    protected ResultActions mudarStatus(String token, long id, String novoStatus) throws Exception {
        return mvc.perform(patch("/api/reservas/" + id + "/status").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", novoStatus))));
    }

    protected ResultActions consultar(String token, String url) throws Exception {
        return token == null ? mvc.perform(get(url)) : mvc.perform(get(url).header("Authorization", bearer(token)));
    }

    protected long idDaReserva(ResultActions resultado) throws Exception {
        return mapper.readTree(resultado.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
}
