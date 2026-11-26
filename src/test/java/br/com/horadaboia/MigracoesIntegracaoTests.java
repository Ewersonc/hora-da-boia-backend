package br.com.horadaboia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Confirma que o contexto sobe e que as migrações Flyway rodam em um MySQL real. */
class MigracoesIntegracaoTests extends TesteIntegracaoBase {

    @Test
    void migracoesCriamConfiguracaoEHorariosIniciais() {
        Integer horarios = jdbc.queryForObject("SELECT COUNT(*) FROM horario_funcionamento", Integer.class);
        Integer configuracoes = jdbc.queryForObject("SELECT COUNT(*) FROM configuracao_reserva", Integer.class);

        assertThat(horarios).isEqualTo(7);
        assertThat(configuracoes).isEqualTo(1);
    }

    @Test
    void administradorInicialECriadoNaPrimeiraExecucao() {
        Integer administradores = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE perfil = 'ADMINISTRADOR'", Integer.class);

        assertThat(administradores).isEqualTo(1);
    }
}
