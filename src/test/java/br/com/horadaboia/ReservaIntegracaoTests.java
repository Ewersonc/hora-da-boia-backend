package br.com.horadaboia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import br.com.horadaboia.config.TempoConfig;
import br.com.horadaboia.service.ReservaService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** US07 a US15: disponibilidade, criação, alteração, cancelamento, status e não comparecimento. */
class ReservaIntegracaoTests extends TesteIntegracaoBase {

    @Autowired
    private ReservaService reservaService;

    @Test
    @DisplayName("TC-007: disponibilidade lista os horários de 30 em 30 minutos com mesa livre")
    void disponibilidadeListaHorariosComMesaLivre() throws Exception {
        criarMesa(4);
        consultar(null, "/api/disponibilidade?data=" + AMANHA + "&pessoas=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios", hasSize(7)))
                .andExpect(jsonPath("$.horarios[0]").value(containsString("18:00")));
    }

    @Test
    @DisplayName("TC-008: grupo maior que a maior mesa retorna mensagem de capacidade")
    void grupoMaiorQueAMaiorMesa() throws Exception {
        criarMesa(4);
        consultar(null, "/api/disponibilidade?data=" + AMANHA + "&pessoas=10")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("não comporta")));
    }

    @Test
    @DisplayName("TC-009: data fora da janela de reservas retorna mensagem explicando o limite")
    void dataForaDaJanela() throws Exception {
        criarMesa(4);
        consultar(null, "/api/disponibilidade?data=" + HOJE.plusDays(31) + "&pessoas=2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("janela")));
    }

    @Test
    @DisplayName("TC-010: cliente cria reserva confirmada com código único")
    void clienteCriaReservaConfirmada() throws Exception {
        criarMesa(4);
        String token = cadastrarCliente();
        criarReserva(token, AMANHA, "19:00", 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.codigo").isString())
                .andExpect(jsonPath("$.inicio").value(containsString("19:00")));
        consultar(token, "/api/reservas/minhas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("TC-011: última mesa ocupada deixa o horário indisponível para outro cliente")
    void ultimaMesaOcupadaGeraConflito() throws Exception {
        criarMesa(4);
        criarReserva(cadastrarCliente(), AMANHA, "19:00", 2).andExpect(status().isCreated());
        criarReserva(cadastrarCliente(), AMANHA, "19:00", 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("indisponível")));
    }

    @Test
    @DisplayName("TC-012: reservas que apenas encostam (fim igual ao início) são permitidas")
    void reservasQueEncostamSaoPermitidas() throws Exception {
        criarMesa(4);
        criarReserva(cadastrarCliente(), AMANHA, "18:00", 2).andExpect(status().isCreated());
        criarReserva(cadastrarCliente(), AMANHA, "20:00", 2).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-013: cliente não pode ter duas reservas confirmadas sobrepostas")
    void clienteNaoTemDuasReservasSobrepostas() throws Exception {
        criarMesa(4);
        criarMesa(4);
        String token = cadastrarCliente();
        criarReserva(token, AMANHA, "19:00", 2).andExpect(status().isCreated());
        criarReserva(token, AMANHA, "20:00", 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("sobrepõe")));
    }

    @Test
    @DisplayName("TC-014: horário fora do funcionamento é recusado")
    void foraDoHorarioDeFuncionamento() throws Exception {
        criarMesa(4);
        criarReserva(cadastrarCliente(), AMANHA, "12:00", 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("funcionamento")));
    }

    @Test
    @DisplayName("TC-015: reserva sem a antecedência mínima é recusada")
    void semAntecedenciaMinima() throws Exception {
        criarMesa(4);
        relogio.definir(ZonedDateTime.of(2026, 11, 16, 17, 30, 0, 0, TempoConfig.FUSO).toInstant());
        criarReserva(cadastrarCliente(), HOJE, "18:00", 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("antecedência")));
    }

    @Test
    @DisplayName("TC-016: cliente cancela dentro do prazo e o horário volta a ficar livre")
    void clienteCancelaDentroDoPrazo() throws Exception {
        criarMesa(4);
        String token = cadastrarCliente();
        long id = idDaReserva(criarReserva(token, AMANHA, "19:00", 2).andExpect(status().isCreated()));

        cancelar(token, id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));

        criarReserva(cadastrarCliente(), AMANHA, "19:00", 2).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-017: cliente não cancela fora do prazo, mas o atendente cancela a qualquer momento")
    void cancelamentoForaDoPrazo() throws Exception {
        criarMesa(4);
        String cliente = cadastrarCliente();
        long id = idDaReserva(criarReserva(cliente, HOJE, "18:30", 2).andExpect(status().isCreated()));

        cancelar(cliente, id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("cancelamento")));
        cancelar(criarAtendenteEEntrar(), id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    @DisplayName("TC-018: cliente não vê nem cancela a reserva de outro cliente")
    void clienteNaoMexeEmReservaAlheia() throws Exception {
        criarMesa(4);
        long id = idDaReserva(criarReserva(cadastrarCliente(), AMANHA, "19:00", 2).andExpect(status().isCreated()));

        cancelar(cadastrarCliente(), id).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-019: alteração válida mantém o código da reserva")
    void alteracaoMantemOCodigo() throws Exception {
        criarMesa(4);
        String token = cadastrarCliente();
        String resposta = criarReserva(token, AMANHA, "19:00", 2).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(resposta).get("id").asLong();
        String codigo = mapper.readTree(resposta).get("codigo").asText();

        alterarReserva(token, id, AMANHA, "20:00", 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.inicio").value(containsString("20:00")))
                .andExpect(jsonPath("$.numeroPessoas").value(3));
    }

    @Test
    @DisplayName("TC-020: alteração para horário indisponível mantém a reserva original")
    void alteracaoIndisponivelMantemOriginal() throws Exception {
        criarMesa(4);
        String cliente1 = cadastrarCliente();
        long id = idDaReserva(criarReserva(cliente1, AMANHA, "18:00", 2).andExpect(status().isCreated()));
        criarReserva(cadastrarCliente(), AMANHA, "20:00", 2).andExpect(status().isCreated());

        alterarReserva(cliente1, id, AMANHA, "20:00", 2).andExpect(status().isConflict());

        consultar(cliente1, "/api/reservas/minhas")
                .andExpect(jsonPath("$[0].inicio").value(containsString("18:00")));
    }

    @Test
    @DisplayName("TC-021: alteração fora do prazo é recusada")
    void alteracaoForaDoPrazo() throws Exception {
        criarMesa(4);
        String token = cadastrarCliente();
        long id = idDaReserva(criarReserva(token, HOJE, "18:30", 2).andExpect(status().isCreated()));

        alterarReserva(token, id, AMANHA, "19:00", 2).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("alteração")));
    }

    @Test
    @DisplayName("TC-022: atendente registra reserva de balcão e ela aparece na agenda")
    void reservaDeBalcaoApareceNaAgenda() throws Exception {
        criarMesa(4);
        String atendente = criarAtendenteEEntrar();
        criarReservaNoBalcao(atendente, AMANHA, "19:00", 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteNome").value("Cliente de Balcão"));

        consultar(atendente, "/api/agenda?data=" + AMANHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clienteTelefone").value("83988880000"));
    }

    @Test
    @DisplayName("TC-023: reserva de balcão sem nome retorna 400")
    void reservaDeBalcaoSemNome() throws Exception {
        criarMesa(4);
        String corpo = json(java.util.Map.of("nome", "", "telefone", "83988880000",
                "data", AMANHA.toString(), "hora", "19:00", "pessoas", 2));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/atendimento/reservas")
                        .header("Authorization", bearer(criarAtendenteEEntrar()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-024: atendente marca compareceu e o status final não pode ser alterado")
    void statusFinalNaoMuda() throws Exception {
        criarMesa(4);
        String atendente = criarAtendenteEEntrar();
        long id = idDaReserva(criarReservaNoBalcao(atendente, AMANHA, "19:00", 2).andExpect(status().isCreated()));

        mudarStatus(atendente, id, "COMPARECEU").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPARECEU"));
        mudarStatus(atendente, id, "NAO_COMPARECEU").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-025: não comparecido libera a mesa para novas reservas")
    void naoCompareceuLiberaAMesa() throws Exception {
        criarMesa(4);
        String atendente = criarAtendenteEEntrar();
        long id = idDaReserva(criarReservaNoBalcao(atendente, AMANHA, "19:00", 2).andExpect(status().isCreated()));

        mudarStatus(atendente, id, "NAO_COMPARECEU").andExpect(status().isOk());

        criarReserva(cadastrarCliente(), AMANHA, "19:00", 2).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-026: o agendador marca não comparecimento após a tolerância de atraso")
    void naoComparecimentoAutomatico() throws Exception {
        criarMesa(4);
        String atendente = criarAtendenteEEntrar();
        criarReservaNoBalcao(atendente, HOJE, "18:30", 2).andExpect(status().isCreated());

        relogio.definir(ZonedDateTime.of(2026, 11, 16, 18, 40, 0, 0, TempoConfig.FUSO).toInstant());
        assertThat(reservaService.marcarNaoComparecimento()).isZero();

        relogio.definir(ZonedDateTime.of(2026, 11, 16, 18, 50, 0, 0, TempoConfig.FUSO).toInstant());
        assertThat(reservaService.marcarNaoComparecimento()).isEqualTo(1);

        consultar(atendente, "/api/agenda?data=" + HOJE)
                .andExpect(jsonPath("$[0].status").value("NAO_COMPARECEU"));
    }

    @Test
    @DisplayName("TC-027: o agendador não altera reserva que já teve a chegada registrada")
    void agendadorNaoAlteraReservaQueCompareceu() throws Exception {
        criarMesa(4);
        String atendente = criarAtendenteEEntrar();
        long id = idDaReserva(criarReservaNoBalcao(atendente, HOJE, "18:30", 2).andExpect(status().isCreated()));
        mudarStatus(atendente, id, "COMPARECEU").andExpect(status().isOk());

        relogio.definir(ZonedDateTime.of(2026, 11, 16, 19, 30, 0, 0, TempoConfig.FUSO).toInstant());
        assertThat(reservaService.marcarNaoComparecimento()).isZero();
    }

    @Test
    @DisplayName("TC-028 (TEC06): reservas simultâneas disputando a última mesa, apenas uma é aceita")
    void apenasUmaReservaSimultaneaVenceAUltimaMesa() throws Exception {
        criarMesa(4);
        int tentativas = 8;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            tokens.add(cadastrarCliente());
        }

        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (String token : tokens) {
            resultados.add(pool.submit(() -> {
                largada.await();
                return criarReserva(token, AMANHA, "19:00", 2).andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown();

        int criadas = 0;
        int recusadas = 0;
        for (Future<Integer> resultado : resultados) {
            int codigoHttp = resultado.get(60, TimeUnit.SECONDS);
            if (codigoHttp == 201) {
                criadas++;
            } else if (codigoHttp == 409) {
                recusadas++;
            }
        }
        pool.shutdown();

        assertThat(criadas).isEqualTo(1);
        assertThat(recusadas).isEqualTo(tentativas - 1);
        Integer confirmadas = jdbc.queryForObject("SELECT COUNT(*) FROM reserva WHERE status = 'CONFIRMADA'", Integer.class);
        assertThat(confirmadas).isEqualTo(1);
    }
}
